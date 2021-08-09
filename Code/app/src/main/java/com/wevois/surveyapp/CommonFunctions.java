package com.wevois.surveyapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Html;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

public class CommonFunctions {
    int st = 0, total = 0, mean, moveMarker = 1;
    double std = 0, stdDiff = 0, stdPre = 0;
    ArrayList<Float> azi = new ArrayList<>();
    private float accelerator[] = new float[3], mags[] = new float[3], azimuth, moveDistance[] = new float[1];
    private float[] values = new float[3];
    ProgressDialog dialog;
    LatLng previousLatLng;
    HttpURLConnection urlc = null;
    Marker markerManOne, markerManTwo, markerManThree, markerManFour, markerManFive, markerManSix, markerManStop;

    public boolean internetIsConnected() {
        try
        {
            urlc = (HttpURLConnection) (new URL("https://google.com").openConnection());
            urlc.setRequestProperty("User-Agent", "Test");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(10000);
            urlc.setReadTimeout(10000);
            urlc.connect();
            if (urlc.getResponseCode() == 200) {
                closedInternetCheckMethod();
            }
            return (urlc.getResponseCode() == 200);
        } catch (IOException e) {
            closedInternetCheckMethod();
            return (false);
        }
    }

    public void closedInternetCheckMethod(){
        try {
            urlc.getInputStream().close();
            urlc.getOutputStream().close();
            urlc.disconnect();
        } catch (Exception ea) {
        }
    }

    public boolean network(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected() && internetIsConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public DatabaseReference getDatabaseForApplication(Context context) {
        DatabaseReference databaseReferencePath = FirebaseDatabase.getInstance(getSp(context).getString("dbPath", "")).getReference();
        return databaseReferencePath;
    }

    public String getDatabaseStorage(Context context) {
        String storagePath = getSp(context).getString("storagePath", "");
        return storagePath;
    }

    private SharedPreferences getSp(Context context) {
        SharedPreferences sp = context.getSharedPreferences("FirebasePath", MODE_PRIVATE);
        return sp;
    }

    public SensorEventListener mySensorEventListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
        public void onSensorChanged(SensorEvent event) {
            float[] magnetic, gravity;
            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mags = event.values.clone();
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    accelerator = event.values.clone();
                    break;
            }
            if (mags != null && accelerator != null) {
                gravity = new float[9];
                magnetic = new float[9];
                SensorManager.getRotationMatrix(gravity, magnetic, accelerator, mags);
                float[] outGravity = new float[9];
                SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_Y, SensorManager.AXIS_Z, outGravity);
                SensorManager.getOrientation(outGravity, values);

                azimuth = values[0] * 57.2957795f;
                mags = null;
                accelerator = null;
                azimuth -= 90;
                if (azi.size() > 10) {
                    azi.clear();
                    st = 0;
                }
                azi.add(st, azimuth);
                total += azimuth;
                mean = total / azi.size();
                st++;
                std = 0;
                if (azi.size() == 10) {
                    for (int st = 0; st < azi.size(); st++) {
                        std += Math.pow(azi.get(st) - mean, 2);
                    }
                    azi.clear();
                    st = 0;
                    total = 0;
                    std = Math.sqrt(std / 9);
                    stdDiff = std - stdPre;
                    stdPre = std;
                }
            }
        }
    };

    @SuppressLint("MissingPermission")
    public void currentLocationShow(LatLng currentLatLng) {
        Location.distanceBetween(currentLatLng.latitude, currentLatLng.longitude, previousLatLng.latitude, previousLatLng.longitude, moveDistance);
        if (moveDistance[0] > 2) {
            final LatLng startPosition = previousLatLng;
            final LatLng finalPosition = currentLatLng;
            final Handler handler = new Handler();
            final long start = SystemClock.uptimeMillis();
            final Interpolator interpolator = new AccelerateDecelerateInterpolator();
            final float durationInMs = 2000;
            handler.post(new Runnable() {
                long elapsed;
                float t;
                float v;

                @Override
                public void run() {
                    elapsed = SystemClock.uptimeMillis() - start;
                    t = elapsed / durationInMs;
                    v = interpolator.getInterpolation(t);
                    LatLng currentPosition = new LatLng(
                            startPosition.latitude * (1 - t) + finalPosition.latitude * t,
                            startPosition.longitude * (1 - t) + finalPosition.longitude * t);
                    markerManStop.setPosition(currentPosition);
                    markerManOne.setPosition(currentPosition);
                    markerManTwo.setPosition(currentPosition);
                    markerManThree.setPosition(currentPosition);
                    markerManFour.setPosition(currentPosition);
                    markerManFive.setPosition(currentPosition);
                    markerManSix.setPosition(currentPosition);
                    if (t < 1) {
                        setVisibleMarker();
                        handler.postDelayed(this, 150);
                    } else {
                        moveMarker = 1;
                        markerManOne.setVisible(false);
                        markerManTwo.setVisible(false);
                        markerManThree.setVisible(false);
                        markerManFour.setVisible(false);
                        markerManFive.setVisible(false);
                        markerManSix.setVisible(false);
                        markerManStop.setVisible(true);
                    }
                }
            });
            previousLatLng = currentLatLng;
        }
    }

    public void setMovingMarker(GoogleMap mMap,LatLng currentLatLng, Context context) {
        previousLatLng = currentLatLng;
        int height = 150;
        int width = 60;
        BitmapDrawable bitManOne = (BitmapDrawable) context.getResources().getDrawable(R.drawable.manone);
        BitmapDrawable bitManTwo = (BitmapDrawable) context.getResources().getDrawable(R.drawable.mantwo);
        BitmapDrawable bitManThree = (BitmapDrawable) context.getResources().getDrawable(R.drawable.manthree);
        BitmapDrawable bitManFour = (BitmapDrawable) context.getResources().getDrawable(R.drawable.manfour);
        BitmapDrawable bitManFive = (BitmapDrawable) context.getResources().getDrawable(R.drawable.manfive);
        BitmapDrawable bitManSix = (BitmapDrawable) context.getResources().getDrawable(R.drawable.mansix);
        BitmapDrawable bitManStop = (BitmapDrawable) context.getResources().getDrawable(R.drawable.manstop);
        MarkerOptions markerOptionsOne = new MarkerOptions();
        MarkerOptions markerOptionsTwo = new MarkerOptions();
        MarkerOptions markerOptionsThree = new MarkerOptions();
        MarkerOptions markerOptionsFour = new MarkerOptions();
        MarkerOptions markerOptionsFive = new MarkerOptions();
        MarkerOptions markerOptionsSix = new MarkerOptions();
        MarkerOptions markerOptionsStop = new MarkerOptions();
        markerOptionsOne.position(currentLatLng);
        markerOptionsTwo.position(currentLatLng);
        markerOptionsThree.position(currentLatLng);
        markerOptionsFour.position(currentLatLng);
        markerOptionsFive.position(currentLatLng);
        markerOptionsSix.position(currentLatLng);
        markerOptionsStop.position(currentLatLng);
        markerOptionsOne.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitManOne.getBitmap(), width, height, false)));
        markerOptionsTwo.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitManTwo.getBitmap(), width, height, false)));
        markerOptionsThree.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitManThree.getBitmap(), width, height, false)));
        markerOptionsFour.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitManFour.getBitmap(), width, height, false)));
        markerOptionsFive.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitManFive.getBitmap(), width, height, false)));
        markerOptionsSix.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitManSix.getBitmap(), width, height, false)));
        markerOptionsStop.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createScaledBitmap(bitManStop.getBitmap(), width, height, false)));
        markerManOne = mMap.addMarker(markerOptionsOne);
        markerManTwo = mMap.addMarker(markerOptionsTwo);
        markerManThree = mMap.addMarker(markerOptionsThree);
        markerManFour = mMap.addMarker(markerOptionsFour);
        markerManFive = mMap.addMarker(markerOptionsFive);
        markerManSix = mMap.addMarker(markerOptionsSix);
        markerManStop = mMap.addMarker(markerOptionsStop);
        markerManOne.setVisible(false);
        markerManTwo.setVisible(false);
        markerManThree.setVisible(false);
        markerManFour.setVisible(false);
        markerManFive.setVisible(false);
        markerManSix.setVisible(false);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 19));
    }

    private void setVisibleMarker() {
        if (moveMarker == 1) {
            moveMarker = 2;
            markerManStop.setVisible(false);
            markerManTwo.setVisible(false);
            markerManThree.setVisible(false);
            markerManFour.setVisible(false);
            markerManFive.setVisible(false);
            markerManSix.setVisible(false);
            markerManOne.setVisible(true);
        } else if (moveMarker == 2) {
            moveMarker = 3;
            markerManStop.setVisible(false);
            markerManOne.setVisible(false);
            markerManThree.setVisible(false);
            markerManFour.setVisible(false);
            markerManFive.setVisible(false);
            markerManSix.setVisible(false);
            markerManTwo.setVisible(true);
        } else if (moveMarker == 3) {
            moveMarker = 4;
            markerManStop.setVisible(false);
            markerManTwo.setVisible(false);
            markerManOne.setVisible(false);
            markerManFour.setVisible(false);
            markerManFive.setVisible(false);
            markerManSix.setVisible(false);
            markerManThree.setVisible(true);
        } else if (moveMarker == 4) {
            moveMarker = 5;
            markerManStop.setVisible(false);
            markerManTwo.setVisible(false);
            markerManThree.setVisible(false);
            markerManOne.setVisible(false);
            markerManFive.setVisible(false);
            markerManSix.setVisible(false);
            markerManFour.setVisible(true);
        } else if (moveMarker == 5) {
            moveMarker = 6;
            markerManStop.setVisible(false);
            markerManTwo.setVisible(false);
            markerManThree.setVisible(false);
            markerManFour.setVisible(false);
            markerManOne.setVisible(false);
            markerManSix.setVisible(false);
            markerManFive.setVisible(true);
        } else {
            moveMarker = 1;
            markerManStop.setVisible(false);
            markerManTwo.setVisible(false);
            markerManThree.setVisible(false);
            markerManFour.setVisible(false);
            markerManFive.setVisible(false);
            markerManOne.setVisible(false);
            markerManSix.setVisible(true);
        }
    }

    public void doVibrate(Context context) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(500);
        }
    }

    public void showAlertBox(String message, boolean chancel, Context context) {
        closeDialog();
        AlertDialog.Builder alertAssignment = new AlertDialog.Builder(context);
        alertAssignment.setMessage(Html.fromHtml(message));
        alertAssignment.setCancelable(chancel);
        alertAssignment.setPositiveButton("OK", (dialog1, which) -> new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alertDAssignment = alertAssignment.create();
        if (!alertDAssignment.isShowing()) {
            alertDAssignment.show();
        }
    }

    public void setProgressBar(String title, Context context,Activity activity) {
        closeDialog();
        dialog = new ProgressDialog(context);
        dialog.setCancelable(false);
        dialog.setTitle(title);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        if (!activity.isFinishing()) {
            dialog.show();
        }
    }

    public void closeDialog() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    public void getKml(Context context){
        getDatabaseForApplication(context).child("Defaults/KmlBoundary").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue()!=null){
                    JSONObject jsonKmlBoundary = new JSONObject();
                    for (DataSnapshot snapshot:dataSnapshot.getChildren()){
                        try {
                            jsonKmlBoundary.put(snapshot.getKey(),snapshot.getValue().toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    getSp(context).edit().putString("kmlBoundaryList",jsonKmlBoundary.toString()).commit();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public String getKmlFilePath(String wardNo,Context context) {
        String kmlFilepath="";
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(getSp(context).getString("kmlBoundaryList",""));
            kmlFilepath = jsonObject.getString(wardNo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return kmlFilepath;
    }

    public BitmapDescriptor BitmapFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getMinimumWidth(), vectorDrawable.getMinimumHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}