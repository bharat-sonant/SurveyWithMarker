package com.wevois.surveyapp.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

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
import com.wevois.surveyapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

public class OfflineActivity extends AppCompatActivity {
    Button btnAllDataSync;
    DatabaseReference databaseReferencePath;
    SharedPreferences preferences;
    CommonFunctions common = new CommonFunctions();
    JSONObject wardJsonObject = new JSONObject();
    JSONObject jsonObject = new JSONObject();
    JSONObject cardJsonObject = new JSONObject();
    boolean isDelete = false;
    ArrayList<String> cardNumbers = new ArrayList<>();
    int position = 0;
    JSONObject tempObject = new JSONObject();
    String currentDate, countCheck = "2", markingKey = "", ward, address, cardNo, colonyName, createdDate, houseType, lat, lng, line, name, rfid, mobile, servingCount = "", cardType = "";

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        btnAllDataSync = findViewById(R.id.AllDataSync);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd-MM-yyyy");
        currentDate = dateFormat1.format(new Date());
        databaseReferencePath = common.getDatabaseForApplication(OfflineActivity.this);
        preferences = getSharedPreferences("surveyApp", MODE_PRIVATE);
        btnAllDataSync.setOnClickListener(v -> {
            try {
                jsonObject = new JSONObject(preferences.getString("scanHousesData", ""));
                wardJsonObject = jsonObject.getJSONObject(preferences.getString("ward", ""));
                Iterator<String> listKEY = wardJsonObject.keys();
                while (listKEY.hasNext()) {
                    String key = (String) listKEY.next();
                    cardNumbers.add(key);
                }
            } catch (Exception ignored) {
            }
            if (wardJsonObject.length() == 0) {
                Toast.makeText(OfflineActivity.this, "Data not available for sync.", Toast.LENGTH_SHORT).show();
            } else {
                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        common.setProgressBar("Please Wait...", OfflineActivity.this, OfflineActivity.this);
                    }

                    @Override
                    protected Boolean doInBackground(Void... p) {
                        return common.network(OfflineActivity.this);
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        if (result) {
                            position = 0;
                            sendData();
                        } else {
                            common.closeDialog();
                            Toast.makeText(OfflineActivity.this, "No internet connection.\n try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }.execute();
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    private void sendData() {
        if (wardJsonObject.length() > 0) {
            common.setProgressBar("Data Uploading...\n" + wardJsonObject.length(), OfflineActivity.this, OfflineActivity.this);
            try {
                tempObject = new JSONObject();
                cardJsonObject = wardJsonObject.getJSONObject(cardNumbers.get(position));
                ward = preferences.getString("ward", "");
                cardNo = cardJsonObject.getString("cardno");
                mobile = cardJsonObject.getString("mobile");
                address = cardJsonObject.getString("address");
                colonyName = cardJsonObject.getString("colonyname");
                createdDate = cardJsonObject.getString("createddate");
                houseType = cardJsonObject.getString("housetype");
                lat = cardJsonObject.getString("lat");
                lng = cardJsonObject.getString("lng");
                line = cardJsonObject.getString("line");
                name = cardJsonObject.getString("name");
                rfid = cardJsonObject.getString("rfid");
                markingKey = cardJsonObject.getString("markingKey");
                cardType = cardJsonObject.getString("cardType");
                tempObject = cardJsonObject.getJSONObject("details");
                String lastCharacterOfNumber = mobile.substring(mobile.length() - 1);
                if (lastCharacterOfNumber.equalsIgnoreCase(",")) {
                    mobile = mobile.substring(0, mobile.length() - 1);
                }
                try {
                    servingCount = cardJsonObject.getString("servingcount");
                } catch (Exception e) {
                    servingCount = "";
                }
                fillSurveyDetailsIfAlreadyExists();
            } catch (Exception e) {
            }
        }
    }

    private void fillSurveyDetailsIfAlreadyExists() {
        Query query = databaseReferencePath.child("Houses/" + preferences.getString("ward", "") + "/" + line).orderByChild("rfid").equalTo(rfid);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    countCheck = "1";
                } else {
                    countCheck = "2";
                }
                saveSurveyDetails();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void saveSurveyDetails() {
        databaseReferencePath.child("CardWardMapping/" + cardNo).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    String line = "", ward = "";
                    if (dataSnapshot.hasChild("line")) {
                        line = dataSnapshot.child("line").getValue().toString();
                    }
                    if (dataSnapshot.hasChild("ward")) {
                        ward = dataSnapshot.child("ward").getValue().toString();
                    }
                    if (line.equalsIgnoreCase(preferences.getString("line", "")) && ward.equalsIgnoreCase(preferences.getString("ward", ""))) {
                        callMethod();
                    } else {
                        sendSurveyRequiredRevisited();
                    }
                } else {
                    callMethod();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void callMethod() {
        OfflineActivity.this.runOnUiThread(() -> {
            databaseReferencePath.child("EntityMarkingData/MarkedHouses/" + ward + "/" + line + "/" + markingKey + "/cardNumber").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean isSaveData;
                    if (dataSnapshot.getValue() != null) {
                        if (dataSnapshot.getValue().toString().equalsIgnoreCase(cardNo)) {
                            isSaveData = true;
                        } else {
                            isSaveData = false;
                        }
                    } else {
                        isSaveData = true;
                    }
                    if (isSaveData) {
                        try {
                            if (countCheck.equals("2")) {
                                if (tempObject.getString("DailyHouseCount").equalsIgnoreCase("no")) {
                                    OfflineActivity.this.runOnUiThread(() -> {
                                        dailyHouseCount();
                                    });
                                }
                                if (tempObject.getString("TotalHouseCount").equalsIgnoreCase("no")) {
                                    OfflineActivity.this.runOnUiThread(() -> {
                                        saveTotalHouseCount();
                                    });
                                }
                                if (tempObject.getString("SurveyDateWise").equalsIgnoreCase("no")) {
                                    OfflineActivity.this.runOnUiThread(() -> {
                                        saveSurveyDateWise();
                                    });
                                }
                            } else {
                                removeLocalData("DailyHouseCount");
                                removeLocalData("TotalHouseCount");
                                removeLocalData("SurveyDateWise");
                            }
                            if (tempObject.getString("StorageImage").equalsIgnoreCase("no")) {
                                saveImageData();
                            }
                            if (tempObject.getString("Houses").equalsIgnoreCase("no")) {
                                saveHousesData();
                            }
                            if (tempObject.getString("CardWardMapping").equalsIgnoreCase("no")) {
                                saveCardWardMapping();
                            }
                            if (tempObject.getString("CardScanData").equalsIgnoreCase("no")) {
                                saveCardScanData();
                            }
                            if (tempObject.getString("EntityMarking").equalsIgnoreCase("no")) {
                                saveEntityData();
                            }
                            if (tempObject.getString("SurveyStartDate").equalsIgnoreCase("no")) {
                                setSurveyStartDate();
                            }
                        } catch (Exception e) {
                        }
                    } else {
                        removeCardLocalData();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        });
    }

    @SuppressLint("StaticFieldLeak")
    private void saveImageData() {
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
                mypath = new File(root, cardNo + ".jpg");
                FileInputStream fos = null;
                Bitmap bitmap = null;
                try {
                    fos = new FileInputStream(mypath);
                    bitmap = BitmapFactory.decodeStream(fos);
                    fos.close();
                    mypath.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                    OfflineActivity.this.runOnUiThread(() -> {
                        removeCardLocalData();
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
                            StorageReference storageRef = storage.getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + common.getDatabaseStorage(OfflineActivity.this) + "/SurveyCardImage/" + preferences.getString("ward", "") + "/" + preferences.getString("line", ""));
                            StorageReference mountainImagesRef = storageRef.child(cardNo + ".jpg");
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            result.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                            byte[] data = baos.toByteArray();
                            UploadTask uploadTask = mountainImagesRef.putBytes(data);
                            uploadTask.addOnFailureListener(exception -> {
                                removeCardLocalData();
                            }).addOnSuccessListener(taskSnapshot -> {
                                removeLocalData("StorageImage");
                                checkAllDataSend("StorageImage");
                            });
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                        }
                    }.execute();
                } else {
                    removeCardLocalData();
                }
            }
        }.execute();
    }

    private void saveHousesData() {
        OfflineActivity.this.runOnUiThread(() -> {
            databaseReferencePath.child("EntityMarkingData/MarkedHouses/" + ward + "/" + line + "/" + markingKey + "/cardNumber").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean isSaveData;
                    if (dataSnapshot.getValue() != null) {
                        if (dataSnapshot.getValue().toString().equalsIgnoreCase(cardNo)) {
                            isSaveData = true;
                        } else {
                            isSaveData = false;
                        }
                    } else {
                        isSaveData = true;
                    }
                    if (isSaveData) {
                        HashMap<String, Object> housesMap = new HashMap<>();
                        housesMap.put("address", address);
                        housesMap.put("cardNo", cardNo);
                        housesMap.put("phaseNo", "2");
                        housesMap.put("colonyName", colonyName);
                        if (countCheck.equals("2")) {
                            housesMap.put("surveyorId", preferences.getString("userId", ""));
                        } else {
                            if (isDelete) {
                                housesMap.put("surveyorId", preferences.getString("userId", ""));
                            }
                            housesMap.put("surveyModifierId", preferences.getString("userId", ""));
                        }
                        housesMap.put("houseType", houseType);
                        housesMap.put("latLng", "(" + lat + "," + lng + ")");
                        housesMap.put("line", line);
                        housesMap.put("name", name);
                        housesMap.put("mobile", mobile);
                        housesMap.put("cardType", cardType);
                        housesMap.put("rfid", rfid);
                        housesMap.put("ward", ward);
                        housesMap.put("cardImage", cardNo + ".jpg");
                        if (!servingCount.equals("")) {
                            housesMap.put("servingCount", servingCount);
                        }
                        databaseReferencePath.child("Houses/" + ward + "/" + line + "/" + cardNo).updateChildren(housesMap).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                removeLocalData("Houses");
                                checkAllDataSend("Houses");
                            }
                        });
                    } else {
                        removeCardLocalData();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        });
    }

    private void saveCardWardMapping() {
        if (mobile.contains(",")) {
            String[] mobiles = mobile.trim().split(",");
            for (int i = 0; i < mobiles.length; i++) {
                DatabaseReference houseWardMapPath = databaseReferencePath.child("HouseWardMapping/" + mobiles[i]);
                houseWardMapPath.child("line").setValue(line);
                houseWardMapPath.child("ward").setValue(ward);
            }
        } else {
            DatabaseReference houseWardMapPath = databaseReferencePath.child("HouseWardMapping/" + mobile);
            houseWardMapPath.child("line").setValue(line);
            houseWardMapPath.child("ward").setValue(ward);
        }
        HashMap<String, Object> houseWardMapping = new HashMap<>();
        houseWardMapping.put("line", line);
        houseWardMapping.put("ward", ward);
        databaseReferencePath.child("CardWardMapping/" + cardNo).updateChildren(houseWardMapping).addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()) {
                removeLocalData("CardWardMapping");
                checkAllDataSend("CardWardMapping");
            }
        });
    }

    private void saveCardScanData() {
        databaseReferencePath.child("CardScanData/" + rfid).child("cardInstalled").setValue("yes").addOnCompleteListener(task11 -> {
            if (task11.isSuccessful()) {
                removeLocalData("CardScanData");
                checkAllDataSend("CardScanData");
            }
        });
    }

    private void saveEntityData() {
        HashMap<String, Object> datas = new HashMap<>();
        datas.put("cardNumber", cardNo);
        datas.put("isSurveyed", "yes");
        databaseReferencePath.child("EntityMarkingData/MarkedHouses/" + ward + "/" + line + "/" + markingKey).updateChildren(datas).addOnCompleteListener(task111 -> {
            if (task111.isSuccessful()) {
                removeLocalData("EntityMarking");
                checkAllDataSend("EntityMarking");
            }
        });
    }

    private void dailyHouseCount() {
        String userId = preferences.getString("userId", "");
        databaseReferencePath.child("EntitySurveyData/DailyHouseCount/" + ward + "/" + userId + "/" + currentDate).runTransaction(new Transaction.Handler() {
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
                    checkAllDataSend("DailyHouseCount");
                }
            }
        });
        removeLocalData("DailyHouseCount");
    }

    private void saveTotalHouseCount() {
        databaseReferencePath.child("EntitySurveyData/TotalHouseCount/" + ward).runTransaction(new Transaction.Handler() {
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
                    checkAllDataSend("TotalHouseCount");
                }
            }
        });
        removeLocalData("TotalHouseCount");
    }

    private void saveSurveyDateWise() {
        databaseReferencePath.child("EntitySurveyData/SurveyDateWise/" + preferences.getString("userId", "")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot1) {
                int dateCount = 1, totalCount = 1;
                if (dataSnapshot1.getValue() != null) {
                    if (dataSnapshot1.hasChild(currentDate)) {
                        dateCount = Integer.parseInt(dataSnapshot1.child(currentDate).getValue().toString()) + 1;
                    }
                    if (dataSnapshot1.hasChild("totalCount")) {
                        totalCount = Integer.parseInt(dataSnapshot1.child("totalCount").getValue().toString()) + 1;
                    }
                }
                HashMap<String, Object> surveyDateWiseMap = new HashMap<>();
                surveyDateWiseMap.put("totalCount", totalCount);
                surveyDateWiseMap.put(currentDate, dateCount);
                databaseReferencePath.child("EntitySurveyData/SurveyDateWise/" + preferences.getString("userId", "")).updateChildren(surveyDateWiseMap).addOnCompleteListener(task1111 -> {
                    if (task1111.isSuccessful()) {
                        checkAllDataSend("SurveyDateWise");
                    }
                });
                removeLocalData("SurveyDateWise");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setSurveyStartDate() {
        String userId = preferences.getString("userId", "");
        databaseReferencePath.child("EntitySurveyData/SurveyStartDate/" + ward + "/" + userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    databaseReferencePath.child("EntitySurveyData/SurveyStartDate/" + ward + "/" + userId).setValue(currentDate).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            removeLocalData("SurveyStartDate");
                            checkAllDataSend("SurveyStartDate");
                        }
                    });
                } else {
                    removeLocalData("SurveyStartDate");
                    checkAllDataSend("SurveyStartDate");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void sendSurveyRequiredRevisited() {
        DatabaseReference housesPath = databaseReferencePath.child("RequiredSurveyHouses/" + ward + "/" + line + "/" + cardNo);
        housesPath.child("address").setValue(address);
        housesPath.child("cardNo").setValue(cardNo);
        housesPath.child("phaseNo").setValue("2");
        housesPath.child("colonyName").setValue(colonyName);
        housesPath.child("createdDate").setValue(createdDate);
        housesPath.child("houseType").setValue(houseType);
        housesPath.child("latLng").setValue("(" + lat + "," + lng + ")");
        housesPath.child("line").setValue(line);
        housesPath.child("name").setValue(name);
        housesPath.child("mobile").setValue(mobile);
        housesPath.child("rfid").setValue(rfid);
        housesPath.child("ward").setValue(ward);
        if (!servingCount.equals("")) {
            housesPath.child("servingCount").setValue(servingCount);
        }
        removeCardLocalData();
    }

    private void removeLocalData(String data) {
        try {
            tempObject.put(data, "yes");
            cardJsonObject.put("details", tempObject);
            wardJsonObject.put(cardNo, cardJsonObject);
            jsonObject.put(preferences.getString("ward", ""), wardJsonObject);
            preferences.edit().putString("scanHousesData", jsonObject.toString()).apply();
        } catch (Exception e) {
        }
    }

    private void removeCardLocalData() {
        try {
            wardJsonObject.remove(cardNo);
            jsonObject.put(preferences.getString("ward", ""), wardJsonObject);
            preferences.edit().putString("scanHousesData", jsonObject.toString()).apply();
        } catch (Exception e) {
        }
        position = position + 1;
        if (wardJsonObject.length() == 0) {
            showAlertBox();
        } else {
            sendData();
        }
    }

    private void checkAllDataSend(String message) {
        Log.d("TAG", "checkAllDataSend: check message " + message);
        try {
            if (tempObject.getString("StorageImage").equalsIgnoreCase("yes") && tempObject.getString("Houses").equalsIgnoreCase("yes") &&
                    tempObject.getString("CardWardMapping").equalsIgnoreCase("yes") && tempObject.getString("CardScanData").equalsIgnoreCase("yes") &&
                    tempObject.getString("EntityMarking").equalsIgnoreCase("yes") && tempObject.getString("DailyHouseCount").equalsIgnoreCase("yes") &&
                    tempObject.getString("TotalHouseCount").equalsIgnoreCase("yes") && tempObject.getString("SurveyDateWise").equalsIgnoreCase("yes") &&
                    tempObject.getString("SurveyStartDate").equalsIgnoreCase("yes")) {
                removeCardLocalData();
            }
        } catch (Exception e) {
        }
    }

    private void showAlertBox() {
        Log.d("TAG", "sendData: check data L " + tempObject + "   " + cardNo);
        common.closeDialog();
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("आपका सर्वे पूरा हुआ, धन्यवाद !");
        builder1.setCancelable(true);
        builder1.setPositiveButton("OK", (dialog, id) -> {
            dialog.cancel();
            finish();
        });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }
}