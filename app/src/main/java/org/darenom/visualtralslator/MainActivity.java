package org.darenom.visualtralslator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemSelected;

public class MainActivity extends Activity {

    static final String LANG1 = "lang_1";
    static final String LANG2 = "lang_2";
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_CODE_SPEECH_INPUT = 2;
    static final int REQUEST_CHECK_TTS_DATA = 3;
    private final String TAG = getClass().getSimpleName();

    @BindView(R.id.spin_lang_1)
    Spinner sLang1;
    @BindView(R.id.spin_lang_2)
    Spinner sLang2;
    @BindView(R.id.edit)
    EditText mEdit;
    @BindView(R.id.text)
    TextView mText;
    @BindView(R.id.web)
    WebView mWebView;
    @BindView(R.id.main_layout_translate)
    RelativeLayout translateLayout;
    @BindView(R.id.main_layout_preview)
    RelativeLayout previewLayout;
    @BindView(R.id.reload)
    Button mReload;
    @BindView(R.id.button_camera)
    Button mCam;
    @BindView(R.id.button_hear)
    Button mHear;
    @BindView(R.id.button_swap)
    Button mSwap;
    @BindView(R.id.button_say)
    Button mSay;
    @BindView(R.id.clear)
    Button mClear;
    @BindView(R.id.button_translate)
    Button mTrans;


    Locale currentLocale;
    InputMethodSubtype originalIMEsubType;
    String ime1;
    String ime2;
    SharedPreferences mPrefs;
    InputMethodManager imeManager;
    sAdapter sLangAdapter;
    TextToSpeech tts;
    TextToSpeech.OnInitListener ttsListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                Log.i(TAG, "Getting TTS available languages");
                Set<Locale> all_lang = tts.getAvailableLanguages();
                for (String hearCode : sLangAdapter.mCodList) {
                    boolean found = false;
                    String locLang = "";
                    for (Locale loc : all_lang) {
                        locLang = loc.getLanguage();
                        if (hearCode.contentEquals(loc.getCountry())) {
                            found = true;
                            sLangAdapter.mSayList.add(locLang);
                            break;
                        }
                    }
                    if (!found) {
                        if (locLang.contentEquals(sLangAdapter.mHearList.get(sLangAdapter.mCodList.indexOf(hearCode))))
                            sLangAdapter.mSayList.add(locLang);
                        else
                            sLangAdapter.mSayList.add("0");
                    }
                }
                setFlags();
            }
        }
    };

    @OnClick(R.id.reload)
    public void reload() {
        Log.i(TAG, "Getting available speech recognition languages");
        sLangAdapter = new sAdapter(this, R.layout.spinner_item);
        // get available languages for speech recognition
        Intent detailsIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        sendOrderedBroadcast(detailsIntent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle results = getResultExtras(true);
                if (results.containsKey(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE)) {
                    Log.d(TAG, "Prefs : " + results.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE));
                }
                if (results.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)) {
                    ArrayList<String> tmp = results.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
                    if (null != tmp) {
                        Collections.sort(tmp);
                        for (String s : tmp) {
                            String[] split = s.split("-");
                            if (split.length < 2) {
                                sLangAdapter.mHearList.add(split[0]); // hear
                                sLangAdapter.mCodList.add("0"); // country
                            } else {
                                sLangAdapter.mHearList.add(split[0]); // hear
                                sLangAdapter.mCodList.add(split[1]); // country
                            }
                        }
                    }
                }
                Log.i(TAG, "Checking TTS availability");
                // get available languages for tts(
                Intent checkIntent = new Intent();
                checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                startActivityForResult(checkIntent, REQUEST_CHECK_TTS_DATA);
            }
        }, null, Activity.RESULT_OK, null, null);
    }

    @OnClick(R.id.button_camera)
    public void button_camera() {
        Log.i(TAG, "Camera requested");
        mEdit.setText("");
        translateLayout.setVisibility(View.GONE);
        previewLayout.setVisibility(View.VISIBLE);

        // Cam stuff
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @OnClick(R.id.button_hear)
    public void button_hear() {
        mHear.setVisibility(View.GONE);
        Log.i(TAG, "Speech recognition requested");
        mEdit.setText("");
        translateLayout.setVisibility(View.VISIBLE);
        previewLayout.setVisibility(View.GONE);

        Locale hearLocale = new Locale(sLangAdapter.mHearList.get(sLang1.getSelectedItemPosition()));

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, hearLocale);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.main_speech_prompt));
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.main_speech_not_supported), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.button_swap)
    public void button_swap() {
        Log.i(TAG, "Swaping languages");
        mEdit.setText("");
        int f_ref = sLang1.getSelectedItemPosition();
        int l_ref = sLang2.getSelectedItemPosition();
        sLang1.setSelection(0);
        sLang2.setSelection(f_ref);
        sLang1.setSelection(l_ref);


        // within same type
        String tmp = ime2;
        ime2 = ime1;
        ime1 = tmp;

        //imeManager.setCurrentInputMethodSubtype(ime2);

    }

    @OnClick(R.id.button_say)
    public void button_say() {
        Log.i(TAG, "TTS requested");
        String toSay = mText.getText().toString();
        if (!toSay.isEmpty()) {
            Log.e(TAG, "Que faire...");
            Locale saylocale = new Locale(sLangAdapter.mHearList.get(sLang2.getSelectedItemPosition()));
            tts.setLanguage(saylocale);
            tts.speak(mText.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    @OnClick(R.id.clear)
    public void clear() {
        mEdit.setText("");
        mText.setText("");
    }

    @OnClick(R.id.button_translate)
    public void button_translate() {
        mTrans.setVisibility(View.GONE);
        Log.i(TAG, "Translation requested");
        mTrans.setVisibility(View.GONE);
        previewLayout.setVisibility(View.GONE);
        translateLayout.setVisibility(View.VISIBLE);
        String toTranslate = mEdit.getText().toString();
        if (!toTranslate.isEmpty()) {
            translate(toTranslate,
                    sLangAdapter.mHearList.get(sLang2.getSelectedItemPosition()),
                    sLangAdapter.mHearList.get(sLang1.getSelectedItemPosition()));
        }
    }

    @OnItemSelected(R.id.spin_lang_1)
    void onLang1Selected(int position) {
        //ime1 = "ime_" + sLangAdapter.mHearList.get(position) + "_" + sLangAdapter.mCodList.get(position);
        Log.e(TAG, "Lang1");
    }

    @OnItemSelected(R.id.spin_lang_2)
    void onLang2Selected(int position) {
        imeOps(sLangAdapter.mHearList.get(position), sLangAdapter.mCodList.get(position));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);



        // disable cam stuff for now
        mCam.setVisibility(View.GONE);

        mPrefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        imeManager = (InputMethodManager) getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
        originalIMEsubType = imeManager.getCurrentInputMethodSubtype();
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
        Log.i(TAG, "Activity created");
        currentLocale = Locale.getDefault();
        // get current ime
        // onChange compare languages
        reload();
    }

    @Override
    protected void onPause() {
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putInt(LANG1, sLang1.getSelectedItemPosition());
        ed.putInt(LANG2, sLang2.getSelectedItemPosition());
        ed.apply();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        previewLayout.setVisibility(View.GONE);
        translateLayout.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Log.i(TAG, "Image received from camera");
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            String decode = decode(imageBitmap);
            mEdit.setText(decode);
            previewLayout.setVisibility(View.GONE);
            translateLayout.setVisibility(View.VISIBLE);
        }
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK) {
                Log.i(TAG, "Speech received");
                mHear.setVisibility(View.VISIBLE);
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                mEdit.setText(result.get(0));
            }
        }
        if (requestCode == REQUEST_CHECK_TTS_DATA) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                tts = new TextToSpeech(this, ttsListener);
                Log.i(TAG, "Starting TTS");
            } else {
                Log.i(TAG, "No TTS Engine found moving to store");
                // No engine found, go to store
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }

    void setFlags() {
        List<String> flags = Arrays.asList(getResources().getStringArray(R.array.country_drawable));
        List<String> codes = Arrays.asList(getResources().getStringArray(R.array.country_code));
        // put matching drawable reference
        for (String flagCode : sLangAdapter.mCodList) {
            if (!flagCode.contentEquals("0")) {
                boolean found = false;
                for (String c : codes) {
                    if (c.contentEquals(flagCode)) {
                        found = true;
                        sLangAdapter.mDrawList.add(flags.get(codes.indexOf(c)));
                        break;
                    }
                }
                if (!found)
                    sLangAdapter.mDrawList.add(flags.get(0));
            } else {
                sLangAdapter.mDrawList.add(flags.get(0));
            }
        }
        sLangAdapter.notifyDataSetChanged();

        sLangAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sLang1.setAdapter(sLangAdapter);
        sLang2.setAdapter(sLangAdapter);

        sLang1.setSelection(mPrefs.getInt(LANG1, 0));
        sLang2.setSelection(mPrefs.getInt(LANG2, 0));

        Log.i(TAG, "All Done.");
    }

    void imeOps(String lang, String country) {

        boolean isPresent = false;
        boolean isEnabled = false;
        boolean isSet = false;

        String currentInput = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);

        InputMethodSubtype currentSubType = imeManager.getCurrentInputMethodSubtype();
        Log.e(TAG, String.format("%s => mode : %s // extra : %s", currentInput, currentSubType.getMode(), currentSubType.getExtraValue()));

        ArrayList<String> inputMethods = new ArrayList<>();
        List<InputMethodInfo> bip = imeManager.getInputMethodList();
        for (InputMethodInfo inputMethodInfo : bip) {
            Log.e(TAG, String.format("input type => %s - %s - %s", inputMethodInfo.getId(), inputMethodInfo.loadLabel(getPackageManager()).toString(), inputMethodInfo.getPackageName()));
            inputMethods.add(inputMethodInfo.getId());
            for (int i = 0; i < inputMethodInfo.getSubtypeCount(); i++){
                // list keysets
                Log.e(TAG, inputMethodInfo.getSubtypeAt(i).getExtraValue());
                //if (inputMethodInfo.getSubtypeAt(i).equals(current)) {
                //    isPresent = true;
                //    break;
                //}

            }
        }

        if (lang.contentEquals("ja")){

        }

        if (lang.contentEquals("cmn")){
           // cmn_Hans
           // cmn_Hant
        }

        // know active input method
        Log.e(TAG, String.format("%s_%s : %s ", lang, country, Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD)));




        //
      //  InputMethodSubtype current = imeManager.getCurrentInputMethodSubtype();
      //  Log.e(TAG, String.format("current input subtype => mode : %s // extra : %s", current.getMode(), current.getExtraValue()));


        //    if (!isPresent){
    //        imeManager.showInputMethodPicker();
    //    } else {
    //        final Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS);
    //        intent.putExtra(Settings.EXTRA_INPUT_METHOD_ID, inputMethods.get(0));
    //        intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.ime_select));
    //        startActivity(intent);
    //    }

     //   List<InputMethodInfo> InputMethods = imeManager.getEnabledInputMethodList();
     //   for (InputMethodInfo inputMethodInfo : InputMethods) {
     //       Log.e(TAG, String.format("enabled input list => %s - %s", inputMethodInfo.loadLabel(getPackageManager()).toString(), inputMethodInfo.getPackageName()));
     //   }

    }

    void getFromStore(String appName) {
        if (!appName.isEmpty()) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appName)));
            }
        }
    }

    /**
     * Detect texts in image
     * should download dependencies automatically
     * may need a restart?
     */
    private String decode(Bitmap imageBitmap) {
        String decode = "";
        TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();
        Frame frame = new Frame.Builder().setBitmap(imageBitmap).build();
        SparseArray<TextBlock> texts = textRecognizer.detect(frame);
        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Detector dependencies are not yet available.");
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.main_low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.main_low_storage_error));
            }
        }
        for (int i = 0; i < texts.size(); i++) {
            TextBlock text = texts.valueAt(i);
            Log.i(TAG, "detected lang from picture" + text.getLanguage());
            decode = decode + text.getValue() + "\n";
        }
        return decode;
    }

    /**
     * Sends text to translate to google trad, sets answer in view
     *
     * @param text to translate
     */
    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void translate(String text, String from, String to) {

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
                                mTrans.setVisibility(View.VISIBLE);
                                if (!html.contentEquals("ul"))
                                    mText.setText(html.substring(1, html.length() - 1));
                                else
                                    Toast.makeText(getApplicationContext(), getString(R.string.no_net), Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                mTrans.setVisibility(View.VISIBLE);
                Log.e(TAG, error.toString());
            }
        });
        String url = null;
        try {
            url = "https://translate.google.com/m?hl=" + currentLocale.getLanguage()
                    + "&sl=" + from + "&tl=" + to
                    + "&ie=UTF-8&prev=_m&q=" + URLEncoder.encode(text, "utf-8");

            Log.e(TAG, url);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        mWebView.loadUrl(url);
    }

    class sAdapter extends ArrayAdapter {

        List<String> mHearList;
        List<String> mSayList;
        List<String> mCodList;
        List<String> mDrawList;

        sAdapter(Context context, int viewResourceId) {
            super(context, viewResourceId);
            this.mCodList = new ArrayList<>();
            this.mDrawList = new ArrayList<>();
            this.mHearList = new ArrayList<>();
            this.mSayList = new ArrayList<>();
        }

        @Override
        public int getCount() {
            return mCodList.size();
        }


        @NonNull
        @Override
        public View getView(int position, View view, @NonNull ViewGroup parent) {
            ViewHolder holder;
            if (view != null) {
                holder = (ViewHolder) view.getTag();
            } else {
                view = getLayoutInflater().inflate(R.layout.spinner_item, parent, false);
                holder = new ViewHolder(view);
                view.setTag(holder);
            }
            holder.txt.setText(mSayList.get(position).equals("0") ? mHearList.get(position) : mSayList.get(position));
            holder.img.setImageResource(getResources().getIdentifier(mDrawList.get(position), "drawable", getPackageName()));
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            convertView = getLayoutInflater().inflate(R.layout.spinner_item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
            holder.txt.setText(mSayList.get(position).equals("0") ? mHearList.get(position) : mSayList.get(position));
            holder.img.setImageResource(getResources().getIdentifier(mDrawList.get(position), "drawable", getPackageName()));
            return convertView;
        }

        class ViewHolder {
            @BindView(R.id.txt)
            TextView txt;
            @BindView(R.id.img)
            ImageView img;

            ViewHolder(View view) {
                ButterKnife.bind(this, view);
            }
        }

    }
}
