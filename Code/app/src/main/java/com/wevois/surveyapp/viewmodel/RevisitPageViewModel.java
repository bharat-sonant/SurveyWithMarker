package com.wevois.surveyapp.viewmodel;

import static android.content.Context.MODE_PRIVATE;

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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.R;
import com.wevois.surveyapp.repository.Repository;
import com.wevois.surveyapp.views.RevisitPageActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class RevisitPageViewModel extends ViewModel {
    Activity activity;
    SharedPreferences preferences;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    CommonFunctions common = new CommonFunctions();
    Spinner spinnerRevisit, spinnerHouseType;
    Bitmap identityBitmap = null;
    List<String> houseTypeList = new ArrayList<>();
    final List<String> revisitTypeList = new ArrayList<>();
    JSONArray jsonArrayHouseType = new JSONArray();
    public ObservableField<Boolean> isChecked = new ObservableField<>(false);
    public ObservableField<Boolean> isCheckedAwasiye = new ObservableField<>(true);
    public final ObservableField<String> userTv = new ObservableField<>("");
    AlertDialog customTimerAlertBox, customTimerAlertBoxForImage;
    private Camera mCamera;
    private static final int FOCUS_AREA_SIZE = 300;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    SurfaceHolder.Callback surfaceViewCallBack;
    Camera.PictureCallback pictureCallback;
    boolean isMoved = true;

    public void init(RevisitPageActivity revisitPageActivity, Spinner spinner3, Spinner spnrHouseTypeRevisit) {
        activity = revisitPageActivity;
        preferences = activity.getSharedPreferences("surveyApp", MODE_PRIVATE);
        spinnerRevisit = spinner3;
        spinnerHouseType = spnrHouseTypeRevisit;
        revisitTypeList.add("Select Reason type");
        String listAsString = preferences.getString("revisitReasonList", null);
        String[] reasonString = listAsString.substring(1, listAsString.length() - 1).split(",");
        for (int i = 0; i < reasonString.length; i++) {
            String reasonType = reasonString[i].replace("~", ",");
            revisitTypeList.add(reasonType);
        }
        final ArrayAdapter<String> spinnerArrayAdapter1 = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_dropdown_item, revisitTypeList) {
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
        spinnerArrayAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRevisit.setAdapter(spinnerArrayAdapter1);
        getHouseTypes(false);
    }

    public void awasiyeButtonClick() {
        isChecked.set(false);
        isCheckedAwasiye.set(true);
        getHouseTypes(false);
    }

    public void commercialButtonClick() {
        isChecked.set(true);
        isCheckedAwasiye.set(false);
        getHouseTypes(true);
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
        final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_dropdown_item, houseTypeList) {
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

    public void saveImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
            } else {
                showAlertDialog();
            }
        }
    }

    public void saveRevisitData() {
        if (userTv.get().trim().length() == 0) {
            common.showAlertBox("Please enter name.",false,activity);
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
            common.showAlertBox("कृपया पहले फोटो खींचे .", false, activity);
        }else {
            if (isMoved) {
                isMoved = false;
                common.setProgressBar("Please Wait...", activity, activity);
                String pushKey = common.getDatabaseForApplication(activity).child("EntitySurveyData/RevisitRequest/"+preferences.getString("ward", "")+"/"+preferences.getString("line", "")).push().getKey();

                HashMap<String,Object> revisitData = new HashMap<>();
                revisitData.put("lat",preferences.getString("lat", ""));
                revisitData.put("lng",preferences.getString("lng", ""));
                revisitData.put("reason",spinnerRevisit.getSelectedItem().toString());
                String houseType = "1";
                try{
                    houseType = jsonArrayHouseType.get(spinnerHouseType.getSelectedItemPosition() - 1).toString();
                }catch (Exception e){}
                revisitData.put("houseType",houseType);
                revisitData.put("date",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                revisitData.put("id",preferences.getString("userId", ""));
                revisitData.put("revisitedBy","Surveyor");
                revisitData.put("name",userTv.get());
                revisitData.put("image",pushKey + ".jpg");

                new Repository().saveRevisitData(activity, revisitData,identityBitmap,pushKey).observeForever(dataSnapshot -> {
                    common.closeDialog();
                    AlertDialog.Builder alertAssignment = new AlertDialog.Builder(activity);
                    alertAssignment.setMessage("आपका सर्वे पूरा हुआ, धन्यवाद !");
                    alertAssignment.setCancelable(false);
                    alertAssignment.setPositiveButton("OK", (dialog, id) -> {
                        saveMarkingData(3,pushKey);
                        dialog.cancel();
                        preferences.edit().putString("isOnResumeCall", "yes").apply();
                        activity.finish();
                    });
                    AlertDialog alertDAssignment = alertAssignment.create();
                    if (!activity.isFinishing()) {
                        alertDAssignment.show();
                    }
                });
            }
        }
    }

    private void saveMarkingData(int i,String v) {
        JSONObject jsonObject = new JSONObject();
        if (!preferences.getString("markingData", "").equalsIgnoreCase("")) {
            try {
                jsonObject = new JSONObject(preferences.getString("markingData", ""));
            } catch (Exception e) {
            }
        }
        JSONObject markingDataObject = new JSONObject();
        try {
            markingDataObject = jsonObject.getJSONObject(preferences.getString("line", ""));
            JSONArray jsonArray = markingDataObject.getJSONArray(preferences.getString("markingKey", ""));
            jsonArray.put(i,v);
            try {
                markingDataObject.put(preferences.getString("markingKey", ""), jsonArray);
            } catch (Exception e) {
            }
        } catch (Exception e) {
        }
        try {
            jsonObject.put(preferences.getString("line", ""), markingDataObject);
        } catch (Exception e) {
        }
        preferences.edit().putString("markingData", jsonObject.toString()).apply();
    }

    public void onBackClick(){
        activity.finish();
    }

    @SuppressLint("StaticFieldLeak")
    public void showAlertDialog() {
            try {
                if (customTimerAlertBox != null) {
                    customTimerAlertBox.dismiss();
                }
            } catch (Exception e) {
            }
            LayoutInflater inflater = activity.getLayoutInflater();
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
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
                    setCameraDisplayOrientation(activity, 0, mCamera);
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
                common.setProgressBar("Processing...", activity, activity);
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
            if (!activity.isFinishing()) {
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
        if (Math.abs(touchCoordinateInCameraReper) + FOCUS_AREA_SIZE / 2 > 1000) {
            if (touchCoordinateInCameraReper > 0) {
                result = 1000 - FOCUS_AREA_SIZE / 2;
            } else {
                result = -1000 + FOCUS_AREA_SIZE / 2;
            }
        } else {
            result = touchCoordinateInCameraReper - FOCUS_AREA_SIZE / 2;
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

    private void showAlertBoxForImage(Bitmap i) {
        try {
            if (customTimerAlertBoxForImage != null) {
                customTimerAlertBoxForImage.dismiss();
            }
        } catch (Exception e) {
        }
        LayoutInflater inflater = activity.getLayoutInflater();
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
        View dialogLayout = inflater.inflate(R.layout.image_view_layout, null);
        alertDialog.setView(dialogLayout);
        alertDialog.setCancelable(false);
        ImageView markerImage = dialogLayout.findViewById(R.id.marker_iv);
        if (i != null) {
            markerImage.setImageBitmap(i);
        }
        dialogLayout.findViewById(R.id.okeyBtn).setOnClickListener(view1 -> {
            common.setProgressBar("Processing...", activity, activity);
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
        if (!activity.isFinishing()) {
            customTimerAlertBoxForImage.show();
        }
    }

    public void showAlertBox(String message, boolean surveyCompleted, String from, String value) {
        common.closeDialog();
        AlertDialog.Builder alertAssignment = new AlertDialog.Builder(activity);
        alertAssignment.setMessage(message);
        alertAssignment.setCancelable(false);
        alertAssignment.setPositiveButton("OK", (dialog, id) -> {
            if (surveyCompleted) {
                preferences.edit().putString("isOnResumeCall", "yes").apply();
                if (from.equalsIgnoreCase("survey")) {
                    saveMarkingData(4, value);
                } else if (from.equalsIgnoreCase("revisit")) {
                    saveMarkingData(3, value);
                } else {
                    saveMarkingData(5, value);
                }
            }
        });
        AlertDialog alertDAssignment = alertAssignment.create();
        if (!activity.isFinishing()) {
            alertDAssignment.show();
        }
    }
}
