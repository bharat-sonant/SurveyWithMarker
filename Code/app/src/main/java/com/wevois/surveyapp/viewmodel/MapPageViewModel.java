package com.wevois.surveyapp.viewmodel;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.maps.android.data.kml.KmlLayer;
import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.R;
import com.wevois.surveyapp.repository.Repository;
import com.wevois.surveyapp.views.FormPageActivity;
import com.wevois.surveyapp.views.MapPageActivity;
import com.wevois.surveyapp.views.OfflinePageActivity;
import com.wevois.surveyapp.views.RevisitPageActivity;
import com.wevois.surveyapp.views.ScanCardPageActivity;
import com.wevois.surveyapp.views.VerifyPageActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

public class MapPageViewModel extends ViewModel {
    private final static int REQUEST_CHECK_SETTINGS = 500;
    private GoogleMap mMap;
    Activity activity;
    SupportMapFragment mapFragment;
    SharedPreferences preferences;
    private LocationCallback locationCallback;
    double n, m, lat, lng;
    CountDownTimer countDownTimerLocation;
    int currentLine = 1, position = 0, endLineNo = 1;
    LatLng latLng, currentLatLng;
    CommonFunctions common = CommonFunctions.getInstance();
    JSONObject jsonObjectLines;
    JSONArray jsonArray;
    String rfID, houseT = "", markingKey = "";
    ArrayList<Integer> lines = new ArrayList<>();
    ArrayList<LatLng> directionPositionList = new ArrayList<>();
    boolean isFirst = true, isFirstTime = true;
    JSONObject markingDataObject = new JSONObject();
    HashMap<Integer, Marker> hashMapMarker = new HashMap<>();
    HashMap<Integer, Polyline> hashLineMarker = new HashMap<>();
    AlertDialog customTimerAlertBox;
    Marker selectedMarker = null;
    public MutableLiveData<String> lineNumber = new MutableLiveData<>("1");
    public MutableLiveData<String> todayCardScanShow = new MutableLiveData<>("0");
    public MutableLiveData<String> totalCard = new MutableLiveData<>("0");
    public MutableLiveData<String> showOfflineCount = new MutableLiveData<>("Offline Data : 0");
    public MutableLiveData<Drawable> buttonBackground = new MutableLiveData<>();

    public void init(MapPageActivity mapPageActivity, SupportMapFragment map) {
        activity = mapPageActivity;
        mapFragment = map;
        preferences = activity.getSharedPreferences("surveyApp", MODE_PRIVATE);
        if (isFirst) {
            isFirst = false;
            String[] linesData = preferences.getString("lines", "").split(",");
            for (int i = 0; i < linesData.length; i++) {
                lines.add(Integer.parseInt(linesData[i]));
            }
            lineData();
            new Repository().DownloadCurrentLine(activity).observeForever(dataSnapshot -> {
                if ((dataSnapshot.getValue() != null)) {
                    if (lines.contains(Integer.parseInt(dataSnapshot.getValue().toString()))) {
                        position = lines.indexOf(Integer.parseInt(dataSnapshot.getValue().toString()));
                        currentLine = lines.get(position);
                        lineNumber.setValue("" + currentLine);
                    } else {
                        position = 0;
                        currentLine = lines.get(position);
                        lineNumber.setValue("" + currentLine);
                        common.getDatabaseForApplication(activity).child("EntitySurveyData").child("CurrentLine").child(preferences.getString("ward", "")).
                                child(preferences.getString("userId", "")).setValue("" + currentLine);
                    }
                } else {
                    position = 0;
                    currentLine = lines.get(position);
                    lineNumber.setValue("" + currentLine);
                    common.getDatabaseForApplication(activity).child("EntitySurveyData").child("CurrentLine").child(preferences.getString("ward", "")).
                            child(preferences.getString("userId", "")).setValue("" + currentLine);
                }
                setMap();
                setLocation();
                setTodayCardScan();
            });
        } else {
            setMap();
        }
        checkWhetherLocationSettingsAreSatisfied();
    }

    public void setTodayCardScan() {
        new Repository().DownloadTodayCardScan(activity).observeForever(string -> {
            todayCardScanShow.setValue(string);
        });
        JSONObject jsonObjectSyncData = new JSONObject();
        if (preferences.getString("scanHousesData", "").length() > 0) {
            try {
                try {
                    jsonObjectSyncData = new JSONObject(preferences.getString("scanHousesData", "")).getJSONObject(preferences.getString("ward", ""));
                } catch (Exception ignored) {
                }
                if (jsonObjectSyncData.length() > 0) {
                    showOfflineCount.setValue("Offline Data : " + jsonObjectSyncData.length());
                    buttonBackground.setValue(activity.getDrawable(R.drawable.datasyncpng));
                } else {
                    showOfflineCount.setValue("Offline Data : " + 0);
                    buttonBackground.setValue(activity.getDrawable(R.drawable.datasyncgreen));
                }
            } catch (Exception ignored) {
            }
        } else {
            showOfflineCount.setValue("Offline Data : " + 0);
            buttonBackground.setValue(activity.getDrawable(R.drawable.datasyncgreen));
        }
    }

    public void onResumeCall() {
        try {
            setMarker();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setMap() {
        mapFragment.getMapAsync(googleMap -> {
            if (googleMap != null) {
                mMap = googleMap;
            }
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission((activity), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.getUiSettings().setCompassEnabled(false);
            mMap.setOnMarkerClickListener(marker -> {
                selectedMarker = marker;
                checkWhetherLocationSettingsAreSatisfied();
                return false;
            });
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(activity, R.raw.style_json));
            try {
                try {
                    byte[] array = Base64.decode(preferences.getString("kmlByteArray", ""), Base64.DEFAULT);
                    KmlLayer kmlLayer = new KmlLayer(mMap, new ByteArrayInputStream(array), activity);
                    kmlLayer.addLayerToMap();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
            }
            isFirstTime = true;
            lineDraw();
        });
    }

    @SuppressLint("StaticFieldLeak")
    public void checkWhetherLocationSettingsAreSatisfied() {
        LocationRequest mLocationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(1000).setNumUpdates(2);
        final LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        builder.setNeedBle(false);
        SettingsClient client = LocationServices.getSettingsClient(activity);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(activity, locationSettingsResponse -> {
            try {
                if (selectedMarker != null) {
                    String markerKeyBy = selectedMarker.getTag().toString();
                    selectedMarker = null;
                    if (markingDataObject.length() > 0) {
                        try {
                            if (markingDataObject.has(markerKeyBy)) {
                                common.setProgressBar("Please wait...", activity, activity);
                                try {
                                    JSONArray jsonArray1 = markingDataObject.getJSONArray(markerKeyBy);
                                    String houseType = jsonArray1.get(2).toString();
                                    String image = jsonArray1.get(1).toString();
                                    String[] tempStr = String.valueOf(jsonArray1.get(0)).split(",");
                                    float lineDis[] = new float[1];
                                    Location.distanceBetween(Double.parseDouble(tempStr[0]), Double.parseDouble(tempStr[1]), currentLatLng.latitude, currentLatLng.longitude, lineDis);
                                    if (lineDis[0] <= preferences.getInt("minimumDistanceBetweenMarkerAndSurvey", 10)) {
                                        common.setProgressBar("Please wait...", activity, activity);
                                        new Repository().checkNetWork(activity).observeForever(response -> {
                                            File fileOrDirectory = new File(Environment.getExternalStorageDirectory(), "SurveyApp/MarkingImages");
                                            Bitmap bitmap = checkImageOnLocal(fileOrDirectory, image);
                                            if (response) {
                                                common.getDatabaseForApplication(activity).child("EntityMarkingData/MarkedHouses/" + preferences.getString("ward", "") + "/" + currentLine + "/" + markerKeyBy).addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot snapshot) {
                                                        if (snapshot.getValue() != null) {
                                                            if (snapshot.hasChild("latLng")) {
                                                                JSONArray jsonArray = new JSONArray();
                                                                jsonArray.put(String.valueOf(snapshot.child("latLng").getValue()));
                                                                jsonArray.put(snapshot.child("image").getValue().toString());
                                                                jsonArray.put(snapshot.child("houseType").getValue().toString());
                                                                if (snapshot.hasChild("revisitKey")) {
                                                                    jsonArray.put(snapshot.child("revisitKey").getValue().toString());
                                                                } else {
                                                                    jsonArray.put("no");
                                                                }
                                                                if (snapshot.hasChild("cardNumber")) {
                                                                    jsonArray.put(snapshot.child("cardNumber").getValue().toString());
                                                                } else {
                                                                    jsonArray.put("no");
                                                                }
                                                                if (snapshot.hasChild("rfidNotFoundKey")) {
                                                                    jsonArray.put(snapshot.child("rfidNotFoundKey").getValue().toString());
                                                                } else {
                                                                    jsonArray.put("no");
                                                                }
                                                                try {
                                                                    markingDataObject.put(snapshot.getKey(), jsonArray);
                                                                } catch (Exception e) {
                                                                }
                                                            }
                                                            JSONObject jsonObject = new JSONObject();
                                                            if (!preferences.getString("markingData", "").equalsIgnoreCase("")) {
                                                                try {
                                                                    jsonObject = new JSONObject(preferences.getString("markingData", ""));
                                                                } catch (Exception e) {
                                                                }
                                                            }
                                                            try {
                                                                jsonObject.put(String.valueOf(currentLine), markingDataObject);
                                                            } catch (Exception e) {
                                                            }
                                                            preferences.edit().putString("markingData", jsonObject.toString()).apply();
                                                            try {
                                                                setMarker();
                                                            } catch (JSONException e) {
                                                                e.printStackTrace();
                                                            }
                                                        }

                                                        if (bitmap == null) {
                                                            StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + common.getDatabaseStorage(activity) + "/MarkingSurveyImages/" + preferences.getString("ward", "") + "/" + currentLine).child(Objects.requireNonNull(image));
                                                            storageReference.getBytes(Long.MAX_VALUE).addOnSuccessListener(bytes -> {
                                                                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                                                setOnLocal(bmp, image);
                                                                dialogForMarkerImage(bmp, houseType, markerKeyBy);
                                                            }).addOnFailureListener(e -> {
                                                                dialogForMarkerImage(null, houseType, markerKeyBy);
                                                            });
                                                        } else {
                                                            dialogForMarkerImage(bitmap, houseType, markerKeyBy);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {

                                                    }
                                                });
                                            } else {
                                                dialogForMarkerImage(bitmap, houseType, markerKeyBy);
                                            }
                                        });
                                    } else {
                                        common.showAlertBox(preferences.getString("messageMinimumDistanceMarkerAndSurvey", ""), false, activity);
                                    }
                                } catch (Exception e) {
                                }
                            }
                        } catch (Exception e) {
                        }
                    }

                }
            }catch (NullPointerException e){
                e.printStackTrace();
            }
        });
        task.addOnFailureListener(activity, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private Bitmap checkImageOnLocal(File fileOrDirectory, String image) {
        Bitmap isFound = null;
        try {
            try {
                if (fileOrDirectory.isDirectory()) {
                    for (File child : fileOrDirectory.listFiles()) {
                        if (child.getName().equalsIgnoreCase(preferences.getString("ward", ""))) {
                            for (File child1 : child.listFiles()) {
                                if (lines.contains(Integer.parseInt(child1.getName()))) {
                                    for (File child2 : child1.listFiles()) {
                                        if (child2.getName().equalsIgnoreCase(image)) {
                                            FileInputStream fos = null;
                                            Bitmap bitmap = null;
                                            try {
                                                fos = new FileInputStream(child2);
                                                bitmap = BitmapFactory.decodeStream(fos);
                                                isFound = bitmap;
                                                fos.close();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                        }
                                    }
                                } else {
                                    child1.delete();
                                }
                            }
                        } else {
                            child.delete();
                        }
                    }
                }
            } catch (Exception e) {
            }
        } catch (Exception e) {
        }
        return isFound;
    }

    @SuppressLint("StaticFieldLeak")
    private void setOnLocal(Bitmap bitmap, String name) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                File root = new File(Environment.getExternalStorageDirectory(), "SurveyApp/MarkingImages/" + preferences.getString("ward", "") + "/" + currentLine);
                if (!root.exists()) {
                    root.mkdirs();
                }
                File myPath = new File(root, name);
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(myPath);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    public void nextClick() {
        common.setProgressBar("Please wait...", activity, activity);
        if (endLineNo > 0) {
            try {
                if (position < (lines.size() - 1)) {
                    new Repository().checkNetWork(activity).observeForever(response -> {
                        if (response) {
                            position = position + 1;
                            currentLine = lines.get(position);
                            lineNumber.setValue(String.valueOf(currentLine));
                            lineDraw();
                        } else {
                            if (preferences.getString("isOfflineAllowed", "").equalsIgnoreCase("yes")) {
                                position = position + 1;
                                currentLine = lines.get(position);
                                lineNumber.setValue(String.valueOf(currentLine));
                                lineDraw();
                            } else {
                                common.showAlertBox("कृपया इंटरनेट चेक करे और फिर आगे बढे |", false, activity);
                            }
                        }
                    });
                } else {
                    common.showAlertBox("No Line Found", false, activity);
                }
            } catch (Exception ignored) {
            }
        } else {
            common.showAlertBox("File Not Download Yet", false, activity);
        }
    }

    public void offlineClick() {
        Drawable drawable = buttonBackground.getValue();
        if (drawable.getConstantState().equals(activity.getResources().getDrawable(R.drawable.datasyncpng).getConstantState())) {
            activity.startActivity(new Intent(activity, OfflinePageActivity.class));
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void previousClick() {
        common.setProgressBar("Please wait...", activity, activity);
        if (endLineNo > 0) {
            try {
                if (position > 0) {
                    new Repository().checkNetWork(activity).observeForever(response -> {
                        if (response) {
                            position = position - 1;
                            currentLine = lines.get(position);
                            lineNumber.setValue(String.valueOf(currentLine));
                            lineDraw();
                        } else {
                            if (preferences.getString("isOfflineAllowed", "").equalsIgnoreCase("yes")) {
                                position = position - 1;
                                currentLine = lines.get(position);
                                lineNumber.setValue(String.valueOf(currentLine));
                                lineDraw();
                            } else {
                                common.showAlertBox("कृपया इंटरनेट चेक करे और फिर आगे बढे |", false, activity);
                            }
                        }
                    });
                } else {
                    common.showAlertBox("No Line Found", false, activity);
                }
            } catch (Exception ignored) {
            }
        } else {
            common.showAlertBox("File Not Download Yet", false, activity);
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void lineData() {
        try {
            File file = new File(Environment.getExternalStorageDirectory(), "WardJson/" +
                    CommonFunctions.getInstance().getDatabaseStorage(activity) + "/" + preferences.getString("ward", "") + "/" + preferences.getString("commonReferenceDate", "") + ".json");
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder result = new StringBuilder();
            String str;
            while ((str = br.readLine()) != null) {
                result.append(str);
            }
            jsonObjectLines = new JSONObject(String.valueOf(result));
            if (jsonObjectLines.has("totalLines")) {
                endLineNo = Integer.parseInt(jsonObjectLines.get("totalLines").toString());
            }
        } catch (Exception ignored) {
        }
    }

    public void lineDraw() {
        directionPositionList.clear();
        try {
            int size = hashLineMarker.size();
            for (int i = 0; i < size; i++) {
                try {
                    Polyline marker = hashLineMarker.get((hashLineMarker.keySet().toArray())[0]);
                    marker.remove();
                    hashLineMarker.remove((hashLineMarker.keySet().toArray())[0]);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
        try {
            for (int j = lines.get(0); j <= lines.get(lines.size() - 1); j++) {
                try {
                    jsonArray = jsonObjectLines.getJSONObject(String.valueOf(j)).getJSONArray("points");
                } catch (Exception e) {
                }
                ArrayList<LatLng> commonDirectionPositionList = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    n = jsonArray.getJSONArray(i).getDouble(0);
                    m = jsonArray.getJSONArray(i).getDouble(1);
                    if (j == currentLine) {
                        directionPositionList.add(new LatLng(n, m));
                    } else {
                        commonDirectionPositionList.add(new LatLng(n, m));
                    }
                    latLng = new LatLng(n, m);
                }
                Polyline polyline = null;
                if (j == currentLine) {
                    polyline = mMap.addPolyline(new PolylineOptions().addAll((directionPositionList)).endCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.upper60),
                            30)).startCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.start50),
                            30)).width(8));
                } else {
                    polyline = mMap.addPolyline(new PolylineOptions().addAll((commonDirectionPositionList)).width(8));
                    polyline.setColor(Color.parseColor("#5abcff"));
                    commonDirectionPositionList.clear();
                }
                polyline.setTag(String.valueOf(j));
                hashLineMarker.put(j, polyline);
            }
        } catch (JSONException ignored) {

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        showMarker();
    }

    @SuppressLint("StaticFieldLeak")
    private void showMarker() {
        new Repository().checkNetWork(activity).observeForever(response -> {
            JSONObject jsonObjectMarking = new JSONObject();
            if (!preferences.getString("markingData", "").equalsIgnoreCase("")) {
                try {
                    jsonObjectMarking = new JSONObject(preferences.getString("markingData", ""));
                } catch (Exception e) {
                }
            }
            if (jsonObjectMarking.has(String.valueOf(currentLine))) {
                try {
                    setMarker();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                JSONObject finalJsonObjectMarking = jsonObjectMarking;
                new Repository().MarkingLine(activity, "" + currentLine).observeForever(dataSnapshot -> {
                    if (dataSnapshot.getValue() != null) {
                        JSONObject markingDataObject = new JSONObject();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            if (snapshot.hasChild("latLng")) {
                                JSONArray jsonArray = new JSONArray();
                                jsonArray.put(String.valueOf(snapshot.child("latLng").getValue()));
                                jsonArray.put(snapshot.child("image").getValue().toString());
                                jsonArray.put(snapshot.child("houseType").getValue().toString());
                                if (snapshot.hasChild("revisitKey")) {
                                    jsonArray.put(snapshot.child("revisitKey").getValue().toString());
                                } else {
                                    jsonArray.put("no");
                                }
                                if (snapshot.hasChild("cardNumber")) {
                                    jsonArray.put(snapshot.child("cardNumber").getValue().toString());
                                } else {
                                    jsonArray.put("no");
                                }
                                if (snapshot.hasChild("rfidNotFoundKey")) {
                                    jsonArray.put(snapshot.child("rfidNotFoundKey").getValue().toString());
                                } else {
                                    jsonArray.put("no");
                                }
                                try {
                                    markingDataObject.put(snapshot.getKey(), jsonArray);
                                } catch (Exception e) {
                                }
                            }
                        }
                        try {
                            finalJsonObjectMarking.put(String.valueOf(currentLine), markingDataObject);
                        } catch (Exception e) {
                        }
                        preferences.edit().putString("markingData", finalJsonObjectMarking.toString()).apply();
                        try {
                            setMarker();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            if (!response) {
                try {
                    setMarker();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setMarker() throws JSONException {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        try {
            int size = hashMapMarker.size();
            for (int i = 0; i < size; i++) {
                try {
                    Marker marker = hashMapMarker.get((hashMapMarker.keySet().toArray())[0]);
                    marker.remove();
                    hashMapMarker.remove((hashMapMarker.keySet().toArray())[0]);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
        if (!preferences.getString("markingData", "").equalsIgnoreCase("")) {
            JSONObject jsonObject = new JSONObject(preferences.getString("markingData", ""));
            if (jsonObject.has(String.valueOf(currentLine))) {
                markingDataObject = jsonObject.getJSONObject(String.valueOf(currentLine));
                Iterator<String> listKEY = markingDataObject.keys();
                while (listKEY.hasNext()) {
                    String key = (String) listKEY.next();
                    try {
                        JSONArray jsonArray = markingDataObject.getJSONArray(key);
                        String[] tempStr = String.valueOf(jsonArray.get(0)).split(",");
                        builder.include(new LatLng(Double.parseDouble(tempStr[0]), Double.parseDouble(tempStr[1])));
                        printMarkerWithLine(new LatLng(Double.parseDouble(tempStr[0]), Double.parseDouble(tempStr[1])),
                                Integer.parseInt(String.valueOf(jsonArray.get(2))), Integer.parseInt(key), jsonArray.get(3).toString(), jsonArray.get(4).toString(), jsonArray.get(5).toString());
                    } catch (Exception e) {
                    }
                }
                totalCard.setValue("" + markingDataObject.length());
            } else {
                markingDataObject = new JSONObject();
                totalCard.setValue("0");
            }
        }

        if (directionPositionList.size() > 0) {
            for (int i = 0; i < directionPositionList.size(); i++) {
                builder.include(directionPositionList.get(i));
            }
        }
        if (currentLatLng != null) {
            builder.include(currentLatLng);
        }
        LatLngBounds bounds = builder.build();
        int padding = 100;
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.animateCamera(cu);
        common.closeDialog();
    }

    private void printMarkerWithLine(LatLng latLng, int type, Integer tag, String revisit, String survey, String rfidNotFound) {
        try {
            Marker marker = hashMapMarker.get(tag);
            marker.remove();
            hashMapMarker.remove(tag);
        } catch (Exception e) {
        }
        if (!survey.equalsIgnoreCase("no")) {
            int height = 50;
            int width = 60;
            BitmapDrawable bitMapDraw = (BitmapDrawable) activity.getResources().getDrawable(R.drawable.green_marker);
            Bitmap b = bitMapDraw.getBitmap();
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

            Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
            markers.setTag(tag);
            hashMapMarker.put(tag, markers);
        } else if (!revisit.equalsIgnoreCase("no")) {
            int height = 40;
            int width = 40;
            BitmapDrawable bitMapDraw = (BitmapDrawable) activity.getResources().getDrawable(R.drawable.pin_e);
            Bitmap b = bitMapDraw.getBitmap();
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

            Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
            markers.setTag(tag);
            hashMapMarker.put(tag, markers);
        } else if (!rfidNotFound.equalsIgnoreCase("no")) {
            int height = 50;
            int width = 60;
            BitmapDrawable bitMapDraw = (BitmapDrawable) activity.getResources().getDrawable(R.drawable.green_marker);
            Bitmap b = bitMapDraw.getBitmap();
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

            Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
            markers.setTag(tag);
            hashMapMarker.put(tag, markers);
        } else {
            if (type == 1 || type == 19) {
                Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(common.BitmapFromVector(activity, R.drawable.house)));
                markers.setTag(tag);
                hashMapMarker.put(tag, markers);
            } else if (type == 2 || type == 3 || type == 6 || type == 7 || type == 8 || type == 9 || type == 10 || type == 20) {
                Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(common.BitmapFromVector(activity, R.drawable.shop)));
                markers.setTag(tag);
                hashMapMarker.put(tag, markers);
            } else if (type == 14 || type == 15) {
                Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(common.BitmapFromVector(activity, R.drawable.warehouse)));
                markers.setTag(tag);
                hashMapMarker.put(tag, markers);
            } else if (type == 21 || type == 22) {
                Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(common.BitmapFromVector(activity, R.drawable.institute)));
                markers.setTag(tag);
                hashMapMarker.put(tag, markers);
            } else if (type == 4 || type == 5) {
                Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(common.BitmapFromVector(activity, R.drawable.hotel)));
                markers.setTag(tag);
                hashMapMarker.put(tag, markers);
            } else if (type == 16 || type == 17) {
                Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(common.BitmapFromVector(activity, R.drawable.hall)));
                markers.setTag(tag);
                hashMapMarker.put(tag, markers);
            } else if (type == 18) {
                Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(common.BitmapFromVector(activity, R.drawable.thela)));
                markers.setTag(tag);
                hashMapMarker.put(tag, markers);
            } else if (type == 11 || type == 12 || type == 13) {
                Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(common.BitmapFromVector(activity, R.drawable.hospital)));
                markers.setTag(tag);
                hashMapMarker.put(tag, markers);
            }
        }
    }

    private void dialogForMarkerImage(Bitmap bitmap, String datum, String tag) {
        activity.runOnUiThread(() -> {
            common.closeDialog();
            try {
                if (customTimerAlertBox != null) {
                    customTimerAlertBox.dismiss();
                }
            } catch (Exception e) {
            }
            LayoutInflater inflater = activity.getLayoutInflater();
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
            View dialogLayout = inflater.inflate(R.layout.markers_image_view_layout, null);
            alertDialog.setView(dialogLayout);
            alertDialog.setCancelable(false);
            ImageView markerImage = dialogLayout.findViewById(R.id.marker_iv);
            Button revisitBtn = dialogLayout.findViewById(R.id.revisit_btn);
            EditText scanS = dialogLayout.findViewById(R.id.cardscans);
            TextView closeBtn = dialogLayout.findViewById(R.id.close_btn);
            TextView noteTv = dialogLayout.findViewById(R.id.noteText);
            Button ocrScanCardBtns = dialogLayout.findViewById(R.id.ocrScanCardBtns);
            scanS.setFocusableInTouchMode(true);
            scanS.requestFocus();
            houseT = datum;
            markingKey = tag;
            if (preferences.getString("byRFID", "no").equalsIgnoreCase("yes")) {
                ocrScanCardBtns.setVisibility(View.GONE);
                scanS.setVisibility(View.VISIBLE);
                noteTv.setText(preferences.getString("scanByRfidNoteMessage", ""));
            } else {
                scanS.setVisibility(View.GONE);
                ocrScanCardBtns.setVisibility(View.VISIBLE);
                noteTv.setText(preferences.getString("scanByCameraNoteMessage", ""));
            }
            JSONArray jsonArray1 = new JSONArray();
            try {
                jsonArray1 = markingDataObject.getJSONArray(markingKey);
            } catch (Exception e) {
            }
            JSONArray finalJsonArray = jsonArray1;
            ocrScanCardBtns.setOnClickListener(v1 -> {
                if (customTimerAlertBox != null) {
                    customTimerAlertBox.dismiss();
                }
                try {
                    if (finalJsonArray.get(5).toString().equalsIgnoreCase("no")) {
                        Intent intent = new Intent(activity, ScanCardPageActivity.class);
                        preferences.edit().putInt("line", currentLine).apply();
                        preferences.edit().putString("lat", String.valueOf(lat)).apply();
                        preferences.edit().putString("lng", String.valueOf(lng)).apply();
                        preferences.edit().putString("houseType", datum).apply();
                        preferences.edit().putString("markingKey", markingKey).apply();
                        preferences.edit().putString("markingDatas", markingDataObject.toString()).apply();
                        preferences.edit().putString("markingCard", finalJsonArray.get(4).toString()).apply();
                        preferences.edit().putString("markingRevisit", finalJsonArray.get(3).toString()).apply();
                        activity.startActivity(intent);
                    } else {
                        common.showAlertBox("इस मार्किंग पे पहले ही survey हो चूका है |", false, activity);
                    }
                } catch (Exception e) {
                }
            });
            scanS.setOnKeyListener((view1, i, keyEvent) -> {
                if (scanS.getText().length() == 10) {
                    if (customTimerAlertBox != null) {
                        customTimerAlertBox.dismiss();
                    }
                    cardScanMethod(scanS.getText().toString(), scanS);
                }
                if (scanS.getText().toString().trim().length() > 10) {
                    final MediaPlayer mp = MediaPlayer.create(activity, R.raw.warningsound);
                    mp.start();
                    scanS.getText().clear();
                }
                if (i == KeyEvent.KEYCODE_BACK && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    activity.onBackPressed();
                }
                return true;
            });
            if (bitmap != null) {
                markerImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                markerImage.setImageBitmap(bitmap);
            }
            revisitBtn.setOnClickListener(view1 -> {
                if (customTimerAlertBox != null) {
                    customTimerAlertBox.dismiss();
                }
                try {
                    if (finalJsonArray.get(3).toString().equalsIgnoreCase("no") && finalJsonArray.get(4).toString().equalsIgnoreCase("no") && finalJsonArray.get(5).toString().equalsIgnoreCase("no")) {
                        preferences.edit().putString("markingKey", markingKey).apply();
                        preferences.edit().putString("houseType", datum).apply();
                        preferences.edit().putString("lat", String.valueOf(lat)).apply();
                        preferences.edit().putString("lng", String.valueOf(lng)).apply();
                        preferences.edit().putString("line", String.valueOf(currentLine)).apply();
                        Intent intent = new Intent(activity, RevisitPageActivity.class);
                        activity.startActivity(intent);
                    } else {
                        if (!finalJsonArray.get(4).toString().equalsIgnoreCase("no")) {
                            common.showAlertBox("इस मार्किंग पे पहले ही Survey हो चूका है |", false, activity);
                        } else if (!finalJsonArray.get(5).toString().equalsIgnoreCase("no")) {
                            common.showAlertBox("इस मार्किंग पे पहले ही survey हो चूका है |", false, activity);
                        } else {
                            common.showAlertBox("इस मार्किंग पे पहले ही Revisit हो चूका है |", false, activity);
                        }
                    }
                } catch (Exception e) {
                }
            });
            closeBtn.setOnClickListener(view1 -> {
                if (customTimerAlertBox != null) {
                    customTimerAlertBox.dismiss();
                }
            });
            customTimerAlertBox = alertDialog.create();
            customTimerAlertBox.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            if (!activity.isFinishing()) {
                customTimerAlertBox.show();
            }
        });
    }

    private void cardScanMethod(String cardNumber, EditText scanS) {
        if (cardNumber.contains(";")) {
            String[] hexa = cardNumber.trim().split(";");
            String[] hexas = hexa[1].trim().split("\\?");
            long num = Long.parseLong(hexas[0], 16);
            cardNumber = "" + num;
            if (String.valueOf(num).length() == 9) {
                cardNumber = "0" + num;
            } else if (String.valueOf(num).length() == 8) {
                cardNumber = "00" + num;
            }
        }
        try {
            Long.parseLong(cardNumber);
            common.doVibrate(activity);
            rfID = cardNumber;
            common.setProgressBar("Please wait...", activity, activity);
            preferences.edit().putString("rfid", rfID).apply();
            preferences.edit().putString("lat", String.valueOf(lat)).apply();
            preferences.edit().putString("lng", String.valueOf(lng)).apply();
            preferences.edit().putString("line", String.valueOf(currentLine)).apply();
            preferences.edit().putString("cardNo", "").apply();
            preferences.edit().putString("houseType", houseT).apply();
            preferences.edit().putString("markingKey", markingKey).apply();

            JSONObject jsonObject;
            int isCardFound = 1;
            String cardNo = "";
            try {
                jsonObject = new JSONObject(preferences.getString("CardScanData", ""));
                if (jsonObject.has(rfID)) {
                    JSONArray jsonArray = jsonObject.getJSONArray(rfID);
                    cardNo = jsonArray.get(0).toString();
                    preferences.edit().putString("cardNo", cardNo).apply();
                } else {
                    isCardFound = 2;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (isCardFound == 1) {
                try {
                    JSONArray jsonArray1 = markingDataObject.getJSONArray(markingKey);
                    preferences.edit().putString("markingRevisit", jsonArray1.get(3).toString()).apply();
                    if (!jsonArray1.get(5).toString().equalsIgnoreCase("no")) {
                        common.showAlertBox("इस मार्किंग पे पहले ही survey हो चूका है |", false, activity);
                    } else {
                        if (jsonArray1.get(4).toString().equalsIgnoreCase("no")) {
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
                                moveNextActivity(1);
                            }
                        } else {
                            if (jsonArray1.get(4).toString().equalsIgnoreCase(preferences.getString("cardNo", ""))) {
                                moveNextActivity(1);
                            } else {
                                common.closeDialog();
                                String messageString = "";
                                try {
                                    String[] message = preferences.getString("sameMarkerOnTwoCard", "").split("#");
                                    messageString = message[0] + jsonArray1.get(4) + message[1];
                                } catch (Exception e) {
                                }
                                common.showAlertBox(messageString, false, activity);
                            }
                        }
                    }
                } catch (Exception e) {
                }
            } else {
                try {
                    JSONArray jsonArray1 = markingDataObject.getJSONArray(markingKey);
                    if (jsonArray1.get(3).toString().equalsIgnoreCase("no") && jsonArray1.get(4).toString().equalsIgnoreCase("no") && jsonArray1.get(5).toString().equalsIgnoreCase("no")) {
                        moveNextActivity(2);
                    } else {
                        if (!jsonArray1.get(4).toString().equalsIgnoreCase("no")) {
                            common.showAlertBox("इस मार्किंग पे पहले ही Survey हो चूका है |", false, activity);
                        } else if (!jsonArray1.get(5).toString().equalsIgnoreCase("no")) {
                            common.showAlertBox("इस मार्किंग पे पहले ही survey हो चूका है |", false, activity);
                        } else {
                            common.showAlertBox("इस मार्किंग पे पहले ही Revisit हो चूका है |", false, activity);
                        }
                    }
                } catch (Exception e) {
                }
            }
            scanS.getText().clear();
        } catch (NumberFormatException e) {
            final MediaPlayer mp = MediaPlayer.create(activity, R.raw.warningsound);
            mp.start();
        }
    }

    public void moveNextActivity(int i) {
        try {
            if (!(countDownTimerLocation == null)) {
                countDownTimerLocation.cancel();
                countDownTimerLocation = null;
            }
        } catch (Exception e) {
        }
        common.closeDialog();
        if (i == 2) {
            Intent intent = new Intent(activity, FormPageActivity.class);
            intent.putExtra("from", "map");
            activity.startActivity(intent);
        } else {
            Intent intent = new Intent(activity, VerifyPageActivity.class);
            activity.startActivity(intent);
        }
    }

    public void setLocation() {
        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission((activity), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null && locationResult.getLocations().size() > 0) {
                    int latestLocationIndex = locationResult.getLocations().size() - 1;
                    lat = locationResult.getLocations().get(latestLocationIndex).getLatitude();
                    lng = locationResult.getLocations().get(latestLocationIndex).getLongitude();
                    currentLatLng = new LatLng(lat, lng);
                    if (countDownTimerLocation == null) {
                        if (isFirstTime) {
                            if (activity != null) {
                                isFirstTime = false;
                                common.setMovingMarker(mMap, currentLatLng, activity);
                            }
                        }
                        if (activity != null) {
                            currentLocationShow();
                        }
                    } else {
                        if (isFirstTime) {
                            if (activity != null) {
                                isFirstTime = false;
                                common.setMovingMarker(mMap, currentLatLng, activity);
                            }
                        }
                    }
                }
            }
        };
        LocationServices.getFusedLocationProviderClient((activity)).requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @SuppressLint("MissingPermission")
    public void currentLocationShow() {
        countDownTimerLocation = new CountDownTimer(2000, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                common.currentLocationShow(currentLatLng);
                currentLocationShow();
            }
        }.start();
    }
}
