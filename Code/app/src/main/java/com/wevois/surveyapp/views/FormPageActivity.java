package com.wevois.surveyapp.views;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import com.wevois.surveyapp.R;
import com.wevois.surveyapp.databinding.ActivityFormPageBinding;
import com.wevois.surveyapp.viewmodel.FormPageViewModel;

public class FormPageActivity extends AppCompatActivity {
    ActivityFormPageBinding binding;
    FormPageViewModel viewModel;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_page);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_form_page);
        viewModel = ViewModelProviders.of(this).get(FormPageViewModel.class);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding.setFormpageviewmodel(viewModel);
        binding.setLifecycleOwner(this);
        viewModel.init(this,binding.spnrHouseType,getIntent().getStringExtra("from"),binding.spnrHouseTypeCardRevisit,binding.spnrReason);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == MY_CAMERA_PERMISSION_CODE) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    viewModel.showAlertDialog(true);
                } else {
                    Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
                }
            }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        viewModel.onBack();
    }
}