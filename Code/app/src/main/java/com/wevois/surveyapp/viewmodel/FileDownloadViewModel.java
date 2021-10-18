package com.wevois.surveyapp.viewmodel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.repository.Repository;
import com.wevois.surveyapp.views.FileDownloadPageActivity;
import com.wevois.surveyapp.views.MapPageActivity;

public class FileDownloadViewModel extends ViewModel {
    Activity activity;
    SharedPreferences preferences;
    CommonFunctions common = new CommonFunctions();
    boolean isMoved = true,isFirstTime = true;
    public ObservableField<Boolean> isVisible = new ObservableField<>(false);

    public void init(FileDownloadPageActivity fileDownloadPageActivity) {
        activity = fileDownloadPageActivity;
        preferences = activity.getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
        if (isFirstTime) {
            isFirstTime = false;
            common.setProgressBar("Please wait...",activity,activity);
            new Repository.DownloadKmlFile(common.getKmlFilePath(preferences.getString("ward", ""), activity),activity).execute();
            new Repository().storageFileDownload(activity);
            new Repository().fileDownload(activity).observeForever(dataSnapshot -> {
                if (dataSnapshot.equalsIgnoreCase("success")) {
                    common.closeDialog();
                    isVisible.set(true);
                }else {
                    showAlertBox(dataSnapshot);
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
