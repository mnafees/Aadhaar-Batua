package org.aadhaar.batua;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Xml;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private IntentIntegrator mIntentIntegrator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIntentIntegrator = new IntentIntegrator(this);
        mIntentIntegrator.setCaptureActivity(CaptureActivityAnyOrientation.class);
        mIntentIntegrator.setPrompt("Scan your Aadhaar QR code");
        mIntentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        mIntentIntegrator.setOrientationLocked(false);
        mIntentIntegrator.initiateScan();
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
                    mIntentIntegrator.initiateScan();
                    return;
                }
                String uid = parser.getAttributeValue(null, "uid");
                String name = parser.getAttributeValue(null, "name");

                Intent accountIntent = new Intent(this, AccountActivity.class);
                accountIntent.putExtra("uid", uid);
                accountIntent.putExtra("name", name);
                startActivity(accountIntent);
                finish();
            } catch(XmlPullParserException xppe) {
                mIntentIntegrator.initiateScan();
            } catch (IOException ioe) {
                mIntentIntegrator.initiateScan();
            } finally {
                try {
                    stream.close();
                } catch (IOException ioe) {
                    mIntentIntegrator.initiateScan();
                }
            }
        }
    }

}
