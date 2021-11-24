package com.example.alpha;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.util.List;


public class LocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    MapView mapView;
    GoogleMap gmap;
    Bundle mapViewBundle;

    TextView latitudeTV, longitudeTV, addressTV;

    double latitude;
    double longitude;


    FusedLocationProviderClient fusedLocationProviderClient;
    CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        mapView = findViewById(R.id.map_view);
        latitudeTV = findViewById(R.id.latitudeTV);
        longitudeTV = findViewById(R.id.longitudeTV);
        addressTV = findViewById(R.id.addressTV);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED); //Disable Screen Rotation

        mapViewBundle = null;

        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);

        if (ActivityCompat.checkSelfPermission(LocationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            turnGPSOn();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);
    }

    private void turnGPSOn() {
        try
        {
            String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

            if(!provider.contains("gps")){ //if gps is disabled
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
                intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
                intent.setData(Uri.parse("3"));
                sendBroadcast(intent);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void turnGPSOff(){
        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        if(provider.contains("gps")){ //if gps is enabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            sendBroadcast(poke);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void Logout() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("Are you sure?");
        SharedPreferences settings = getSharedPreferences("Status",MODE_PRIVATE);
        Variable.setEmailVer(settings.getString("email",""));
        adb.setMessage(Variable.getEmailVer().substring(0,Variable.emailVer.indexOf("@"))+" will logged out");

        adb.setNegativeButton("DISMISS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                FirebaseAuth.getInstance().signOut();
                Intent si = new Intent(LocationActivity.this, LoginActivity.class);

                SharedPreferences settings = getSharedPreferences("Status",MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("email", "");
                editor.putBoolean("stayConnect",false);
                editor.apply();

                startActivity(si);
                finish();
            }
        });

        AlertDialog ad = adb.create();
        ad.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        Intent si;
        if (id == R.id.logOut){
            Logout();
        }
        else if (id == R.id.pdf){
            si = new Intent(this, pdfCreatorActivity.class);
            startActivity(si);
        }
        else if (id == R.id.record){
            si = new Intent(this, RecordActivity.class);
            startActivity(si);
        }
        else if (id == R.id.calander){
            si = new Intent(this, MainActivity.class);
            startActivity(si);
        }


        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        turnGPSOn();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }
    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
        turnGPSOff();
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        turnGPSOn();

        gmap = googleMap;
        gmap.setMaxZoomPreference(21);
        gmap.setMinZoomPreference(0);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        }

        gmap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public boolean onMyLocationButtonClick() {
                if (ActivityCompat.checkSelfPermission(LocationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    turnGPSOn();

                    try {
                        gmap.clear();
                        Geocoder geocoder = new Geocoder(LocationActivity.this);

                        Location location = gmap.getMyLocation();
                        if(location != null){
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();

                            List<Address> addresses = geocoder.getFromLocation(latitude,longitude,1);

                            LatLng cl = new LatLng(latitude, longitude);

                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(cl);
                            gmap.addMarker(markerOptions);

                            float zoomLevel = 17.0f; //This goes up to 21
                            gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(cl, zoomLevel));

                            latitudeTV.setText("Latitude: "+latitude);
                            longitudeTV.setText("Longitude: "+longitude);
                            addressTV.setText("Address: "+ addresses.get(0).getAddressLine(0));
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(LocationActivity.this, "Error!", Toast.LENGTH_SHORT).show();
                    }

                    turnGPSOff();
                } else {
                    ActivityCompat.requestPermissions(LocationActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
                }
                return false;
            }
        });
        gmap.setIndoorEnabled(true);

        UiSettings uiSettings = gmap.getUiSettings();
        uiSettings.setIndoorLevelPickerEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomControlsEnabled(true);

        LatLng isr = new LatLng(31.014582403708065, 34.815507205709196);
        gmap.moveCamera(CameraUpdateFactory.newLatLng(isr));

        float zoomLevel = 7.0f; //This goes up to 21
        gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(isr, zoomLevel));

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @SuppressLint("MissingPermission")
    public void getAddress(View view) {
        EditText locationSearch = (EditText) findViewById(R.id.editText);
        String address = locationSearch.getText().toString();

        if (ActivityCompat.checkSelfPermission(LocationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(LocationActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                Location location = task.getResult();
                if (location != null) {
                    Geocoder geocoder = new Geocoder(LocationActivity.this);
                    try {
                        List<Address> addressList = geocoder.getFromLocationName(address, 6);
                        Address user_address = addressList.get(0);

                        LatLng latLng = new LatLng(user_address.getLatitude(), user_address.getLongitude());

                        gmap.clear();
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(latLng);
                        gmap.addMarker(markerOptions);

                        float zoomLevel = 17.0f; //This goes up to 21
                        gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));

                        latitudeTV.setText("Latitude: " + user_address.getLatitude());
                        longitudeTV.setText("Longitude: " + user_address.getLongitude());
                        addressTV.setText("Address: " + user_address.getAddressLine(0));
                        locationSearch.setText("");

                    } catch (Exception e) {
                        Toast.makeText(LocationActivity.this, "Error Address!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
