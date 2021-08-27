package com.wevois.surveyapp.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

import static com.google.android.gms.internal.zzagr.runOnUiThread;

public class ScanCardActivity extends AppCompatActivity {
    SurfaceView mCameraView;
    CameraSource mCameraSource;
    ProgressBar progressBar;
    LinearLayout linearLayout, cardPhotoLinearLayout;
    int lineNumber = 1;
    private final static int REQUEST_CHECK_SETTINGS = 500, requestPermissionID = 1888;
    Button scanningBtn;
    CountDownTimer countDownTimer;
    String rfID, houseType = "", markingKey = "", markingCard = "",markingData="";

    CommonFunctions common = new CommonFunctions();
    TextView scanTv, progressPercentage;
    SharedPreferences preferences;
    DatabaseReference databaseReferencePath;
    private LocationCallback locationCallback;
    boolean isCardMatched = true;
    LatLng currentLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_card);
        initMethod();
        setAction();
    }

    private void initMethod() {
        databaseReferencePath = common.getDatabaseForApplication(ScanCardActivity.this);
        preferences = getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
        lineNumber = getIntent().getIntExtra("line", 0);
        houseType = getIntent().getStringExtra("houseType");
        markingKey = getIntent().getStringExtra("markingKey");
        markingCard = getIntent().getStringExtra("markingCard");
        markingData = getIntent().getStringExtra("markingData");
        progressBar = findViewById(R.id.progressBars);
        progressPercentage = findViewById(R.id.progressPercentage);
        mCameraView = findViewById(R.id.surfaceView);
        scanTv = findViewById(R.id.scanTv);
        scanningBtn = findViewById(R.id.scanningButton);
        linearLayout = findViewById(R.id.scanLinearLayout);
        cardPhotoLinearLayout = findViewById(R.id.cardPhotoLayout);
    }

    private void setAction() {
        TextView backBtn = findViewById(R.id.BackBtn);
        backBtn.setOnClickListener(v -> {
            onBackPressed();
        });
        startScanning();
        scanningBtn.setOnClickListener(view1 -> {
            startScanning();
        });
    }

    private void startScanning() {
        isCardMatched = true;
        linearLayout.setVisibility(View.VISIBLE);
        mCameraView.setVisibility(View.VISIBLE);
        cardPhotoLinearLayout.setVisibility(View.GONE);
        startCameraSource();
        countDownTimer = new CountDownTimer(preferences.getInt("cardScanningTime", 10000), 500) {
            public void onTick(long millisUntilFinished) {
                int progress = preferences.getInt("cardScanningTime", 10000) - (int) millisUntilFinished;
                progressBar.setProgress(((progress * 100) / preferences.getInt("cardScanningTime", 10000)));
                progressPercentage.setText("Scanning progress : " + ((progress * 100) / preferences.getInt("cardScanningTime", 10000)) + "%");
            }

            @SuppressLint("SetTextI18n")
            public void onFinish() {
                progressBar.setProgress(100);
                progressPercentage.setText("100%");
                linearLayout.setVisibility(View.GONE);
                if (mCameraSource!=null)
                    mCameraSource.stop();
                mCameraView.setVisibility(View.GONE);
                cardPhotoLinearLayout.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode != RESULT_OK) {
                checkWhetherLocationSettingsAreSatisfied();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void startCameraSource() {
        ScanCardActivity.this.runOnUiThread(() -> {
            final TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();
            if (!textRecognizer.isOperational()) {
                common.showAlertBox(preferences.getString("cameraNotSupportMessage",""),false,this);
            } else {
                mCameraSource = new CameraSource.Builder(this, textRecognizer)
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        .setRequestedPreviewSize(768, 1080)
                        .setAutoFocusEnabled(true)
                        .setRequestedFps(40.0f)
                        .build();

                mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(SurfaceHolder holder) {
                        try {
                            if (ActivityCompat.checkSelfPermission(ScanCardActivity.this,
                                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions((ScanCardActivity.this),
                                        new String[]{Manifest.permission.CAMERA},
                                        requestPermissionID);
                                return;
                            }
                            mCameraSource.start(mCameraView.getHolder());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder holder) {
                        if (mCameraSource!=null)
                            mCameraSource.stop();
                    }
                });

                textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                    @Override
                    public void release() {
                    }

                    @Override
                    public void receiveDetections(Detector.Detections<TextBlock> detections) {
                        final SparseArray<TextBlock> items = detections.getDetectedItems();
                        if (items.size() != 0) {
                            StringBuilder stringBuilder = new StringBuilder();
                            for (int i = 0; i < items.size(); i++) {
                                final TextBlock item = items.valueAt(i);
                                stringBuilder.append(item.getValue());
                                stringBuilder.append("\n");
                                runOnUiThread(() -> scanTv.setText("" + item.getValue()));
                                if (item.getValue().contains("SIKA") || item.getValue().contains("RENA") || item.getValue().contains("RENC")|| item.getValue().contains("SHA")) {
                                    runOnUiThread(() -> {
                                        try {
                                            JSONObject serialNoDataJsonObject = new JSONObject(preferences.getString("SerialNoData", ""));
                                            if (serialNoDataJsonObject.has(item.getValue()) && isCardMatched) {
                                                common.setProgressBar("Please wait...", ScanCardActivity.this, ScanCardActivity.this);
                                                isCardMatched = false;
                                                rfID = serialNoDataJsonObject.getString(item.getValue());
                                                if (mCameraSource!=null)
                                                    mCameraSource.stop();
                                                mCameraView.setVisibility(View.GONE);
                                                checkWhetherLocationSettingsAreSatisfied();
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                }
                            }
                        }
                    }
                });
            }
        });
    }

    public void checkWhetherLocationSettingsAreSatisfied() {
        LocationRequest mLocationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(1000).setNumUpdates(2);
        final LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        builder.setNeedBle(false);
        SettingsClient client = LocationServices.getSettingsClient(ScanCardActivity.this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(ScanCardActivity.this, locationSettingsResponse -> {
            setLocation();
        });
        task.addOnFailureListener(ScanCardActivity.this, e -> {
            common.closeDialog();
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(ScanCardActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void setLocation() {
        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(ScanCardActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ScanCardActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null && locationResult.getLocations().size() > 0) {
                    int latestLocationIndex = locationResult.getLocations().size() - 1;
                    Double lat = locationResult.getLocations().get(latestLocationIndex).getLatitude();
                    Double lng = locationResult.getLocations().get(latestLocationIndex).getLongitude();
                    currentLatLng = new LatLng(lat, lng);
                    LocationServices.getFusedLocationProviderClient(ScanCardActivity.this).removeLocationUpdates(locationCallback);
                    preferences.edit().putString("rfid", rfID).apply();
                    preferences.edit().putString("lat", String.valueOf(lat)).apply();
                    preferences.edit().putString("lng", String.valueOf(lng)).apply();
                    preferences.edit().putString("line", String.valueOf(lineNumber)).apply();
                    preferences.edit().putString("cardNo", "").apply();
                    preferences.edit().putString("cardNoPre", "").apply();
                    preferences.edit().putString("houseType", houseType).apply();
                    preferences.edit().putString("markingKey", markingKey).apply();
                    JSONObject jsonObject;
                    int isVerified = 1;
                    String cardNo = "";
                    try {
                        jsonObject = new JSONObject(preferences.getString("CardScanData", ""));
                        if (jsonObject.has(rfID)) {
                            JSONArray jsonArray = jsonObject.getJSONArray(rfID);
                            cardNo = jsonArray.get(1).toString();
                            if (jsonArray.get(0).equals("no")) {
                                preferences.edit().putString("cardNo", cardNo).apply();
                                preferences.edit().putString("cardNoPre", cardNo).apply();
                            } else {
                                isVerified = 2;
                            }
                        } else {
                            isVerified = 3;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (isVerified == 1) {
                        if (markingCard.equalsIgnoreCase("no")){
                            try {
                                JSONObject markingDataObject = new JSONObject(markingData);
                                Iterator<String> listKEY = markingDataObject.keys();
                                boolean isFound = false;
                                while (listKEY.hasNext()) {
                                    String key = (String) listKEY.next();
                                    try {
                                        JSONArray jsonArray = markingDataObject.getJSONArray(key);
                                        if (jsonArray.get(4).toString().equalsIgnoreCase(preferences.getString("cardNo", ""))) {
                                            isFound = true;
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                                if (isFound) {
                                    common.showAlertBox(preferences.getString("sameCardOnTwoMarkerMessage",""), false, ScanCardActivity.this);
                                } else {
                                    if (countDownTimer != null) {
                                        countDownTimer.cancel();
                                    }
                                    moveNextActivity();
                                }
                            }catch (Exception e){}
                        }else{
                            if (markingCard.equalsIgnoreCase(cardNo)){
                                if (countDownTimer != null) {
                                    countDownTimer.cancel();
                                }
                                moveNextActivity();
                            }else {
                                String messageString="";
                                try {
                                    String[] message = preferences.getString("sameMarkerOnTwoCard","").split("#");
                                    messageString = message[0] + markingCard + message[1];
                                } catch (Exception e) {
                                }
                                common.showAlertBox(messageString, false, ScanCardActivity.this);
                            }
                        }

                    } else if (isVerified == 2) {
                        common.showAlertBox(cardNo + " यह कार्ड Already Verified है | ", true, ScanCardActivity.this);
                    } else {
                        common.showAlertBox(cardNo + " यह कार्ड डेटाबेस में नहीं  है | ", true, ScanCardActivity.this);
                    }
                    common.closeDialog();
                }
            }
        };
        LocationServices.getFusedLocationProviderClient(ScanCardActivity.this).requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    public void moveNextActivity() {
        if (!(countDownTimer == null)) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (mCameraSource!=null)
            mCameraSource.stop();
        mCameraView.setVisibility(View.GONE);
        Intent intent = new Intent(ScanCardActivity.this, VerifyActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!(countDownTimer == null)) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        progressBar.setProgress(100);
        progressPercentage.setText("100%");
        linearLayout.setVisibility(View.GONE);
        if (mCameraSource!=null)
        mCameraSource.stop();
        mCameraView.setVisibility(View.GONE);
        cardPhotoLinearLayout.setVisibility(View.VISIBLE);
    }
}