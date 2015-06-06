package org.aadhaar.batua;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private Handler mHandler;
    private TextView mTextView;
    private boolean mQrCodeScanned;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler(getMainLooper());
        mTextView = (TextView)findViewById(R.id.text_view);

        IntentIntegrator intentIntegrator = new IntentIntegrator(this);
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        intentIntegrator.setOrientationLocked(false);
        intentIntegrator.initiateScan();
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
                String uid = parser.getAttributeValue(null, "uid");
                final String name = parser.getAttributeValue(null, "name");
                String gender = parser.getAttributeValue(null, "gender");
                String yob = parser.getAttributeValue(null, "yob");
                String co = parser.getAttributeValue(null, "co");
                String house = parser.getAttributeValue(null, "house");
                String lm = parser.getAttributeValue(null, "lm");
                String loc = parser.getAttributeValue(null, "loc");
                String vtc = parser.getAttributeValue(null, "vtc");
                String dist = parser.getAttributeValue(null, "dist");
                String subdist = parser.getAttributeValue(null, "subdist");
                String state = parser.getAttributeValue(null, "state");
                String pc = parser.getAttributeValue(null, "pc");
                mTextView.setText(name);
            } catch(XmlPullParserException xppe) {
                throw new IllegalStateException(xppe);
            } catch (IOException ioe) {
                throw new IllegalStateException(ioe);
            } finally {
                try {
                    stream.close();
                } catch (IOException ioe) {
                    throw new IllegalStateException(ioe);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }
}
