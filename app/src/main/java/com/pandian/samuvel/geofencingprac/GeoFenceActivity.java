package com.pandian.samuvel.geofencingprac;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class GeoFenceActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, LocationListener {
    private MapFragment mMapFragment;
    private Button mAddgeofenceButton;
    private Button mClearGeofenceButton;
    private EditText mRadiusEditText;
    private GoogleMap mGoogleMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private Marker mLocationMarker;
    private Marker mGeoFenceMarker;
    private PendingIntent mGeofencePendingIntent;
    private Circle mGeofenceLimits;
    private Polygon mPolygonLimits;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private ChildEventListener mChildEventListener;
    private DataSnapshot mDataSnapshot;
    private float mGeoFenceRadius = 500.0f;
    private final static int GEOFENCE_REQ_CODE = 0;
    private final static int UPDATE_INTERVAL = 3 * 60 * 1000;
    private final static int FASTEST_INTERVAL = 30 * 1000;
    private final static String GEOFENCE_REQ_ID = "Know geofence";
    private final String KEY_GEOFENCE_LAT = "GEOFENCE LATITUDE";
    private final String KEY_GEOFENCE_LON = "GEOFENCE LONGITUDE";
    private final String KEY_GEOFENCE_RADIUS = "GEOFENCE_RADIUS";
    private final int REQ_CODE = 1;

    ArrayList<LatLng> polygonList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geo_fence);

        mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.addGeoFenceMap);
        mMapFragment.getMapAsync(this);
        mAddgeofenceButton = (Button) findViewById(R.id.addGeofence);
        mClearGeofenceButton = (Button) findViewById(R.id.clearGeofenceButton);
        mRadiusEditText = (EditText) findViewById(R.id.radiusEditText);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabaseReference = mFirebaseDatabase.getReference().child("geofence");


        createGoogleApi();
        checkGpsIsEnabled();

        mAddgeofenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mGeoFenceMarker !=null) {
                    //startGeofence();
                    drawGeoFence();
                    //drawPolygonGeoFence();

                    mGeoFenceRadius = Float.parseFloat(mRadiusEditText.getText().toString());
                    saveGeofence();
                }
                else Toast.makeText(getApplicationContext(),"Please select the geofencing location on map!",Toast.LENGTH_SHORT).show();
            }
        });
        mClearGeofenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mGeoFenceMarker!=null) {
                    mGeoFenceMarker.remove();
                    mGeoFenceMarker = null;
                    removeGeofenceDraw();
                    removeGeofence();
                }
            }
        });

    }
    private void writeGeofence(LatLng latLng,float radius){
        GeoFenceModel geoFenceModel = new GeoFenceModel(latLng.latitude,latLng.longitude,radius);
        mDatabaseReference.setValue(geoFenceModel);
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setOnMapClickListener(this);
        mGoogleMap.setOnMarkerClickListener(this);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        setmGeoFenceMarker(latLng);
        polygonList.add(latLng);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getLastKnowLocation();
        recoverGeofenceMarker();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void createGoogleApi() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    private void getLastKnowLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            startLocationUpdate();
        } else {
            startLocationUpdate();
        }
    }

    private void startLocationUpdate() {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        setMarkerLocation(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    private void setMarkerLocation(LatLng latLng) {
        String title = latLng.latitude + " " + latLng.longitude;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title);
        if (mGoogleMap != null) {
            if (mLocationMarker != null)
                mLocationMarker.remove();
            mLocationMarker = mGoogleMap.addMarker(markerOptions);
            float zoom = 14f;
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
            mGoogleMap.animateCamera(cameraUpdate);
        }
    }

    private void setmGeoFenceMarker(LatLng latLng) {
        String title = latLng.latitude + " " + latLng.longitude;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        if (mGoogleMap != null) {
            if (mGeoFenceMarker != null)
                mGeoFenceMarker.remove();
            mGeoFenceMarker = mGoogleMap.addMarker(markerOptions);
        }
    }
    private void drawPolygonGeoFence(){
        PolygonOptions polygonOptions = new PolygonOptions()
                .addAll(polygonList)
                .strokeColor(Color.argb(50,70,70,70))
                .strokeColor(Color.argb(50, 70, 70, 70))
                .fillColor(Color.argb(100, 150, 150, 150));
        mPolygonLimits = mGoogleMap.addPolygon(polygonOptions);
    }
    private void drawGeoFence(){
        if ( mGeofenceLimits != null )
            mGeofenceLimits.remove();

            CircleOptions circleOptions = new CircleOptions()
                    .center(mGeoFenceMarker.getPosition())
                    .strokeColor(Color.argb(50, 70, 70, 70))
                    .fillColor(Color.argb(100, 150, 150, 150))
                    .radius(mGeoFenceRadius);
            mGeofenceLimits = mGoogleMap.addCircle(circleOptions);


    }

   /* private Geofence createGeofence(LatLng latLng, float radius) {
        return new Geofence.Builder().setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion(latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

    }

    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }

    private void startGeofence() {
        if (mGeoFenceMarker != null) {
            Geofence geofence = createGeofence(mGeoFenceMarker.getPosition(), GEOFENCE_RADIUS);
            GeofencingRequest geofencingRequest = createGeofenceRequest(geofence);
            addGeofence(geofencingRequest);
        }
    }

    private PendingIntent createGeofencePendingIntent() {
        if (mGeofencePendingIntent != null)
            return mGeofencePendingIntent;
        else {
            Intent intent = new Intent(this, GeofenceTransitionService.class);
            return PendingIntent.getService(this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    private void addGeofence(GeofencingRequest request) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, request, createGeofencePendingIntent()).setResultCallback(this);
    }

    @Override
    public void onResult(@NonNull Status status) {
        if(status.isSuccess()){
            drawGeoFence();
            saveGeofence();
            Toast.makeText(getApplicationContext(),"Geofence added!",Toast.LENGTH_SHORT).show();
        }
    }*/

     private void saveGeofence() {
        writeGeofence(mGeoFenceMarker.getPosition(),mGeoFenceRadius);
     }


    private void recoverGeofenceMarker() {
       //attachChildListener();
        attachValueEventListener();
    }


    private void attachValueEventListener(){
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                GeoFenceModel geoFenceModel = dataSnapshot.getValue(GeoFenceModel.class);
                if(dataSnapshot.exists()) {
                    mDataSnapshot = dataSnapshot;
                    double latitude = geoFenceModel.getmLatitude();
                    double longitude = geoFenceModel.getmLongitude();
                    float radius = geoFenceModel.getmRadius();
                    mGeoFenceRadius = radius;
                    LatLng latLng = new LatLng(latitude, longitude);
                    setmGeoFenceMarker(latLng);
                    drawGeoFence();
                    //drawPolygonGeoFence();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mDatabaseReference.addValueEventListener(valueEventListener);
    }
    private void removeGeofenceDraw() {
        if ( mGeoFenceMarker != null) {
            mGeoFenceMarker.remove();
            mGeoFenceMarker =null;

        }
        if ( mGeofenceLimits != null )
            mGeofenceLimits.remove();

    }
    private void checkGpsIsEnabled(){
        LocationManager locationManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled && !network_enabled) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage(this.getResources().getString(R.string.location_not_enabled));
            dialog.setPositiveButton(getApplicationContext().getResources().getString(R.string.open_location_setting), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent intent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            dialog.setNegativeButton(getApplicationContext().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub
                }
            });
            dialog.show();
        }
    }
    private void removeGeofence(){
        if(mDataSnapshot!=null)
        mDatabaseReference.removeValue();
    }

}
