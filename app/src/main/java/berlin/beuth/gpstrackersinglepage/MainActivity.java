package berlin.beuth.gpstrackersinglepage;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.Console;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    public static final int DEFAULT_UPDATE_INTERVAL = 30;
    public static final int FASTEST_UPDATE_INTERVAL = 5;
    private static final int PERMISSION_FINE_LOCATION = 99;
    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address;

    Switch sw_locationsupdates, sw_gps;

    // variable ob location getrackt werden soll, oder nicht
    boolean updateOn = false;

    // Google Location Service API - installiert über dependencies in der gradle datei
    FusedLocationProviderClient fusedLocationProviderClient;

    // LocationRequest ist eine config datei für alle einstellungen in verbindung zum FusedLocationProviderClient
    LocationRequest locationRequest;

    // LocationCallBack
    LocationCallback locationCallBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // jeder variablen den richtigen UI element zuteilen
        tv_lat = (TextView) findViewById(R.id.tv_lat);
        tv_lon = (TextView) findViewById(R.id.tv_lon);
        tv_altitude = (TextView) findViewById(R.id.tv_altitude);
        tv_accuracy = (TextView) findViewById(R.id.tv_accuracy);
        tv_speed = (TextView) findViewById(R.id.tv_speed);
        tv_sensor = (TextView) findViewById(R.id.tv_sensor);
        tv_updates = (TextView) findViewById(R.id.tv_updates);
        tv_address = (TextView) findViewById(R.id.tv_address);
        sw_locationsupdates = (Switch) findViewById(R.id.sw_locationsupdates);
        sw_gps = (Switch) findViewById(R.id.sw_gps);

        Boolean switchState = sw_gps.isChecked();

        // eigenschaften für LocationRequest
        locationRequest = new LocationRequest();

        // wie oft defaultmäßig die location-überprüfung durchgeführt wird(ms)
        locationRequest.setInterval(1000 * DEFAULT_UPDATE_INTERVAL);

        // wie oft die location-überprüfung durchgeführt wird, bei der schnellsten verbindung
        locationRequest.setFastestInterval(1000 * FASTEST_UPDATE_INTERVAL);

        // Batterie-Nutzung anhand der prio
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


        locationCallBack = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                updateUIValues(locationResult.getLastLocation());
            }
        };

        // ActionBar deaktivieren
        getSupportActionBar().hide();

        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_gps.isChecked()) {
                    // am genausten - benutzt das GPS
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_sensor.setText("Using GPS sensors");
                    ;
                } else {
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_sensor.setText("Using Towers + WiFi");
                    ;
                }
            }
        });

        sw_locationsupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_locationsupdates.isChecked()) {
                    startLocationUpdates();
                } else {
                    stopLocationUpdates();
                }
            }
        });

        updateGPS();
    } // ende der onCreate methode

    private void stopLocationUpdates() {
        tv_updates.setText("Location is Not being tracked");
        tv_lon.setText("Not tracking location");
        tv_lat.setText("Not tracking location");
        tv_speed.setText("Not tracking location");
        tv_accuracy.setText("Not tracking location");
        tv_altitude.setText("Not tracking location");
        tv_address.setText("Not tracking location");

        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);


    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "This app requires permission to be granted in order to work properly", Toast.LENGTH_LONG).show();
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        tv_updates.setText("Location is being tracked");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_FINE_LOCATION:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    updateGPS();
                }
                else {
                    Toast.makeText(this, "This app requires permission to be granted in order to work properly", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
    }

    private void updateGPS(){
        // permission vom user erhalten
        // die letzte position bekommen von fusedClient
        // UI updaten

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);


        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            // permission erlaubt
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {

                    // =! null um nullPointer zu vermeiden
                    if (location  != null)
                    {
                        // haben permission, ausgabe der werte der location und update der werte in der UI
                        updateUIValues(location);
                    }
                }
            });
        }
        else{
            // permission nicht erlaubt
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                requestPermissions( new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
            }

        }
    }

    private void updateUIValues(Location location) {
        // update alle Textviews mit den werten des Location-Objects
        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));
        tv_accuracy.setText(String.valueOf(location.getAccuracy()));

        if(location.hasAltitude()){
            tv_altitude.setText(String.valueOf(location.getAltitude()));

        }
        else{
            tv_altitude.setText("Not avaible");
        }

        if(location.hasSpeed()){
            tv_speed.setText(String.valueOf(location.getSpeed()));
        }
        else{
            tv_speed.setText("Not avaible");
        }

        Geocoder geocoder = new Geocoder(MainActivity.this);

        try{
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(), 1);
            tv_address.setText(addresses.get(0).getAddressLine(0));

        }
        catch (Exception e){
            tv_address.setText("Unable to get address");
        }


    }

}


