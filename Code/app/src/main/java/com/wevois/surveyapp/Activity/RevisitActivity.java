package com.wevois.surveyapp.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.R;
import com.wevois.surveyapp.Revisited;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class RevisitActivity extends AppCompatActivity {
    final List<String> typesList1 = new ArrayList<>();
    DatabaseReference databaseReferencePath;
    CommonFunctions common = new CommonFunctions();
    SharedPreferences preferences;
    Button revisitBtn;
    EditText name;
    String storagePath="";
    List<String> houseTypeList = new ArrayList<>();
    RadioButton awasiyeBtn, commercialBtn;
    Spinner spinnerRevisit, spinnerHouseType;
    JSONArray jsonArrayHouseType = new JSONArray();
    boolean isMoved = true;
    Bitmap identityBitmap = null;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private Camera mCamera;
    private static final int FOCUS_AREA_SIZE = 300;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    SurfaceHolder.Callback surfaceViewCallBack;
    Camera.PictureCallback pictureCallback;
    AlertDialog customTimerAlertBox, customTimerAlertBoxForImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revisit);

        databaseReferencePath = common.getDatabaseForApplication(this);
        storagePath = common.getDatabaseStorage(this);
        preferences = getSharedPreferences("surveyApp", MODE_PRIVATE);
        spinnerRevisit = findViewById(R.id.spinner3);
        name = findViewById(R.id.revisitName);
        revisitBtn = findViewById(R.id.revisitedpage);
        awasiyeBtn = findViewById(R.id.radio_awasiyeRevisit);
        commercialBtn = findViewById(R.id.radio_comRevisit);
        spinnerHouseType = findViewById(R.id.spnrHouseTypeRevisit);
        typesList1.add("Select Reason");
        TextView backBtn = findViewById(R.id.BackBtn);
        backBtn.setOnClickListener(v -> {
            onBackPressed();
        });
        databaseReferencePath.child("Defaults").child("CardRevisitReasons").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    typesList1.add(String.valueOf(snapshot.getValue()));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        final ArrayAdapter<String> spinnerArrayAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, typesList1) {
            @Override
            public boolean isEnabled(int position) {
                if (position == 0) {
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if (position == 0) {
                    tv.setTextColor(Color.GRAY);
                } else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        spinnerArrayAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRevisit.setAdapter(spinnerArrayAdapter1);
        revisitBtn.setOnClickListener(view1 -> {
            if (name.getText().toString().trim().length() == 0) {
                name.setError("Please enter name");
                name.requestFocus();
            } else if (spinnerRevisit.getSelectedItem().toString().equals("Select Reason")) {
                common.closeDialog();
                View selectedView = spinnerRevisit.getSelectedView();
                if (selectedView != null && selectedView instanceof TextView) {
                    spinnerRevisit.requestFocus();
                    TextView selectedTextView = (TextView) selectedView;
                    selectedTextView.setError("error");
                    selectedTextView.setTextColor(Color.RED);
                    selectedTextView.setText("please select type");
                    spinnerRevisit.performClick();
                }
                return;
            } else if (spinnerHouseType.getSelectedItem().toString().equals("Select Entity type")) {
                View selectedView = spinnerHouseType.getSelectedView();
                if (selectedView != null && selectedView instanceof TextView) {
                    spinnerHouseType.requestFocus();
                    TextView selectedTextView = (TextView) selectedView;
                    selectedTextView.setError("error");
                    selectedTextView.setTextColor(Color.RED);
                    selectedTextView.setText("please select type");
                    spinnerHouseType.performClick();
                }
                return;
            } else if (identityBitmap == null) {
                common.showAlertBox("कृपया पहले फोटो खींचे .", false, this);
            }else {
                if (isMoved) {
                    isMoved = false;
                    common.setProgressBar("Please Wait...", this, this);
                    String pushKey = databaseReferencePath.child("EntitySurveyData/RevisitRequest/"+preferences.getString("ward", "")+"/"+preferences.getString("line", "")).push().getKey();
                    saveImageData(pushKey);
                }
            }
        });
        awasiyeBtn.setOnClickListener(view12 -> {
            getHouseTypes(false);
        });
        commercialBtn.setOnClickListener(view13 -> {
            getHouseTypes(true);
        });
        findViewById(R.id.button_image).setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                } else {
                    showAlertDialog();
                }
            }
        });
        getHouseTypes(false);
    }

    @SuppressLint("StaticFieldLeak")
    private void saveImageData(String pushKey) {
        RevisitActivity.this.runOnUiThread(() -> {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                }

                @Override
                protected Boolean doInBackground(Void... p) {
                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference storageRef = storage.getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + storagePath + "/RevisitCardImage/" + preferences.getString("ward", "") + "/" + preferences.getString("line", ""));
                    StorageReference mountainImagesRef = storageRef.child(pushKey + ".jpg");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    identityBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                    byte[] data = baos.toByteArray();
                    UploadTask uploadTask = mountainImagesRef.putBytes(data);
                    uploadTask.addOnFailureListener(exception -> {
                        isMoved = true;
                        common.closeDialog();
                    }).addOnSuccessListener(taskSnapshot -> {
                        databaseReferencePath.child("EntityMarkingData/MarkedHouses/"+preferences.getString("ward", "")+"/"+preferences.getString("line", "")+"/"+"lineRevisitCount").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                int count = 1;
                                if (dataSnapshot.getValue() != null) {
                                    count = Integer.parseInt(dataSnapshot.getValue().toString()) + 1;
                                }
                                try {
                                    HashMap<String,String> revisitData = new HashMap<>();
                                    revisitData.put("lat",preferences.getString("lat", ""));
                                    revisitData.put("lng",preferences.getString("lng", ""));
                                    revisitData.put("reason",spinnerRevisit.getSelectedItem().toString());
                                    revisitData.put("houseType",jsonArrayHouseType.get(spinnerHouseType.getSelectedItemPosition() - 1).toString());
                                    revisitData.put("date",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                                    revisitData.put("id",preferences.getString("userId", ""));
                                    revisitData.put("revisitedBy","Surveyor");
                                    revisitData.put("name",name.getText().toString());
                                    revisitData.put("image",pushKey + ".jpg");
                                    databaseReferencePath.child("EntitySurveyData/RevisitRequest/"+preferences.getString("ward", "")+"/"+preferences.getString("line", "")).child(pushKey).setValue(revisitData);
                                    databaseReferencePath.child("EntityMarkingData/MarkedHouses/"+ preferences.getString("ward", "") + "/" + preferences.getString("line", "") + "/lineRevisitCount").setValue("" + count);
                                    databaseReferencePath.child("EntityMarkingData/MarkedHouses/"+ preferences.getString("ward", "") + "/" + preferences.getString("line", "")+"/"+preferences.getString("markingKey", "") + "/revisitKey").setValue("" + pushKey);
                                    dailyRevisitRequestCount();
                                    totalRevisitRequest();
                                    common.closeDialog();
                                    AlertDialog.Builder builder1 = new AlertDialog.Builder(RevisitActivity.this);
                                    builder1.setMessage("आपका सर्वे पूरा हुआ, धन्यवाद !");
                                    builder1.setCancelable(true);
                                    builder1.setPositiveButton("OK", (dialog, id) -> {
                                        dialog.cancel();
                                        finish();
                                    });
                                    AlertDialog alert11 = builder1.create();
                                    alert11.show();
                                } catch (Exception e) {
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    });
                    return null;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                }
            }.execute();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isMoved=true;
    }

    private void totalRevisitRequest() {
        String ward = preferences.getString("ward", "");
        databaseReferencePath.child("EntitySurveyData/TotalRevisitRequest/" + ward).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    String count = String.valueOf(Integer.parseInt(dataSnapshot.getValue().toString()) + 1);
                    databaseReferencePath.child("EntitySurveyData/TotalRevisitRequest/" + ward).setValue(count);
                } else {
                    databaseReferencePath.child("EntitySurveyData/TotalRevisitRequest/" + ward).setValue("1");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void dailyRevisitRequestCount() {
        SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd-MM-yyyy");
        String date1 = dateFormat1.format(new Date());
        String ward = preferences.getString("ward", "");
        String userId = preferences.getString("userId", "");
        databaseReferencePath.child("EntitySurveyData/DailyRevisitRequestCount/" + ward + "/" + userId + "/" + date1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    String count = String.valueOf(Integer.parseInt(dataSnapshot.getValue().toString()) + 1);
                    databaseReferencePath.child("EntitySurveyData/DailyRevisitRequestCount/" + ward + "/" + userId + "/" + date1).setValue(count);
                } else {
                    databaseReferencePath.child("EntitySurveyData/DailyRevisitRequestCount/" + ward + "/" + userId + "/" + date1).setValue("1");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void getHouseTypes(Boolean isCommercial) {
        houseTypeList.clear();
        houseTypeList.add("Select Entity type");
        JSONObject jsonObject, commercialJsonObject, residentialJsonObject;
        jsonArrayHouseType = new JSONArray();
        try {
            jsonObject = new JSONObject(preferences.getString("housesTypeList", ""));
            commercialJsonObject = new JSONObject(preferences.getString("commercialHousesTypeList", ""));
            residentialJsonObject = new JSONObject(preferences.getString("residentialHousesTypeList", ""));
            for (int i = 1; i <= jsonObject.length(); i++) {
                if (isCommercial) {
                    try {
                        houseTypeList.add(commercialJsonObject.getString(String.valueOf(i)));
                        jsonArrayHouseType.put(i);
                    } catch (JSONException e) {
                    }
                } else {
                    try {
                        houseTypeList.add(residentialJsonObject.getString(String.valueOf(i)));
                        jsonArrayHouseType.put(i);
                    } catch (JSONException e) {
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        bindHouseTypesToSpinner();
    }

    private void bindHouseTypesToSpinner() {
        final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, houseTypeList) {
            @Override
            public boolean isEnabled(int position) {
                return !(position == 0);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(position == 0 ? Color.GRAY : Color.BLACK);
                return view;
            }
        };
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHouseType.setAdapter(spinnerArrayAdapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        RevisitActivity.this.runOnUiThread(() -> {
            if (requestCode == MY_CAMERA_PERMISSION_CODE) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showAlertDialog();
                } else {
                    Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void focusOnTouch(MotionEvent event) throws Exception {
        if (mCamera != null) {
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                if (parameters.getMaxNumMeteringAreas() > 0) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    Rect rect = calculateFocusArea(event.getX(), event.getY());
                    List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                    meteringAreas.add(new Camera.Area(rect, 800));
                    parameters.setFocusAreas(meteringAreas);
                    mCamera.setParameters(parameters);
                }
                mCamera.autoFocus((success, camera) -> {

                });

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private Rect calculateFocusArea(float x, float y) {
        int left = clamp(Float.valueOf((x / surfaceView.getWidth()) * 2000 - 1000).intValue());
        int top = clamp(Float.valueOf((y / surfaceView.getHeight()) * 2000 - 1000).intValue());

        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    private int clamp(int touchCoordinateInCameraReper) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper) + RevisitActivity.FOCUS_AREA_SIZE / 2 > 1000) {
            if (touchCoordinateInCameraReper > 0) {
                result = 1000 - RevisitActivity.FOCUS_AREA_SIZE / 2;
            } else {
                result = -1000 + RevisitActivity.FOCUS_AREA_SIZE / 2;
            }
        } else {
            result = touchCoordinateInCameraReper - RevisitActivity.FOCUS_AREA_SIZE / 2;
        }
        return result;
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @SuppressLint("StaticFieldLeak")
    public void showAlertDialog() {
        RevisitActivity.this.runOnUiThread(() -> {
            try {
                if (customTimerAlertBox != null) {
                    customTimerAlertBox.dismiss();
                }
            } catch (Exception e) {
            }
            LayoutInflater inflater = getLayoutInflater();
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(RevisitActivity.this);
            View dialogLayout = inflater.inflate(R.layout.custom_camera_alertbox, null);
            alertDialog.setView(dialogLayout);
            alertDialog.setCancelable(false);
            customTimerAlertBox = alertDialog.create();
            surfaceView = (SurfaceView) dialogLayout.findViewById(R.id.surfaceViews);
            surfaceHolder = surfaceView.getHolder();
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_HARDWARE);
            surfaceViewCallBack = new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    try {
                        mCamera = Camera.open();
                    } catch (RuntimeException e) {
                    }
                    Camera.Parameters parameters;
                    parameters = mCamera.getParameters();
                    List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
                    parameters.setPictureSize(sizes.get(0).width, sizes.get(0).height);
                    mCamera.setParameters(parameters);
                    setCameraDisplayOrientation(RevisitActivity.this, 0, mCamera);
                    try {
                        mCamera.setPreviewDisplay(surfaceHolder);
                        mCamera.startPreview();
                    } catch (Exception e) {
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

                }
            };
            surfaceHolder.addCallback(surfaceViewCallBack);
            Button btn = dialogLayout.findViewById(R.id.capture_image_btn);
            btn.setOnClickListener(v -> {
                common.setProgressBar("Processing...", RevisitActivity.this, RevisitActivity.this);
                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                    }

                    @Override
                    protected Boolean doInBackground(Void... p) {
                        mCamera.takePicture(null, null, null, pictureCallback);
                        return null;
                    }
                }.execute();
            });
            Button closeBtn = dialogLayout.findViewById(R.id.close_image_btn);
            closeBtn.setOnClickListener(v -> {
                try {
                    if (customTimerAlertBox != null) {
                        customTimerAlertBox.dismiss();
                    }
                } catch (Exception e) {
                }
            });
            if (!isFinishing()) {
                customTimerAlertBox.show();
            }
            pictureCallback = (bytes, camera) -> {
                Matrix matrix = new Matrix();
                matrix.postRotate(90F);
                Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                Bitmap bitmaps = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                Bitmap bitmap = Bitmap.createScaledBitmap(bitmaps, 400, 600, false);

                camera.stopPreview();
                if (camera != null) {
                    camera.release();
                    mCamera = null;
                }
                try {
                    if (customTimerAlertBox != null) {
                        customTimerAlertBox.dismiss();
                    }
                } catch (Exception e) {
                }
                showAlertBoxForImage(bitmap);
            };
            surfaceView.setOnTouchListener((view, motionEvent) -> {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    try {
                        focusOnTouch(motionEvent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            });
        });
    }

    private void showAlertBoxForImage(Bitmap i) {
        try {
            if (customTimerAlertBoxForImage != null) {
                customTimerAlertBoxForImage.dismiss();
            }
        } catch (Exception e) {
        }
        LayoutInflater inflater = getLayoutInflater();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(RevisitActivity.this);
        View dialogLayout = inflater.inflate(R.layout.image_view_layout, null);
        alertDialog.setView(dialogLayout);
        alertDialog.setCancelable(false);
        ImageView markerImage = dialogLayout.findViewById(R.id.marker_iv);
        if (i != null) {
            markerImage.setImageBitmap(i);
        }
        dialogLayout.findViewById(R.id.okeyBtn).setOnClickListener(view1 -> {
            common.setProgressBar("Processing...", RevisitActivity.this, RevisitActivity.this);
            if (customTimerAlertBoxForImage != null) {
                customTimerAlertBoxForImage.dismiss();
            }
            identityBitmap = i;
            common.closeDialog();
        });
        Button closeBtn = dialogLayout.findViewById(R.id.close_view_btn);
        closeBtn.setOnClickListener(view1 -> {
            common.closeDialog();
            if (customTimerAlertBoxForImage != null) {
                customTimerAlertBoxForImage.dismiss();
            }
        });
        customTimerAlertBoxForImage = alertDialog.create();
        if (!isFinishing()) {
            customTimerAlertBoxForImage.show();
        }
    }
}