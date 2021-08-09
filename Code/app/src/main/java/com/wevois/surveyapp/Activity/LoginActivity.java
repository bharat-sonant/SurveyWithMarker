package com.wevois.surveyapp.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.R;
import com.wevois.surveyapp.SelectActivity;

public class LoginActivity extends AppCompatActivity {

    DatabaseReference databaseReferencePath;
    CommonFunctions common = new CommonFunctions();
    Handler handler;
    EditText userID;
    boolean isMoved = true;
    private final static int LOCATION_REQUEST = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        databaseReferencePath = common.getDatabaseForApplication(this);
        allowPermissions();
        userID = findViewById(R.id.user_id);
        findViewById(R.id.button_login).setOnClickListener(v -> {
                if (!(userID.getText().toString().length() > 0)) {
                    userID.setError("Please Enter UserId.");
                    userID.requestFocus();
                } else {
                    common.setProgressBar("Please wait...", this, this);
                    checkNetwork();
                }
        });
        findViewById(R.id.register_txt).setOnClickListener(v -> {
            if (isMoved) {
                isMoved = false;
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isMoved=true;
    }

    @SuppressLint("StaticFieldLeak")
    public void checkNetwork() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                return common.network(LoginActivity.this);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    loginCheck(userID.getText().toString());
                } else {
                    isMoved=true;
                    handler = new Handler();
                    handler.postDelayed(runnable, 50);
                }
            }
        }.execute();
    }

    public Runnable runnable = this::checkNetwork;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logout, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            SharedPreferences dbPathSP = getSharedPreferences("FirebasePath", MODE_PRIVATE);
            dbPathSP.edit().putString("login", "no").apply();
            startActivity(new Intent(LoginActivity.this, SelectActivity.class));
            finish();
        } catch (Exception ignored) {
        }
        return super.onOptionsItemSelected(item);
    }

    private void loginCheck(String user_id) {
        databaseReferencePath.child("Surveyors").orderByChild("pin").equalTo(user_id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        if (snapshot.child("status").getValue().equals("2")) {
                            if (snapshot.hasChild("surveyor-type")) {
                                if (snapshot.child("surveyor-type").getValue().equals("Surveyor")) {
                                    common.closeDialog();
                                    if (isMoved) {
                                        isMoved = false;
                                        SharedPreferences preferences = LoginActivity.this.getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
                                        preferences.edit().putString("mno", snapshot.child("mobile").getValue().toString()).apply();
                                        preferences.edit().putString("userId", snapshot.getKey()).apply();
                                        startActivity(new Intent(LoginActivity.this, FileDownloadActivity.class));
                                        finish();
                                    }
                                } else {
                                    common.showAlertBox("Invalid User!", false, LoginActivity.this);
                                }
                            } else {
                                common.showAlertBox("Invalid User!", false, LoginActivity.this);
                            }
                        } else if (snapshot.child("status").getValue().equals("3")) {
                            common.showAlertBox("आपकी यूजर आईडी इनएक्टिव कर दि गयी है कृपया सर्वे मैनेजर से संपर्क करे", false, LoginActivity.this);
                        } else {
                            common.showAlertBox("आप अभी एक्टिव यूजर नहीं है कृपया सर्वे मैनेजर से संपर्क करे", false, LoginActivity.this);
                        }
                    }
                } else {
                    common.showAlertBox("Invalid User!", false, LoginActivity.this);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void allowPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_SMS, Manifest.permission.CALL_PHONE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, LOCATION_REQUEST);
            return;
        }
    }
}