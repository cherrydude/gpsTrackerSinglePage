package berlin.beuth.gpstrackersinglepage;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.List;


public class MainActivity extends AppCompatActivity {

    public static final int DEFAULT_UPDATE_INTERVAL = 30;
    public static final int FASTEST_UPDATE_INTERVAL = 5;
    private static final int PERMISSION_FINE_LOCATION = 99;
    private TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address, tv_mbar, tv_altitudeFromMbar, tv_calibratingMbar;
    private SensorManager sensoreManager;
    private Sensor pressureSensor;


    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] values = event.values;
            double mbar = values[0];

            double resultAltituteFromMBar;

            double calibratingMbar = 0;

            calibratingMbar = Double.valueOf(String.valueOf(tv_calibratingMbar.getText()));

            Log.d("test","aH: " + tv_calibratingMbar.getText());

            // Formel aus den Vorlesungsmaterial
            resultAltituteFromMBar = (288.15/0.0065)*( 1-(Math.pow( ((mbar+calibratingMbar) / 1013.25), 1/5.255 )));

            tv_mbar.setText(String.valueOf(values[0]));
            tv_altitudeFromMbar.setText(String.valueOf(resultAltituteFromMBar));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    Switch sw_locationsupdates, sw_gps;

    // Google Location Service API - installiert über dependencies in der gradle datei
    FusedLocationProviderClient fusedLocationProviderClient;

    // LocationRequest ist eine config datei für alle einstellungen in verbindung zum FusedLocationProviderClient
    LocationRequest locationRequest;

    // LocationCallBack
    LocationCallback locationCallBack;

    // LocationManager
    LocationManager locationManager;

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
        tv_mbar = (TextView)findViewById(R.id.txtmBar);
        tv_calibratingMbar = (TextView) findViewById(R.id.txtActualHeight);
        tv_altitudeFromMbar = (TextView)findViewById(R.id.txtAltFromMbar);
        sw_locationsupdates = (Switch) findViewById(R.id.sw_locationsupdates);
        sw_gps = (Switch) findViewById(R.id.sw_gps);


        // eigenschaften für LocationRequest
        locationRequest = new LocationRequest();

        // wie oft defaultmäßig die location-überprüfung durchgeführt wird(ms)
        locationRequest.setInterval(1000 * DEFAULT_UPDATE_INTERVAL);

        // wie oft die location-überprüfung durchgeführt wird, bei der schnellsten verbindung
        locationRequest.setFastestInterval(1000 * FASTEST_UPDATE_INTERVAL);

        // Batterie-Nutzung anhand der prio
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        // LocationCallBack
        locationCallBack = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                updateUIValues(locationResult.getLastLocation());
            }
        };

        // SensorManager
        sensoreManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // PressureSensor
        pressureSensor = sensoreManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        // LocationListener
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

//        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, (LocationListener) this);
//
//        // Acquire a reference to the system Location Manager
//        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
//
//        // Define a listener that responds to location updates
//        LocationListener locationListener = new LocationListener() {
//            public void onLocationChanged(Location location) {
//                // Called when a new location is found by the network location provider.
//                makeUseOfNewLocation(location);
//            }
//            public void onStatusChanged(String provider, int status, Bundle extras) {}
//            public void onProviderEnabled(String provider) {}
//            public void onProviderDisabled(String provider) {}
//        };
//
//        // Register the listener with the Location Manager to receive location updates
//        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

        // ActionBar deaktivieren
        getSupportActionBar().hide();

        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_gps.isChecked()) {
                    // am genausten - benutzt das GPS - höchster verbrauch
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_sensor.setText("Using GPS sensors");

                } else {
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_sensor.setText("Using Towers + WiFi");

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

    protected void onResume() {
        super.onResume();
        sensoreManager.registerListener(sensorEventListener, pressureSensor, SensorManager.SENSOR_DELAY_UI);
        Log.d("MainActivity","@Resume");
        Toast.makeText(this, "The app is now resuming", Toast.LENGTH_LONG).show();


    }

    protected void onPause() {
        super.onPause();
        sensoreManager.unregisterListener(sensorEventListener);
        Log.d("MainActivity","@Pause");
        Toast.makeText(this, "The app is on pause", Toast.LENGTH_LONG).show();

    }

    protected void onStop() {
        super.onStop();
        Log.d("MainActivity","@Stop");
        Toast.makeText(this, "The app was stopped", Toast.LENGTH_LONG).show();

    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d("MainActivity","@Destroy");
        Toast.makeText(this, "The app was destroyed", Toast.LENGTH_LONG).show();

    }

    private void makeUseOfNewLocation(Location location) {
    }


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
        tv_accuracy.setText(String.valueOf(location.getAccuracy()+ " m"));

        if(location.hasAltitude()){
            tv_altitude.setText(String.valueOf(location.getAltitude()));

        }
        else{
            tv_altitude.setText("Not avaible");
        }

        if(location.hasSpeed()){
            tv_speed.setText(String.valueOf(location.getSpeed()+ " km/h"));
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


