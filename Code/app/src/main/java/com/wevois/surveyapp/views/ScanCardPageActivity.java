package com.wevois.surveyapp.views;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;
import android.view.WindowManager;

import com.wevois.surveyapp.R;
import com.wevois.surveyapp.databinding.ActivityScanCardPageBinding;
import com.wevois.surveyapp.viewmodel.ScanCardPageViewModel;

public class ScanCardPageActivity extends AppCompatActivity {
    ActivityScanCardPageBinding binding;
    ScanCardPageViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_scan_card_page);
        viewModel = ViewModelProviders.of(this).get(ScanCardPageViewModel.class);
        binding.setScancardpageviewmodel(viewModel);
        binding.setLifecycleOwner(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        viewModel.init(this, binding.surfaceView);
    }
}