package com.wevois.surveyapp.viewmodel;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;

import androidx.lifecycle.ViewModel;

import com.wevois.surveyapp.views.LoginPageActivity;
import com.wevois.surveyapp.views.SelectCityActivity;

public class SelectCityViewModel extends ViewModel {
    Activity activity;

    public void init(SelectCityActivity selectCityActivity) {
        activity = selectCityActivity;
    }

    public void SikarCity() {
        saveData("Sikar","https://dtdnavigator.firebaseio.com/");
    }
    public void ReengusCity() {
        saveData("Reengus","https://dtdreengus.firebaseio.com/");
    }
    public void JaipurCity() {
        saveData("Jaipur","https://dtdjaipur.firebaseio.com/");
    }
    public void ShahpuraCity() {
        saveData("Shahpura","https://dtdshahpura.firebaseio.com/");
    }

    public Boolean onLongClickOnHeading(View v) {
        saveData("Test","https://dtdnavigatortesting.firebaseio.com/");
        return true;
    }

    public void saveData(String storagePath, String dbPath){
        SharedPreferences dbPathSP = activity.getSharedPreferences("FirebasePath", MODE_PRIVATE);
        dbPathSP.edit().putString("dbPath", dbPath).apply();
        dbPathSP.edit().putString("storagePath", storagePath).apply();
        dbPathSP.edit().putString("login", "yes").apply();

        activity.startActivity(new Intent(activity, LoginPageActivity.class));
        activity.finish();
    }
}
