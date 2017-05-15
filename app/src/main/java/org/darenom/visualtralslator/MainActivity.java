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
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
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

public class MainActivity extends Activity {

    static final String LANG1 = "lang_1"; //local
    static final String LANG2 = "lang_2"; //foreign
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_CODE_SPEECH_INPUT = 2;
    static final int REQUEST_CHECK_TTS_DATA = 3;
    private final String TAG = getClass().getSimpleName();
    public sAdapter sLangAdapter;
    List<String> hear_langs;
    List<String> say_langs;
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
    private Locale currentLocale;
    private SharedPreferences mPrefs;
    private TextToSpeech tts;
    private AdapterView.OnItemSelectedListener slistener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // local lang ? no way.
            if (parent.getId() == R.id.spin_lang_2 && id == 0) {
                for (String code : sLangAdapter.mCodList) {
                    if (code.contentEquals(currentLocale.getLanguage().toUpperCase())) {
                        sLang2.setSelection(sLangAdapter.mCodList.indexOf(code));
                        sLang1.setSelection(0);
                        break;
                    }
                }
                // cant' get same value on both sides
            } else if (sLang1.getSelectedItemId() == sLang2.getSelectedItemId()) {
                Toast.makeText(getApplicationContext(), R.string.error_selec_lang, Toast.LENGTH_SHORT).show();
                sLang1.setSelection(0);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };
    private TextToSpeech.OnInitListener ttsListener = new TextToSpeech.OnInitListener() {

        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                Log.i(TAG, "Getting TTS available languages");
                Set<Locale> all_lang = tts.getAvailableLanguages();
                say_langs = new ArrayList<>();
                for (Locale loc : all_lang) {
                    say_langs.add(loc.getLanguage() + "-" + loc.getCountry());
                }
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "[say country]");
                    for (String s : say_langs) {
                        Log.d(TAG, s);
                    }
                }
                finishInit();
            }
        }
    };

    @OnClick(R.id.reload)
    public void reload() {
        Log.i(TAG, "Getting available speech recognition languages");
        // get available languages for speech recognition
        Intent detailsIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        LanguageDetailsChecker checker = new LanguageDetailsChecker();
        sendOrderedBroadcast(detailsIntent, null, checker, null, Activity.RESULT_OK, null, null);
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
        Log.i(TAG, "Translation requested");
        previewLayout.setVisibility(View.GONE);
        translateLayout.setVisibility(View.VISIBLE);
        String toTranslate = mEdit.getText().toString();
        if (!toTranslate.isEmpty()) {
            translate(toTranslate,
                    sLangAdapter.mHearList.get(sLang1.getSelectedItemPosition()),
                    sLangAdapter.mHearList.get(sLang2.getSelectedItemPosition()));
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mPrefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);

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

        reload();
    }

    @Override
    protected void onPause() {
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putInt(LANG1, sLang2.getSelectedItemPosition());
        ed.putInt(LANG2, sLang1.getSelectedItemPosition());
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
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK) {
            Log.i(TAG, "Speech received");
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            mEdit.setText(result.get(0));
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

    // set adapter lists
    private void finishInit() {

        Log.i(TAG, "Building Spinner");
        List<String> flags = Arrays.asList(getResources().getStringArray(R.array.country_drawable));
        List<String> codes = Arrays.asList(getResources().getStringArray(R.array.country_code));

        // ---------     [hear, country, say, drawable]    --------------//
        // hear detected languages
        int resultsLenght = 0;
        String[][] results = new String[4][150];
        for (String s : hear_langs) {
            String[] split = s.split("-");
            if (!split[0].contentEquals("cmn") && !split[0].contentEquals("yue") && !split[1].contentEquals("001")) {
                if (split.length < 2) {
                    results[0][resultsLenght] = split[0]; // hear
                    results[1][resultsLenght] = "0"; // country
                } else {
                    results[0][resultsLenght] = split[0]; // hear
                    results[1][resultsLenght] = split[1]; // country
                }
                resultsLenght++;
            }
        }
        hear_langs = null;

        // say detected languages
        // todo: toAdd unused
        String[] fc = results[1]; // ref country
        List<String> toAdd = new ArrayList<>();
        List<String> allLang = new ArrayList<>();
        for (int i = 0; i < resultsLenght; i++) {
            boolean found = false;
            for (String s : say_langs) {
                String[] split = s.split("-");
                if (!allLang.contains(split[0]))
                    allLang.add(split[0]);
                if (split.length < 2) {
                    if (!toAdd.contains(split[0]))
                        toAdd.add(split[0]);
                } else {
                    //country present
                    if (split[1].contentEquals(fc[i])) {
                        found = true;
                        if (results[0][i].contentEquals(split[0])) {
                            results[2][i] = split[0];
                        } else {
                            results[2][i] = results[0][i];
                        }
                        break;
                    }
                }
            }
            if (!found) {
                if (allLang.contains(results[0][i]))
                    results[2][i] = results[0][i];
                else
                    results[2][i] = "0"; // unsupported say language
            }
        }
        say_langs = null;

        // put matching drawable reference
        for (int i = 0; i < resultsLenght; i++) {
            boolean found = false;
            for (String c : codes) {
                if (c.contentEquals(fc[i])) {
                    results[3][i] = flags.get(codes.indexOf(fc[i]));
                    found = true;
                    break;
                }
            }
            if (!found)
                results[3][i] = flags.get(0);
        }


        results[0][resultsLenght] = "cmn";
        results[1][resultsLenght] = "CN";
        results[2][resultsLenght] = "zh";
        results[3][resultsLenght] = "flag_cn";
        resultsLenght++;

        results[0][resultsLenght] = "cmn";
        results[1][resultsLenght] = "CN";
        results[2][resultsLenght] = "yue";
        results[3][resultsLenght] = "flag_cn";
        resultsLenght++;

        sLangAdapter = new sAdapter(this, R.layout.spinner_item,
                Arrays.asList(results[1]).subList(0, resultsLenght),
                Arrays.asList(results[3]).subList(0, resultsLenght),
                Arrays.asList(results[0]).subList(0, resultsLenght),
                Arrays.asList(results[2]).subList(0, resultsLenght));

        sLangAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sLang1.setAdapter(sLangAdapter);
        sLang1.setOnItemSelectedListener(slistener);

        sLang2.setAdapter(sLangAdapter);
        sLang2.setOnItemSelectedListener(slistener);

        sLang2.setSelection(mPrefs.getInt(LANG1, 0));
        sLang1.setSelection(mPrefs.getInt(LANG2, 0));

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[hear, country, say, drawable] - " + resultsLenght);
            for (int i = 0; i < resultsLenght; i++)
                Log.d(TAG, String.format(currentLocale, "%s - %s - %s - %s", results[0][i], results[1][i], results[2][i], results[3][i]));
            Log.d(TAG, "[toAdd] - " + toAdd.size());
            for (String s : toAdd)
                Log.d(TAG, s);
        }
        Log.i(TAG, "All Done.");
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

        private List<String> mHearList;
        private List<String> mSayList;
        private List<String> mCodList;
        private List<String> mDrawList;

        sAdapter(Context context, int viewResourceId, List<String> codList, List<String> drawList, List<String> hearList, List<String> sayList) {

            super(context, viewResourceId);
            this.mCodList = codList;
            this.mDrawList = drawList;
            this.mHearList = hearList;
            this.mSayList = sayList;
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

    public class LanguageDetailsChecker extends BroadcastReceiver {

        private static final String TAG = "LanguageDetailsChecker";

        private String languagePreference;

        public LanguageDetailsChecker() {

        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle results = getResultExtras(true);
            if (results.containsKey(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE)) {
                languagePreference = results
                        .getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE);
                Log.d(TAG, "Prefs : " + languagePreference);
            }
            if (results.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)) {
                hear_langs = new ArrayList<>();
                hear_langs = results.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
            }
            if (null != hear_langs)
                Collections.sort(hear_langs);
            Log.i(TAG, "Checking TTS availability");
            // get available languages for tts(
            Intent checkIntent = new Intent();
            checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            startActivityForResult(checkIntent, REQUEST_CHECK_TTS_DATA);
        }
    }
}
