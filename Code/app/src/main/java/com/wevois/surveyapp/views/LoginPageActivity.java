package com.wevois.surveyapp.views;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.R;
import com.wevois.surveyapp.databinding.ActivityLoginPageBinding;
import com.wevois.surveyapp.viewmodel.LoginPageViewModel;

public class LoginPageActivity extends AppCompatActivity {
    ActivityLoginPageBinding binding;
    LoginPageViewModel viewModel;
    private final static int LOCATION_REQUEST = 500;
    boolean checkPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login_page);
        viewModel = ViewModelProviders.of(this).get(LoginPageViewModel.class);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.setLoginpageviewmodel(viewModel);
        binding.setLifecycleOwner(this);
        viewModel.init(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logout, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences preferences = getSharedPreferences("surveyApp", MODE_PRIVATE);
        if (preferences.getString("userId","").equalsIgnoreCase("")){
            try {
                SharedPreferences dbPathSP = getSharedPreferences("FirebasePath", MODE_PRIVATE);
                dbPathSP.edit().putString("login", "no").apply();
                preferences.edit().putString("userId", "").apply();
                startActivity(new Intent(LoginPageActivity.this, SelectCityActivity.class));
                finish();
            } catch (Exception ignored) {
            }
        }else {
            CommonFunctions.getInstance().setProgressBar("Check user id.", this, this);
            CommonFunctions.getInstance().getDatabaseForApplication(this).child("Surveyors/" + preferences.getString("userId", "") + "/isLogin").setValue("no").addOnCompleteListener(task -> {
                CommonFunctions.getInstance().closeDialog();
                try {
                    SharedPreferences dbPathSP = getSharedPreferences("FirebasePath", MODE_PRIVATE);
                    dbPathSP.edit().putString("login", "no").apply();
                    preferences.edit().putString("userId", "").apply();
                    startActivity(new Intent(LoginPageActivity.this, SelectCityActivity.class));
                    finish();
                } catch (Exception ignored) {
                }
            });
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST) {
            if (grantResults.length > 0) {
                for (int i = 0; i < permissions.length; i++) {
                    String per = permissions[i];
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, per)) {
                            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                            alertBuilder.setCancelable(false);
                            alertBuilder.setTitle("जरूरी सूचना");
                            alertBuilder.setMessage("सभी permissions देना अनिवार्य है बिना permissions के आप आगे नहीं बढ़ सकते है |");
                            alertBuilder.setPositiveButton(android.R.string.yes, (dialog, which) -> viewModel.allowPermissions());

                            AlertDialog alert = alertBuilder.create();
                            alert.show();
                        } else {
                            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                            alertBuilder.setCancelable(false);
                            alertBuilder.setTitle("जरूरी सूचना");
                            alertBuilder.setMessage("सभी permissions देना अनिवार्य है बिना permissions के आप आगे नहीं बढ़ सकते है |");
                            alertBuilder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                checkPermission = true;
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            });

                            AlertDialog alert = alertBuilder.create();
                            alert.show();
                        }
                        return;
                    }
                }
                viewModel.allowPermissions();
            } else {
                viewModel.allowPermissions();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATION_REQUEST) {
            viewModel.allowPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermission) {
            checkPermission = false;
            viewModel.allowPermissions();
        }
    }
}