package com.wevois.surveyapp.viewmodel;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.repository.Repository;
import com.wevois.surveyapp.views.FileDownloadPageActivity;
import com.wevois.surveyapp.views.LoginPageActivity;
import com.wevois.surveyapp.views.RegisterPageActivity;

public class LoginPageViewModel extends ViewModel {
    private final static int LOCATION_REQUEST = 500;
    Activity activity;
    Handler handler;
    SharedPreferences preferences;
    CommonFunctions common = CommonFunctions.getInstance();
    Boolean isMoved = true, isFirstTime = true, checkNetBy = true;
    public final ObservableField<String> userTv = new ObservableField<>("");
    public ObservableField<Boolean> isVisible = new ObservableField<>(false);
    String[] PERMISSIONS = {
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CALL_PHONE,
            Manifest.permission.CAMERA,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
    };

    public void init(LoginPageActivity selectCityActivity) {
        activity = selectCityActivity;
        common.getKml(activity);
        preferences = activity.getSharedPreferences("surveyApp", MODE_PRIVATE);
        if (isFirstTime) {
            isFirstTime = false;
            allowPermissions();
        }
    }

    public void allowPermissions() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS, LOCATION_REQUEST);
            return;
        }
        common.setProgressBar("Check internet connection...", activity, activity);
        checkVersion();
    }

    @SuppressLint("StaticFieldLeak")
    private void checkVersion() {
        new Repository().checkNetWork(activity).observeForever(response -> {
            if (response) {
                if (checkNetBy) {
                    checkNetBy = false;
                    common.setProgressBar("Check application version.", activity, activity);
                    new Repository().checkVersion(activity).observeForever(dataSnapshot -> {
                        common.closeDialog();
                        try {
                            String localVersion = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
                            if (dataSnapshot.getValue() != null) {
                                String version = dataSnapshot.getValue().toString();
                                if (!version.equals(localVersion)) {
                                    showVersionAlertBox();
                                } else {
                                    if (preferences.getString("userId","").equalsIgnoreCase("")) {
                                        isVisible.set(true);
                                    }else {
                                        callLoginCheck("SUR"+preferences.getString("userId",""),true);
                                    }
                                }
                            } else {
                                showVersionAlertBox();
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    common.setProgressBar("Check user id.", activity, activity);
                    callLoginCheck(userTv.get(),false);
                }
            } else {
                handler = new Handler();
                handler.postDelayed(runnable, 50);
            }
        });
    }

    private void callLoginCheck(String s,boolean isLogin) {
        new Repository().loginUserId(activity, s).observeForever(dataSnapshot -> {
            if (dataSnapshot.getValue() != null) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (snapshot.child("status").getValue().equals("2")) {
                        if (snapshot.hasChild("surveyor-type")) {
                            if (snapshot.child("surveyor-type").getValue().equals("Surveyor")) {
                                if (isLogin){
                                    if (isMoved) {
                                        isMoved = false;
                                        String isOfflineAllowed = "no";
                                        SharedPreferences preferences = activity.getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
                                        preferences.edit().putString("mno", snapshot.child("mobile").getValue().toString()).apply();
                                        preferences.edit().putString("userId", snapshot.getKey()).apply();
                                        if (snapshot.hasChild("isOfflineAllowed")) {
                                            isOfflineAllowed = snapshot.child("isOfflineAllowed").getValue().toString();
                                        }
                                        preferences.edit().putString("isOfflineAllowed", isOfflineAllowed).apply();
                                        common.closeDialog();
                                        activity.startActivity(new Intent(activity, FileDownloadPageActivity.class));
                                        activity.finish();
                                    }
                                }else {
                                    boolean isAlreadyLogin = false;
                                    if (snapshot.hasChild("isLogin")){
                                        if (snapshot.child("isLogin").getValue().toString().equalsIgnoreCase("yes")){
                                            isAlreadyLogin = true;
                                        }
                                    }
                                    if (!isAlreadyLogin) {
                                        if (isMoved) {
                                            isMoved = false;
                                            CommonFunctions.getInstance().getDatabaseForApplication(activity).child("Surveyors/"+snapshot.getKey()+"/isLogin").setValue("yes").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    String isOfflineAllowed = "no";
                                                    SharedPreferences preferences = activity.getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
                                                    preferences.edit().putString("mno", snapshot.child("mobile").getValue().toString()).apply();
                                                    preferences.edit().putString("userId", snapshot.getKey()).apply();
                                                    if (snapshot.hasChild("isOfflineAllowed")) {
                                                        isOfflineAllowed = snapshot.child("isOfflineAllowed").getValue().toString();
                                                    }
                                                    preferences.edit().putString("isOfflineAllowed", isOfflineAllowed).apply();
                                                    common.closeDialog();
                                                    activity.startActivity(new Intent(activity, FileDownloadPageActivity.class));
                                                    activity.finish();
                                                }
                                            });
                                        }
                                    }else {
                                        common.showAlertBox("Someone already logged in!\nPlease connect with technical team.", false, activity);
                                    }
                                }
                            } else {
                                common.showAlertBox("Invalid User!", false, activity);
                            }
                        } else {
                            common.showAlertBox("Invalid User!", false, activity);
                        }
                    } else if (snapshot.child("status").getValue().equals("3")) {
                        common.showAlertBox("आपकी यूजर आईडी इनएक्टिव कर दि गयी है कृपया सर्वे मैनेजर से संपर्क करे", false, activity);
                    } else {
                        common.showAlertBox("आप अभी एक्टिव यूजर नहीं है कृपया सर्वे मैनेजर से संपर्क करे", false, activity);
                    }
                }
            } else {
                common.showAlertBox("Invalid User!", false, activity);
            }
        });
    }

    private void showVersionAlertBox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);
        builder.setTitle("Version Expired");
        builder.setMessage("Your App version is not Matched. Please update your app.");
        builder.setPositiveButton("Ok", (dialog, which) -> {
                    dialog.dismiss();
                    common.closeDialog();
                    activity.finish();
                }
        );

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public Runnable runnable = this::checkVersion;

    public void loginClick() {
        if (userTv.get().trim().length() == 0) {
            common.showAlertBox("Please enter user id.", false, activity);
        } else {
            common.setProgressBar("Please wait...", activity, activity);
            checkVersion();
        }
    }

    public void registerClick() {
        activity.startActivity(new Intent(activity, RegisterPageActivity.class));
    }

    public TextWatcher userTvWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                userTv.set(editable.toString());
            }
        };
    }
}
