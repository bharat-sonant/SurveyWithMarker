package com.wevois.surveyapp.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class FormActivity extends AppCompatActivity {
    Button btnRevisitHouse, btnSaveRevisitReason;
    RadioGroup revisitRadioGroup;
    RadioButton awasiyeBtn, commercialBtn, awasiyeBtnCardRevisit, commercialBtnCardRevisit;
    EditText name, revisitName, address, colony, mobileNo, getTotalHouse;
    TextView tvRevisit;
    String mobileNumber = "", hT = "", markingKey = "";
    ArrayList<String> oldMobiles = new ArrayList<>(), newMobiles = new ArrayList<>();
    Spinner spinnerHouseType, spinnerRevisitReason, spinnerRevisitHouseType;
    SharedPreferences preferences;
    CommonFunctions common = new CommonFunctions();
    DatabaseReference databaseReferencePath;
    String currentDate, countCheck = "2", currentCardNumber;
    List<String> houseTypeListRevisit = new ArrayList<>();
    List<String> houseTypeList = new ArrayList<>();
    List<String> revisitTypeList = new ArrayList<>();
    boolean isValid = true, isDelete = false;
    JSONObject jsonObjectWard = new JSONObject();
    JSONArray jsonArrayHouseType = new JSONArray();
    JSONArray jsonArrayHouseTypeRevisit = new JSONArray();
    boolean isMoved = true;
    String storagePath = "";
    Bitmap identityBitmap = null;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private Camera mCamera;
    private static final int FOCUS_AREA_SIZE = 300;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    SurfaceHolder.Callback surfaceViewCallBack;
    Camera.PictureCallback pictureCallback;
    File myPath = null;
    JSONObject dataObject = new JSONObject();
    JSONObject jsonObject = new JSONObject();
    AlertDialog customTimerAlertBox, customTimerAlertBoxForImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initControls();
        databaseReferencePath = common.getDatabaseForApplication(this);
        storagePath = common.getDatabaseStorage(this);
        setDefaultValues();
        setActions();
        if (Integer.parseInt(hT) != 1 && Integer.parseInt(hT) != 19) {
            commercialBtn.setChecked(true);
            getHouseTypes(true);
        }
        try {
            for (int i = 0; i < jsonArrayHouseType.length(); i++) {
                if (jsonArrayHouseType.get(i).toString().equals(hT)) {
                    spinnerHouseType.setSelection(i + 1);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        fillSurveyDetailsIfAlreadyExists();
    }

    private void initControls() {
        revisitRadioGroup = findViewById(R.id.radioGroupCardRevisit);
        awasiyeBtn = findViewById(R.id.radio_awasiye);
        commercialBtn = findViewById(R.id.radio_com);
        awasiyeBtnCardRevisit = findViewById(R.id.radio_awasiyeCardRevisit);
        commercialBtnCardRevisit = findViewById(R.id.radio_comCardRevisit);
        revisitName = findViewById(R.id.revisitNameForm);
        name = findViewById(R.id.etName);
        mobileNo = findViewById(R.id.etMobile);
        getTotalHouse = findViewById(R.id.etTotalHouse);
        spinnerHouseType = findViewById(R.id.spnrHouseType);
        spinnerRevisitHouseType = findViewById(R.id.spnrHouseTypeCardRevisit);
        tvRevisit = findViewById(R.id.tvRevisitNote);
        address = findViewById(R.id.etAddress);
        colony = findViewById(R.id.etColonyName);
        spinnerRevisitReason = findViewById(R.id.spnrReason);
        btnRevisitHouse = findViewById(R.id.btnRevisit);
        btnSaveRevisitReason = findViewById(R.id.btnSaveReason);
    }

    private void setDefaultValues() {
        preferences = getSharedPreferences("surveyApp", MODE_PRIVATE);
        hT = preferences.getString("houseType", "");
        markingKey = preferences.getString("markingKey", "");
        SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd-MM-yyyy");
        currentDate = dateFormat1.format(new Date());
        getHouseTypes(false);
        getHouseTypesCardRevisit(false);
        getCardRevisitReasons();
    }

    @SuppressLint("StaticFieldLeak")
    private void setActions() {
        TextView backBtn = findViewById(R.id.BackBtn);
        backBtn.setOnClickListener(v -> {
            onBackPressed();
        });
        findViewById(R.id.btnSaveDetails).setOnClickListener(view1 -> {
            FormActivity.this.runOnUiThread(() -> {
                if (isMoved) {
                    isMoved = false;
                    new AsyncTask<Void, Void, Boolean>() {
                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                            common.setProgressBar("Please Wait...", FormActivity.this, FormActivity.this);
                        }

                        @Override
                        protected Boolean doInBackground(Void... p) {
                            return common.network(FormActivity.this);
                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            int i = 0;
                            if (result) {
                                i = 1;
                            } else {
                                i = 2;
                            }
                            if (validateSurveyForm()) {
                                String mobile = mobileNo.getText().toString();
                                String rfID = preferences.getString("rfid", "");
                                saveOfflineData(mobile, rfID, i);
                            } else {
                                isMoved = true;
                            }
                        }
                    }.execute();
                }
            });
        });
        btnRevisitHouse.setOnClickListener(view1 -> {
            revisitName.setVisibility(View.VISIBLE);
            spinnerRevisitReason.setVisibility(View.VISIBLE);
            btnSaveRevisitReason.setVisibility(View.VISIBLE);
            revisitRadioGroup.setVisibility(View.VISIBLE);
            spinnerRevisitHouseType.setVisibility(View.VISIBLE);
            tvRevisit.setVisibility(View.GONE);
            btnRevisitHouse.setVisibility(View.GONE);
        });
        btnSaveRevisitReason.setOnClickListener(view1 -> {
            sendRevisitData();
        });

        findViewById(R.id.button_image).setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                } else {
                    showAlertDialog();
                }
            }
        });

        awasiyeBtn.setOnClickListener(view12 -> {
            getHouseTypes(false);
        });
        commercialBtn.setOnClickListener(view13 -> {
            getHouseTypes(true);
        });
        awasiyeBtnCardRevisit.setOnClickListener(view12 -> {
            getHouseTypesCardRevisit(false);
        });
        commercialBtnCardRevisit.setOnClickListener(view13 -> {
            getHouseTypesCardRevisit(true);
        });
    }

    private void fillSurveyDetailsIfAlreadyExists() {
        FormActivity.this.runOnUiThread(() -> {
            common.setProgressBar("Please Wait...", this, this);
            currentCardNumber = preferences.getString("cardNo", "");
            databaseReferencePath.child("CardWardMapping/" + currentCardNumber).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        if (dataSnapshot.hasChild("ward") && dataSnapshot.hasChild("line")) {
                            databaseReferencePath.child("Houses/" + dataSnapshot.child("ward").getValue().toString() + "/" + dataSnapshot.child("line").getValue().toString()).orderByChild("cardNo").equalTo(currentCardNumber).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot1) {
                                    common.closeDialog();
                                    if (dataSnapshot1.getValue() != null) {
                                        for (DataSnapshot snapshot : dataSnapshot1.getChildren()) {
                                            if (snapshot.child("name").getValue() != null && snapshot.child("name").getValue().toString().length() > 0)
                                                name.setText(snapshot.child("name").getValue().toString());
                                            if (snapshot.child("houseType").getValue() != null && snapshot.child("houseType").getValue().toString().length() > 0) {
                                                if (Integer.parseInt(snapshot.child("houseType").getValue().toString()) != 1 && Integer.parseInt(snapshot.child("houseType").getValue().toString()) != 19) {
                                                    commercialBtn.setChecked(true);
                                                    getHouseTypes(true);
                                                }
                                                try {
                                                    for (int i = 0; i < jsonArrayHouseType.length(); i++) {
                                                        if (jsonArrayHouseType.get(i).toString().equals(snapshot.child("houseType").getValue().toString())) {
                                                            spinnerHouseType.setSelection(i + 1);
                                                        }
                                                    }
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            if (snapshot.child("colonyName").getValue() != null && snapshot.child("colonyName").getValue().toString().length() > 0)
                                                colony.setText(snapshot.child("colonyName").getValue().toString());
                                            if (snapshot.child("address").getValue() != null && snapshot.child("address").getValue().toString().length() > 0)
                                                address.setText(snapshot.child("address").getValue().toString());
                                            if (snapshot.child("servingCount").getValue() != null && snapshot.child("servingCount").getValue().toString().length() > 0)
                                                getTotalHouse.setText(snapshot.child("servingCount").getValue().toString());
                                            if (snapshot.child("mobile").getValue() != null && snapshot.child("mobile").getValue().toString().length() > 0) {
                                                mobileNumber = snapshot.child("mobile").getValue().toString();
                                                if (mobileNumber.contains(",")) {
                                                    String[] mobile = mobileNumber.trim().split(",");
                                                    for (int i = 0; i < mobile.length; i++) {
                                                        oldMobiles.add(mobile[i].trim());
                                                    }
                                                } else {
                                                    oldMobiles.add(mobileNumber);
                                                }
                                                mobileNo.setText(mobileNumber);
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                }
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
            Query query = databaseReferencePath.child("Houses/" + preferences.getString("ward", "") + "/" + preferences.getString("line", "")).orderByChild("rfid").equalTo(preferences.getString("rfid", ""));
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        countCheck = "1";
                    } else {
                        countCheck = "2";
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
            common.closeDialog();
        });
    }

    private boolean validateSurveyForm() {
        FormActivity.this.runOnUiThread(() -> {
            if (name.getText().toString().length() == 0) {
                name.setError("Please enter name");
                name.requestFocus();
                common.closeDialog();
                isValid = false;
            } else if (spinnerHouseType.getSelectedItem().toString().equals("Select Entity type")) {
                View selectedView = spinnerHouseType.getSelectedView();
                if (selectedView != null && selectedView instanceof TextView) {
                    spinnerHouseType.requestFocus();
                    TextView selectedTextView = (TextView) selectedView;
                    selectedTextView.setError("error");
                    selectedTextView.setTextColor(Color.RED);
                    selectedTextView.setText("please select type");
                    spinnerHouseType.performClick();
                }
                common.closeDialog();
                isValid = false;
            } else if (getTotalHouse.getVisibility() == View.VISIBLE && getTotalHouse.getText().toString().length() == 0) {
                getTotalHouse.setError("Please fill");
                getTotalHouse.requestFocus();
                isValid = false;
                common.closeDialog();
            } else if (address.getText().toString().length() == 0) {
                address.setError("Please enter address");
                address.requestFocus();
                common.closeDialog();
                isValid = false;
            } else if (colony.getText().toString().length() == 0) {
                colony.setError("Please enter colony name");
                colony.requestFocus();
                common.closeDialog();
                isValid = false;
            } else if (identityBitmap == null) {
                common.showAlertBox("कृपया पहले फोटो खींचे .", false, this);
                isValid = false;
            } else {
                newMobiles = new ArrayList<>();
                if (mobileNo.getText().toString().contains(",")) {
                    String[] mobile = mobileNo.getText().toString().trim().split(",");
                    for (int i = 0; i < mobile.length; i++) {
                        if (mobile[i].trim().length() != 10) {
                            mobileNo.setError("Please enter correct mobile number");
                            mobileNo.requestFocus();
                            common.closeDialog();
                            isValid = false;
                            break;
                        } else {
                            newMobiles.add(mobile[i].trim());
                            isValid = true;
                        }
                    }
                } else {
                    if (mobileNo.getText().toString().trim().length() != 10) {
                        mobileNo.setError("Please enter correct mobile number");
                        mobileNo.requestFocus();
                        common.closeDialog();
                        isValid = false;
                    } else {
                        newMobiles.add(mobileNo.getText().toString().trim());
                        isValid = true;
                    }
                }
            }
        });
        return isValid;
    }

    private void saveOfflineData(String mobile, String rfID, int i) {
        FormActivity.this.runOnUiThread(() -> {
            SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String line = preferences.getString("line", "");
            String ward = preferences.getString("ward", "");
            String cardNo = preferences.getString("cardNo", "");
            try {
                if (preferences.getString("scanHousesData", "").length() > 0) {
                    try {
                        dataObject = new JSONObject(preferences.getString("scanHousesData", "")).getJSONObject(ward);
                    } catch (Exception ignored) {
                    }
                }
                jsonObject.put("mobile", mobile);
                jsonObject.put("ward", ward);
                jsonObject.put("address", address.getText().toString());
                jsonObject.put("cardno", cardNo);
                jsonObject.put("colonyname", colony.getText().toString());
                jsonObject.put("createddate", timeFormat.format(new Date()));
                jsonObject.put("housetype", jsonArrayHouseType.get(spinnerHouseType.getSelectedItemPosition() - 1).toString());
                jsonObject.put("lat", preferences.getString("lat", ""));
                jsonObject.put("lng", preferences.getString("lng", ""));
                jsonObject.put("line", line);
                jsonObject.put("name", name.getText().toString());
                jsonObject.put("rfid", rfID);
                jsonObject.put("markingKey", markingKey);
                JSONObject temp = new JSONObject();
                temp.put("StorageImage", "no");
                temp.put("Houses", "no");
                temp.put("CardWardMapping", "no");
                temp.put("CardScanData", "no");
                temp.put("EntityMarking", "no");
                temp.put("DailyHouseCount", "no");
                temp.put("TotalHouseCount", "no");
                temp.put("SurveyDateWise", "no");
                temp.put("SurveyStartDate", "no");
                jsonObject.put("details", temp);
                if (awasiyeBtn.isChecked()) {
                    jsonObject.put("cardType", "आवासीय");
                } else {
                    jsonObject.put("cardType", "व्यावसायिक");
                }
                if (getTotalHouse.getVisibility() == View.VISIBLE) {
                    jsonObject.put("servingcount", getTotalHouse.getText().toString());
                }
                dataObject.put(cardNo, jsonObject);
                jsonObjectWard.put(ward, dataObject);
                preferences.edit().putString("scanHousesData", jsonObjectWard.toString()).apply();
                if (i == 1) {
                    saveSurveyDetails();
                } else {
                    showAlertBox("आपका सर्वे पूरा हुआ, धन्यवाद |", true);
                }
            } catch (Exception e) {
            }
        });
    }

    private void saveSurveyDetails() {
        FormActivity.this.runOnUiThread(() -> {
            if (validateSurveyForm()) {
                databaseReferencePath.child("CardWardMapping/" + currentCardNumber).addListenerForSingleValueEvent(new ValueEventListener() {
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
                                saveSurveyData();
                            } else {
                                isMoved = true;
                                removeCardLocalData();
                                showAlertBox("Error", true);
                            }
                        } else {
                            saveSurveyData();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            } else {
                isMoved = true;
                common.closeDialog();
            }
        });
    }

    private void saveSurveyData() {
        if (countCheck.equals("2")) {
            FormActivity.this.runOnUiThread(() -> {
                saveDailyHouseCountData();
            });
            FormActivity.this.runOnUiThread(() -> {
                saveTotalHouseCountData();
            });
            FormActivity.this.runOnUiThread(() -> {
                saveSurveyDateWiseData();
            });
        } else {
            removeLocalData("DailyHouseCount");
            removeLocalData("TotalHouseCount");
            removeLocalData("SurveyDateWise");
        }
        FormActivity.this.runOnUiThread(() -> {
            saveImageData();
        });
        FormActivity.this.runOnUiThread(() -> {
            saveHousesData();
        });
        FormActivity.this.runOnUiThread(() -> {
            saveCardWardMappingData();
        });
        FormActivity.this.runOnUiThread(() -> {
            saveCardScanDataData();
        });
        FormActivity.this.runOnUiThread(() -> {
            saveEntityMarkingData();
        });
        FormActivity.this.runOnUiThread(() -> {
            saveSurveyStartDateData();
        });
    }

    @SuppressLint("StaticFieldLeak")
    private void saveImageData() {
        FormActivity.this.runOnUiThread(() -> {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                }

                @Override
                protected Boolean doInBackground(Void... p) {
                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference storageRef = storage.getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + storagePath + "/SurveyCardImage/" + preferences.getString("ward", "") + "/" + preferences.getString("line", ""));
                    StorageReference mountainImagesRef = storageRef.child(currentCardNumber + ".jpg");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    identityBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                    byte[] data = baos.toByteArray();
                    UploadTask uploadTask = mountainImagesRef.putBytes(data);
                    uploadTask.addOnFailureListener(exception -> {
                        common.closeDialog();
                    }).addOnSuccessListener(taskSnapshot -> {
                        try {
                            if (myPath != null) {
                                myPath.delete();
                            }
                        } catch (Exception e) {
                        }
                        removeLocalData("StorageImage");
                        checkAllDataSend("image");
                    });
                    return null;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                }
            }.execute();
        });
    }

    private void saveHousesData() {
        FormActivity.this.runOnUiThread(() -> {
            SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String mobile = mobileNo.getText().toString();
            String lastCharacterOfNumber = mobile.substring(mobile.length() - 1);
            if (lastCharacterOfNumber.equalsIgnoreCase(",")) {
                mobile = mobile.substring(0, mobile.length() - 1);
            }
            HashMap<String, Object> housesMap = new HashMap<>();
            housesMap.put("address", address.getText().toString());
            housesMap.put("cardNo", currentCardNumber);
            housesMap.put("phaseNo", "2");
            housesMap.put("colonyName", colony.getText().toString());
            if (countCheck.equals("2")) {
                housesMap.put("createdDate", timeFormat.format(new Date()));
                housesMap.put("surveyorId", preferences.getString("userId", ""));
            } else {
                if (isDelete) {
                    housesMap.put("createdDate", timeFormat.format(new Date()));
                    housesMap.put("surveyorId", preferences.getString("userId", ""));
                }
                housesMap.put("surveyModifierId", preferences.getString("userId", ""));
                housesMap.put("modifiedDate", timeFormat.format(new Date()));
            }
            try {
                housesMap.put("houseType", jsonArrayHouseType.get(spinnerHouseType.getSelectedItemPosition() - 1).toString());
            } catch (JSONException e) {
            }
            housesMap.put("latLng", "(" + preferences.getString("lat", "") + "," + preferences.getString("lng", "") + ")");
            housesMap.put("line", preferences.getString("line", ""));
            housesMap.put("name", name.getText().toString());
            housesMap.put("mobile", mobile);
            if (awasiyeBtn.isChecked()) {
                housesMap.put("cardType", "आवासीय");
            } else {
                housesMap.put("cardType", "व्यावसायिक");
            }
            housesMap.put("rfid", preferences.getString("rfid", ""));
            housesMap.put("ward", preferences.getString("ward", ""));
            housesMap.put("cardImage", currentCardNumber + ".jpg");
            if (getTotalHouse.getVisibility() == View.VISIBLE) {
                housesMap.put("servingCount", getTotalHouse.getText().toString());
            }
            databaseReferencePath.child("Houses/" + preferences.getString("ward", "") + "/" + preferences.getString("line", "") + "/" + currentCardNumber).updateChildren(housesMap).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    removeLocalData("Houses");
                    checkAllDataSend("Houses");
                }
            });
        });
    }

    private void saveCardWardMappingData() {
        FormActivity.this.runOnUiThread(() -> {
            for (int i = 0; i < newMobiles.size(); i++) {
                DatabaseReference houseWardMapPath = databaseReferencePath.child("HouseWardMapping/" + newMobiles.get(i));
                houseWardMapPath.child("line").setValue(preferences.getString("line", ""));
                houseWardMapPath.child("ward").setValue(preferences.getString("ward", ""));
            }
            HashMap<String, Object> houseWardMapping = new HashMap<>();
            houseWardMapping.put("line", preferences.getString("line", ""));
            houseWardMapping.put("ward", preferences.getString("ward", ""));
            databaseReferencePath.child("CardWardMapping/" + currentCardNumber).updateChildren(houseWardMapping).addOnCompleteListener(task1 -> {
                if (task1.isSuccessful()) {
                    removeLocalData("CardWardMapping");
                    checkAllDataSend("cardWard");
                }
            });
        });
    }

    private void saveCardScanDataData() {
        FormActivity.this.runOnUiThread(() -> {
            databaseReferencePath.child("CardScanData/" + preferences.getString("rfid", "")).child("cardInstalled").setValue("yes").addOnCompleteListener(task11 -> {
                if (task11.isSuccessful()) {
                    removeLocalData("CardScanData");
                    checkAllDataSend("cardScan");
                }
            });
        });
    }

    private void saveEntityMarkingData() {
        FormActivity.this.runOnUiThread(() -> {
            HashMap<String, Object> datas = new HashMap<>();
            datas.put("cardNumber", currentCardNumber);
            datas.put("isSurveyed", "yes");
            databaseReferencePath.child("EntityMarkingData/MarkedHouses/" + preferences.getString("ward", "") + "/" + preferences.getString("line", "") + "/" + markingKey).updateChildren(datas).addOnCompleteListener(task111 -> {
                if (task111.isSuccessful()) {
                    removeLocalData("EntityMarking");
                    checkAllDataSend("entityMarking");
                }
            });
        });
    }

    private void saveDailyHouseCountData() {
        FormActivity.this.runOnUiThread(() -> {
            String wardNo = preferences.getString("ward", "");
            String userId = preferences.getString("userId", "");
            databaseReferencePath.child("EntitySurveyData/DailyHouseCount/" + wardNo + "/" + userId + "/" + currentDate).runTransaction(new Transaction.Handler() {
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
                        checkAllDataSend("dailyHouses");
                    }
                }
            });
            removeLocalData("DailyHouseCount");
        });
    }

    private void saveTotalHouseCountData() {
        FormActivity.this.runOnUiThread(() -> {
            databaseReferencePath.child("EntitySurveyData/TotalHouseCount/" + preferences.getString("ward", "")).runTransaction(new Transaction.Handler() {
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
                        checkAllDataSend("totalHouses");
                    }
                }
            });
            removeLocalData("TotalHouseCount");
        });
    }

    private void saveSurveyDateWiseData() {
        FormActivity.this.runOnUiThread(() -> {
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
                            checkAllDataSend("DateWise");
                        }
                    });
                    removeLocalData("SurveyDateWise");
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        });
    }

    private void saveSurveyStartDateData() {
        FormActivity.this.runOnUiThread(() -> {
            String wardNo = preferences.getString("ward", "");
            String userId = preferences.getString("userId", "");
            databaseReferencePath.child("EntitySurveyData/SurveyStartDate/" + wardNo + "/" + userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() == null) {
                        databaseReferencePath.child("EntitySurveyData/SurveyStartDate/" + wardNo + "/" + userId).setValue(currentDate).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(Task<Void> task) {
                                if (task.isSuccessful()) {
                                    removeLocalData("SurveyStartDate");
                                    checkAllDataSend("startDate");
                                }
                            }
                        });
                    } else {
                        removeLocalData("SurveyStartDate");
                        checkAllDataSend("startDate");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        });
    }

    private void checkAllDataSend(String message) {
        try {
            JSONObject tempData = jsonObject.getJSONObject("details");
            if (tempData.getString("StorageImage").equalsIgnoreCase("yes") && tempData.getString("Houses").equalsIgnoreCase("yes") &&
                    tempData.getString("CardWardMapping").equalsIgnoreCase("yes") && tempData.getString("CardScanData").equalsIgnoreCase("yes") &&
                    tempData.getString("EntityMarking").equalsIgnoreCase("yes") && tempData.getString("DailyHouseCount").equalsIgnoreCase("yes") &&
                    tempData.getString("TotalHouseCount").equalsIgnoreCase("yes") && tempData.getString("SurveyDateWise").equalsIgnoreCase("yes") &&
                    tempData.getString("SurveyStartDate").equalsIgnoreCase("yes")) {
                removeCardLocalData();
                showAlertBox("आपका सर्वे पूरा हुआ, धन्यवाद |", true);
            }
        } catch (Exception e) {
        }
    }

    private void bindHouseTypesToSpinner() {
        FormActivity.this.runOnUiThread(() -> {
            final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, houseTypeList) {
                @Override
                public boolean isEnabled(int position) {
                    return !(position == 0);
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View view = super.getDropDownView(position, convertView, parent);
                    TextView tv = (TextView) view;
                    tv.setTextColor(position == 0 ? Color.GRAY : Color.BLACK);
                    return view;
                }
            };
            spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerHouseType.setAdapter(spinnerArrayAdapter);
            spinnerHouseType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {

                    String hintText = "";
                    boolean isVisible = false;
                    try {
                        switch (Integer.parseInt(jsonArrayHouseType.get(position - 1).toString())) {
                            case 19:
                                hintText = "Enter No of Houses";
                                isVisible = true;
                                break;
                            case 20:
                                hintText = "Enter No of Shops";
                                isVisible = true;
                                break;
                        }
                    } catch (JSONException e) {
                    }

                    getTotalHouse.setVisibility(isVisible ? View.VISIBLE : View.GONE);
                    getTotalHouse.setHint(hintText);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        });
    }

    private void getCardRevisitReasons() {
        FormActivity.this.runOnUiThread(() -> {
            revisitTypeList.add("Select Reason type");
            String listAsString = preferences.getString("revisitReasonList", null);
            String[] reasonString = listAsString.substring(1, listAsString.length() - 1).split(",");
            for (int i = 0; i < reasonString.length; i++) {
                String reasonType = reasonString[i].replace("~", ",");
                revisitTypeList.add(reasonType);
            }
            final ArrayAdapter<String> spinnerArrayAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, revisitTypeList) {
                @Override
                public boolean isEnabled(int position) {
                    return !(position == 0);
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View view = super.getDropDownView(position, convertView, parent);
                    TextView tv = (TextView) view;
                    tv.setTextColor(position == 0 ? Color.GRAY : Color.BLACK);
                    return view;
                }
            };
            spinnerArrayAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRevisitReason.setAdapter(spinnerArrayAdapter1);
        });
    }

    private void getHouseTypes(Boolean isCommercial) {
        FormActivity.this.runOnUiThread(() -> {
            houseTypeList.clear();
            houseTypeList.add("Select Entity type");
            JSONObject jsonObject, commercialJsonObject, residentialJsonObject;
            jsonArrayHouseType = new JSONArray();
            try {
                jsonObject = new JSONObject(preferences.getString("housesTypeList", ""));
                commercialJsonObject = new JSONObject(preferences.getString("commercialHousesTypeList", ""));
                residentialJsonObject = new JSONObject(preferences.getString("residentialHousesTypeList", ""));
                for (int i = 1; i <= jsonObject.length(); i++) {
                    if (isCommercial) {
                        try {
                            houseTypeList.add(commercialJsonObject.getString(String.valueOf(i)));
                            jsonArrayHouseType.put(i);
                        } catch (JSONException e) {
                        }
                    } else {
                        try {
                            houseTypeList.add(residentialJsonObject.getString(String.valueOf(i)));
                            jsonArrayHouseType.put(i);
                        } catch (JSONException e) {
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            bindHouseTypesToSpinner();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (isMoved) {
            try {
                if (myPath != null) {
                    myPath.delete();
                }
            } catch (Exception e) {
            }
            isMoved = false;
            Intent intent = new Intent(FormActivity.this, VerifyActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isMoved = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        FormActivity.this.runOnUiThread(() -> {
            if (requestCode == MY_CAMERA_PERMISSION_CODE) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showAlertDialog();
                } else {
                    Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void focusOnTouch(MotionEvent event) throws Exception {
        if (mCamera != null) {
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                if (parameters.getMaxNumMeteringAreas() > 0) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    Rect rect = calculateFocusArea(event.getX(), event.getY());
                    List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                    meteringAreas.add(new Camera.Area(rect, 800));
                    parameters.setFocusAreas(meteringAreas);
                    mCamera.setParameters(parameters);
                }
                mCamera.autoFocus((success, camera) -> {

                });

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private Rect calculateFocusArea(float x, float y) {
        int left = clamp(Float.valueOf((x / surfaceView.getWidth()) * 2000 - 1000).intValue());
        int top = clamp(Float.valueOf((y / surfaceView.getHeight()) * 2000 - 1000).intValue());

        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    private int clamp(int touchCoordinateInCameraReper) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper) + FormActivity.FOCUS_AREA_SIZE / 2 > 1000) {
            if (touchCoordinateInCameraReper > 0) {
                result = 1000 - FormActivity.FOCUS_AREA_SIZE / 2;
            } else {
                result = -1000 + FormActivity.FOCUS_AREA_SIZE / 2;
            }
        } else {
            result = touchCoordinateInCameraReper - FormActivity.FOCUS_AREA_SIZE / 2;
        }
        return result;
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @SuppressLint("StaticFieldLeak")
    public void showAlertDialog() {
        FormActivity.this.runOnUiThread(() -> {
            try {
                if (customTimerAlertBox != null) {
                    customTimerAlertBox.dismiss();
                }
            } catch (Exception e) {
            }
            LayoutInflater inflater = getLayoutInflater();
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(FormActivity.this);
            View dialogLayout = inflater.inflate(R.layout.custom_camera_alertbox, null);
            alertDialog.setView(dialogLayout);
            alertDialog.setCancelable(false);
            customTimerAlertBox = alertDialog.create();
            surfaceView = (SurfaceView) dialogLayout.findViewById(R.id.surfaceViews);
            surfaceHolder = surfaceView.getHolder();
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_HARDWARE);
            surfaceViewCallBack = new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    try {
                        mCamera = Camera.open();
                    } catch (RuntimeException e) {
                    }
                    Camera.Parameters parameters;
                    parameters = mCamera.getParameters();
                    List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
                    parameters.setPictureSize(sizes.get(0).width, sizes.get(0).height);
                    mCamera.setParameters(parameters);
                    setCameraDisplayOrientation(FormActivity.this, 0, mCamera);
                    try {
                        mCamera.setPreviewDisplay(surfaceHolder);
                        mCamera.startPreview();
                    } catch (Exception e) {
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

                }
            };
            surfaceHolder.addCallback(surfaceViewCallBack);
            Button btn = dialogLayout.findViewById(R.id.capture_image_btn);
            btn.setOnClickListener(v -> {
                common.setProgressBar("Processing...", FormActivity.this, FormActivity.this);
                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                    }

                    @Override
                    protected Boolean doInBackground(Void... p) {
                        mCamera.takePicture(null, null, null, pictureCallback);
                        return null;
                    }
                }.execute();
            });
            Button closeBtn = dialogLayout.findViewById(R.id.close_image_btn);
            closeBtn.setOnClickListener(v -> {
                try {
                    if (customTimerAlertBox != null) {
                        customTimerAlertBox.dismiss();
                    }
                } catch (Exception e) {
                }
            });
            if (!isFinishing()) {
                customTimerAlertBox.show();
            }
            pictureCallback = (bytes, camera) -> {
                Matrix matrix = new Matrix();
                matrix.postRotate(90F);
                Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                Bitmap bitmaps = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                Bitmap bitmap = Bitmap.createScaledBitmap(bitmaps, 400, 600, false);

                camera.stopPreview();
                if (camera != null) {
                    camera.release();
                    mCamera = null;
                }
                try {
                    if (customTimerAlertBox != null) {
                        customTimerAlertBox.dismiss();
                    }
                } catch (Exception e) {
                }
                showAlertBoxForImage(bitmap);
            };
            surfaceView.setOnTouchListener((view, motionEvent) -> {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    try {
                        focusOnTouch(motionEvent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            });
        });
    }

    private void showAlertBoxForImage(Bitmap i) {
        try {
            if (customTimerAlertBoxForImage != null) {
                customTimerAlertBoxForImage.dismiss();
            }
        } catch (Exception e) {
        }
        LayoutInflater inflater = getLayoutInflater();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(FormActivity.this);
        View dialogLayout = inflater.inflate(R.layout.image_view_layout, null);
        alertDialog.setView(dialogLayout);
        alertDialog.setCancelable(false);
        ImageView markerImage = dialogLayout.findViewById(R.id.marker_iv);
        if (i != null) {
            markerImage.setImageBitmap(i);
        }
        dialogLayout.findViewById(R.id.okeyBtn).setOnClickListener(view1 -> {
            common.setProgressBar("Processing...", FormActivity.this, FormActivity.this);
            if (customTimerAlertBoxForImage != null) {
                customTimerAlertBoxForImage.dismiss();
            }
            identityBitmap = i;
            setOnLocal();
        });
        Button closeBtn = dialogLayout.findViewById(R.id.close_view_btn);
        closeBtn.setOnClickListener(view1 -> {
            common.closeDialog();
            if (customTimerAlertBoxForImage != null) {
                customTimerAlertBoxForImage.dismiss();
            }
            isMoved = true;
        });
        customTimerAlertBoxForImage = alertDialog.create();
        if (!isFinishing()) {
            customTimerAlertBoxForImage.show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void setOnLocal() {
        FormActivity.this.runOnUiThread(() -> {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                }

                @Override
                protected Boolean doInBackground(Void... p) {
                    File root = new File(Environment.getExternalStorageDirectory(), "SurveyCardImage");
                    if (!root.exists()) {
                        root.mkdirs();
                    }
                    myPath = new File(root, currentCardNumber + ".jpg");
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(myPath);
                        identityBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    common.closeDialog();
                    return null;
                }
            }.execute();
        });
    }

    private void removeLocalData(String data) {
        try {
            JSONObject tempData = jsonObject.getJSONObject("details");
            tempData.put(data, "yes");
            jsonObject.put("details", tempData);
            dataObject.put(currentCardNumber, jsonObject);
            jsonObjectWard.put(preferences.getString("ward", ""), dataObject);
            preferences.edit().putString("scanHousesData", jsonObjectWard.toString()).apply();
        } catch (Exception e) {
        }
    }

    private void removeCardLocalData() {
        try {
            dataObject.remove(currentCardNumber);
            jsonObjectWard.put(preferences.getString("ward", ""), dataObject);
            preferences.edit().putString("scanHousesData", jsonObjectWard.toString()).apply();
        } catch (Exception e) {
        }
    }

    private void getHouseTypesCardRevisit(Boolean isCommercial) {
        FormActivity.this.runOnUiThread(() -> {
            houseTypeListRevisit.clear();
            houseTypeListRevisit.add("Select Entity type");
            JSONObject jsonObject, commercialJsonObject, residentialJsonObject;
            jsonArrayHouseTypeRevisit = new JSONArray();
            try {
                jsonObject = new JSONObject(preferences.getString("housesTypeList", ""));
                commercialJsonObject = new JSONObject(preferences.getString("commercialHousesTypeList", ""));
                residentialJsonObject = new JSONObject(preferences.getString("residentialHousesTypeList", ""));
                for (int i = 1; i <= jsonObject.length(); i++) {
                    if (isCommercial) {
                        try {
                            houseTypeListRevisit.add(commercialJsonObject.getString(String.valueOf(i)));
                            jsonArrayHouseTypeRevisit.put(i);
                        } catch (JSONException e) {
                        }
                    } else {
                        try {
                            houseTypeListRevisit.add(residentialJsonObject.getString(String.valueOf(i)));
                            jsonArrayHouseTypeRevisit.put(i);
                        } catch (JSONException e) {
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            bindHouseTypesToSpinnerCardRevisit();
        });
    }

    private void bindHouseTypesToSpinnerCardRevisit() {
        FormActivity.this.runOnUiThread(() -> {
            final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, houseTypeListRevisit) {
                @Override
                public boolean isEnabled(int position) {
                    return !(position == 0);
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View view = super.getDropDownView(position, convertView, parent);
                    TextView tv = (TextView) view;
                    tv.setTextColor(position == 0 ? Color.GRAY : Color.BLACK);
                    return view;
                }
            };
            spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRevisitHouseType.setAdapter(spinnerArrayAdapter);
        });
    }

    private void sendRevisitData() {
        FormActivity.this.runOnUiThread(() -> {
            if (revisitName.getText().toString().trim().length() == 0) {
                revisitName.setError("Please enter name");
                revisitName.requestFocus();
            } else if (spinnerRevisitReason.getSelectedItem().toString().equals("Select Reason type")) {
                common.closeDialog();
                View selectedView = spinnerRevisitReason.getSelectedView();
                if (selectedView != null && selectedView instanceof TextView) {
                    spinnerRevisitReason.requestFocus();
                    TextView selectedTextView = (TextView) selectedView;
                    selectedTextView.setError("error");
                    selectedTextView.setTextColor(Color.RED);
                    selectedTextView.setText("please select type");
                    spinnerRevisitReason.performClick();
                }
                return;
            } else if (spinnerRevisitHouseType.getSelectedItem().toString().equals("Select Entity type")) {
                View selectedView = spinnerRevisitHouseType.getSelectedView();
                if (selectedView != null && selectedView instanceof TextView) {
                    spinnerRevisitHouseType.requestFocus();
                    TextView selectedTextView = (TextView) selectedView;
                    selectedTextView.setError("error");
                    selectedTextView.setTextColor(Color.RED);
                    selectedTextView.setText("please select type");
                    spinnerRevisitHouseType.performClick();
                }
                return;
            } else {
                common.setProgressBar("Please Wait...", this, this);
                databaseReferencePath.child("EntitySurveyData").child("RevisitRequest/" + preferences.getString("ward", "") + "/" + preferences.getString("line", "") + "/lineRevisitCount").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int count = 1;
                        if (dataSnapshot.getValue() != null) {
                            count = Integer.parseInt(dataSnapshot.getValue().toString()) + 1;
                        }
                        databaseReferencePath.child("EntitySurveyData").child("RevisitRequest/" + preferences.getString("ward", "") + "/" + preferences.getString("line", "") + "/lineRevisitCount").setValue("" + count);
                        try {
                            HashMap<String, String> data = new HashMap<>();
                            data.put("lat", preferences.getString("lat", ""));
                            data.put("lng", preferences.getString("lng", ""));
                            data.put("reason", spinnerRevisitReason.getSelectedItem().toString());
                            data.put("houseType", jsonArrayHouseTypeRevisit.get(spinnerRevisitHouseType.getSelectedItemPosition() - 1).toString());
                            data.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                            data.put("id", preferences.getString("userId", ""));
                            data.put("revisitedBy", "Surveyor");
                            data.put("name", revisitName.getText().toString());
                            databaseReferencePath.child("EntitySurveyData").child("RevisitRequest").child(preferences.getString("ward", "")).child(preferences.getString("line", "")).child(preferences.getString("cardNo", "")).setValue(data);
                            dailyRevisitRequestCount();
                            totalRevisitRequest();
                            showAlertBox("आपका सर्वे पूरा हुआ, धन्यवाद !", true);
                        } catch (Exception e) {
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        });
    }

    private void totalRevisitRequest() {
        FormActivity.this.runOnUiThread(() -> {
            String ward = preferences.getString("ward", "");
            databaseReferencePath.child("EntitySurveyData/TotalRevisitRequest/" + ward).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        String count = String.valueOf(Integer.parseInt(dataSnapshot.getValue().toString()) + 1);
                        databaseReferencePath.child("EntitySurveyData/TotalRevisitRequest/" + ward).setValue(count);
                    } else {
                        databaseReferencePath.child("EntitySurveyData/TotalRevisitRequest/" + ward).setValue("1");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        });
    }

    private void dailyRevisitRequestCount() {
        FormActivity.this.runOnUiThread(() -> {
            String ward = preferences.getString("ward", "");
            String userId = preferences.getString("userId", "");
            databaseReferencePath.child("EntitySurveyData/DailyRevisitRequestCount/" + ward + "/" + userId + "/" + currentDate).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        String count = String.valueOf(Integer.parseInt(dataSnapshot.getValue().toString()) + 1);
                        databaseReferencePath.child("EntitySurveyData/DailyRevisitRequestCount/" + ward + "/" + userId + "/" + currentDate).setValue(count);
                    } else {
                        databaseReferencePath.child("EntitySurveyData/DailyRevisitRequestCount/" + ward + "/" + userId + "/" + currentDate).setValue("1");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        });
    }

    private void showAlertBox(String message, boolean surveyCompleted) {
        FormActivity.this.runOnUiThread(() -> {
            common.closeDialog();
            AlertDialog.Builder alertAssignment = new AlertDialog.Builder(this);
            alertAssignment.setMessage(message);
            alertAssignment.setCancelable(false);
            alertAssignment.setPositiveButton("OK", (dialog, id) -> {
                if (surveyCompleted) {
                    finish();
                }
            });
            AlertDialog alertDAssignment = alertAssignment.create();
            if (!isFinishing()) {
                alertDAssignment.show();
            }
        });
    }
}