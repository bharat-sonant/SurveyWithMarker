package com.wevois.surveyapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.wevois.surveyapp.Activity.LoginActivity;

public class HomeActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    CommonFunctions common = new CommonFunctions();
    DatabaseReference databaseReferencePath;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mAuth = FirebaseAuth.getInstance();
        databaseReferencePath = common.getDatabaseForApplication(this);
        common.setProgressBar("Please wait...",HomeActivity.this,HomeActivity.this);
        checkVersion();
    }

    @SuppressLint("StaticFieldLeak")
    private void checkVersion() {
        new AsyncTask<Void, Void, Boolean>()
        {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... p)
            {
                return common.network(HomeActivity.this);
            }

            @Override
            protected void onPostExecute(Boolean result)
            {
                if (result) {
                    databaseReferencePath.child("Settings/LatestVersions/survey").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            common.closeDialog();
                            try {
                                String localVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                                if (dataSnapshot.getValue() != null) {
                                    String version = dataSnapshot.getValue().toString();
                                    if (!version.equals(localVersion)) {
                                        showVersionAlertBox();
                                    }else {
                                        startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                                        finish();
                                    }
                                }else {
                                    showVersionAlertBox();
                                }
                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }else {
                    handler = new Handler();
                    handler.postDelayed(runnable, 50);
                }
            }
        }.execute();
    }

    private void showVersionAlertBox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
        builder.setCancelable(false);
        builder.setTitle("Version Expired");
        builder.setMessage("Your App version is not Matched. Please update your app.");
        builder.setPositiveButton("Ok", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                }
        );

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public Runnable runnable = this::checkVersion;

    @Override
    protected void onStart() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
        } else {
            signInAnonymously();
        }
        super.onStart();
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously().addOnSuccessListener(this, authResult -> {
        })
                .addOnFailureListener(this, exception -> {
                });
    }
}