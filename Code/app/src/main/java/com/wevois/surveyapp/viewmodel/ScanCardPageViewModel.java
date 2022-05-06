package com.wevois.surveyapp.viewmodel;

import static com.google.android.gms.internal.zzagr.runOnUiThread;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.app.ActivityCompat;
import androidx.databinding.ObservableField;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.views.ScanCardPageActivity;
import com.wevois.surveyapp.views.VerifyPageActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

public class ScanCardPageViewModel extends ViewModel {
    Activity activity;
    private final static int requestPermissionID = 1888;
    boolean isCardMatched = true;
    int lineNumber = 1;
    String rfID = "", houseType = "", markingKey = "", markingCard = "", markingData = "", markingRevisit = "";
    public SurfaceView mCameraView;
    CameraSource mCameraSource;
    SharedPreferences preferences;
    CountDownTimer countDownTimer;
    CommonFunctions common = CommonFunctions.getInstance();
    public ObservableField<Boolean> isScanVisible = new ObservableField<>(true);
    public ObservableField<Boolean> isSurfaceVisible = new ObservableField<>(true);
    public ObservableField<Boolean> isCardVisible = new ObservableField<>(false);
    public MutableLiveData<Integer> progressBar = new MutableLiveData<>(0);
    public MutableLiveData<String> percentageShow = new MutableLiveData<>("Scanning progress : 100%");
    public final ObservableField<String> scanTv = new ObservableField<>("");

    public void init(ScanCardPageActivity scanCardPageActivity, SurfaceView surfaceView) {
        activity = scanCardPageActivity;
        mCameraView = surfaceView;
        preferences = activity.getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
        lineNumber = preferences.getInt("line", 0);
        houseType = preferences.getString("houseType", "");
        markingKey = preferences.getString("markingKey", "");
        markingCard = preferences.getString("markingCard", "");
        markingData = preferences.getString("markingDatas", "");
        markingRevisit = preferences.getString("markingRevisit", "");
        try {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
        } catch (Exception e) {
        }
        startScanning();
    }

    public void onClick() {
        try {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
        } catch (Exception e) {
        }
        startScanning();
    }

    private void startScanning() {
        isCardMatched = true;
        isScanVisible.set(true);
        isSurfaceVisible.set(true);
        isCardVisible.set(false);
        startCameraSource();
        countDownTimer = new CountDownTimer(preferences.getInt("cardScanningTime", 10000), 500) {
            public void onTick(long millisUntilFinished) {
                int progress = preferences.getInt("cardScanningTime", 10000) - (int) millisUntilFinished;
                progressBar.setValue(((progress * 100) / preferences.getInt("cardScanningTime", 10000)));
                percentageShow.setValue("Scanning progress : " + ((progress * 100) / preferences.getInt("cardScanningTime", 10000)) + "%");
            }

            @SuppressLint("SetTextI18n")
            public void onFinish() {
                progressBar.setValue(100);
                percentageShow.setValue("100%");
                isScanVisible.set(false);
                isSurfaceVisible.set(false);
                if (mCameraSource != null)
                    mCameraSource.stop();
                isCardVisible.set(true);
            }
        }.start();
    }

    private void startCameraSource() {
        try {
            if (mCameraSource != null)
                mCameraSource.stop();
        } catch (Exception e) {
        }
        final TextRecognizer textRecognizer = new TextRecognizer.Builder(activity).build();
        if (!textRecognizer.isOperational()) {
            common.showAlertBox(preferences.getString("cameraNotSupportMessage", ""), false, activity);
        } else {
            mCameraSource = new CameraSource.Builder(activity, textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(768, 1080)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(40.0f)
                    .build();

            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    try {
                        if (ActivityCompat.checkSelfPermission(activity,
                                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions((activity),
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
                    if (mCameraSource != null)
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
                            scanTv.set("" + item.getValue());
                            if (item.getValue().contains("BHER") || item.getValue().contains("NWI")||item.getValue().contains("SIKA") || item.getValue().contains("RENA") || item.getValue().contains("RENC") || item.getValue().contains("SHAH")|| item.getValue().contains("KNGH")) {
                                try {
                                    JSONObject serialNoDataJsonObject = new JSONObject(preferences.getString("SerialNoData", ""));

                                    if (serialNoDataJsonObject.has(item.getValue()) && isCardMatched) {
                                        runOnUiThread(() -> {
                                            isCardMatched = false;
                                            try {
                                                rfID = serialNoDataJsonObject.getString(item.getValue());
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            if (!(countDownTimer == null)) {
                                                countDownTimer.cancel();
                                                countDownTimer = null;
                                            }
                                            isScanVisible.set(false);
                                            isSurfaceVisible.set(false);
                                            if (mCameraSource != null)
                                                mCameraSource.stop();
                                            isCardVisible.set(true);
                                            preferences.edit().putString("rfid", rfID).apply();
                                            preferences.edit().putString("line", String.valueOf(lineNumber)).apply();
                                            preferences.edit().putString("cardNo", "").apply();
                                            preferences.edit().putString("cardNoPre", "").apply();
                                            preferences.edit().putString("houseType", houseType).apply();
                                            preferences.edit().putString("markingKey", markingKey).apply();
                                            preferences.edit().putString("markingRevisit", markingRevisit).apply();
                                            JSONObject jsonObject;
                                            int isVerified = 1;
                                            String cardNo = "";
                                            try {
                                                jsonObject = new JSONObject(preferences.getString("CardScanData", ""));
                                                if (jsonObject.has(rfID)) {
                                                    JSONArray jsonArray = jsonObject.getJSONArray(rfID);
                                                    cardNo = jsonArray.get(0).toString();
                                                    preferences.edit().putString("cardNo", cardNo).apply();
                                                    preferences.edit().putString("cardNoPre", cardNo).apply();
                                                } else {
                                                    isVerified = 2;
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            if (isVerified == 1) {
                                                if (markingCard.equalsIgnoreCase("no")) {
                                                    try {
                                                        JSONObject markingDataObject = new JSONObject(markingData);
                                                        Iterator<String> listKEY = markingDataObject.keys();
                                                        boolean isFound = false;
                                                        while (listKEY.hasNext()) {
                                                            String key = listKEY.next();
                                                            try {
                                                                JSONArray jsonArray = markingDataObject.getJSONArray(key);
                                                                if (jsonArray.get(4).toString().equalsIgnoreCase(preferences.getString("cardNo", ""))) {
                                                                    isFound = true;
                                                                }
                                                            } catch (Exception e) {
                                                            }
                                                        }
                                                        if (isFound) {
                                                            common.showAlertBox(preferences.getString("sameCardOnTwoMarkerMessage", ""), false, activity);
                                                        } else {
                                                            if (countDownTimer != null) {
                                                                countDownTimer.cancel();
                                                            }
                                                            moveNextActivity();
                                                        }
                                                    } catch (Exception e) {
                                                    }
                                                } else {
                                                    if (markingCard.equalsIgnoreCase(cardNo)) {
                                                        if (countDownTimer != null) {
                                                            countDownTimer.cancel();
                                                        }
                                                        moveNextActivity();
                                                    } else {
                                                        String messageString = "";
                                                        try {
                                                            String[] message = preferences.getString("sameMarkerOnTwoCard", "").split("#");
                                                            messageString = message[0] + markingCard + message[1];
                                                        } catch (Exception e) {
                                                        }
                                                        common.showAlertBox(messageString, false, activity);
                                                    }
                                                }
                                            } else {
                                                common.showAlertBox(cardNo + " यह कार्ड डेटाबेस में नहीं  है | ", true, activity);
                                            }
                                        });
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    public void onBack() {
        activity.finish();
    }

    public void moveNextActivity() {
        if (!(countDownTimer == null)) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (mCameraSource!=null)
            mCameraSource.stop();
        isSurfaceVisible.set(false);
        Intent intent = new Intent(activity, VerifyPageActivity.class);
        activity.startActivity(intent);
        activity.finish();
    }
}
