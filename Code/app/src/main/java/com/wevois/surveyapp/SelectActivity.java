package com.wevois.surveyapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SelectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);
        findViewById(R.id.sikar).setOnClickListener(view -> {
            setDatabasePath("sikar");
        });
        findViewById(R.id.reengus).setOnClickListener(view -> {
            setDatabasePath("reengus");
        });
        findViewById(R.id.jaipur).setOnClickListener(view -> setDatabasePath("jaipur"));
        TextView versionName = findViewById(R.id.versionName);
        versionName.setOnLongClickListener(v -> {
            setDatabasePath("test");
            return true;
        });
    }

    private void setDatabasePath(String city) {
        SelectActivity.this.runOnUiThread(() -> {
            String dbPath, storagePath;
            SharedPreferences dbPathSP = getSharedPreferences("FirebasePath", MODE_PRIVATE);
            if (city.equals("test")) {
                dbPath = "https://dtdnavigatortesting.firebaseio.com/";
                storagePath = "Test";
            } else if (city.equals("reengus")) {
                dbPath = "https://dtdreengus.firebaseio.com/";
                storagePath = "Reengus";
            }else if (city.equals("jaipur")) {
                dbPath = "https://dtdjaipur.firebaseio.com/";
                storagePath = "Jaipur";
            } else {
                dbPath = "https://dtdnavigator.firebaseio.com/";
                storagePath = "Sikar";
            }

            dbPathSP.edit().putString("dbPath", dbPath).apply();
            dbPathSP.edit().putString("storagePath", storagePath).apply();
            dbPathSP.edit().putString("login", "yes").apply();

            startActivity(new Intent(SelectActivity.this, HomeActivity.class));
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        SelectActivity.this.runOnUiThread(() -> {
            SharedPreferences dbPathSP = getSharedPreferences("FirebasePath", MODE_PRIVATE);
            if (dbPathSP.getString("login", "").equals("yes")) {
                startActivity(new Intent(SelectActivity.this, HomeActivity.class));
                finish();
            }
        });
    }
}
