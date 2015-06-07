package org.aadhaar.batua;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aadhaarconnect.bridge.capture.model.auth.AuthCaptureRequest;
import com.aadhaarconnect.bridge.capture.model.common.Location;
import com.aadhaarconnect.bridge.capture.model.common.LocationType;
import com.aadhaarconnect.bridge.capture.model.common.request.CertificateType;
import com.aadhaarconnect.bridge.capture.model.common.request.Modality;
import com.aadhaarconnect.bridge.capture.model.otp.OtpCaptureRequest;
import com.aadhaarconnect.bridge.capture.model.otp.OtpChannel;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import com.rengwuxian.materialedittext.MaterialEditText;

public class PaymentActivity extends AppCompatActivity {

    // UI elements
    private TextView mName;
    private TextView mUid;
    private TextView mReceiverName;
    private TextView mReceiverUid;
    private TextView mReceiverAmount;
    private TextView mFutureBalance;
    private MaterialEditText mOtpEditText;
    private Button mConfirm;
    private Button mResend;

    // Other members
    private Firebase mFirebase;
    private ProgressDialog mProgressDialog;
    private JsonObject mOtpJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        Firebase.setAndroidContext(this);

        mFirebase = new Firebase("https://aadhaar-batua.firebaseio.com");
        final User receiver = (User)getIntent().getSerializableExtra("receiver");
        receiver.updateBalance(Long.parseLong(getIntent().getStringExtra("receiverAmount")));

        mName = (TextView)findViewById(R.id.name);
        mName.setText(((AadhaarBatuaApplication)getApplication()).getUser().getName());
        mUid = (TextView)findViewById(R.id.uid);
        mUid.setText(((AadhaarBatuaApplication)getApplication()).getUser().getUid());
        mReceiverName = (TextView)findViewById(R.id.receiver_name);
        mReceiverName.setText(receiver.getName());
        mReceiverUid = (TextView)findViewById(R.id.receiver_uid);
        mReceiverUid.setText("********" + receiver.getUid().substring(receiver.getUid().length() - 4));
        mReceiverAmount = (TextView)findViewById(R.id.receiver_amount);
        mReceiverAmount.setText("\u20B9" + getIntent().getStringExtra("receiverAmount"));
        mFutureBalance = (TextView)findViewById(R.id.future_balance);
        mFutureBalance.setText("Your Batua balance will be: \u20B9" + getIntent().getStringExtra("futureBalance"));
        mOtpEditText = (MaterialEditText)findViewById(R.id.etOtp);
        mConfirm = (Button)findViewById(R.id.bconfirm);
        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProgressDialog.setMessage("Processing payment ...");
                mProgressDialog.show();
                AuthCaptureRequest captureRequest = new AuthCaptureRequest();
                captureRequest.setAadhaar(AadhaarBatuaApplication.getInstance().getUser().getUid());
                captureRequest.setCertificateType(CertificateType.preprod);
                captureRequest.setOtp(mOtpEditText.getText().toString());
                captureRequest.setModality(Modality.otp);
                Location location = new Location();
                location.setType(LocationType.pincode);
                location.setPincode(AadhaarBatuaApplication.getInstance().getUser().getPincode());
                captureRequest.setLocation(location);
                JsonObject json = new Gson().fromJson(new Gson().toJson(captureRequest), JsonObject.class);
                Ion.with(PaymentActivity.this)
                        .load("POST", "https://ac.khoslalabs.com/hackgate/hackathon/auth/raw")
                        .setJsonObjectBody(json)
                        .asJsonObject()
                        .withResponse()
                        .setCallback(new FutureCallback<Response<JsonObject>>() {
                            @Override
                            public void onCompleted(Exception e, Response<JsonObject> result) {
                                if (result.getHeaders().code() == 200) {
                                    mProgressDialog.hide();
                                    Toast.makeText(PaymentActivity.this, "Thank you! Your transaction has been successfully completed", Toast.LENGTH_LONG)
                                            .show();
                                    mFirebase.child(receiver.getUid()).child("balance").setValue(
                                            receiver.getBalance() +
                                                    Integer.parseInt(getIntent().getStringExtra("receiverAmount"))
                                    );
                                    mFirebase.child(AadhaarBatuaApplication.getInstance().getUser().getUid()).child("balance").setValue(
                                            Long.parseLong(getIntent().getStringExtra("futureBalance"))
                                    );
                                    finish();
                                } else {
                                    Toast.makeText(PaymentActivity.this, "Not a valid OTP", Toast.LENGTH_SHORT)
                                            .show();
                                }
                            }
                        });
            }
        });
        mResend = (Button)findViewById(R.id.bresend);
        mResend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendOtpRequest();
            }
        });

        Query balanceQuery = mFirebase.child(receiver.getUid()).child("balance").orderByValue();
        balanceQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    mFirebase.child(receiver.getUid()).child("balance").setValue(5000);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                mProgressDialog.hide();
                Toast.makeText(PaymentActivity.this, "Please try again.", Toast.LENGTH_SHORT)
                        .show();
            }
        });

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
                    return true;
                }
                return false;
            }
        });

        OtpCaptureRequest otpCaptureRequest = new OtpCaptureRequest();
        otpCaptureRequest.setAadhaar(AadhaarBatuaApplication.getInstance().getUser().getUid());
        otpCaptureRequest.setCertificateType(CertificateType.preprod);
        otpCaptureRequest.setChannel(OtpChannel.SMS);
        Location location = new Location();
        location.setType(LocationType.pincode);
        location.setPincode(receiver.getPincode());
        otpCaptureRequest.setLocation(location);
        mOtpJson = new Gson().fromJson(new Gson().toJson(otpCaptureRequest), JsonObject.class);

        sendOtpRequest();
    }

    public void sendOtpRequest() {
        mProgressDialog.setMessage("Sending OTP ...");
        mProgressDialog.show();
        Ion.with(this)
                .load("POST", "https://ac.khoslalabs.com/hackgate/hackathon/otp")
                .setJsonObjectBody(mOtpJson)
                .asJsonObject()
                .withResponse()
                .setCallback(new FutureCallback<Response<JsonObject>>() {
                    @Override
                    public void onCompleted(Exception e, Response<JsonObject> result) {
                        if (result.getHeaders().code() == 200) {
                            JsonObject response = result.getResult();
                            if (response.has("success")) {
                                if (response.get("success").getAsBoolean()) {
                                    // OTP has been sent
                                    mProgressDialog.hide();
                                    Toast.makeText(PaymentActivity.this, "OTP has been sent to your Aadhaar registered mobile", Toast.LENGTH_LONG)
                                            .show();
                                }
                            }
                        } else {
                            mProgressDialog.hide();
                            Toast.makeText(PaymentActivity.this, "An error occurred during OTP request", Toast.LENGTH_LONG)
                                    .show();
                        }
                    }
                });
    }

}
