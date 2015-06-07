package org.aadhaar.batua;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

public class AccountActivity extends AppCompatActivity {

    // UI elements
    private TextView mName;
    private TextView mUid;
    private TextView mBalance;

    // Other members
    private Firebase mFirebase;
    private long mBatuaBalance = 5000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        Firebase.setAndroidContext(this);

        mName = (TextView)findViewById(R.id.name);
        mName.setText(getIntent().getStringExtra("name"));
        mUid = (TextView)findViewById(R.id.uid);
        mUid.setText("UID: " + getIntent().getStringExtra("uid"));
        mBalance = (TextView)findViewById(R.id.balance);

        mFirebase = new Firebase("https://aadhaar-batua.firebaseio.com");
        Query balanceQuery = mFirebase.child(mUid.getText().toString()).child("balance").orderByValue();
        balanceQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    mBatuaBalance = (long)dataSnapshot.getValue();
                    mBalance.setText("Your Batua Balance: \u20B9" +
                            Long.toString(mBatuaBalance));
                } else {
                    mFirebase.child(mUid.getText().toString()).child("balance").setValue(mBatuaBalance);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                mBalance.setText("Could not retrieve your Batua.");
            }
        });

        final MaterialEditText amountPay = (MaterialEditText)findViewById(R.id.amountPay);
        Button pay = (Button)findViewById(R.id.bpay);
        pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (amountPay.getText().length() == 0) {
                    amountPay.setError("Please enter an amount");
                } else {

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
                    mFirebase.child(mUid.getText().toString()).child("balance").setValue(Integer.parseInt(
                            amountRefill.getText().toString()) + mBatuaBalance);
                }
            }
        });
    }

}
