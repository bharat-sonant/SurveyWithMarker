package com.wevois.surveyapp.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.R;

public class VerifyActivity extends AppCompatActivity {

    TextView cardNoText;
    String rfidNumber, houseType = "", markingKey = "";
    SharedPreferences preferences;
    Button continueBtn;
    CommonFunctions common = new CommonFunctions();
    DatabaseReference databaseReferencePath;
    int currentLine = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);

        databaseReferencePath = common.getDatabaseForApplication(this);
        preferences = getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
        currentLine = Integer.parseInt(preferences.getString("line",""));
        houseType = preferences.getString("houseType","");
        markingKey = preferences.getString("markingKey","");
        cardNoText = findViewById(R.id.cardNo);
        continueBtn = findViewById(R.id.continueEnter);
        rfidNumber = preferences.getString("rfid", "");
        if (common.getDatabaseStorage(this).equals("Sikar") || common.getDatabaseStorage(this).equals("Jaipur") || common.getDatabaseStorage(this).equals("Test")|| common.getDatabaseStorage(this).equals("Shahpura")) {
            cardNoText.setText(preferences.getString("cardNo", ""));
        }
        setAction();
    }

    @SuppressLint("StaticFieldLeak")
    private void setAction() {
        continueBtn.setOnClickListener(view1 -> {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    common.setProgressBar("Please Wait...", VerifyActivity.this, VerifyActivity.this);
                }

                @Override
                protected Boolean doInBackground(Void... p) {
                    return common.network(VerifyActivity.this);
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    if (result) {
                        continueToSurveyReengusForm();
                    } else {
                        common.closeDialog();
                        moveToSurveyForm();
                    }
                }
            }.execute();
        });
        TextView backBtn = findViewById(R.id.BackBtn);
        backBtn.setOnClickListener(v -> {
            onBackPressed();
        });
    }

    private void continueToSurveyReengusForm() {
        String alreadyMappedCardNumber = preferences.getString("cardNo", "");
        if (alreadyMappedCardNumber.length() > 0) {
            databaseReferencePath.child("CardWardMapping/" + alreadyMappedCardNumber).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        String assignedLine = dataSnapshot.child("line").getValue().toString();
                        String assignedWard = dataSnapshot.child("ward").getValue().toString();
                        if (assignedLine.equalsIgnoreCase(preferences.getString("line", "")) && assignedWard.equalsIgnoreCase(preferences.getString("ward", ""))) {
                            moveToSurveyForm();
                        }else {
                            showDetailToSurveyor(alreadyMappedCardNumber, assignedWard, assignedLine, true);
                        }
                    } else {
                        moveToSurveyForm();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }

    private void moveToSurveyForm() {
        common.closeDialog();
        Intent intent = new Intent(VerifyActivity.this, FormActivity.class);
        intent.putExtra("line", currentLine);
        intent.putExtra("houseType", houseType);
        intent.putExtra("markingKey", markingKey);
        startActivity(intent);
        finish();
    }

    private void showDetailToSurveyor(String cardNumber, String assignedWard, String assignedLine, boolean cardExistsAtAnotherLine) {
        databaseReferencePath.child("Houses/" + assignedWard + "/" + assignedLine).orderByChild("cardNo").equalTo(cardNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        if (snapshot.hasChild("name")) {
                            String name = snapshot.child("name").getValue().toString();
                            String mobileNo = snapshot.getKey();
                            String message;
                            if (cardExistsAtAnotherLine) {
                                message = "यह कार्ड पहिले से ही वार्ड <b>" + assignedWard + " </b> में लाइन <b> " + assignedLine + "</b>" + " पर <b>" + cardNumber + "</b> नंबर के साथ " + "<b>" + name + " (" + mobileNo + ")" + "</b>" + " को अलोकेटेड है |";
                            } else {
                                message = "<b>" + cardNumber + "</b> पहिले से ही वार्ड <b>" + assignedWard + " </b> में लाइन <b> " + assignedLine + "</b>" + " पर " + "<b>" + name + " (" + mobileNo + ")" + "</b>" + " को अलोकेटेड है | " + " कृपया कार्ड नंबर फिर से चेक करे |<br><br><br>" + "अन्यथा अपने सर्वे मैनेजर से समपर्क करे |";
                            }
                            common.showAlertBox(message, false, VerifyActivity.this);
                        }
                    }
                    common.closeDialog();
                } else {
                    common.closeDialog();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
}