package com.wevois.surveyapp.viewmodel;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.ViewModel;

import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.repository.Repository;
import com.wevois.surveyapp.views.OfflinePageActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class OfflinePageViewModel extends ViewModel {
    Activity activity;
    SharedPreferences preferences;
    CommonFunctions common = new CommonFunctions();
    JSONObject wardJsonObject = new JSONObject(),jsonObject = new JSONObject(),cardJsonObject = new JSONObject();
    boolean isDelete = false;
    ArrayList<String> cardNumbers = new ArrayList<>();
    int position = 0;
    JSONObject tempObject = new JSONObject();
    String currentDate, countCheck = "2", markingKey = "", markingRevisit = "", ward, address, cardNo, createdDate, houseType, lat, lng, line, name, rfid, mobile, servingCount = "", cardType = "";
    ArrayList<String> newMobiles=new ArrayList<>();

    public void init(OfflinePageActivity offlinePageActivity) {
        activity = offlinePageActivity;
        preferences = activity.getSharedPreferences("surveyApp", MODE_PRIVATE);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd-MM-yyyy");
        currentDate = dateFormat1.format(new Date());
    }

    @SuppressLint("StaticFieldLeak")
    public void syncBtn(){
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
            Toast.makeText(activity, "Data not available for sync.", Toast.LENGTH_SHORT).show();
        } else {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    common.setProgressBar("Please Wait...", activity, activity);
                }

                @Override
                protected Boolean doInBackground(Void... p) {
                    return common.network(activity);
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    if (result) {
                        position = 0;
                        sendData();
                    } else {
                        common.closeDialog();
                        Toast.makeText(activity, "No internet connection.\n try again.", Toast.LENGTH_SHORT).show();
                    }
                }
            }.execute();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void sendData() {
        if (wardJsonObject.length() > 0) {
            common.setProgressBar("Data Uploading...\n" + wardJsonObject.length(), activity, activity);
            try {
                tempObject = new JSONObject();
                cardJsonObject = wardJsonObject.getJSONObject(cardNumbers.get(position));
                ward = preferences.getString("ward", "");
                cardNo = cardJsonObject.getString("cardno");
                mobile = cardJsonObject.getString("mobile");
                address = cardJsonObject.getString("address");
                createdDate = cardJsonObject.getString("createddate");
                houseType = cardJsonObject.getString("housetype");
                lat = cardJsonObject.getString("lat");
                lng = cardJsonObject.getString("lng");
                line = cardJsonObject.getString("line");
                name = cardJsonObject.getString("name");
                rfid = cardJsonObject.getString("rfid");
                markingKey = cardJsonObject.getString("markingKey");
                markingRevisit = cardJsonObject.getString("markingRevisit");
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
        new Repository().checkRfidAlreadyExists(activity,preferences.getString("ward", ""),line,rfid).observeForever(dataSnapshot -> {
            if (dataSnapshot!=null) {
                if (dataSnapshot.getValue() != null) {
                    countCheck = "1";
                } else {
                    countCheck = "2";
                }
            }
            saveSurveyDetails();
        });
    }

    private void saveSurveyDetails() {
        new Repository().CheckWardMapping(activity,cardNo).observeForever(dataSnapshot -> {
            if (dataSnapshot!=null) {
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
        });
    }

    private void callMethod() {
        new Repository().checkMarkedHouses(activity,ward,line,markingKey).observeForever(dataSnapshot -> {
            boolean isSaveData = true;
            if (dataSnapshot != null) {
                if (dataSnapshot.getValue() != null) {
                    if (dataSnapshot.getValue().toString().equalsIgnoreCase(cardNo)) {
                        isSaveData = true;
                    } else {
                        isSaveData = false;
                    }
                }
            }
            if (isSaveData) {
                HashMap<String, Object> housesMap = new HashMap<>();
                housesMap.put("address", address);
                housesMap.put("cardNo", cardNo);
                housesMap.put("phaseNo", "2");
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

                newMobiles = new ArrayList<>();
                if (mobile.contains(",")) {
                    String[] mobiles = mobile.trim().split(",");
                    for (int i = 0; i < mobiles.length; i++) {
                        newMobiles.add(mobiles[i].trim());
                    }
                } else {
                    newMobiles.add(mobile.trim());
                }
                new Repository().sendHousesData(activity, countCheck, cardNo, null, null, newMobiles, housesMap, markingKey, cardJsonObject, wardJsonObject, jsonObject,preferences.getString("ward", ""),
                        preferences.getString("userId", ""),line,rfid,markingRevisit,currentDate).observeForever(dataSnapshots -> {
                    Log.d("TAG", "saveSurveyData: check A " + dataSnapshots);
                    if (dataSnapshots.equalsIgnoreCase("success")) {
                        saveMarkingData(4,cardNo,line,markingKey);
                        removeCardLocalData();
                    }else if (dataSnapshots.equalsIgnoreCase("successData")){
                        removeCardLocalData();
                    }
                });
            } else {
                try {
                    wardJsonObject.remove(cardNo);
                    jsonObject.put(preferences.getString("ward", ""), wardJsonObject);
                    preferences.edit().putString("scanHousesData", jsonObject.toString()).apply();
                } catch (Exception e) {
                }
                removeCardLocalData();
            }
        });
    }

    private void sendSurveyRequiredRevisited() {
        HashMap<String,Object> housesHashMap = new HashMap<>();
        housesHashMap.put("address",address);
        housesHashMap.put("cardNo",cardNo);
        housesHashMap.put("phaseNo","2");
        housesHashMap.put("createdDate",createdDate);
        housesHashMap.put("houseType",houseType);
        housesHashMap.put("latLng","(" + lat + "," + lng + ")");
        housesHashMap.put("line",line);
        housesHashMap.put("name",name);
        housesHashMap.put("mobile",mobile);
        housesHashMap.put("rfid",rfid);
        housesHashMap.put("ward",ward);
        if (!servingCount.equals("")) {
            housesHashMap.put("servingCount",servingCount);
        }
        new Repository().RequiredSurveyHouses(activity,housesHashMap,common.getDatabaseForApplication(activity).child("RequiredSurveyHouses/" + ward + "/" + line + "/" + cardNo)).observeForever(string->{
            if (string.equalsIgnoreCase("success")){
                try {
                    wardJsonObject.remove(cardNo);
                    jsonObject.put(preferences.getString("ward", ""), wardJsonObject);
                    preferences.edit().putString("scanHousesData", jsonObject.toString()).apply();
                } catch (Exception e) {
                }
                removeCardLocalData();
            }
        });
    }

    private void removeCardLocalData() {
        position = position + 1;
        if (wardJsonObject.length() == 0) {
            showAlertBox();
        } else {
            sendData();
        }
    }

    private void saveMarkingData(int i,String v,String lineNo,String marking) {
        JSONObject jsonObject = new JSONObject();
        if (!preferences.getString("markingData", "").equalsIgnoreCase("")) {
            try {
                jsonObject = new JSONObject(preferences.getString("markingData", ""));
            } catch (Exception e) {
            }
        }
        JSONObject markingDataObject = new JSONObject();
        try {
            markingDataObject = jsonObject.getJSONObject(lineNo);
            JSONArray jsonArray = markingDataObject.getJSONArray(marking);
            jsonArray.put(i,v);
            try {
                markingDataObject.put(marking, jsonArray);
            } catch (Exception e) {
            }
        } catch (Exception e) {
        }
        try {
            jsonObject.put(lineNo, markingDataObject);
        } catch (Exception e) {
        }
        preferences.edit().putString("markingData", jsonObject.toString()).apply();
    }

    private void showAlertBox() {
        common.closeDialog();
        AlertDialog.Builder builder1 = new AlertDialog.Builder(activity);
        builder1.setMessage("आपका सर्वे पूरा हुआ, धन्यवाद !");
        builder1.setCancelable(true);
        builder1.setPositiveButton("OK", (dialog, id) -> {
            dialog.cancel();
            preferences.edit().putString("isOnResumeCall", "yes").apply();
            activity.finish();
        });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    public void onBackClick(){
        activity.finish();
    }
}
