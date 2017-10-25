package com.pandian.samuvel.geofencingprac;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private Button geofencebutton;
    private Button usermapButton;
    private static String NOTIFICATION_MSG = "NOTIFICATION_MSG";

    public static Intent makeNotificationIntent(Context context, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(NOTIFICATION_MSG, message);
        return intent;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        geofencebutton = findViewById(R.id.addGeofenceButton);
        usermapButton =  findViewById(R.id.userMapButton);

        geofencebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,GeoFenceActivity.class);
                startActivity(intent);
            }
        });
        usermapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,UserMapActivity.class);
                startActivity(intent);
            }
        });
    }
}
