package com.wevois.surveyapp.views;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.R;
import com.wevois.surveyapp.databinding.ActivityFileDownloadPageBinding;
import com.wevois.surveyapp.viewmodel.FileDownloadViewModel;

public class FileDownloadPageActivity extends AppCompatActivity {
    ActivityFileDownloadPageBinding binding;
    FileDownloadViewModel viewModel;
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_download_page);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_file_download_page);
        viewModel = ViewModelProviders.of(this).get(FileDownloadViewModel.class);
        binding.setFiledownloadviewmodel(viewModel);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
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
                startActivity(new Intent(FileDownloadPageActivity.this, SelectCityActivity.class));
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
                    startActivity(new Intent(FileDownloadPageActivity.this, SelectCityActivity.class));
                    finish();
                } catch (Exception ignored) {
                }
            });
        }
        return super.onOptionsItemSelected(item);
    }
}