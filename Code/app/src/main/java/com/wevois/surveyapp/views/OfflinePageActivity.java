package com.wevois.surveyapp.views;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;
import android.view.WindowManager;

import com.wevois.surveyapp.R;
import com.wevois.surveyapp.databinding.ActivityOfflinePageBinding;
import com.wevois.surveyapp.viewmodel.FormPageViewModel;
import com.wevois.surveyapp.viewmodel.OfflinePageViewModel;

public class OfflinePageActivity extends AppCompatActivity {
    ActivityOfflinePageBinding binding;
    OfflinePageViewModel viewModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_offline_page);
        viewModel = ViewModelProviders.of(this).get(OfflinePageViewModel.class);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding.setOfflinepageviewmodel(viewModel);
        binding.setLifecycleOwner(this);
        viewModel.init(this);
    }
}