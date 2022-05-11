package com.wevois.surveyapp.viewmodel;

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
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.databinding.BindingAdapter;
import androidx.databinding.ObservableField;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.R;
import com.wevois.surveyapp.repository.Repository;
import com.wevois.surveyapp.views.RegisterPageActivity;
import java.util.ArrayList;
import java.util.List;

public class RegisterPageViewModel extends ViewModel {
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    Activity activity;
    CommonFunctions common = CommonFunctions.getInstance();
    public final ObservableField<String> userNameTv = new ObservableField<>("");
    public final ObservableField<String> userMobileTv = new ObservableField<>("");
    public ObservableField<Boolean> isVisible = new ObservableField<>(false);
    public MutableLiveData<Bitmap> imageViewUrl = new MutableLiveData<>();

    private Camera mCamera;
    private static final int FOCUS_AREA_SIZE = 300;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    SurfaceHolder.Callback surfaceViewCallBack;
    Camera.PictureCallback pictureCallback;

    public void init(RegisterPageActivity registerPageActivity) {
        activity = registerPageActivity;
    }

    public void saveClick() {
        if(isVisible.get().booleanValue()){
            common.setProgressBar("Please Wait...", activity, activity);
            if (!(userNameTv.get().length() > 0)) {
                common.closeDialog();
                common.showAlertBox("Please enter name.",false,activity);
            } else if (!(userMobileTv.get().length() == 10)) {
                common.closeDialog();
                common.showAlertBox("Please Enter Valid Mobile No.",false,activity);
            } else {
                new Repository().sendRegisterData(activity,userMobileTv.get(),userNameTv.get()).observeForever(result -> {
                    if (result.equalsIgnoreCase("Error")) {
                        common.showAlertBox("This number not exists in our system.", true, activity);
                    } else {
                        common.closeDialog();
                        AlertDialog.Builder alertShowDN = new AlertDialog.Builder(activity);
                        alertShowDN.setMessage(Html.fromHtml("आपकी यूजर आईडी " + "<b>" + result + "</b>" + " है , इसे आप नोट कर लेवें. लॉगिन करते वक्त यहि आपकी लॉगिन आईडी होगी धन्यवाद "));
                        alertShowDN.setCancelable(false);
                        alertShowDN.setPositiveButton("OK", (dialog, id) -> {
                            dialog.cancel();
                            activity.finish();
                        });
                        AlertDialog alertDShowDN = alertShowDN.create();
                        if (!activity.isFinishing()) {
                            alertDShowDN.show();
                        }
                    }
                });
            }
        }else {
            common.showAlertBox("First click photo.",false,activity);
        }
    }

    public void imageClick() {
        common.setProgressBar("Please Wait...", activity, activity);
        if (!(userMobileTv.get().length() == 10)) {
            common.closeDialog();
            common.showAlertBox("Please enter valid mobile no.", false, activity);
        } else {
            new Repository().checkMobile(activity, userMobileTv.get()).observeForever(dataSnapshot -> {
                if (dataSnapshot.getValue() != null) {
                    common.showAlertBox("You are already logged in. Please contact your administrator.", true, activity);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                        common.closeDialog();
                    } else {
                        showAlertDialog();
                    }
                }
            });
        }
    }

    @BindingAdapter({"imageUrl"})
    public static void loadImage(ImageView view, Bitmap bitmap) {
        if (bitmap == null) {
        } else {
            view.setImageBitmap(bitmap);
        }

    }

    public TextWatcher userNameTvWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                userNameTv.set(editable.toString());
            }
        };
    }

    public TextWatcher userMobileTvWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                userMobileTv.set(editable.toString());
            }
        };
    }

    private void focusOnTouch(MotionEvent event) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getMaxNumMeteringAreas() > 0) {
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
        common.closeDialog();
        LayoutInflater inflater = activity.getLayoutInflater();
        androidx.appcompat.app.AlertDialog.Builder alertDialog = new androidx.appcompat.app.AlertDialog.Builder(activity);
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
            Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Bitmap bitmaps = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
            Bitmap identityBitmap = Bitmap.createScaledBitmap(bitmaps, 400, 600, false);
            isVisible.set(true);
            imageViewUrl.setValue(identityBitmap);
            camera.stopPreview();
            if (camera != null) {
                camera.release();
                mCamera = null;
            }
            dialog.cancel();
            new Repository().uploadRegisterImage(activity,identityBitmap,userMobileTv.get()).observeForever(result -> {
                common.closeDialog();
            });
        };

        surfaceView.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                focusOnTouch(motionEvent);
            }
            return false;
        });
    }
}
