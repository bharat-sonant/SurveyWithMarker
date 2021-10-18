package com.wevois.surveyapp.views;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import com.wevois.surveyapp.R;
import com.wevois.surveyapp.databinding.ActivityFileDownloadPageBinding;
import com.wevois.surveyapp.viewmodel.FileDownloadViewModel;
import com.wevois.surveyapp.viewmodel.LoginPageViewModel;

public class FileDownloadPageActivity extends AppCompatActivity {
    ActivityFileDownloadPageBinding binding;
    FileDownloadViewModel viewModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_download_page);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_file_download_page);
        viewModel = ViewModelProviders.of(this).get(FileDownloadViewModel.class);
        binding.setFiledownloadviewmodel(viewModel);
        binding.setLifecycleOwner(this);
        viewModel.init(this);
    }
}