package com.wevois.surveyapp.views;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import com.wevois.surveyapp.R;
import com.wevois.surveyapp.databinding.ActivityRegisterPageBinding;
import com.wevois.surveyapp.viewmodel.LoginPageViewModel;
import com.wevois.surveyapp.viewmodel.RegisterPageViewModel;

public class RegisterPageActivity extends AppCompatActivity {
    ActivityRegisterPageBinding binding;
    RegisterPageViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_register_page);
        viewModel = ViewModelProviders.of(this).get(RegisterPageViewModel.class);
        binding.setRegisterpageviewmodel(viewModel);
        binding.setLifecycleOwner(this);
        viewModel.init(this);
    }
}