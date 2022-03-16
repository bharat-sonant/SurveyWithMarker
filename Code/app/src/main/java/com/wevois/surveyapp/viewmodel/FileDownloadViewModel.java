package com.wevois.surveyapp.viewmodel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.repository.Repository;
import com.wevois.surveyapp.views.FileDownloadPageActivity;
import com.wevois.surveyapp.views.MapPageActivity;

public class FileDownloadViewModel extends ViewModel {
    Activity activity;
    SharedPreferences preferences;
    CommonFunctions common = CommonFunctions.getInstance();
    boolean isMoved = true,isFirstTime = true;
    public ObservableField<Boolean> isVisible = new ObservableField<>(false);

    @RequiresApi(api = Build.VERSION_CODES.P)
    public void init(FileDownloadPageActivity fileDownloadPageActivity) {
        activity = fileDownloadPageActivity;
        preferences = activity.getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
        if (isFirstTime) {
            isFirstTime = false;
            common.setProgressBar("Please wait...",activity,activity);
            new Repository.DownloadKmlFile(common.getKmlFilePath(preferences.getString("ward", ""), activity),activity).execute();
            new Repository().storageFileDownload(activity);
            new Repository().fileDownload(activity).observeForever(dataSnapshot -> {
                Log.d("TAG", "init: check "+dataSnapshot);
                if (dataSnapshot.equalsIgnoreCase("आज आपका कोई कार्य असाइन नहीं है।  कृपया सुपरवाईज़र से कांटेक्ट करे || ")) {
                    showAlertBox(dataSnapshot);
                }else {
                    common.closeDialog();
                    isVisible.set(true);
                }
            });
        }
    }

    private void showAlertBox(String message) {
        common.closeDialog();
        AlertDialog.Builder alertAssignment = new AlertDialog.Builder(activity);
        alertAssignment.setMessage(message);
        alertAssignment.setCancelable(false);
        alertAssignment.setPositiveButton("OK", (dialog, id) -> {
            dialog.cancel();
            if (dialog != null) {
                dialog.dismiss();
                activity.finish();
            }
        });
        AlertDialog alertDAssignment = alertAssignment.create();
        if (!activity.isFinishing()) {
            alertDAssignment.show();
        }
    }

    public void moveClick(){
        if (isMoved) {
            isMoved = false;
            activity.startActivity(new Intent(activity, MapPageActivity.class));
            activity.finish();
        }
    }
}
