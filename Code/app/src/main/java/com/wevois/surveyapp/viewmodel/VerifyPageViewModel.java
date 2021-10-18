package com.wevois.surveyapp.viewmodel;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.google.firebase.database.DataSnapshot;
import com.wevois.surveyapp.CommonFunctions;
import com.wevois.surveyapp.repository.Repository;
import com.wevois.surveyapp.views.FormPageActivity;
import com.wevois.surveyapp.views.VerifyPageActivity;
public class VerifyPageViewModel extends ViewModel {
    Activity activity;
    CommonFunctions common = new CommonFunctions();
    SharedPreferences preferences;
    int currentLine = 1;
    String rfidNumber, houseType = "", markingKey = "";
    public final ObservableField<String> cardNumber = new ObservableField<>("");

    public void init(VerifyPageActivity verifyPageActivity){
        activity = verifyPageActivity;
        preferences = activity.getSharedPreferences("surveyApp", Context.MODE_PRIVATE);
        currentLine = Integer.parseInt(preferences.getString("line",""));
        houseType = preferences.getString("houseType","");
        markingKey = preferences.getString("markingKey","");
        rfidNumber = preferences.getString("rfid", "");
        if (common.getDatabaseStorage(activity).equals("Sikar") || common.getDatabaseStorage(activity).equals("Jaipur") || common.getDatabaseStorage(activity).equals("Test")|| common.getDatabaseStorage(activity).equals("Shahpura")) {
            cardNumber.set(preferences.getString("cardNo", ""));
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void onClick(){
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                common.setProgressBar("Please Wait...", activity, activity);
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                return common.network(activity);
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
    }

    public void onBack(){
        activity.finish();
    }

    private void continueToSurveyReengusForm() {
        String alreadyMappedCardNumber = preferences.getString("cardNo", "");
        if (alreadyMappedCardNumber.length() > 0) {
            new Repository().CheckWardMapping(activity, alreadyMappedCardNumber).observeForever(dataSnapshot -> {
                if (dataSnapshot.getValue() != null) {
                    String assignedLine = dataSnapshot.child("line").getValue().toString();
                    String assignedWard = dataSnapshot.child("ward").getValue().toString();
                    if (assignedLine.equalsIgnoreCase(preferences.getString("line", "")) && assignedWard.equalsIgnoreCase(preferences.getString("ward", ""))) {
                        moveToSurveyForm();
                    } else {
                        showDetailToSurveyor(alreadyMappedCardNumber, assignedWard, assignedLine, true);
                    }
                } else {
                    moveToSurveyForm();
                }
            });
        }
    }

    private void moveToSurveyForm() {
        common.closeDialog();
        Intent intent = new Intent(activity, FormPageActivity.class);
        intent.putExtra("from", "verify");
        activity.startActivity(intent);
        activity.finish();
    }

    private void showDetailToSurveyor(String cardNumber, String assignedWard, String assignedLine, boolean cardExistsAtAnotherLine) {
        new Repository().CheckHousesDetails(activity, assignedWard,assignedLine,cardNumber).observeForever(dataSnapshot -> {
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
                        common.showAlertBox(message, false, activity);
                    }
                }
                common.closeDialog();
            } else {
                common.closeDialog();
            }
        });
    }
}
