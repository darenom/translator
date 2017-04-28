package org.darenom.visualtralslator;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.darenom.visualtralslator.ui.camera.CameraSourcePreview;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_CODE_SPEECH_INPUT = 2;
    private final String TAG = getClass().getSimpleName();
    private Button mCam;
    private Button mTrans;
    private Button mHear;
    private Spinner sHear;
    private Button mSwap;
    private Button mSay;
    private Spinner sSay;
    private EditText mEdit;
    private TextView mText;
    private WebView mWebView;
    private CameraSourcePreview mPreview;
    private LinearLayout translateLayout;
    private RelativeLayout previewLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        translateLayout = (LinearLayout) findViewById(R.id.main_layout_translate);
        previewLayout = (RelativeLayout) findViewById(R.id.main_layout_preview);

        mCam = (Button) findViewById(R.id.button_camera);
        mHear = (Button) findViewById(R.id.button_hear);
        mSwap = (Button) findViewById(R.id.button_swap);
        mSay = (Button) findViewById(R.id.button_say);
        mTrans = (Button) findViewById(R.id.button_translate);

        mWebView = (WebView) findViewById(R.id.web);
        mEdit = (EditText) findViewById(R.id.edit);
        mText = (TextView) findViewById(R.id.text);
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);

        mEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0)
                    mText.setText("");

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                translateLayout.setVisibility(View.GONE);
                previewLayout.setVisibility(View.VISIBLE);

                // Cam stuff
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

        mHear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                translateLayout.setVisibility(View.VISIBLE);
                previewLayout.setVisibility(View.GONE);
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.main_speech_prompt));
                try {
                    startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.main_speech_not_supported), Toast.LENGTH_SHORT).show();
                }

            }
        });

        mSwap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        mSay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextToSpeech tts = new TextToSpeech(getApplicationContext(), null);
                tts.setLanguage(Locale.FRANCE);
                tts.speak("Text to say aloud", TextToSpeech.QUEUE_ADD, null);
            }
        });

        mTrans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreview.setVisibility(View.GONE);
                translateLayout.setVisibility(View.VISIBLE);
                String decode = mEdit.getText().toString();
                if (!decode.isEmpty())
                    translate(decode);
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
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            mEdit.setText(result.get(0));
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
                Toast.makeText(this, R.string.main_low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.main_low_storage_error));
            }
        }

        for (int i = 0; i < texts.size(); i++) {
            TextBlock text = texts.valueAt(i);
            decode = decode + text.getValue() + "\n";
        }

        return decode;
    }


    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private String translate(String text) {
        String tranlate = "";
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mWebView.evaluateJavascript(
                        "(function() { return (document.getElementsByClassName('t0')[0].innerHTML); })();",
                        new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String html) {
                                mText.setText(html.substring(1, html.length() - 1));
                            }
                        });
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Log.e(TAG, error.toString());
            }
        });
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
