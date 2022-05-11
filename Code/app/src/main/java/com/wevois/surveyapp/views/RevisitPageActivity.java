package com.wevois.surveyapp.views;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;
import android.view.WindowManager;

import com.wevois.surveyapp.R;
import com.wevois.surveyapp.databinding.ActivityRevisitPageBinding;
import com.wevois.surveyapp.viewmodel.RevisitPageViewModel;

public class RevisitPageActivity extends AppCompatActivity {
    ActivityRevisitPageBinding binding;
    RevisitPageViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revisit_page);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_revisit_page);
        viewModel = ViewModelProviders.of(this).get(RevisitPageViewModel.class);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding.setRevisitpageviewmodel(viewModel);
        binding.setLifecycleOwner(this);
        viewModel.init(this,binding.spinner3,binding.spnrHouseTypeRevisit);
    }
}