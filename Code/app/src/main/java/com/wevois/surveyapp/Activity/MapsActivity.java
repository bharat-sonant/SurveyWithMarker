package com.wevois.surveyapp.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
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
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Looper;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.AvoidType;
import com.akexorcist.googledirection.constant.RequestResult;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.util.DirectionConverter;
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
import com.google.android.gms.maps.OnMapReadyCallback;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.maps.android.data.kml.KmlLayer;
import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private final static int REQUEST_CHECK_SETTINGS = 500;
    Button btnDataSync;
    TextView lineNumber, todayCardScanShow, totalMarkingCard, showOffLineData;
    SharedPreferences preferences;
    DatabaseReference databaseReferencePath, currentLocationDatabaseRef, markingDatabaseReference;
    ValueEventListener markingValueEventListener;
    ValueEventListener valueEventListener;
    JSONObject jsonObject;
    JSONArray jsonArray;
    String str, date, rfID, houseT = "", markingKey = "";
    int currentLine = 1, position = 0, currentLocationCaptureTime = 10000;
    double n, m, lat, lng;
    Polyline polyline2, polyline1;
    ArrayList<LatLng> directionPositionList = new ArrayList<>();
    LatLng latLng, currentLatLng;
    CommonFunctions common = new CommonFunctions();
    boolean line = false, isFirstTime = true,isChecked = true;
    CountDownTimer countDownTimerLocation, countDownTimerCurrentLocation;
    private LocationCallback locationCallback;
    JSONObject markingDataObject = new JSONObject();

    AlertDialog customTimerAlertBox;
    ProgressDialog dialog;
    ArrayList<Integer> lines = new ArrayList<>();

    HashMap<Integer, Marker> hashMapMarker = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initPage();
        sharedPreference();
        showTodayCardScan();
        setCurrentLine();
        getTimeDisForCurrentLoc();
        setActions();
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission((MapsActivity.this), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.setOnMarkerClickListener(marker -> {
            if (markingDataObject.length() > 0) {
                try {
                    if (markingDataObject.has(marker.getTag().toString())) {
                        setProgressBar("", "Please wait...", this, this);
                        try {
                            JSONArray jsonArray1 = markingDataObject.getJSONArray(marker.getTag().toString());
                            String houseType = jsonArray1.get(2).toString();
                            String image = jsonArray1.get(1).toString();
                            new AsyncTask<Void, Void, Boolean>() {
                                @Override
                                protected void onPreExecute() {
                                    super.onPreExecute();
                                }

                                @Override
                                protected Boolean doInBackground(Void... p) {
                                    return common.network(MapsActivity.this);
                                }

                                @Override
                                protected void onPostExecute(Boolean result) {
                                    if (result) {
                                        MapsActivity.this.runOnUiThread(() -> {
                                            try {
                                                StorageReference storageReference = FirebaseStorage.getInstance()
                                                        .getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + common.getDatabaseStorage(MapsActivity.this) + "/MarkingSurveyImages/" + preferences.getString("ward", "") + "/" + currentLine)
                                                        .child(Objects.requireNonNull(image));
                                                storageReference.getBytes(Long.MAX_VALUE).addOnSuccessListener(bytes -> {
                                                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                                    dialogForMarkerImage(bmp, houseType, marker.getTag().toString());
                                                }).addOnFailureListener(e -> {
                                                    dialogForMarkerImage(null, houseType, marker.getTag().toString());
                                                });
                                            }catch (Exception e){}
                                        });
                                    } else {
                                        dialogForMarkerImage(null, houseType, marker.getTag().toString());
                                    }
                                }
                            }.execute();
                        } catch (Exception e) {
                        }
                    }
                } catch (Exception e) {
                }
            }
            return false;
        });
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(MapsActivity.this, R.raw.style_json));
        lineData();
        checkWhetherLocationSettingsAreSatisfied(0);
    }

    private void initPage() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        databaseReferencePath = common.getDatabaseForApplication(this);
        preferences = getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
        btnDataSync = findViewById(R.id.dataSync);
        showOffLineData = findViewById(R.id.showOffLineData);
        lineNumber = findViewById(R.id.line5);
        todayCardScanShow = findViewById(R.id.scancardshow);
        totalMarkingCard = findViewById(R.id.totalCard);

        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        date = dateFormat.format(new Date());
    }

    private void sharedPreference() {
        String[] linesData = preferences.getString("lines", "").split(",");
        for (int i = 0; i < linesData.length; i++) {
            lines.add(Integer.parseInt(linesData[i]));
        }
        offlineData();
    }

    private void offlineData() {
        JSONObject jsonObjectSyncData = new JSONObject();
        if (preferences.getString("scanHousesData", "").length() > 0) {
            try {
                try {
                    jsonObjectSyncData = new JSONObject(preferences.getString("scanHousesData", "")).getJSONObject(preferences.getString("ward", ""));
                } catch (Exception ignored) {
                }
                if (jsonObjectSyncData.length() > 0) {
                    showOffLineData.setText("Offline Data : " + jsonObjectSyncData.length());
                    btnDataSync.setBackground(this.getDrawable(R.drawable.datasyncpng));
                } else {
                    showOffLineData.setText("Offline Data : " + 0);
                    btnDataSync.setBackground(this.getDrawable(R.drawable.datasyncgreen));
                }
            } catch (Exception ignored) {
            }
        } else {
            showOffLineData.setText("Offline Data : " + 0);
            btnDataSync.setBackground(this.getDrawable(R.drawable.datasyncgreen));
        }
        btnDataSync.setOnClickListener(v -> {
            Drawable drawable = btnDataSync.getBackground();
            if (drawable.getConstantState().equals(getResources().getDrawable(R.drawable.datasyncpng).getConstantState())) {
                startActivity(new Intent(MapsActivity.this, OfflineActivity.class));
            }
        });
    }

    private void showTodayCardScan() {
        databaseReferencePath.child("EntitySurveyData").child("DailyHouseCount").child(preferences.getString("ward", "")).child(preferences.getString("userId", "")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if ((dataSnapshot.getValue() != null) && dataSnapshot.hasChild(date)) {
                    todayCardScanShow.setText(dataSnapshot.child(date).getValue().toString());
                } else {
                    todayCardScanShow.setText("0");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void setCurrentLine() {
        common.setProgressBar("Please wait...",this,this);
        databaseReferencePath.child("EntitySurveyData").child("CurrentLine").child(preferences.getString("ward", "") + "/" + preferences.getString("userId", "")).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if ((dataSnapshot.getValue() != null)) {
                    if (lines.contains(Integer.parseInt(dataSnapshot.getValue().toString()))) {
                        position = lines.indexOf(Integer.parseInt(dataSnapshot.getValue().toString()));
                        currentLine = lines.get(position);
                        lineNumber.setText("" + currentLine);
                    } else {
                        position = 0;
                        currentLine = lines.get(position);
                        lineNumber.setText("" + currentLine);
                        databaseReferencePath.child("EntitySurveyData").child("CurrentLine").child(preferences.getString("ward", "")).
                                child(preferences.getString("userId", "")).setValue("" + currentLine);
                    }
                } else {
                    position = 0;
                    currentLine = lines.get(position);
                    lineNumber.setText("" + currentLine);
                    databaseReferencePath.child("EntitySurveyData").child("CurrentLine").child(preferences.getString("ward", "")).
                            child(preferences.getString("userId", "")).setValue("" + currentLine);
                }
                SensorManager sManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                sManager.registerListener(common.mySensorEventListener, sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
                sManager.registerListener(common.mySensorEventListener, sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                if (mapFragment != null) {
                    mapFragment.getMapAsync(MapsActivity.this);
                }
                jsonObject = new JSONObject();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setActions() {
        findViewById(R.id.lineprevious).setOnClickListener(view1 -> previousLineButton());

        findViewById(R.id.linenext).setOnClickListener(view1 -> nextLineButton());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isChecked) {
            isChecked = false;
        } else {
            offlineData();
            showTodayCardScan();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menuafterinstallation, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            if (item.getItemId() == R.id.scanByRFID) {
                preferences.edit().putString("byRFID", "yes").apply();
            }

            if (item.getItemId() == R.id.scanByCamera) {
                preferences.edit().putString("byRFID", "no").apply();
            }

            if (item.getItemId() == R.id.item1) {
                if (jsonObject.length() > 0 && currentLine <= jsonObject.length()) {
                    if (lat != 0.0 && lng != 0.0) {
                        Intent intent = new Intent(MapsActivity.this, RevisitActivity.class);
                        intent.putExtra("line", String.valueOf(currentLine));
                        intent.putExtra("lat", String.valueOf(lat));
                        intent.putExtra("lng", String.valueOf(lng));
                        startActivity(intent);
                    }
                } else {
                    common.showAlertBox("File Not Download Yet OR Line Completed", false, MapsActivity.this);
                }
            }

//            if (item.getItemId() == R.id.item3) {
//                LinearLayout layout = new LinearLayout(MapsActivity.this);
//                layout.setOrientation(LinearLayout.VERTICAL);
//                final AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
//                builder.setCancelable(false);
//                builder.setTitle("एंटर लाइन नंबर");
//                final EditText input = new EditText(MapsActivity.this);
//                input.setInputType(InputType.TYPE_CLASS_NUMBER);
//                input.setHint("लाइन नंबर डाले");
//                layout.addView(input);
//                builder.setView(layout);
//                builder.setPositiveButton(Html.fromHtml("OK"), (dialog, which) -> {
//                });
//                builder.setNegativeButton(Html.fromHtml("Cancel"), (dialogInterface, i) -> dialogInterface.cancel());
//                AlertDialog dialog = builder.create();
//                if (!((Activity) MapsActivity.this).isFinishing())
//                    dialog.show();
//
//                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
//                    boolean isError = true;
//                    if (input.getText().toString().length() == 0) {
//                        input.setError("Line number cannot be empty");
//                    }
//                    if (Integer.parseInt(input.getText().toString()) > jsonObject.length()) {
//                        input.setError("Line number not valid");
//                    }
//                    if (Integer.parseInt(input.getText().toString()) <= jsonObject.length() && Integer.parseInt(input.getText().toString()) > 0) {
//                        isError = false;
//                        if (markers != null) {
//                            for (int a = 0; a < markers.size(); a++) {
//                                markers.get(a).remove();
//                            }
//                        }
//                        showMarker();
//                    }
//                    if (!isError)
//                        dialog.dismiss();
//                });
//            }

            if (item.getItemId() == R.id.item4) {
                common.showAlertBox("1.0.3.4", false, MapsActivity.this);
            }
        } catch (Exception ignored) {
        }
        return super.onOptionsItemSelected(item);
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
            common.doVibrate(MapsActivity.this);
            rfID = cardNumber;
            common.setProgressBar("Please wait...", MapsActivity.this, MapsActivity.this);
            preferences.edit().putString("rfid", rfID).apply();
            preferences.edit().putString("lat", String.valueOf(lat)).apply();
            preferences.edit().putString("lng", String.valueOf(lng)).apply();
            preferences.edit().putString("line", String.valueOf(currentLine)).apply();
            preferences.edit().putString("cardNo", "").apply();
            preferences.edit().putString("houseType", houseT).apply();
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
                try {
                    JSONArray jsonArray1 = markingDataObject.getJSONArray(markingKey);
                    if (jsonArray1.get(4).toString().equalsIgnoreCase("no")){
                        Iterator<String> listKEY = markingDataObject.keys();
                        boolean isFound =false;
                        while (listKEY.hasNext()) {
                            String key = (String) listKEY.next();
                            try {
                                JSONArray jsonArray = markingDataObject.getJSONArray(key);
                                if (jsonArray.get(4).toString().equalsIgnoreCase(preferences.getString("cardNo", ""))){
                                    isFound = true;
                                }
                            }catch (Exception e){}
                        }
                        if (isFound){
                            common.showAlertBox(preferences.getString("sameCardOnTwoMarkerMessage",""), false, this);
                        }else {
                            moveNextActivity();
                        }
                    }else {
                        if (jsonArray1.get(4).toString().equalsIgnoreCase(preferences.getString("cardNo", ""))) {
                            moveNextActivity();
                        } else {
                            closeDialog();
                            String messageString="";
                            try {
                                String[] message = preferences.getString("sameMarkerOnTwoCard","").split("#");
                                messageString = message[0] + jsonArray1.get(4) + message[1];
                            } catch (Exception e) {
                            }
                            common.showAlertBox(messageString, false, this);
                        }
                    }
                } catch (Exception e) {
                }
            } else if (isVerified == 2) {
                common.showAlertBox(cardNo + " यह कार्ड Already Verified है | ", true, MapsActivity.this);
            } else {
                common.showAlertBox(cardNo + " यह कार्ड डेटाबेस में नहीं  है |", true, MapsActivity.this);
            }
            scanS.getText().clear();
        } catch (NumberFormatException e) {
            final MediaPlayer mp = MediaPlayer.create(MapsActivity.this, R.raw.warningsound);
            mp.start();
        }
    }

    public void moveNextActivity() {
        try {
            if (!(countDownTimerLocation == null)) {
                countDownTimerLocation.cancel();
                countDownTimerLocation = null;
            }
            if (!(countDownTimerCurrentLocation == null)) {
                countDownTimerCurrentLocation.cancel();
                countDownTimerCurrentLocation = null;
            }
        } catch (Exception e) {
        }
        common.closeDialog();
        Intent intent = new Intent(MapsActivity.this, VerifyActivity.class);
        startActivity(intent);
    }

    @SuppressLint("StaticFieldLeak")
    private void nextLineButton() {
        if (jsonObject.length() > 0) {
            try {
                if (position < (lines.size() - 1)) {
                    setProgressBar("Please wait...", "", this, this);
                    new AsyncTask<Void, Void, Boolean>() {
                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                        }

                        @Override
                        protected Boolean doInBackground(Void... p) {
                            return common.network(MapsActivity.this);
                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            if (result) {
                                int count = 0;
                                Iterator<String> listKEY = markingDataObject.keys();
                                while (listKEY.hasNext()) {
                                    String key = (String) listKEY.next();
                                    try {
                                        JSONArray jsonArray = markingDataObject.getJSONArray(key);
                                        if (jsonArray.get(3).toString().equalsIgnoreCase("yes")) {
                                            count++;
                                        }
                                    } catch (Exception e) {
                                    }
                                }
                                if (markingDataObject.length() == count) {
                                    position = position + 1;
                                    currentLine = lines.get(position);
                                    lineNumber.setText(String.valueOf(currentLine));
                                    lineDraw();
                                } else {
                                    databaseReferencePath.child("EntitySurveyData/SurveyLineSkippedData/" + preferences.getString("ward", "") + "/" + currentLine + "/" + preferences.getString("userId", "")).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            String data = "";
                                            if (dataSnapshot.getValue() != null) {
                                                data = dataSnapshot.getValue().toString();
                                            }
                                            if (data.equalsIgnoreCase("yes")) {
                                                position = position + 1;
                                                currentLine = lines.get(position);
                                                lineNumber.setText(String.valueOf(currentLine));
                                                lineDraw();
                                            } else {
                                                closeDialog();
                                                common.showAlertBox(preferences.getString("lineNotCompleteMessage",""), false, MapsActivity.this);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });
                                }
                            } else {
                                position = position + 1;
                                currentLine = lines.get(position);
                                lineNumber.setText(String.valueOf(currentLine));
                                lineDraw();
                            }
                        }
                    }.execute();
                } else {
                    common.showAlertBox("No Line Found", false, MapsActivity.this);
                }
            } catch (Exception ignored) {
            }
        } else {
            common.showAlertBox("File Not Download Yet", false, MapsActivity.this);
        }
    }

    private void previousLineButton() {
        if (jsonObject.length() > 0) {
            try {
                if (position > 0) {
                    position = position - 1;
                    currentLine = lines.get(position);
                    lineNumber.setText(String.valueOf(currentLine));
                    lineDraw();
                } else {
                    common.showAlertBox("No Line Found", false, MapsActivity.this);
                }
            } catch (Exception ignored) {
            }
        } else {
            common.showAlertBox("File Not Download Yet", false, MapsActivity.this);
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void lineData() {
        try {
            File file = new File(Environment.getExternalStorageDirectory(), "WardJson/" + preferences.getString("ward", "") + ".json");
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder result = new StringBuilder();
            while ((str = br.readLine()) != null) {
                result.append(str);
            }
            jsonObject = new JSONObject(String.valueOf(result));
            lineDraw();
        } catch (Exception ignored) {
        }
    }

    public void lineDraw() {
        mMap.clear();
        isFirstTime = true;
        directionPositionList.clear();
        try {
            for (int j = lines.get(0); j <= lines.get(lines.size() - 1); j++) {
                if (j == currentLine) {
                    jsonArray = jsonObject.getJSONObject(String.valueOf(j)).getJSONArray("points");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        n = jsonArray.getJSONArray(i).getDouble(0);
                        m = jsonArray.getJSONArray(i).getDouble(1);
                        directionPositionList.add(new LatLng(n, m));
                        latLng = new LatLng(n, m);
                    }
                    polyline1 = mMap.addPolyline(new PolylineOptions().addAll((directionPositionList)).endCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.upper60),
                            30)).startCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.start50),
                            30)).width(8));
                    line = true;
                    isFirstTime = true;
                } else {
                    setPolyLine(String.valueOf(j), Color.parseColor("#5abcff"));
                }
            }
        } catch (JSONException ignored) {
        }
        databaseReferencePath.child("EntitySurveyData").child("CurrentLine").child(preferences.getString("ward", "")).
                child(preferences.getString("userId", "")).setValue("" + currentLine);
        try {
            markingDatabaseReference.removeEventListener(markingValueEventListener);
        } catch (Exception e) {
        }
        showMarker();
        new DownloadKmlFile(common.getKmlFilePath(preferences.getString("ward", ""), this)).execute();
    }

    private void setPolyLine(String lineNo, int color) {
        ArrayList<LatLng> commonDirectionPositionList = new ArrayList<>();
        try {
            jsonArray = jsonObject.getJSONObject(lineNo).getJSONArray("points");
            for (int i = 0; i < jsonArray.length(); i++) {
                n = jsonArray.getJSONArray(i).getDouble(0);
                m = jsonArray.getJSONArray(i).getDouble(1);
                commonDirectionPositionList.add(new LatLng(n, m));
                latLng = new LatLng(n, m);
                if (i == jsonArray.length() - 1) {
                    polyline2 = mMap.addPolyline(new PolylineOptions().addAll((commonDirectionPositionList)).width(8));
                    polyline2.setColor(color);
                    commonDirectionPositionList.clear();
                }
            }
        } catch (Exception ignored) {
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void showMarker() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                return common.network(MapsActivity.this);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                markingDatabaseReference = databaseReferencePath.child("EntityMarkingData/MarkedHouses/" + preferences.getString("ward", "") + "/" + currentLine);
                markingValueEventListener = markingDatabaseReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() != null) {
                            JSONObject jsonObject = new JSONObject();
                            if (!preferences.getString("markingData", "").equalsIgnoreCase("")) {
                                try {
                                    jsonObject = new JSONObject(preferences.getString("markingData", ""));
                                } catch (Exception e) {
                                }
                            }
                            JSONObject markingDataObject = new JSONObject();
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                if (snapshot.hasChild("latLng")) {
                                    JSONArray jsonArray = new JSONArray();
                                    jsonArray.put(String.valueOf(snapshot.child("latLng").getValue()));
                                    jsonArray.put(snapshot.child("image").getValue().toString());
                                    jsonArray.put(snapshot.child("houseType").getValue().toString());
                                    if (snapshot.hasChild("isSurveyed")) {
                                        jsonArray.put(snapshot.child("isSurveyed").getValue().toString());
                                    } else {
                                        jsonArray.put("no");
                                    }
                                    if (snapshot.hasChild("cardNumber")) {
                                        jsonArray.put(snapshot.child("cardNumber").getValue().toString());
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
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                if (!result) {
                    try {
                        setMarker();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.execute();
    }

    private void setMarker() throws JSONException {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
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
                                Integer.parseInt(String.valueOf(jsonArray.get(2))), Integer.parseInt(key), jsonArray.get(3).toString());
                    } catch (Exception e) {
                    }
                }
                totalMarkingCard.setText("" + markingDataObject.length());
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
        closeDialog();
    }

    private void printMarkerWithLine(LatLng latLng, int type, Integer tag, String isSurveyed) {
        try {
            Marker marker = hashMapMarker.get(tag);
            marker.remove();
            hashMapMarker.remove(tag);
        } catch (Exception e) {
        }
        if (isSurveyed.equalsIgnoreCase("yes")) {
            int height = 60;
            int width = 60;
            BitmapDrawable bitMapDraw = (BitmapDrawable) MapsActivity.this.getResources().getDrawable(R.drawable.pin_e);
            Bitmap b = bitMapDraw.getBitmap();
            Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

            Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
            markers.setTag(tag);
            hashMapMarker.put(tag, markers);
        } else {
            if (type == 1 || type == 19) {
                Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(common.BitmapFromVector(MapsActivity.this, R.drawable.house)));
                markers.setTag(tag);
                hashMapMarker.put(tag, markers);
            } else if (type == 2 || type == 3 || type == 6 || type == 7 || type == 8 || type == 9 || type == 10 || type == 20) {
                Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(common.BitmapFromVector(MapsActivity.this, R.drawable.shop)));
                markers.setTag(tag);
                hashMapMarker.put(tag, markers);
            } else if (type == 14 || type == 15) {
                Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(common.BitmapFromVector(MapsActivity.this, R.drawable.warehouse)));
                markers.setTag(tag);
                hashMapMarker.put(tag, markers);
            } else if (type == 21 || type == 22) {
                Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(common.BitmapFromVector(MapsActivity.this, R.drawable.institute)));
                markers.setTag(tag);
                hashMapMarker.put(tag, markers);
            } else if (type == 4 || type == 5) {
                Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(common.BitmapFromVector(MapsActivity.this, R.drawable.hotel)));
                markers.setTag(tag);
                hashMapMarker.put(tag, markers);
            } else if (type == 16 || type == 17) {
                Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(common.BitmapFromVector(MapsActivity.this, R.drawable.hall)));
                markers.setTag(tag);
                hashMapMarker.put(tag, markers);
            } else if (type == 18) {
                Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(common.BitmapFromVector(MapsActivity.this, R.drawable.thela)));
                markers.setTag(tag);
                hashMapMarker.put(tag, markers);
            } else if (type == 11 || type == 12 || type == 13) {
                Marker markers = mMap.addMarker(new MarkerOptions().position(latLng)
                        .icon(common.BitmapFromVector(MapsActivity.this, R.drawable.hospital)));
                markers.setTag(tag);
                hashMapMarker.put(tag, markers);
            }
        }
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

    private void getTimeDisForCurrentLoc() {
        currentLocationDatabaseRef = databaseReferencePath.child("Settings/Survey");
        valueEventListener = currentLocationDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("currentLocation")) {
                    currentLocationCaptureTime = Integer.parseInt(dataSnapshot.child("currentLocation").getValue().toString()) * 1000;
                }if (dataSnapshot.hasChild("sameCardOnTwoMarkerMessage")) {
                    preferences.edit().putString("sameCardOnTwoMarkerMessage",dataSnapshot.child("sameCardOnTwoMarkerMessage").getValue().toString()).apply();
                }if (dataSnapshot.hasChild("sameMarkerOnTwoCard")) {
                    preferences.edit().putString("sameMarkerOnTwoCard",dataSnapshot.child("sameMarkerOnTwoCard").getValue().toString()).apply();
                }if (dataSnapshot.hasChild("lineNotCompleteMessage")) {
                    preferences.edit().putString("lineNotCompleteMessage",dataSnapshot.child("lineNotCompleteMessage").getValue().toString()).apply();
                }if (dataSnapshot.hasChild("cameraNotSupportMessage")) {
                    preferences.edit().putString("cameraNotSupportMessage",dataSnapshot.child("cameraNotSupportMessage").getValue().toString()).apply();
                }if (dataSnapshot.hasChild("scanByCameraNoteMessage")) {
                    preferences.edit().putString("scanByCameraNoteMessage",dataSnapshot.child("scanByCameraNoteMessage").getValue().toString()).apply();
                }if (dataSnapshot.hasChild("scanByRfidNoteMessage")) {
                    preferences.edit().putString("scanByRfidNoteMessage",dataSnapshot.child("scanByRfidNoteMessage").getValue().toString()).apply();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (currentLocationDatabaseRef != null) {
                try {
                    currentLocationDatabaseRef.removeEventListener(valueEventListener);
                } catch (Exception e) {
                }
            }
            if (markingDatabaseReference != null) {
                try {
                    markingDatabaseReference.removeEventListener(markingValueEventListener);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
    }

    @SuppressLint("MissingPermission")
    public void sendCurrentLocationData() {
        String ward = preferences.getString("ward", "");
        String userId = preferences.getString("userId", "");
        countDownTimerCurrentLocation = new CountDownTimer(currentLocationCaptureTime, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                databaseReferencePath.child("EntitySurveyData/SurveyorsCurrentLocation/" + ward + "/" + userId).setValue(currentLatLng);
                sendCurrentLocationData();
            }
        }.start();
    }

    public void checkWhetherLocationSettingsAreSatisfied(int i) {
        LocationRequest mLocationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(1000).setNumUpdates(2);
        final LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        builder.setNeedBle(false);
        SettingsClient client = LocationServices.getSettingsClient(MapsActivity.this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(MapsActivity.this, locationSettingsResponse -> {
            setLocation();
        });
        task.addOnFailureListener(MapsActivity.this, e -> {
            common.closeDialog();
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
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

        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission((MapsActivity.this), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                    if (countDownTimerCurrentLocation == null) {
                        if (MapsActivity.this != null) {
                            sendCurrentLocationData();
                        }
                    }
                    if (countDownTimerLocation == null) {
                        if (isFirstTime) {
                            if (MapsActivity.this != null) {
                                isFirstTime = false;
                                common.setMovingMarker(mMap, currentLatLng, MapsActivity.this);
                            }
                        }
                        if (MapsActivity.this != null) {
                            currentLocationShow();
                        }
                    } else {
                        if (isFirstTime) {
                            if (MapsActivity.this != null) {
                                isFirstTime = false;
                                common.setMovingMarker(mMap, currentLatLng, MapsActivity.this);
                            }
                        }
                    }
                }
            }
        };
        LocationServices.getFusedLocationProviderClient((MapsActivity.this)).requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private class DownloadKmlFile extends AsyncTask<String, Void, byte[]> {
        private final String mUrl;

        public DownloadKmlFile(String url) {
            mUrl = url;
        }

        protected byte[] doInBackground(String... params) {
            try {
                InputStream is = new URL(mUrl).openStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                return buffer.toByteArray();
            } catch (IOException e) {
            }
            return null;
        }

        protected void onPostExecute(byte[] byteArr) {
            try {
                if (byteArr != null) {
                    KmlLayer kmlLayer = new KmlLayer(mMap, new ByteArrayInputStream(byteArr), MapsActivity.this);
                    kmlLayer.addLayerToMap();
                }
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                setLocation();
            } else {
                checkWhetherLocationSettingsAreSatisfied(0);
            }
        }
    }

    private void dialogForMarkerImage(Bitmap bitmap, String datum, String tag) {
        MapsActivity.this.runOnUiThread(() -> {
            try {
                if (customTimerAlertBox != null) {
                    customTimerAlertBox.dismiss();
                }
            } catch (Exception e) {
            }
            closeDialog();
            LayoutInflater inflater = getLayoutInflater();
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapsActivity.this);
            View dialogLayout = inflater.inflate(R.layout.markers_image_view_layout, null);
            alertDialog.setView(dialogLayout);
            alertDialog.setCancelable(false);
            ImageView markerImage = dialogLayout.findViewById(R.id.marker_iv);
            Button closeBtn = dialogLayout.findViewById(R.id.close_view_btn);
            EditText scanS = dialogLayout.findViewById(R.id.cardscans);
            TextView noteTv = dialogLayout.findViewById(R.id.noteText);
            Button ocrScanCardBtns = dialogLayout.findViewById(R.id.ocrScanCardBtns);
            scanS.setFocusableInTouchMode(true);
            scanS.requestFocus();
            houseT = datum;
            markingKey = tag;
            if (preferences.getString("byRFID", "no").equalsIgnoreCase("yes")) {
                ocrScanCardBtns.setVisibility(View.GONE);
                scanS.setVisibility(View.VISIBLE);
                noteTv.setText(preferences.getString("scanByRfidNoteMessage",""));

            } else {
                scanS.setVisibility(View.GONE);
                ocrScanCardBtns.setVisibility(View.VISIBLE);
                noteTv.setText(preferences.getString("scanByCameraNoteMessage",""));
            }
            ocrScanCardBtns.setOnClickListener(v1 -> {
                if (customTimerAlertBox != null) {
                    customTimerAlertBox.dismiss();
                }

                Intent intent = new Intent(MapsActivity.this, ScanCardActivity.class);
                intent.putExtra("line", currentLine);
                intent.putExtra("houseType", datum);
                intent.putExtra("markingKey", markingKey);
                intent.putExtra("markingData", markingDataObject.toString());
                try {
                    JSONArray jsonArray1 = markingDataObject.getJSONArray(markingKey);
                    intent.putExtra("markingCard", jsonArray1.get(4).toString());
                } catch (Exception e) {
                    intent.putExtra("markingCard", "no");
                }
                startActivity(intent);
            });
            scanS.setOnKeyListener((view1, i, keyEvent) -> {
                if (scanS.getText().length() == 10) {
                    if (customTimerAlertBox != null) {
                        customTimerAlertBox.dismiss();
                    }
                    cardScanMethod(scanS.getText().toString(), scanS);
                }
                if (scanS.getText().toString().trim().length() > 10) {
                    final MediaPlayer mp = MediaPlayer.create(MapsActivity.this, R.raw.warningsound);
                    mp.start();
                    scanS.getText().clear();
                }
                if (i == KeyEvent.KEYCODE_BACK && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    onBackPressed();
                }
                return true;
            });
            if (bitmap != null) {
                markerImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                markerImage.setImageBitmap(bitmap);
            }
            closeBtn.setOnClickListener(view1 -> {
                if (customTimerAlertBox != null) {
                    customTimerAlertBox.dismiss();
                }
            });
            customTimerAlertBox = alertDialog.create();
            customTimerAlertBox.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            if (!isFinishing()) {
                customTimerAlertBox.show();
            }
        });
    }

    public void setProgressBar(String title, String message, Context context, Activity activity) {
        closeDialog();
        dialog = new ProgressDialog(context);
        dialog.setCancelable(false);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        if (!dialog.isShowing() && !activity.isFinishing()) {
            dialog.show();
        }
    }

    public void closeDialog() {
        if (dialog != null) {
            if (dialog.isShowing() && !isFinishing()) {
                dialog.dismiss();
            }
        }
    }
}