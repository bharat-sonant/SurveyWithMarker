package com.wevois.surveyapp.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.R;
import com.wevois.surveyapp.SelectActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class FileDownloadActivity extends AppCompatActivity {
    Handler handler;
    StorageReference storageReference;
    FirebaseStorage storage;
    String str, storagePath;
    SharedPreferences preferences;
    DatabaseReference databaseReferencePath;
    CommonFunctions common = new CommonFunctions();
    boolean checkStatus = true, isMoved = true;
    LinearLayout startSurvey;

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_download);

        preferences = getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
        databaseReferencePath = common.getDatabaseForApplication(this);
        storagePath = common.getDatabaseStorage(this);
        startSurvey = findViewById(R.id.startSurvey);
        common.setProgressBar("Please wait...", this, this);
        getCardScanData();
        common.getKml(this);
        getHouseTypes();
        getCardRevisitReasons();
        getTimeDisForCurrentLoc();
        checkNetwork();
        findViewById(R.id.HouseSurvey).setOnClickListener(v -> {
            if (isMoved) {
                isMoved = false;
                startActivity(new Intent(FileDownloadActivity.this, MapsActivity.class));
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isMoved = true;
    }

    private void getTimeDisForCurrentLoc() {
        FileDownloadActivity.this.runOnUiThread(() -> {
            databaseReferencePath.child("Settings").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild("Survey")) {
                        if (dataSnapshot.child("Survey").hasChild("cardScanningTime")) {
                            int minCurrentTime = Integer.parseInt(dataSnapshot.child("Survey/cardScanningTime").getValue().toString()) * 1000;
                            preferences.edit().putInt("cardScanningTime", minCurrentTime).apply();
                        }if (dataSnapshot.child("Survey").hasChild("minimumDistanceBetweenMarkerAndSurvey")) {
                            int minCurrentTime = Integer.parseInt(dataSnapshot.child("Survey/minimumDistanceBetweenMarkerAndSurvey").getValue().toString());
                            preferences.edit().putInt("minimumDistanceBetweenMarkerAndSurvey", minCurrentTime).apply();
                        }if (dataSnapshot.child("Survey").hasChild("messageMinimumDistanceMarkerAndSurvey")) {
                            preferences.edit().putString("messageMinimumDistanceMarkerAndSurvey", dataSnapshot.child("Survey/messageMinimumDistanceMarkerAndSurvey").getValue().toString()).apply();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        });
    }

    @SuppressLint("StaticFieldLeak")
    private void getCardScanData() {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                FileDownloadActivity.this.runOnUiThread(() -> {
                    databaseReferencePath.child("CardScanData").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                JSONObject jsonKmlBoundary = new JSONObject();
                                JSONObject jsonCard = new JSONObject();
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    if (snapshot.hasChild("serialNo")) {
                                        if (snapshot.child("serialNo").getValue() != null) {
                                            if (snapshot.hasChild("cardVerified")) {
                                                try {
                                                    JSONArray jsonArray = new JSONArray();
                                                    jsonArray.put(snapshot.child("cardVerified").getValue().toString());
                                                    jsonArray.put(snapshot.child("serialNo").getValue().toString());
                                                    jsonArray.put(snapshot.child("phaseNo").getValue().toString());
                                                    jsonKmlBoundary.put(snapshot.getKey(), jsonArray);
                                                    jsonCard.put(snapshot.child("serialNo").getValue().toString(), snapshot.getKey());
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    }
                                }
                                preferences.edit().putString("CardScanData", jsonKmlBoundary.toString()).commit();
                                preferences.edit().putString("SerialNoData", jsonCard.toString()).commit();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                });
                return null;
            }
        }.execute();
    }

    private void getHouseTypes() {
        FileDownloadActivity.this.runOnUiThread(() -> {
            databaseReferencePath.child("Defaults").child("FinalHousesType").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    JSONObject jsonHousesType = new JSONObject();
                    JSONObject jsonCommercialHousesType = new JSONObject();
                    JSONObject jsonResidentialHousesType = new JSONObject();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            jsonHousesType.put(snapshot.getKey(), snapshot.child("name").getValue().toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (snapshot.hasChild("entity-type")) {
                            if (snapshot.child("entity-type").getValue().toString().equals("commercial")) {
                                try {
                                    jsonCommercialHousesType.put(snapshot.getKey(), snapshot.child("name").getValue().toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                try {
                                    jsonResidentialHousesType.put(snapshot.getKey(), snapshot.child("name").getValue().toString());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    preferences.edit().putString("housesTypeList", jsonHousesType.toString()).apply();
                    preferences.edit().putString("commercialHousesTypeList", jsonCommercialHousesType.toString()).apply();
                    preferences.edit().putString("residentialHousesTypeList", jsonResidentialHousesType.toString()).apply();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        });
    }

    private void getHouseWardMapping(String ward, String lines) {
        FileDownloadActivity.this.runOnUiThread(() -> {
            ArrayList<String> linesArray = new ArrayList<>();
            String[] linesData = lines.split(",");
            for (int i = 0; i < linesData.length; i++) {
                linesArray.add(linesData[i].trim());
            }
            databaseReferencePath.child("EntityMarkingData/MarkedHouses/" + ward).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.getValue() != null) {
                        JSONObject jsonObject = new JSONObject();
                        for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                            if (linesArray.contains(snapshot1.getKey())) {
                                JSONObject markingDataObject = new JSONObject();
                                for (DataSnapshot dataSnapshot : snapshot1.getChildren()) {
                                    Log.d("TAG", "onDataChange: check "+snapshot1.getKey()+"   "+dataSnapshot.getKey());
                                    if (dataSnapshot.hasChild("latLng")) {
                                        JSONArray jsonArray = new JSONArray();
                                        jsonArray.put(String.valueOf(dataSnapshot.child("latLng").getValue()));
                                        jsonArray.put(dataSnapshot.child("image").getValue().toString());
                                        jsonArray.put(dataSnapshot.child("houseType").getValue().toString());
                                        if (dataSnapshot.hasChild("revisitKey")) {
                                            jsonArray.put(dataSnapshot.child("revisitKey").getValue().toString());
                                        } else {
                                            jsonArray.put("no");
                                        }
                                        if (dataSnapshot.hasChild("cardNumber")) {
                                            jsonArray.put(dataSnapshot.child("cardNumber").getValue().toString());
                                        } else {
                                            jsonArray.put("no");
                                        }
                                        if (dataSnapshot.hasChild("rfidNotFoundKey")) {
                                            jsonArray.put(dataSnapshot.child("rfidNotFoundKey").getValue().toString());
                                        } else {
                                            jsonArray.put("no");
                                        }
                                        try {
                                            markingDataObject.put(dataSnapshot.getKey(), jsonArray);
                                        } catch (Exception e) {
                                        }
                                    }
                                }
                                try {
                                    jsonObject.put(snapshot1.getKey(), markingDataObject);
                                } catch (Exception e) {
                                }
                            }
                        }
                        preferences.edit().putString("markingData", jsonObject.toString()).apply();
                    }
                    fileDownLoad(ward);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        });
    }

    private void getCardRevisitReasons() {
        FileDownloadActivity.this.runOnUiThread(() -> {
            databaseReferencePath.child("Defaults").child("CardRevisitReasons").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    final ArrayList<String> revisitReasonList = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String replaceText = String.valueOf(snapshot.getValue()).replace(',', '~');
                        revisitReasonList.add(replaceText);
                    }
                    preferences.edit().putString("revisitReasonList", revisitReasonList.toString()).apply();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        });
    }

    private void showAlertBox(String message) {
        checkStatus = true;
        common.closeDialog();
        AlertDialog.Builder alertAssignment = new AlertDialog.Builder(this);
        alertAssignment.setMessage(message);
        alertAssignment.setCancelable(false);
        alertAssignment.setPositiveButton("OK", (dialog, id) -> {
            dialog.cancel();
            if (dialog != null) {
                dialog.dismiss();
                finish();
            }
        });
        AlertDialog alertDAssignment = alertAssignment.create();
        if (!isFinishing()) {
            alertDAssignment.show();
        }
    }

    public void downloadData() {
        FileDownloadActivity.this.runOnUiThread(() -> {
            databaseReferencePath.child("SurveyorsCuurentAssignment").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        if (dataSnapshot.hasChild(preferences.getString("userId", ""))) {
                            if (dataSnapshot.child(preferences.getString("userId", "")).child("ward").getValue().toString() != null && !dataSnapshot.child(preferences.getString("userId", "")).child("ward").getValue().toString().equals("")) {
                                String getLN = dataSnapshot.child(preferences.getString("userId", "")).child("line").getValue().toString();
                                preferences.edit().putString("ward", dataSnapshot.child(preferences.getString("userId", "")).child("ward").getValue().toString()).apply();
                                preferences.edit().putString("lines", getLN).apply();
                                getHouseWardMapping(dataSnapshot.child(preferences.getString("userId", "")).child("ward").getValue().toString(), getLN);
                            } else {
                                showAlertBox("आज आपका कोई कार्य असाइन नहीं है।  कृपया सुपरवाईज़र से कांटेक्ट करे || ");
                            }
                        } else {
                            showAlertBox("आज आपका कोई कार्य असाइन नहीं है।  कृपया सुपरवाईज़र से कांटेक्ट करे || ");
                        }
                    } else {
                        showAlertBox("आज आपका कोई कार्य असाइन नहीं है।  कृपया सुपरवाईज़र से कांटेक्ट करे || ");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        });
    }

    public Runnable runnable = this::checkNetwork;

    @SuppressLint("StaticFieldLeak")
    private void checkNetwork() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                return common.network(FileDownloadActivity.this);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    downloadData();
                } else {
                    handler = new Handler();
                    handler.postDelayed(runnable, 2000);
                }
            }
        }.execute();
    }

    public void fileDownLoad(String s) {
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + storagePath + "/WardJson").child(s + ".json");
        common.setProgressBar("File downloading...", FileDownloadActivity.this, FileDownloadActivity.this);
        try {
            File sdcard1 = Environment.getExternalStorageDirectory();
            File file1 = new File(sdcard1, "WardJson");
            File filee = new File(file1, s + ".json");
            filee.delete();
            final File localFile = File.createTempFile("images", "jpg");
            storageReference.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(localFile)));
                    StringBuilder result = new StringBuilder();

                    while ((str = reader.readLine()) != null) {

                        result.append(str);
                    }
                    File root = new File(Environment.getExternalStorageDirectory(), "WardJson");
                    if (!root.exists()) {
                        root.mkdirs();
                    }
                    File gpxfile = new File(root, s + ".json");
                    FileWriter writer = new FileWriter(gpxfile, true);
                    writer.append(result.toString());
                    writer.flush();
                    writer.close();
                    common.closeDialog();
                    startSurvey.setVisibility(View.VISIBLE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).addOnFailureListener(exception -> showAlertBox("File Not Downloaded, May be wrong assignment. Please Contact your Superviser."));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}