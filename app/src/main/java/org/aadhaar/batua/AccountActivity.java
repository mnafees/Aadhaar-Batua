/*
 * This file is part of Aadhaar Batua.
 * Copyright (c) 2015 Mohammed Nafees (original author).
 */

package org.aadhaar.batua;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AccountActivity extends AppCompatActivity {

    // UI elements
    private TextView mName;
    private TextView mUid;
    private TextView mBalance;
    private MaterialEditText mAmountPay;

    // Other members
    private Firebase mFirebase;
    private IntentIntegrator mIntentIntegrator;
    private long mBatuaBalance = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        Firebase.setAndroidContext(this);

        final User user = ((AadhaarBatuaApplication)getApplication()).getUser();

        mName = (TextView)findViewById(R.id.name);
        mName.setText(user.getName());
        mUid = (TextView)findViewById(R.id.uid);
        mUid.setText("UID: " + user.getUid());
        mBalance = (TextView)findViewById(R.id.balance);

        mFirebase = new Firebase("https://aadhaar-batua.firebaseio.com");
        Query balanceQuery = mFirebase.child(user.getUid()).child("balance").orderByValue();
        balanceQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    mBatuaBalance = (long)dataSnapshot.getValue();
                    mBalance.setText("Your Batua Balance: \u20B9" +
                            Long.toString(mBatuaBalance));
                } else {
                    mFirebase.child(user.getUid()).child("balance").setValue(mBatuaBalance);
                }
                AadhaarBatuaApplication.getInstance().getUser().updateBalance(mBatuaBalance);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                mBalance.setText("Could not retrieve your Batua.");
            }
        });

        mIntentIntegrator = new IntentIntegrator(this);
        mIntentIntegrator.setCaptureActivity(CaptureActivityAnyOrientation.class);
        mIntentIntegrator.setPrompt("Scan the receiver\'s Aadhaar QR Code");
        mIntentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        mIntentIntegrator.setOrientationLocked(false);

        mAmountPay = (MaterialEditText)findViewById(R.id.amountPay);
        Button pay = (Button)findViewById(R.id.bpay);
        pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAmountPay.getText().length() == 0) {
                    mAmountPay.setError("Please enter an amount");
                } else {
                    if (Long.parseLong(mAmountPay.getText().toString()) > mBatuaBalance) {
                        mAmountPay.setError("Payment amount can\'t be greater than your batua balance");
                    } else {
                        mIntentIntegrator.initiateScan();
                    }
                }
            }
        });

        final MaterialEditText amountRefill = (MaterialEditText)findViewById(R.id.amountRefill);
        Button refill = (Button)findViewById(R.id.brefill);
        refill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (amountRefill.getText().length() == 0) {
                    amountRefill.setError("Please enter an amount");
                } else {
                    if (amountRefill.getText().length() > 6) {
                        amountRefill.setError("You can refill a maximum of \u20B99,99,999 at a time");
                    } else {
                        mFirebase.child(user.getUid()).child("balance").setValue(Integer.parseInt(
                                amountRefill.getText().toString()) + mBatuaBalance);
                    }
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        final IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            // handle scan result
            XmlPullParser parser = null;
            InputStream stream = null;
            try {
                parser = Xml.newPullParser();
                stream = new ByteArrayInputStream(scanResult.getContents().getBytes(StandardCharsets.UTF_8));
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(stream, null);
                parser.nextTag();
                if (!parser.getName().equals("PrintLetterBarcodeData")) {
                    // not an Aadhaar QR Code
                    Toast.makeText(this, "Not a valid Aadhaar QR Code", Toast.LENGTH_LONG)
                            .show();
                    finish();
                    return;
                }
                String uid = parser.getAttributeValue(null, "uid");
                if (uid.equals(AadhaarBatuaApplication.getInstance().getUser().getUid())) {
                    Toast.makeText(this, "Sender\'s and receiver\'s Aadhaar UID has to be different from one another", Toast.LENGTH_LONG)
                            .show();
                    return;
                }
                String name = parser.getAttributeValue(null, "name");

                User receiver = User.newUser()
                        .setUid(uid)
                        .setName(name)
                        .create();
                String futureBalance = Long.toString(mBatuaBalance - Long.parseLong(mAmountPay.getText().toString()));
                Intent paymentIntent = new Intent(this, PaymentActivity.class);
                paymentIntent.putExtra("receiver", receiver);
                paymentIntent.putExtra("receiverAmount", mAmountPay.getText().toString());
                paymentIntent.putExtra("futureBalance", futureBalance);
                startActivity(paymentIntent);
            } catch(XmlPullParserException xppe) {
                Toast.makeText(this, "Not a valid Aadhaar QR Code", Toast.LENGTH_LONG)
                        .show();
                finish();
            } catch (IOException ioe) {
                Toast.makeText(this, "Not a valid Aadhaar QR Code", Toast.LENGTH_LONG)
                        .show();
                finish();
            } finally {
                try {
                    stream.close();
                } catch (IOException ioe) {
                    Toast.makeText(this, "Not a valid Aadhaar QR Code", Toast.LENGTH_LONG)
                            .show();
                    finish();
                }
            }
        }
    }

}
