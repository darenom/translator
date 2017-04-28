package org.darenom.visualtralslator;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private Button mButton;
    private ToggleButton mToggle;
    private EditText mEdit;
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = (Button) findViewById(R.id.button);
        mToggle = (ToggleButton) findViewById(R.id.toggle);

        mWebView = (WebView) findViewById(R.id.web);
        mEdit = (EditText) findViewById(R.id.edit);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.setVisibility(View.GONE);
                mEdit.setVisibility(View.GONE);
                mToggle.setChecked(true);

                // Cam stuff
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

        mToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mToggle.isChecked()){
                    mWebView.setVisibility(View.VISIBLE);
                    mEdit.setVisibility(View.GONE);
                    String decode = mEdit.getText().toString();
                    if (!decode.isEmpty()){
                        translate(decode);
                    }
                } else {
                    mWebView.setVisibility(View.GONE);
                    mEdit.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            String decode = decode(imageBitmap);
            mEdit.setText(decode);
        }
    }

    private String decode(Bitmap imageBitmap) {
        String decode = "";
        TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();
        Frame frame = new Frame.Builder().setBitmap(imageBitmap).build();

        SparseArray<TextBlock> texts = textRecognizer.detect(frame);
        String[] langs = new String[texts.size()];
        if (!textRecognizer.isOperational()) {
            // Note: The first time that an app using a Vision API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any text,
            // barcodes, or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        for (int i = 0; i < texts.size(); i++){
            TextBlock text = texts.valueAt(i);
            decode = decode + text.getValue() + "\n";
        }

        return decode;
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private String translate(String text) {
        String tranlate = "";
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient());
        String url = null;
        try {
            url = "https://translate.google.com/m?hl=" + Locale.getDefault().getLanguage()
                    + "&sl=auto&tl=" + Locale.getDefault().getLanguage()    // todo: replace auto by TextBlock.getLanguage()
                    + "&ie=UTF-8&prev=_m&q=" + URLEncoder.encode(text, "utf-8");

            Log.e(TAG, url);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        mWebView.loadUrl(url);
        return tranlate;
    }
}
