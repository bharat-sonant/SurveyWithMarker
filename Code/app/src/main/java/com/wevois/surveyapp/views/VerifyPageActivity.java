package com.wevois.surveyapp.views;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import com.wevois.surveyapp.R;
import com.wevois.surveyapp.databinding.ActivityVerifyPageBinding;
import com.wevois.surveyapp.viewmodel.VerifyPageViewModel;

public class VerifyPageActivity extends AppCompatActivity {
    ActivityVerifyPageBinding binding;
    VerifyPageViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_verify_page);
        viewModel = ViewModelProviders.of(this).get(VerifyPageViewModel.class);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.setVerifypageviewmodel(viewModel);
        binding.setLifecycleOwner(this);
        viewModel.init(this);
    }
}