package org.aadhaar.batua;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.firebase.client.Firebase;

public class AccountActivity extends AppCompatActivity {

    // UI elements
    private TextView mName;
    private TextView mUid;

    // Other members
    private Firebase mFirebase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Firebase.setAndroidContext(this);

        mName = (TextView)findViewById(R.id.name);
        mName.setText(getIntent().getStringExtra("name"));
        mUid = (TextView)findViewById(R.id.uid);
        mUid.setText(getIntent().getStringExtra("uid"));

        mFirebase = new Firebase("https://aadhaar-batua.firebaseio.com");


    }
}
