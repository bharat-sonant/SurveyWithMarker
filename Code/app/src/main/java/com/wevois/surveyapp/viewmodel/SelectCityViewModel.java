package com.wevois.surveyapp.viewmodel;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;

import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.wevois.surveyapp.CityDetails;
import com.wevois.surveyapp.CityDetailsAdapter;
import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.OnClickInterface;
import com.wevois.surveyapp.views.LoginPageActivity;
import com.wevois.surveyapp.views.SelectCityActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class SelectCityViewModel extends ViewModel implements OnClickInterface {
    Activity activity;
    public ObservableField<CityDetailsAdapter> cityRecyclerViewAdapter = new ObservableField<>();
    SharedPreferences pathSharedPreferences;
    String city,key,databasePath,sPath;
    ArrayList<CityDetails> arrayList = new ArrayList<>();
    CommonFunctions common = CommonFunctions.getInstance();

    public void init(SelectCityActivity selectCityActivity) {
        activity = selectCityActivity;
        pathSharedPreferences =activity.getSharedPreferences("FirebasePath", MODE_PRIVATE);
        common.setProgressBar("Please wait...",activity,activity);
        getCityDetails();
    }

    public Boolean onLongClickOnHeading(View v) {
        pathSharedPreferences.edit().putString("dbPath", databasePath.replaceAll("\\\\", "")).apply();
        pathSharedPreferences.edit().putString("storagePath", city).apply();
        pathSharedPreferences.edit().putString("login", "yes").commit();
        activity.startActivity(new Intent(activity, LoginPageActivity.class));
        activity.finish();
        return true;
    }

    private void getCityDetails() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();
        storageReference.child("CityDetails/CityDetails.json").getMetadata().addOnSuccessListener(storageMetadata -> {
            long fileCreationTime = storageMetadata.getCreationTimeMillis();
            long fileDownloadTime = pathSharedPreferences.getLong("CityDetailsDownloadTime", 0);
            if (fileDownloadTime != fileCreationTime) {
                storageReference.child("CityDetails/CityDetails.json").getBytes(10000000).addOnSuccessListener(taskSnapshot -> {
                    try {
                        String str = new String(taskSnapshot, StandardCharsets.UTF_8);
                        pathSharedPreferences.edit().putString("CityDetails", str).apply();
                        pathSharedPreferences.edit().putLong("CityDetailsDownloadTime", fileCreationTime).apply();
                        setCityList();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }else {
                setCityList();
            }
        });
    }

    public void setCityList(){
        try {
            arrayList.clear();
            JSONArray jsonArray = new JSONArray(pathSharedPreferences.getString("CityDetails",""));
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String cityName = jsonObject.getString("cityName");
                String keyName = jsonObject.getString("key");
                String dbPath = jsonObject.getString("dbPath");
                String storagePath = jsonObject.getString("storagePath");
                if (cityName.equalsIgnoreCase("Test")) {
                    city = cityName;
                    key = keyName;
                    databasePath = dbPath;
                    sPath = storagePath;
                } else {
                    arrayList.add(new CityDetails(cityName,keyName, dbPath, storagePath));
                }
            }
            cityRecyclerViewAdapter.set(new CityDetailsAdapter(arrayList, activity, this));
            common.closeDialog();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(int position) {
        pathSharedPreferences.edit().putString("dbPath", arrayList.get(position).getDbPath()).apply();
        pathSharedPreferences.edit().putString("storagePath", arrayList.get(position).getCityName()).apply();
        pathSharedPreferences.edit().putString("login", "yes").commit();
        activity.startActivity(new Intent(activity, LoginPageActivity.class));
        activity.finish();
    }
}
