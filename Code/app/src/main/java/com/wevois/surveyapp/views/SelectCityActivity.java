package com.wevois.surveyapp.views;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.wevois.surveyapp.R;
import com.wevois.surveyapp.databinding.ActivitySelectCityBinding;
import com.wevois.surveyapp.viewmodel.SelectCityViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class SelectCityActivity extends AppCompatActivity {
    ActivitySelectCityBinding binding;
    SelectCityViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_city);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_select_city);
        viewModel = ViewModelProviders.of(this).get(SelectCityViewModel.class);
        binding.setSelectviewmodel(viewModel);
        binding.setLifecycleOwner(this);
        viewModel.init(this);
    }



    @Override
    protected void onStart() {
        super.onStart();
        runOnUiThread(() -> {
            SharedPreferences dbPathSP = getSharedPreferences("FirebasePath", MODE_PRIVATE);
            if (dbPathSP.getString("login", "").equals("yes")) {
                startActivity(new Intent(SelectCityActivity.this, LoginPageActivity.class));
                finish();
            }
        });
    }
}