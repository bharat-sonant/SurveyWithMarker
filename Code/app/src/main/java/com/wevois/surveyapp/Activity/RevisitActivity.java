package com.wevois.surveyapp.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.R;
import com.wevois.surveyapp.Revisited;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RevisitActivity extends AppCompatActivity {
    final List<String> typesList1 = new ArrayList<>();
    DatabaseReference databaseReferencePath;
    CommonFunctions common = new CommonFunctions();
    SharedPreferences preferences;
    Button revisitBtn;
    EditText name;
    List<String> houseTypeList = new ArrayList<>();
    RadioButton awasiyeBtn, commercialBtn;
    Spinner spinnerRevisit, spinnerHouseType;
    JSONArray jsonArrayHouseType = new JSONArray();
    boolean isBack = true;
    int q = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revisit);

        databaseReferencePath = common.getDatabaseForApplication(this);
        preferences = getSharedPreferences("surveyApp", MODE_PRIVATE);
        spinnerRevisit = findViewById(R.id.spinner3);
        name = findViewById(R.id.revisitName);
        revisitBtn = findViewById(R.id.revisitedpage);
        awasiyeBtn = findViewById(R.id.radio_awasiyeRevisit);
        commercialBtn = findViewById(R.id.radio_comRevisit);
        spinnerHouseType = findViewById(R.id.spnrHouseTypeRevisit);
        q = getIntent().getIntExtra("line", 0);
        typesList1.add("Select Reason");
        TextView backBtn = findViewById(R.id.BackBtn);
        backBtn.setOnClickListener(v -> {
            onBackPressed();
        });
        databaseReferencePath.child("Defaults").child("CardRevisitReasons").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    typesList1.add(String.valueOf(snapshot.getValue()));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        final ArrayAdapter<String> spinnerArrayAdapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, typesList1) {
            @Override
            public boolean isEnabled(int position) {
                if (position == 0) {
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if (position == 0) {
                    tv.setTextColor(Color.GRAY);
                } else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        spinnerArrayAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRevisit.setAdapter(spinnerArrayAdapter1);
        revisitBtn.setOnClickListener(view1 -> {
            if (name.getText().toString().trim().length() == 0) {
                name.setError("Please enter name");
                name.requestFocus();
            } else if (spinnerRevisit.getSelectedItem().toString().equals("Select Reason")) {
                common.closeDialog();
                View selectedView = spinnerRevisit.getSelectedView();
                if (selectedView != null && selectedView instanceof TextView) {
                    spinnerRevisit.requestFocus();
                    TextView selectedTextView = (TextView) selectedView;
                    selectedTextView.setError("error");
                    selectedTextView.setTextColor(Color.RED);
                    selectedTextView.setText("please select type");
                    spinnerRevisit.performClick();
                }
                return;
            } else if (spinnerHouseType.getSelectedItem().toString().equals("Select Entity type")) {
                View selectedView = spinnerHouseType.getSelectedView();
                if (selectedView != null && selectedView instanceof TextView) {
                    spinnerHouseType.requestFocus();
                    TextView selectedTextView = (TextView) selectedView;
                    selectedTextView.setError("error");
                    selectedTextView.setTextColor(Color.RED);
                    selectedTextView.setText("please select type");
                    spinnerHouseType.performClick();
                }
                return;
            } else {
                common.setProgressBar("Please Wait...", this, this);
                databaseReferencePath.child("EntitySurveyData").child("RevisitRequest/" + preferences.getString("ward", "") + "/" + preferences.getString("line", "") + "/lineRevisitCount").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int count = 1;
                        if (dataSnapshot.getValue() != null) {
                            count = Integer.parseInt(dataSnapshot.getValue().toString()) + 1;
                        }
                        databaseReferencePath.child("EntitySurveyData").child("RevisitRequest/" + preferences.getString("ward", "") + "/" + preferences.getString("line", "") + "/lineRevisitCount").setValue("" + count);
                        try {
                            String ward = preferences.getString("ward", "");
                            Revisited revisited = new Revisited(preferences.getString("lat", ""), preferences.getString("lng", ""), spinnerRevisit.getSelectedItem().toString(), jsonArrayHouseType.get(spinnerHouseType.getSelectedItemPosition() - 1).toString(), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), preferences.getString("userId", ""), "Surveyor", name.getText().toString());
                            DatabaseReference databaseReference = databaseReferencePath.child("EntitySurveyData").child("RevisitRequest").child(ward).child(preferences.getString("line", ""));
                            databaseReference.push().setValue(revisited);
                            dailyRevisitRequestCount();
                            totalRevisitRequest();
                            common.closeDialog();
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(RevisitActivity.this);
                            builder1.setMessage("आपका सर्वे पूरा हुआ, धन्यवाद !");
                            builder1.setCancelable(true);
                            builder1.setPositiveButton("OK", (dialog, id) -> {
                                dialog.cancel();
                                finish();
                            });
                            AlertDialog alert11 = builder1.create();
                            alert11.show();
                        } catch (Exception e) {
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        });
        awasiyeBtn.setOnClickListener(view12 -> {
            getHouseTypes(false);
        });
        commercialBtn.setOnClickListener(view13 -> {
            getHouseTypes(true);
        });
        getHouseTypes(false);
    }

    private void totalRevisitRequest() {
        String ward = preferences.getString("ward", "");
        databaseReferencePath.child("EntitySurveyData/TotalRevisitRequest/" + ward).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    String count = String.valueOf(Integer.parseInt(dataSnapshot.getValue().toString()) + 1);
                    databaseReferencePath.child("EntitySurveyData/TotalRevisitRequest/" + ward).setValue(count);
                } else {
                    databaseReferencePath.child("EntitySurveyData/TotalRevisitRequest/" + ward).setValue("1");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void dailyRevisitRequestCount() {
        SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd-MM-yyyy");
        String date1 = dateFormat1.format(new Date());
        String ward = preferences.getString("ward", "");
        String userId = preferences.getString("userId", "");
        databaseReferencePath.child("EntitySurveyData/DailyRevisitRequestCount/" + ward + "/" + userId + "/" + date1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    String count = String.valueOf(Integer.parseInt(dataSnapshot.getValue().toString()) + 1);
                    databaseReferencePath.child("EntitySurveyData/DailyRevisitRequestCount/" + ward + "/" + userId + "/" + date1).setValue(count);
                } else {
                    databaseReferencePath.child("EntitySurveyData/DailyRevisitRequestCount/" + ward + "/" + userId + "/" + date1).setValue("1");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void getHouseTypes(Boolean isCommercial) {
        houseTypeList.clear();
        houseTypeList.add("Select Entity type");
        JSONObject jsonObject, commercialJsonObject, residentialJsonObject;
        jsonArrayHouseType = new JSONArray();
        try {
            jsonObject = new JSONObject(preferences.getString("housesTypeList", ""));
            commercialJsonObject = new JSONObject(preferences.getString("commercialHousesTypeList", ""));
            residentialJsonObject = new JSONObject(preferences.getString("residentialHousesTypeList", ""));
            for (int i = 1; i <= jsonObject.length(); i++) {
                if (isCommercial) {
                    try {
                        houseTypeList.add(commercialJsonObject.getString(String.valueOf(i)));
                        jsonArrayHouseType.put(i);
                    } catch (JSONException e) {
                    }
                } else {
                    try {
                        houseTypeList.add(residentialJsonObject.getString(String.valueOf(i)));
                        jsonArrayHouseType.put(i);
                    } catch (JSONException e) {
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        bindHouseTypesToSpinner();
    }

    private void bindHouseTypesToSpinner() {
        final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, houseTypeList) {
            @Override
            public boolean isEnabled(int position) {
                return !(position == 0);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(position == 0 ? Color.GRAY : Color.BLACK);
                return view;
            }
        };
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHouseType.setAdapter(spinnerArrayAdapter);
    }
}