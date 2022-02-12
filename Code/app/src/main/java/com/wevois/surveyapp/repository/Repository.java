package com.wevois.surveyapp.repository;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.wevois.surveyapp.CommonFunctions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class Repository {
    JSONObject dataObject = new JSONObject(), jsonObject = new JSONObject(), jsonObjectWard = new JSONObject();
    SharedPreferences preferences;
    Activity activityData;
    String cardNumber = "SIKA10010";

    public LiveData<DataSnapshot> checkVersion(Activity activity) {
        MutableLiveData<DataSnapshot> responce = new MutableLiveData<>();
        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("Settings/LatestVersions/survey").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                responce.setValue(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        return responce;
    }

    public LiveData<DataSnapshot> loginUserId(Activity activity, String user_id) {
        MutableLiveData<DataSnapshot> responce = new MutableLiveData<>();
        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("Surveyors").orderByChild("pin").equalTo(user_id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                responce.setValue(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return responce;
    }

    public LiveData<DataSnapshot> checkMobile(Activity activity, String user_id) {
        MutableLiveData<DataSnapshot> responce = new MutableLiveData<>();
        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("Surveyors").orderByChild("mobile").equalTo(user_id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                responce.setValue(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return responce;
    }

    public LiveData<String> uploadRegisterImage(Activity activity, Bitmap identityBitmap, String userMobile) {
        MutableLiveData<String> responce = new MutableLiveData<>();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + CommonFunctions.getInstance().getDatabaseStorage(activity) + "/SurveyorsIdentity");
        StorageReference mountainImagesRef = storageRef.child(userMobile + ".jpg");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        identityBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] data = baos.toByteArray();
        UploadTask uploadTask = mountainImagesRef.putBytes(data);
        uploadTask.addOnFailureListener(exception -> {
            responce.setValue("Error");
        }).addOnSuccessListener(taskSnapshot -> {
            responce.setValue("Success");
        });
        return responce;
    }

    public LiveData<String> sendRegisterData(Activity activity, String mobileNumber, String userName) {
        MutableLiveData<String> responce = new MutableLiveData<>();
        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("Surveyors").orderByChild("mobile").equalTo(mobileNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    CommonFunctions.getInstance().showAlertBox("This number not exists in our system.", true, activity);
                } else {
                    final DatabaseReference deviceDBRef = CommonFunctions.getInstance().getDatabaseForApplication(activity).child("Surveyors");
                    deviceDBRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String namePrefix = "SUR";
                            int lastNumber = 0;
                            if (dataSnapshot.child("LastSorveyourPIN").getValue() != null && !dataSnapshot.child("LastSorveyourPIN").getValue().toString().equals("")) {
                                lastNumber = Integer.parseInt(dataSnapshot.child("LastSorveyourPIN").getValue().toString());
                            }
                            String nameSuffix = String.valueOf(lastNumber + 1);
                            HashMap<String, String> dataSend = new HashMap<>();
                            dataSend.put("name", userName);
                            dataSend.put("mobile", mobileNumber);
                            dataSend.put("pin", namePrefix + nameSuffix);
                            dataSend.put("status", "1");
                            dataSend.put("joining-date", new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
                            dataSend.put("leaving-date", "");
                            dataSend.put("identity-image", mobileNumber + ".jpg");
                            dataSend.put("surveyor-type", "Surveyor");
                            deviceDBRef.child("LastSorveyourPIN").setValue(nameSuffix);
                            String finalNamePrefix = namePrefix + nameSuffix;
                            deviceDBRef.child(nameSuffix).setValue(dataSend).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    responce.setValue(finalNamePrefix);
                                } else {
                                    responce.setValue("Error");
                                    Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return responce;
    }

    public LiveData<String> fileDownload(Activity activity) {
        MutableLiveData<String> responce = new MutableLiveData<>();
        SharedPreferences preferences = activity.getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("SurveyorsCuurentAssignment").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    if (snapshot.hasChild(preferences.getString("userId", ""))) {
                        if (snapshot.child(preferences.getString("userId", "")).child("ward").getValue().toString() != null && !snapshot.child(preferences.getString("userId", "")).child("ward").getValue().toString().equals("")) {
                            String getLN = snapshot.child(preferences.getString("userId", "")).child("line").getValue().toString();
                            String ward = snapshot.child(preferences.getString("userId", "")).child("ward").getValue().toString();
                            preferences.edit().putString("ward", ward).apply();
                            preferences.edit().putString("lines", getLN).apply();
                            preferences.edit().putString("markingData", "").apply();
                            if (preferences.getString("isOfflineAllowed", "no").equalsIgnoreCase("yes")) {
                                String[] linesData = getLN.split(",");
                                JSONObject jsonObject = new JSONObject();
                                for (int i = 0; i < linesData.length; i++) {
                                    int finalI = i;
                                    activity.runOnUiThread(() -> {
                                        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntityMarkingData/MarkedHouses/" + preferences.getString("ward", "") + "/" + linesData[finalI].trim()).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                if (snapshot.getValue() != null) {
                                                    JSONObject markingDataObject = new JSONObject();
                                                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
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
                                                        jsonObject.put(snapshot.getKey(), markingDataObject);
                                                    } catch (Exception e) {
                                                    }
                                                    preferences.edit().putString("markingData", jsonObject.toString()).apply();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                    });
                                }
                            }
                            FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + CommonFunctions.getInstance().getDatabaseStorage(activity) + "/WardJson/" + preferences.getString("ward", "") + ".json").getMetadata().addOnSuccessListener(storageMetadata -> {
                                File file = new File(Environment.getExternalStorageDirectory(), "WardJson/" + preferences.getString("ward", "") + ".json");
                                long fileCreationTime = storageMetadata.getCreationTimeMillis();
                                long fileDownloadTime = preferences.getLong("fileDownloadTime", 0);
                                if ((fileDownloadTime != fileCreationTime) || (!file.exists())) {
                                    File root = new File(Environment.getExternalStorageDirectory(), "WardJson");
                                    if (!root.exists()) {
                                        root.mkdirs();
                                    }
                                    file.delete();
                                    FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + CommonFunctions.getInstance().getDatabaseStorage(activity) + "/WardJson/" + preferences.getString("ward", "") + ".json").getFile(file).addOnSuccessListener(taskSnapshot -> {
                                        try {
                                            String str = "";
                                            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                                            StringBuilder result = new StringBuilder();

                                            while ((str = reader.readLine()) != null) {
                                                result.append(str);
                                            }
                                            File roots = new File(Environment.getExternalStorageDirectory(), "WardJson");
                                            if (!roots.exists()) {
                                                roots.mkdirs();
                                            }
                                            File gpxfile = new File(roots, preferences.getString("ward", "") + ".json");
                                            FileWriter writer = new FileWriter(gpxfile, true);
                                            writer.append(result.toString());
                                            writer.flush();
                                            writer.close();
                                            responce.setValue("success");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            responce.setValue(e.toString());
                                        }
                                        preferences.edit().putLong("fileDownloadTime", fileCreationTime).apply();
                                    }).addOnFailureListener(exception -> {
                                        responce.setValue("File not exist.");
                                    });
                                } else {
                                    responce.setValue("success");
                                }
                            }).addOnFailureListener(e -> {
                            });
                        } else {
                            responce.setValue("आज आपका कोई कार्य असाइन नहीं है।  कृपया सुपरवाईज़र से कांटेक्ट करे || ");
                        }
                    } else {
                        responce.setValue("आज आपका कोई कार्य असाइन नहीं है।  कृपया सुपरवाईज़र से कांटेक्ट करे || ");
                    }
                } else {
                    responce.setValue("आज आपका कोई कार्य असाइन नहीं है।  कृपया सुपरवाईज़र से कांटेक्ट करे || ");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return responce;
    }

    public void storageFileDownload(Activity activity) {
        SharedPreferences preferences = activity.getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
        FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + CommonFunctions.getInstance().getDatabaseStorage(activity) + "/Defaults/FinalHousesType.json").getMetadata().addOnSuccessListener(storageMetadata -> {
            long fileCreationTime = storageMetadata.getCreationTimeMillis();
            long fileDownloadTime = preferences.getLong("housesTypeDownloadTime", 0);
            if (fileDownloadTime != fileCreationTime || preferences.getString("housesTypeList", "").equalsIgnoreCase("")) {
                FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + CommonFunctions.getInstance().getDatabaseStorage(activity) + "/Defaults/FinalHousesType.json").getBytes(10000000).addOnSuccessListener(taskSnapshot -> {
                    String s = new String(taskSnapshot, StandardCharsets.UTF_8);
                    try {
                        JSONArray jsonArray = new JSONArray(s);
                        JSONObject jsonHousesType = new JSONObject();
                        JSONObject jsonCommercialHousesType = new JSONObject();
                        JSONObject jsonResidentialHousesType = new JSONObject();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            if (!jsonArray.get(i).toString().equalsIgnoreCase("null")) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                try {
                                    jsonHousesType.put(String.valueOf(i), jsonObject.getString("name"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                if (jsonObject.has("entity-type")) {
                                    if (jsonObject.getString("entity-type").equals("commercial")) {
                                        try {
                                            jsonCommercialHousesType.put(String.valueOf(i), jsonObject.getString("name"));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        try {
                                            jsonResidentialHousesType.put(String.valueOf(i), jsonObject.getString("name"));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                preferences.edit().putString("housesTypeList", jsonHousesType.toString()).apply();
                                preferences.edit().putString("commercialHousesTypeList", jsonCommercialHousesType.toString()).apply();
                                preferences.edit().putString("residentialHousesTypeList", jsonResidentialHousesType.toString()).apply();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    preferences.edit().putLong("housesTypeDownloadTime", fileCreationTime).apply();
                }).addOnFailureListener(exception -> {
                });
            }
        }).addOnFailureListener(e -> {
        });
        FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + CommonFunctions.getInstance().getDatabaseStorage(activity) + "/Defaults/CardRevisitReasons.json").getMetadata().addOnSuccessListener(storageMetadata -> {
            long fileCreationTime = storageMetadata.getCreationTimeMillis();
            long fileDownloadTime = preferences.getLong("CardRevisitReasonsDownloadTime", 0);
            if (fileDownloadTime != fileCreationTime || preferences.getString("revisitReasonList", "").equalsIgnoreCase("")) {
                FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + CommonFunctions.getInstance().getDatabaseStorage(activity) + "/Defaults/CardRevisitReasons.json").getBytes(10000000).addOnSuccessListener(taskSnapshot -> {
                    String s = new String(taskSnapshot, StandardCharsets.UTF_8);
                    try {
                        JSONArray jsonArray = new JSONArray(s);
                        final ArrayList<String> revisitReasonList = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            if (!jsonArray.get(i).toString().equalsIgnoreCase("null")) {
                                String replaceText = String.valueOf(jsonArray.getString(i)).replace(',', '~');
                                revisitReasonList.add(replaceText);
                            }
                        }
                        preferences.edit().putString("revisitReasonList", revisitReasonList.toString()).apply();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    preferences.edit().putLong("CardRevisitReasonsDownloadTime", fileCreationTime).apply();
                }).addOnFailureListener(exception -> {
                });
            }
        }).addOnFailureListener(e -> {
        });
        FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + CommonFunctions.getInstance().getDatabaseStorage(activity) + "/Settings/Survey.json").getMetadata().addOnSuccessListener(storageMetadata -> {
            long fileCreationTime = storageMetadata.getCreationTimeMillis();
            long fileDownloadTime = preferences.getLong("SurveyDownloadTime", 0);
            if (fileDownloadTime != fileCreationTime || preferences.getString("revisitReasonList", "").equalsIgnoreCase("")) {
                FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + CommonFunctions.getInstance().getDatabaseStorage(activity) + "/Settings/Survey.json").getBytes(10000000).addOnSuccessListener(taskSnapshot -> {
                    String s = new String(taskSnapshot, StandardCharsets.UTF_8);
                    try {
                        JSONObject jsonObject = new JSONObject(s);
                        if (jsonObject.has("cardScanningTime")) {
                            int minCurrentTime = Integer.parseInt(jsonObject.getString("cardScanningTime")) * 1000;
                            preferences.edit().putInt("cardScanningTime", minCurrentTime).apply();
                        }
                        if (jsonObject.has("minimumDistanceBetweenMarkerAndSurvey")) {
                            int minCurrentTime = Integer.parseInt(jsonObject.getString("minimumDistanceBetweenMarkerAndSurvey"));
                            preferences.edit().putInt("minimumDistanceBetweenMarkerAndSurvey", minCurrentTime).apply();
                        }
                        if (jsonObject.has("messageMinimumDistanceMarkerAndSurvey")) {
                            preferences.edit().putString("messageMinimumDistanceMarkerAndSurvey", jsonObject.getString("messageMinimumDistanceMarkerAndSurvey")).apply();
                        }
                        if (jsonObject.has("sameCardOnTwoMarkerMessage")) {
                            preferences.edit().putString("sameCardOnTwoMarkerMessage", jsonObject.getString("sameCardOnTwoMarkerMessage")).apply();
                        }
                        if (jsonObject.has("sameMarkerOnTwoCard")) {
                            preferences.edit().putString("sameMarkerOnTwoCard", jsonObject.getString("sameMarkerOnTwoCard")).apply();
                        }
                        if (jsonObject.has("lineNotCompleteMessage")) {
                            preferences.edit().putString("lineNotCompleteMessage", jsonObject.getString("lineNotCompleteMessage")).apply();
                        }
                        if (jsonObject.has("cameraNotSupportMessage")) {
                            preferences.edit().putString("cameraNotSupportMessage", jsonObject.getString("cameraNotSupportMessage")).apply();
                        }
                        if (jsonObject.has("scanByCameraNoteMessage")) {
                            preferences.edit().putString("scanByCameraNoteMessage", jsonObject.getString("scanByCameraNoteMessage")).apply();
                        }
                        if (jsonObject.has("scanByRfidNoteMessage")) {
                            preferences.edit().putString("scanByRfidNoteMessage", jsonObject.getString("scanByRfidNoteMessage")).apply();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    preferences.edit().putLong("SurveyDownloadTime", fileCreationTime).apply();
                }).addOnFailureListener(exception -> {
                });
            }
        }).addOnFailureListener(e -> {
        });
        FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + CommonFunctions.getInstance().getDatabaseStorage(activity) + "/CardScanData/CardScanData.json").getMetadata().addOnSuccessListener(storageMetadata -> {
            long fileCreationTime = storageMetadata.getCreationTimeMillis();
            long fileDownloadTime = preferences.getLong("CardScanDataDownloadTime", 0);
            if (fileDownloadTime != fileCreationTime || preferences.getString("CardScanData", "").equalsIgnoreCase("")) {
                FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + CommonFunctions.getInstance().getDatabaseStorage(activity) + "/CardScanData/CardScanData.json").getBytes(100000000).addOnSuccessListener(taskSnapshot -> {
                    String s = new String(taskSnapshot, StandardCharsets.UTF_8);
                    JSONObject jsonCard = new JSONObject();
                    try {
                        JSONObject jsonObject = new JSONObject(s);
                        Iterator<?> keys = jsonObject.keys();
                        while (keys.hasNext()) {
                            String key = (String) keys.next();
                            jsonCard.put(jsonObject.getJSONArray(key).get(0).toString(), key);
                        }
                    } catch (Exception e) {
                    }
                    preferences.edit().putString("CardScanData", s).commit();
                    preferences.edit().putString("SerialNoData", jsonCard.toString()).commit();
                    preferences.edit().putLong("CardScanDataDownloadTime", fileCreationTime).apply();
                }).addOnFailureListener(exception -> {
                });
            }
        }).addOnFailureListener(e -> {
        });
    }

    public LiveData<String> DownloadTodayCardScan(Activity activity) {
        MutableLiveData<String> responce = new MutableLiveData<>();
        SharedPreferences preferences = activity.getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/DailyHouseCount/" + preferences.getString("ward", "") + "/" + preferences.getString("userId", "")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final int[] count = {0};
                @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                String date = dateFormat.format(new Date());
                if ((dataSnapshot.getValue() != null) && dataSnapshot.hasChild(date)) {
                    count[0] = Integer.parseInt(dataSnapshot.child(date).getValue().toString());
                }
                CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/DailyRfidNotFoundCount/" + preferences.getString("ward", "") + "/" + preferences.getString("userId", "")).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if ((dataSnapshot.getValue() != null) && dataSnapshot.hasChild(date)) {
                            count[0] = count[0] + Integer.parseInt(dataSnapshot.child(date).getValue().toString());
                        }
                        responce.setValue("" + count[0]);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        return responce;
    }

    public LiveData<DataSnapshot> DownloadCurrentLine(Activity activity) {
        MutableLiveData<DataSnapshot> responce = new MutableLiveData<>();
        SharedPreferences preferences = activity.getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData").child("CurrentLine").child(preferences.getString("ward", "") + "/" + preferences.getString("userId", "")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                responce.setValue(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return responce;
    }

    public LiveData<DataSnapshot> MarkingLine(Activity activity, String currentLine) {
        MutableLiveData<DataSnapshot> response = new MutableLiveData<>();
        SharedPreferences preferences = activity.getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData").child("CurrentLine").child(preferences.getString("ward", "")).child(preferences.getString("userId", "")).setValue("" + currentLine);
        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntityMarkingData/MarkedHouses/" + preferences.getString("ward", "") + "/" + currentLine).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                response.setValue(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return response;
    }

    public LiveData<DataSnapshot> CheckWardMapping(Activity activity, String alreadyMappedCardNumber) {
        MutableLiveData<DataSnapshot> response = new MutableLiveData<>();
        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("CardWardMapping/" + alreadyMappedCardNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                response.setValue(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        return response;
    }

    public LiveData<DataSnapshot> CheckHousesDetails(Activity activity, String assignedWard, String assignedLine, String cardNumber) {
        MutableLiveData<DataSnapshot> response = new MutableLiveData<>();
        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("Houses/" + assignedWard + "/" + assignedLine).orderByChild("cardNo").equalTo(cardNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                response.setValue(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        return response;
    }

    public LiveData<DataSnapshot> checkSurveyDetailsIfAlreadyExists(Activity activity, String currentCardNumber) {
        MutableLiveData<DataSnapshot> response = new MutableLiveData<>();
        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("CardWardMapping/" + currentCardNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    if (dataSnapshot.hasChild("ward") && dataSnapshot.hasChild("line")) {
                        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("Houses/" + dataSnapshot.child("ward").getValue().toString() + "/" + dataSnapshot.child("line").getValue().toString()).orderByChild("cardNo").equalTo(currentCardNumber).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot1) {
                                response.setValue(dataSnapshot1);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                            }
                        });
                    } else {
                        response.setValue(null);
                    }
                } else {
                    response.setValue(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        return response;
    }

    public LiveData<DataSnapshot> checkRfidAlreadyExists(Activity activity, String ward, String line, String rfid) {
        MutableLiveData<DataSnapshot> response = new MutableLiveData<>();
        Query query = CommonFunctions.getInstance().getDatabaseForApplication(activity).child("Houses/" + ward + "/" + line).orderByChild("rfid").equalTo(rfid);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                response.setValue(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        return response;
    }

    public LiveData<DataSnapshot> checkMarkedHouses(Activity activity, String ward, String line, String markingKey) {
        MutableLiveData<DataSnapshot> response = new MutableLiveData<>();
        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntityMarkingData/MarkedHouses/" + ward + "/" + line + "/" + markingKey + "/cardNumber").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                response.setValue(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return response;
    }

    public LiveData<DataSnapshot> saveRfidNotFoundData(Activity activity, HashMap<String, Object> housesMap) {
        MutableLiveData<DataSnapshot> response = new MutableLiveData<>(null);
        SharedPreferences preferences = activity.getSharedPreferences("surveyApp", MODE_PRIVATE);
        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntityMarkingData/MarkedHouses/" + preferences.getString("ward", "") + "/" + preferences.getString("line", "") + "/" + "lineRfidNotFoundCount").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int count = 1;
                if (dataSnapshot.getValue() != null) {
                    count = Integer.parseInt(dataSnapshot.getValue().toString()) + 1;
                }
                try {
                    CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/RFIDNotFoundSurvey/" + preferences.getString("ward", "") + "/" + preferences.getString("line", "") + "/" + preferences.getString("rfid", "")).setValue(housesMap);
                    CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntityMarkingData/MarkedHouses/" + preferences.getString("ward", "") + "/" + preferences.getString("line", "") + "/lineRfidNotFoundCount").setValue("" + count);
                    CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntityMarkingData/MarkedHouses/" + preferences.getString("ward", "") + "/" + preferences.getString("line", "") + "/" + preferences.getString("markingKey", "") + "/rfidNotFoundKey").setValue(preferences.getString("rfid", ""));

                    CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/TotalRfidNotFoundCount/" + preferences.getString("ward", "")).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                String count = String.valueOf(Integer.parseInt(dataSnapshot.getValue().toString()) + 1);
                                CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/TotalRfidNotFoundCount/" + preferences.getString("ward", "")).setValue(count);
                            } else {
                                CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/TotalRfidNotFoundCount/" + preferences.getString("ward", "")).setValue("1");
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                    SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd-MM-yyyy");
                    CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/DailyRfidNotFoundCount/" + preferences.getString("ward", "") + "/" + preferences.getString("userId", "") + "/" + dateFormat1.format(new Date())).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String count = "1";
                            if (dataSnapshot.getValue() != null) {
                                count = String.valueOf(Integer.parseInt(dataSnapshot.getValue().toString()) + 1);
                            }
                            CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/DailyRfidNotFoundCount/" + preferences.getString("ward", "") + "/" + preferences.getString("userId", "") + "/" + dateFormat1.format(new Date())).setValue(count).addOnCompleteListener(task -> response.setValue(dataSnapshot));
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                } catch (Exception e) {
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return response;
    }

    @SuppressLint("StaticFieldLeak")
    public LiveData<String> sendHousesData(Activity activity, String countCheck, String currentCardNumber, Bitmap identityBitmap, File myPath, ArrayList<String> newMobiles, HashMap<String, Object> housesMap, String markingKey, JSONObject jsonObjects, JSONObject dataObjects, JSONObject jsonObjectWards, String wardNo, String userId, String line, String rfid, String markingRevisit, String currentDate) {
        MutableLiveData<String> response = new MutableLiveData<>("");
        cardNumber = currentCardNumber;
        jsonObject = jsonObjects;
        dataObject = dataObjects;
        jsonObjectWard = jsonObjectWards;
        activityData = activity;
        preferences = activity.getSharedPreferences("surveyApp", MODE_PRIVATE);
        if (countCheck.equals("2")) {
            activity.runOnUiThread(() -> {
                CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/DailyHouseCount/" + wardNo + "/" + userId + "/" + currentDate).runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        if (currentData.getValue() == null) {
                            currentData.setValue(1);
                        } else {
                            currentData.setValue(String.valueOf(Integer.parseInt(currentData.getValue().toString()) + 1));
                        }
                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                        if (error == null) {
                            response.setValue(checkAllDataSend("dailyHouses"));
                        }
                    }
                });
                removeLocalData("DailyHouseCount");
            });
            activity.runOnUiThread(() -> {
                CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/TotalHouseCount/" + wardNo).runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        if (currentData.getValue() == null) {
                            currentData.setValue(1);
                        } else {
                            currentData.setValue(String.valueOf(Integer.parseInt(currentData.getValue().toString()) + 1));
                        }
                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                        if (error == null) {
                            response.setValue(checkAllDataSend("totalHouses"));
                        }
                    }
                });
                removeLocalData("TotalHouseCount");
            });
            activity.runOnUiThread(() -> {
                CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/SurveyDateWise/" + userId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot1) {
                        int dateCount = 1, totalCount = 1;
                        if (dataSnapshot1.getValue() != null) {
                            if (dataSnapshot1.hasChild(currentDate) ){
                                dateCount = Integer.parseInt(dataSnapshot1.child(currentDate).getValue().toString()) + 1;
                            }
                            if (dataSnapshot1.hasChild("totalCount")) {
                                totalCount = Integer.parseInt(dataSnapshot1.child("totalCount").getValue().toString()) + 1;
                            }
                        }
                        HashMap<String, Object> surveyDateWiseMap = new HashMap<>();
                        surveyDateWiseMap.put("totalCount", totalCount);
                        surveyDateWiseMap.put(currentDate, dateCount);
                        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/SurveyDateWise/" + userId).updateChildren(surveyDateWiseMap).addOnCompleteListener(task1111 -> {
                            if (task1111.isSuccessful()) {
                                response.setValue(checkAllDataSend("DateWise"));
                            }
                        });
                        removeLocalData("SurveyDateWise");
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            });
        } else {
            removeLocalData("DailyHouseCount");
            removeLocalData("TotalHouseCount");
            removeLocalData("SurveyDateWise");
        }
        activity.runOnUiThread(() -> {
            if (identityBitmap==null){
                new AsyncTask<Void, Void, Bitmap>() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                    }

                    @Override
                    protected Bitmap doInBackground(Void... p) {
                        File root = new File(Environment.getExternalStorageDirectory(), "SurveyCardImage");
                        if (!root.exists()) {
                            root.mkdirs();
                        }
                        File mypath = null;
                        mypath = new File(root, cardNumber + ".jpg");
                        FileInputStream fos = null;
                        Bitmap bitmap = null;
                        try {
                            fos = new FileInputStream(mypath);
                            bitmap = BitmapFactory.decodeStream(fos);
                            fos.close();
                            mypath.delete();
                        } catch (Exception e) {
                            e.printStackTrace();
                            activity.runOnUiThread(() -> {
                                removeCardLocalData();
                                response.setValue("successData");
                            });
                        }
                        return bitmap;
                    }

                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    protected void onPostExecute(Bitmap result) {
                        if (result != null) {
                            new AsyncTask<Void, Void, Boolean>() {
                                @Override
                                protected void onPreExecute() {
                                    super.onPreExecute();
                                }

                                @Override
                                protected Boolean doInBackground(Void... p) {
                                    FirebaseStorage storage = FirebaseStorage.getInstance();
                                    StorageReference storageRef = storage.getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + CommonFunctions.getInstance().getDatabaseStorage(activity) + "/SurveyCardImage");
                                    StorageReference mountainImagesRef = storageRef.child(cardNumber + ".jpg");
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    result.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                                    byte[] data = baos.toByteArray();
                                    UploadTask uploadTask = mountainImagesRef.putBytes(data);
                                    uploadTask.addOnFailureListener(exception -> {
                                        removeCardLocalData();
                                        response.setValue("successData");
                                    }).addOnSuccessListener(taskSnapshot -> {
                                        removeLocalData("StorageImage");
                                        response.setValue(checkAllDataSend("image"));
                                    });
                                    return null;
                                }

                                @Override
                                protected void onPostExecute(Boolean result) {
                                }
                            }.execute();
                        } else {
                            removeCardLocalData();
                            response.setValue("successData");
                        }
                    }
                }.execute();
            }else {
                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                    }

                    @Override
                    protected Boolean doInBackground(Void... p) {
                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        StorageReference storageRef = storage.getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + CommonFunctions.getInstance().getDatabaseStorage(activity) + "/SurveyCardImage");
                        StorageReference mountainImagesRef = storageRef.child(currentCardNumber + ".jpg");
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        identityBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                        byte[] data = baos.toByteArray();
                        UploadTask uploadTask = mountainImagesRef.putBytes(data);
                        uploadTask.addOnFailureListener(exception -> {
                        }).addOnSuccessListener(taskSnapshot -> {
                            try {
                                if (myPath != null) {
                                    myPath.delete();
                                }
                            } catch (Exception e) {
                            }
                            removeLocalData("StorageImage");
                            response.setValue(checkAllDataSend("image"));
                        });
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                    }
                }.execute();
            }
        });
        activity.runOnUiThread(() -> {
            CommonFunctions.getInstance().getDatabaseForApplication(activity).child("Houses/" + wardNo + "/" + line + "/" + currentCardNumber).updateChildren(housesMap).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    removeLocalData("Houses");
                    response.setValue(checkAllDataSend("Houses"));
                }
            });
        });
        activity.runOnUiThread(() -> {
            for (int i = 0; i < newMobiles.size(); i++) {
                DatabaseReference houseWardMapPath = CommonFunctions.getInstance().getDatabaseForApplication(activity).child("HouseWardMapping/" + newMobiles.get(i));
                houseWardMapPath.child("line").setValue(line);
                houseWardMapPath.child("ward").setValue(wardNo);
            }
            HashMap<String, Object> houseWardMapping = new HashMap<>();
            houseWardMapping.put("line", line);
            houseWardMapping.put("ward", wardNo);
            CommonFunctions.getInstance().getDatabaseForApplication(activity).child("CardWardMapping/" + currentCardNumber).updateChildren(houseWardMapping).addOnCompleteListener(task1 -> {
                if (task1.isSuccessful()) {
                    removeLocalData("CardWardMapping");
                    response.setValue(checkAllDataSend("cardWard"));
                }
            });
        });
        activity.runOnUiThread(() -> {
            CommonFunctions.getInstance().getDatabaseForApplication(activity).child("CardScanData/" + rfid).child("cardInstalled").setValue("yes").addOnCompleteListener(task11 -> {
                if (task11.isSuccessful()) {
                    removeLocalData("CardScanData");
                    response.setValue(checkAllDataSend("cardScan"));
                }
            });
        });
        activity.runOnUiThread(() -> {
            CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntityMarkingData/MarkedHouses/" + wardNo + "/" + line + "/surveyedCount").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    int count = 1;
                    if (dataSnapshot.getValue() != null) {
                        count = Integer.parseInt(dataSnapshot.getValue().toString()) + 1;
                    }
                    if (countCheck.equals("2")) {
                        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntityMarkingData/MarkedHouses/" + wardNo + "/" + line + "/surveyedCount").setValue("" + count);
                    }
                    CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntityMarkingData/MarkedHouses/" + wardNo + "/" + line + "/" + markingKey + "/cardNumber").setValue(currentCardNumber).addOnCompleteListener(task111 -> {
                        if (task111.isSuccessful()) {
                            if (!markingRevisit.equalsIgnoreCase("no")) {
                                CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/RevisitRequest/" + wardNo + "/" + line + "/" + markingRevisit).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot1) {
                                        if (dataSnapshot1.getValue() != null) {
                                            CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/RevisitHistory/" + wardNo + "/" + line + "/lineRevisitCount").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                    int revistCount = 1;
                                                    if (dataSnapshot.getValue() != null) {
                                                        revistCount = Integer.parseInt(dataSnapshot.getValue().toString()) + 1;
                                                    }
                                                    CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/RevisitHistory/" + wardNo + "/" + line + "/" + markingRevisit).setValue(dataSnapshot1.getValue());
                                                    CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/RevisitHistory/" + wardNo + "/" + line + "/lineRevisitCount").setValue(revistCount);
                                                    CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/RevisitRequest/" + wardNo + "/" + line + "/" + markingRevisit).removeValue();
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {

                                                }
                                            });
                                            CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/RevisitHistory/" + wardNo + "/totalRevisitCount").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {
                                                    int totalCount = 1;
                                                    if (dataSnapshot.getValue() != null) {
                                                        totalCount = Integer.parseInt(dataSnapshot.getValue().toString()) + 1;
                                                    }
                                                    CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/RevisitHistory/" + wardNo + "/totalRevisitCount").setValue(totalCount);
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {

                                                }
                                            });
                                        }
                                        removeLocalData("EntityMarking");
                                        response.setValue(checkAllDataSend("entityMarking"));
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            } else {
                                removeLocalData("EntityMarking");
                                response.setValue(checkAllDataSend("entityMarking"));
                            }
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        });
        activity.runOnUiThread(() -> {
            CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/SurveyStartDate/" + wardNo + "/" + userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() == null) {
                        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/SurveyStartDate/" + wardNo + "/" + userId).setValue(currentDate).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                removeLocalData("SurveyStartDate");
                                response.setValue(checkAllDataSend("startDate"));
                            }
                        });
                    } else {
                        removeLocalData("SurveyStartDate");
                        response.setValue(checkAllDataSend("startDate"));
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        });
        return response;
    }

    @SuppressLint("StaticFieldLeak")
    public LiveData<String> saveRevisitData(Activity activity, HashMap<String, Object> hashMapData, Bitmap identityBitmap,String cardKey) {
        MutableLiveData<String> response = new MutableLiveData<>("");
        SharedPreferences preferences = activity.getSharedPreferences("surveyApp", MODE_PRIVATE);
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + CommonFunctions.getInstance().getDatabaseStorage(activity) + "/RevisitCardImage");
                StorageReference mountainImagesRef = storageRef.child(cardKey + ".jpg");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                identityBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] data = baos.toByteArray();
                UploadTask uploadTask = mountainImagesRef.putBytes(data);
                uploadTask.addOnFailureListener(exception -> {
                    response.setValue("fail");
                }).addOnSuccessListener(taskSnapshot -> {
                    CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntityMarkingData/MarkedHouses/" + preferences.getString("ward", "") + "/" + preferences.getString("line", "") + "/" + "lineRevisitCount").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            int count = 1;
                            if (dataSnapshot.getValue() != null) {
                                count = Integer.parseInt(dataSnapshot.getValue().toString()) + 1;
                            }
                            CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntityMarkingData/MarkedHouses/" + preferences.getString("ward", "") + "/" + preferences.getString("line", "") + "/lineRevisitCount").setValue("" + count);
                            CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntityMarkingData/MarkedHouses/" + preferences.getString("ward", "") + "/" + preferences.getString("line", "") + "/" + preferences.getString("markingKey", "") + "/revisitKey").setValue(cardKey);
                            CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData").child("RevisitRequest").child(preferences.getString("ward", "")).child(preferences.getString("line", "")).child(cardKey).setValue(hashMapData);

                            String ward = preferences.getString("ward", "");
                            String userId = preferences.getString("userId", "");
                            SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd-MM-yyyy");
                            CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/TotalRevisitRequest/" + ward).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.getValue() != null) {
                                        String count = String.valueOf(Integer.parseInt(dataSnapshot.getValue().toString()) + 1);
                                        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/TotalRevisitRequest/" + ward).setValue(count);
                                    } else {
                                        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/TotalRevisitRequest/" + ward).setValue("1");
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                }
                            });
                            CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/DailyRevisitRequestCount/" + ward + "/" + userId + "/" + dateFormat1.format(new Date())).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.getValue() != null) {
                                        String count = String.valueOf(Integer.parseInt(dataSnapshot.getValue().toString()) + 1);
                                        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/DailyRevisitRequestCount/" + ward + "/" + userId + "/" + dateFormat1.format(new Date())).setValue(count);
                                    } else {
                                        CommonFunctions.getInstance().getDatabaseForApplication(activity).child("EntitySurveyData/DailyRevisitRequestCount/" + ward + "/" + userId + "/" + dateFormat1.format(new Date())).setValue("1");
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                }
                            });
                            response.setValue("Success");
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                });
                return null;
            }

            @Override
            protected void onPostExecute(Boolean result) {
            }
        }.execute();
        return response;
    }

    public LiveData<String> RequiredSurveyHouses(Activity activity, HashMap<String, Object> housesHashMap, DatabaseReference databaseReference) {
        MutableLiveData<String> response = new MutableLiveData<>("");
        databaseReference.updateChildren(housesHashMap).addOnCompleteListener(task -> response.setValue("success"));
        return response;
    }

    public static class DownloadKmlFile extends AsyncTask<String, Void, byte[]> {
        private final String mUrl;
        private final Activity activitys;

        public DownloadKmlFile(String url, Activity activity) {
            mUrl = url;
            activitys = activity;
        }

        protected byte[] doInBackground(String... params) {
            try {
                InputStream is = new URL(mUrl).openStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                return buffer.toByteArray();
            } catch (IOException e) {
            }
            return null;
        }

        protected void onPostExecute(byte[] byteArr) {
            if (byteArr != null) {
                try {
                    SharedPreferences preferences = activitys.getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
                    String saveThis = Base64.encodeToString(byteArr, Base64.DEFAULT);
                    preferences.edit().putString("kmlByteArray", saveThis).apply();
                } catch (Exception e) {
                }
            }
        }
    }

    private String checkAllDataSend(String message) {
        String response = "";
        try {
            JSONObject tempData = jsonObject.getJSONObject("details");
            if (tempData.getString("StorageImage").equalsIgnoreCase("yes") && tempData.getString("Houses").equalsIgnoreCase("yes") &&
                    tempData.getString("CardWardMapping").equalsIgnoreCase("yes") && tempData.getString("CardScanData").equalsIgnoreCase("yes") &&
                    tempData.getString("EntityMarking").equalsIgnoreCase("yes") && tempData.getString("DailyHouseCount").equalsIgnoreCase("yes") &&
                    tempData.getString("TotalHouseCount").equalsIgnoreCase("yes") && tempData.getString("SurveyDateWise").equalsIgnoreCase("yes") &&
                    tempData.getString("SurveyStartDate").equalsIgnoreCase("yes")) {
                removeCardLocalData();
                response = "success";
            }
        } catch (Exception e) {
        }
        return response;
    }

    private void removeLocalData(String data) {
        try {
            JSONObject tempData = jsonObject.getJSONObject("details");
            tempData.put(data, "yes");
            jsonObject.put("details", tempData);
            dataObject.put(cardNumber, jsonObject);
            jsonObjectWard.put(preferences.getString("ward", ""), dataObject);
            preferences.edit().putString("scanHousesData", jsonObjectWard.toString()).apply();
        } catch (Exception e) {
        }
    }

    private void removeCardLocalData() {
        try {
            dataObject.remove(cardNumber);
            jsonObjectWard.put(preferences.getString("ward", ""), dataObject);
            preferences.edit().putString("scanHousesData", jsonObjectWard.toString()).apply();
        } catch (Exception e) {
        }
    }

    @SuppressLint("StaticFieldLeak")
    public LiveData<Boolean> checkNetWork(Activity activity) {
        MutableLiveData<Boolean> response = new MutableLiveData<>();
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                return CommonFunctions.getInstance().network(activity);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                response.setValue(result);
            }
        }.execute();
        return response;
    }
}
