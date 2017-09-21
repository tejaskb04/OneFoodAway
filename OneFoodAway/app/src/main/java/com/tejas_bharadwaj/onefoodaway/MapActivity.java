package com.tejas_bharadwaj.onefoodaway;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private final String GOOGLE_PLACES_API_KEY = "AIzaSyAH008n41rXGsO2oYtJgZduebNYwN127_I";
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100;
    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private HashMap<String, String> placeTitleId = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapActivity.this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMapToolbarEnabled(false);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        else {
            map.setMyLocationEnabled(true);
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(MapActivity.this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            CameraPosition cameraPosition = new CameraPosition.Builder()
                                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                    .zoom(12)
                                    .bearing(270)
                                    .tilt(20)
                                    .build();
                            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                            String placeSearchUrl = createPlaceSearchUrl(location.getLatitude(), location.getLongitude());
                            new PlaceSearchCallBackTask().execute(placeSearchUrl);
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        map.setMyLocationEnabled(true);
                        fusedLocationProviderClient.getLastLocation()
                                .addOnSuccessListener(MapActivity.this, new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {
                                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                                .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                                .zoom(12)
                                                .bearing(270)
                                                .tilt(20)
                                                .build();
                                        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                                        String placeSearchUrl = createPlaceSearchUrl(location.getLatitude(), location.getLongitude());
                                        new PlaceSearchCallBackTask().execute(placeSearchUrl);
                                    }
                                });
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }

                } else {
                    // STUB
                }
                return;
            }
        }
    }

    private String createPlaceSearchUrl(double lat, double lng) {
        StringBuilder stringBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        stringBuilder.append("location=").append(lat).append(",").append(lng);
        stringBuilder.append("&radius=").append(5000);
        stringBuilder.append("&types=").append("cafe|meal_delivery|meal_takeaway|restaurant");
        stringBuilder.append("&sensor=true");
        stringBuilder.append("&key=").append(GOOGLE_PLACES_API_KEY);
        return stringBuilder.toString();
    }

    private String createPlaceDetailsUrl(String placeId) {
        StringBuilder stringBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/place/details/json?");
        stringBuilder.append("placeid=").append(placeId);
        stringBuilder.append("&key=").append(GOOGLE_PLACES_API_KEY);
        return stringBuilder.toString();
    }

    private class PlaceSearchCallBackTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsURLConnection
                        .getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
                return stringBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
                // Show Error Message
                return e.toString();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            String name = null;
            String snippet = null;
            String placeId;
            double lat, lng;
            try {
                JSONObject data = new JSONObject(result);
                if (data.getString("status").equalsIgnoreCase("OK")) {
                    JSONArray jsonArray = data.getJSONArray("results");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        final JSONObject place = jsonArray.getJSONObject(i);
                        if (!place.isNull("name")) {
                            name = place.getString("name");
                        }
                        if (!place.isNull("rating") && !place.isNull("price_level")) {
                            snippet = "Rating: " + place.getString("rating") + " " + "Price Level: "
                                    + place.getString("price_level");
                        }
                        if (!place.isNull("place_id")) {
                            placeId = place.getString("place_id");
                            placeTitleId.put(name, placeId);
                        }
                        lat = place.getJSONObject("geometry").getJSONObject("location")
                                .getDouble("lat");
                        lng = place.getJSONObject("geometry").getJSONObject("location")
                                .getDouble("lng");
                        map.addMarker(new MarkerOptions()
                                .position(new LatLng(lat, lng))
                                .title(name)
                                .snippet(snippet));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                // Show Error Message
            }
            map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    String name = "NA";
                    String snippet = "NA";
                    if (marker.getTitle() != null) {
                        name = marker.getTitle();
                    }
                    if (marker.getSnippet() != null) {
                        snippet = marker.getSnippet();
                    }
                    String placeDetailsUrl = createPlaceDetailsUrl(placeTitleId.get(marker.getTitle()));
                    new PlaceDetailsCallBackTask().execute(placeDetailsUrl);
                    Intent intent = new Intent(MapActivity.this, MarkerActivity.class);
                    intent.putExtra("title", name);
                    intent.putExtra("snippet", snippet);
                    startActivity(intent);
                    return false;
                }
            });
        }
    }

    private class PlaceDetailsCallBackTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsURLConnection
                        .getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
                return stringBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
                // Show Error Message
                return e.toString();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            String phoneNumber = "NA";
            String photoStrings = "";
            try {
                JSONObject jsonObject = new JSONObject(result);
                if (jsonObject.getString("status").equalsIgnoreCase("OK")) {
                    JSONObject data = jsonObject.getJSONObject("result");
                    if (!data.isNull("formatted_phone_number")) {
                        phoneNumber = data.getString("formatted_phone_number");
                    }
                    JSONArray photoArray = data.getJSONArray("photos");
                    for (int i = 0; i < photoArray.length(); i++) {
                        JSONObject pictureObject = photoArray.getJSONObject(i);
                        if (!pictureObject.isNull("photo_reference")) {
                            photoStrings += pictureObject.getString("photo_reference");
                            photoStrings += " ";
                        }
                    }
                }
                photoStrings = photoStrings.trim();
                Intent intent = new Intent("photoIntent");
                intent.putExtra("phoneNumber", phoneNumber);
                intent.putExtra("photoStrings", photoStrings);
                LocalBroadcastManager.getInstance(MapActivity.this).sendBroadcast(intent);
            } catch (JSONException e) {
                e.printStackTrace();
                // Show Error Message
            }
        }
    }
}