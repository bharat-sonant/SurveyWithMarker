package com.wevois.surveyapp.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {
    DatabaseReference databaseReferencePath;
    CommonFunctions common = new CommonFunctions();
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private Camera mCamera;
    private static final int FOCUS_AREA_SIZE = 300;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    SurfaceHolder.Callback surfaceViewCallBack;
    Camera.PictureCallback pictureCallback;
    Button saveBtn;
    EditText userName, userMno;
    String mobileNumber, storagePath;
    ImageView imageId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initPage();
        setDefaults();
    }

    private void initPage() {
        saveBtn = findViewById(R.id.button_save);
        userName = findViewById(R.id.user_name);
        userMno = findViewById(R.id.user_no);
        imageId = findViewById(R.id.imageid);
    }

    private void setDefaults() {
        databaseReferencePath = common.getDatabaseForApplication(this);
        storagePath = common.getDatabaseStorage(this);
        saveBtn.setEnabled(false);
        saveBtn.setOnClickListener(v -> {
            common.setProgressBar("Please Wait...", this, this);
            sendSurveyorData();
        });
        findViewById(R.id.button_identity).setOnClickListener(v -> checkMobileForIdentityCaptured());
        TextView backBtn = findViewById(R.id.BackBtn);
        backBtn.setOnClickListener(v -> {
            onBackPressed();
        });
    }

    private void checkMobileForIdentityCaptured() {
        runOnUiThread(() -> {
            if (!(userMno.getText().toString().length() == 10)) {
                common.closeDialog();
                userMno.setError("Please Enter Valid Mobile No.");
                userMno.requestFocus();
            } else {
                databaseReferencePath.child("Surveyors").orderByChild("mobile").equalTo(userMno.getText().toString()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() != null) {
                            common.showAlertBox("You are already logged in. Please contact your administrator.", true, RegisterActivity.this);
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                            } else {
                                showAlertDialog();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showAlertDialog();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void uploadFile(Bitmap bitmap) {
        mobileNumber = userMno.getText().toString();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + storagePath + "/SurveyorsIdentity");
        StorageReference mountainImagesRef = storageRef.child(userMno.getText().toString() + ".jpg");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 10, baos);
        byte[] data = baos.toByteArray();
        UploadTask uploadTask = mountainImagesRef.putBytes(data);
        uploadTask.addOnFailureListener(exception -> {
            common.closeDialog();
        }).addOnSuccessListener(taskSnapshot -> {
            common.closeDialog();
            saveBtn.setEnabled(true);
        });
    }

    private void sendSurveyorData() {
        if (!(userName.getText().toString().length() > 0)) {
            common.closeDialog();
            userName.setError("Please Enter Full Name.");
            userName.requestFocus();
        } else if (!(mobileNumber.length() == 10)) {
            common.closeDialog();
            userMno.setError("Please Enter Valid Mobile No.");
            userMno.requestFocus();
        } else {
            databaseReferencePath.child("Surveyors").orderByChild("mobile").equalTo(mobileNumber).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        common.showAlertBox("This number not exists in our system.", true, RegisterActivity.this);
                    } else {
                        final DatabaseReference deviceDBRef = databaseReferencePath.child("Surveyors");
                        deviceDBRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String namePrefix = "";
                                if (storagePath.equals("Reengus")) {
                                    namePrefix = "REE";
                                } else {
                                    namePrefix = "SUR";
                                }
                                int lastNumber = 0;
                                if (dataSnapshot.child("LastSorveyourPIN").getValue() != null && !dataSnapshot.child("LastSorveyourPIN").getValue().toString().equals("")) {
                                    lastNumber = Integer.parseInt(dataSnapshot.child("LastSorveyourPIN").getValue().toString());
                                }
                                String nameSuffix = String.valueOf(lastNumber + 1);
                                HashMap<String, String> dataSend = new HashMap<>();
                                dataSend.put("name", userName.getText().toString());
                                dataSend.put("mobile", mobileNumber);
                                dataSend.put("pin", namePrefix + nameSuffix);
                                dataSend.put("status", "1");
                                dataSend.put("joining-date", new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
                                dataSend.put("leaving-date", "");
                                dataSend.put("identity-image", mobileNumber + ".jpg");
                                dataSend.put("surveyor-type", "Surveyor");
                                deviceDBRef.child("LastSorveyourPIN").setValue(nameSuffix);
                                String finalNamePrefix = namePrefix + nameSuffix;
                                deviceDBRef.child(nameSuffix).setValue(dataSend).addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        common.closeDialog();
                                        AlertDialog.Builder alertShowDN = new AlertDialog.Builder(RegisterActivity.this);
                                        alertShowDN.setMessage(Html.fromHtml("आपकी यूजर आईडी " + "<b>" + finalNamePrefix + "</b>" + " है , इसे आप नोट कर लेवें. लॉगिन करते वक्त यहि आपकी लॉगिन आईडी होगी धन्यवाद "));
                                        alertShowDN.setCancelable(false);
                                        alertShowDN.setPositiveButton("OK", (dialog, id) -> {
                                            dialog.cancel();
                                            finish();
                                        });
                                        AlertDialog alertDShowDN = alertShowDN.create();
                                        if (!isFinishing()) {
                                            alertDShowDN.show();
                                        }
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    private void focusOnTouch(MotionEvent event) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getMaxNumMeteringAreas() > 0) {
                Log.i("TAG", "fancy !");
                Rect rect = calculateFocusArea(event.getX(), event.getY());

                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                meteringAreas.add(new Camera.Area(rect, 800));
                parameters.setFocusAreas(meteringAreas);

                mCamera.setParameters(parameters);
                mCamera.autoFocus(mAutoFocusTakePictureCallback);
            } else {
                mCamera.autoFocus(mAutoFocusTakePictureCallback);
            }
        }
    }

    private Camera.AutoFocusCallback mAutoFocusTakePictureCallback = (success, camera) -> {
        if (success) {
        } else {
        }
    };

    private Rect calculateFocusArea(float x, float y) {
        int left = clamp(Float.valueOf((x / surfaceView.getWidth()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);
        int top = clamp(Float.valueOf((y / surfaceView.getHeight()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);

        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    private int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper) + focusAreaSize / 2 > 1000) {
            if (touchCoordinateInCameraReper > 0) {
                result = 1000 - focusAreaSize / 2;
            } else {
                result = -1000 + focusAreaSize / 2;
            }
        } else {
            result = touchCoordinateInCameraReper - focusAreaSize / 2;
        }
        return result;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
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

    public void showAlertDialog() {
        LayoutInflater inflater = getLayoutInflater();
        androidx.appcompat.app.AlertDialog.Builder alertDialog = new androidx.appcompat.app.AlertDialog.Builder(RegisterActivity.this);
        View dialogLayout = inflater.inflate(R.layout.custom_camera_alertbox, null);
        alertDialog.setView(dialogLayout);
        alertDialog.setCancelable(false);
        final androidx.appcompat.app.AlertDialog dialog = alertDialog.create();
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
                setCameraDisplayOrientation(RegisterActivity.this, 0, mCamera);
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
            mCamera.takePicture(null, null, null, pictureCallback);
        });
        Button closeBtn = dialogLayout.findViewById(R.id.close_image_btn);
        closeBtn.setOnClickListener(v -> {
            dialog.cancel();
        });
        dialog.show();

        pictureCallback = (bytes, camera) -> {
            Matrix matrix = new Matrix();
            matrix.postRotate(90F);
            Bitmap identityBitmap = Bitmap.createBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length), 0, 0, BitmapFactory.decodeByteArray(bytes, 0, bytes.length).getWidth(), BitmapFactory.decodeByteArray(bytes, 0, bytes.length).getHeight(), matrix, true);
            imageId.setVisibility(View.VISIBLE);
            imageId.setImageBitmap(identityBitmap);
            camera.stopPreview();
            if (camera != null) {
                camera.release();
                mCamera = null;
            }
            dialog.cancel();
            uploadFile(identityBitmap);
        };

        surfaceView.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                focusOnTouch(motionEvent);
            }
            return false;
        });
    }
}