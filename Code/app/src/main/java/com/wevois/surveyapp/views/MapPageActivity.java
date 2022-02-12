package com.wevois.surveyapp.views;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import com.google.android.gms.maps.SupportMapFragment;
import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.R;
import com.wevois.surveyapp.databinding.ActivityMapPageBinding;
import com.wevois.surveyapp.repository.Repository;
import com.wevois.surveyapp.viewmodel.MapPageViewModel;

public class MapPageActivity extends AppCompatActivity {
    private final static int REQUEST_CHECK_SETTINGS = 500;
    ActivityMapPageBinding binding;
    MapPageViewModel viewModel;
    SharedPreferences preferences;
    SupportMapFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_map_page);
        viewModel = ViewModelProviders.of(this).get(MapPageViewModel.class);
        binding.setMappageviewmodel(viewModel);
        binding.setLifecycleOwner(this);
        preferences = getSharedPreferences("surveyApp", MODE_PRIVATE);
        preferences.edit().putString("isOnResumeCall", "").apply();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        viewModel.init(this, fragment);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode != RESULT_OK) {
                viewModel.checkWhetherLocationSettingsAreSatisfied();
            }else {
                viewModel.checkWhetherLocationSettingsAreSatisfied();
            }
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

            if (item.getItemId() == R.id.item4) {
                CommonFunctions.getInstance().showAlertBox(getPackageManager().getPackageInfo(getPackageName(), 0).versionName, false, MapPageActivity.this);
            }
        } catch (Exception ignored) {
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferences.getString("isOnResumeCall","").equalsIgnoreCase("yes")) {
            preferences.edit().putString("isOnResumeCall", "").apply();
            viewModel.setTodayCardScan();
            viewModel.onResumeCall();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}