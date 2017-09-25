package com.tejas_bharadwaj.onefoodaway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkerActivity extends AppCompatActivity {

    private final String GOOGLE_PLACES_API_KEY = "AIzaSyAH008n41rXGsO2oYtJgZduebNYwN127_I";
    private TextView textViewTitle;
    private TextView textViewRating;
    private RatingBar ratingBar;
    private TextView textViewPriceLevel;
    private TextView textViewPhoneNumber;
    private LinearLayout photoLayout;
    private LinearLayout priceLevelLayout;
    private int width = 0;
    private int height = 0;
    private String phoneNumber = null;
    private String photoStrings = "";
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            phoneNumber = intent.getStringExtra("phoneNumber");
            if (!phoneNumber.equals(null) || !phoneNumber.equals("NA")) {
                textViewPhoneNumber.setText("Phone Number: " + phoneNumber);
            }
            photoStrings = intent.getStringExtra("photoStrings");
            ArrayList<String> parsedPhotoStrings = parsePhotoStrings(photoStrings);
            for (int i = 0; i < parsedPhotoStrings.size(); i++) {
                String photoUrl = createPlacePhotosUrl(parsedPhotoStrings.get(i));
                ImageView imageView = new ImageView(MarkerActivity.this);
                photoLayout.addView(imageView);
                Picasso.with(MarkerActivity.this).load(photoUrl)
                        .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE)
                        .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                        .resize(width, 0)
                        .into(imageView);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker);
        textViewTitle = (TextView) findViewById(R.id.title);
        textViewRating = (TextView) findViewById(R.id.rating);
        ratingBar = (RatingBar) findViewById(R.id.rating_bar);
        textViewPriceLevel = (TextView) findViewById(R.id.price_level);
        textViewPhoneNumber = (TextView) findViewById(R.id.phone_number);
        photoLayout = (LinearLayout) findViewById(R.id.photoLayout);
        photoLayout.setDrawingCacheEnabled(false);
        priceLevelLayout = (LinearLayout) findViewById(R.id.price_level_layout);
        String title = getIntent().getStringExtra("title");
        String snippet = getIntent().getStringExtra("snippet");
        LocalBroadcastManager.getInstance(MarkerActivity.this).registerReceiver(receiver,
                new IntentFilter("photoIntent"));
        if (!title.equals("NA")) {
            textViewTitle.setText(title);
        } else {
            textViewTitle.setText("NA");
        }
        if (!snippet.equals("NA")) {
            String[] vals = parseSnippet(snippet);
            double[] dVals = getNums(vals);
            textViewRating.setText(vals[0]);
            ratingBar.setNumStars(5);
            ratingBar.setRating((float) dVals[0]);
            textViewPriceLevel.setText(vals[1]);
            for (int i = 0; i < (int) dVals[1]; i++) {
                ImageView moneyImage = new ImageView(MarkerActivity.this);
                Drawable moneyDrawable = ContextCompat.getDrawable(MarkerActivity.this, R.drawable.ic_attach_money_black_48dp);
                moneyImage.setImageDrawable(moneyDrawable);
                priceLevelLayout.addView(moneyImage);
            }
        } else {
            ratingBar.setNumStars(5);
            ratingBar.setRating(0);
            textViewRating.setText("NA");
            textViewPriceLevel.setText("NA");
        }
    }

    private String[] parseSnippet(String snippet) {
        String[] vals = new String[2];
        vals[0] = snippet.substring(0, snippet.indexOf("P") - 1);
        vals[1] = snippet.substring(snippet.indexOf("P"), snippet.length());
        return vals;
    }

    private double[] getNums(String[] vals) {
        double[] dVals = new double[2];
        for (int i = 0; i < vals.length; i++) {
            Pattern regex = Pattern.compile("(\\d+(?:\\.\\d+)?)");
            Matcher matcher = regex.matcher(vals[i]);
            if (matcher.find()){
                try {
                    dVals[i] = Double.parseDouble(matcher.group(1));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return dVals;
    }

    /*private Drawable resizeDrawable(Drawable image) {
        Bitmap b = ((BitmapDrawable) image).getBitmap();
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, 50, 50, false);
        return new BitmapDrawable(getResources(), bitmapResized);
    }*/

    private String createPlacePhotosUrl(String photoReference) {
        StringBuilder stringBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/place/photo?");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;
        stringBuilder.append("maxwidth=").append(width);
        stringBuilder.append("&photoreference=").append(photoReference);
        stringBuilder.append("&key=").append(GOOGLE_PLACES_API_KEY);
        return stringBuilder.toString();
    }

    private ArrayList<String> parsePhotoStrings(String photoStrings) {
        ArrayList<String> parsedPhotoStrings = new ArrayList<>();
        String temp = "";
        for (int i = 0; i < photoStrings.length(); i++) {
            if (photoStrings.charAt(i) != ' ') {
                temp += photoStrings.charAt(i);
            } else {
                parsedPhotoStrings.add(temp);
                temp = "";
            }
        }
        parsedPhotoStrings.add(temp);
        return parsedPhotoStrings;
    }

    /*private class PlacePhotosCallBackTask extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;
        public PlacePhotosCallBackTask(ImageView imageView) {
            this.imageView = imageView;
        }
        @Override
        protected Bitmap doInBackground(String... params) {
            String url = params[0];
            Bitmap bitmap = null;
            try {
                InputStream inputStream = new java.net.URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                // Show Error Message
            } catch (IOException e) {
                e.printStackTrace();
                // Show Error Message
            }
            return bitmap;
        }
        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            imageView.setImageBitmap(result);
        }
    }*/

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            for (int i = 0; i < photoLayout.getChildCount(); i++) {
                View v = photoLayout.getChildAt(i);
                if (v instanceof ImageView) {
                    v = null;
                    photoLayout.removeViewAt(i);
                }
            }
            photoLayout.removeAllViews();
            deleteCache(MarkerActivity.this);
        }
        return super.onKeyDown(keyCode, event);
    }

    private void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {}
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    // DEBUG METHOD BELOW
    private void printStringArrayList(ArrayList<String> arrayList) {
        for (int i = 0; i < arrayList.size(); i++) {
            System.out.println(arrayList.get(i));
        }
    }
}