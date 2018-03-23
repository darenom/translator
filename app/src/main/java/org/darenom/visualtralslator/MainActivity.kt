package org.darenom.visualtralslator;

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextRecognizer
import kotlinx.android.synthetic.main.activity_main.*
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), View.OnClickListener {

    var currentLocale: Locale? = null
    var tts: TextToSpeech? = null

    var allCodes: Array<String>? = null
    var allFlags: Array<String>? = null

    val list = ArrayList<String>()
    var ttsList = ArrayList<String>()
    var voiceList = ArrayList<String>()

    val ttsListener = TextToSpeech.OnInitListener {
        if (it == TextToSpeech.SUCCESS) {
            tts?.availableLanguages?.forEach {
                if (!ttsList.contains(it.country)) ttsList.add(it.country) }
            (spin_lang_1.adapter as SpinAdapter).resetTo(ttsList)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUp()
    }

    fun setUp() {
        allCodes = resources.getStringArray(R.array.country_code)
        allFlags = resources.getStringArray(R.array.country_drawable)

        spin_lang_1.adapter = SpinAdapter(this, R.layout.spinner_item)
        spin_lang_1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {

            }

            override fun onNothingSelected(parent: AdapterView<out Adapter>?) {}
        }

        // all available languages
        Locale.getAvailableLocales().forEach { if (!list.contains(it.country)) list.add(it.country) }

        // all languages that can be heard
        val detailsIntent = Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS)
        sendOrderedBroadcast(
                detailsIntent,
                null,
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        val results = getResultExtras(true)
                        if (results.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES))
                            voiceList = results.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)

                    }
                },
                null,
                Activity.RESULT_OK,
                null,
                null)

        // all languages that can be said
        startActivityForResult(
                Intent().setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA),
                REQUEST_CHECK_TTS_DATA)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onClick(v: View?) {
        if (null != v)
            when (v.id) {
                actionClear.id -> {
                    edit.setText("")
                    text.text = ""
                }
            /*
            actionCamera.id -> {
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent.resolveActivity(packageManager) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
            actionHear.id -> {
                val hearLocale = Locale(sLangAdapter.mHearList.get(sLang1.getSelectedItemPosition()))
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, hearLocale)
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.main_speech_prompt))
                try {
                    startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
                } catch (a: ActivityNotFoundException) {
                    Toast.makeText(applicationContext,
                            getString(R.string.main_speech_not_supported), Toast.LENGTH_SHORT).show()
                }

            }
            actionTranslate.id -> {
                translate(edit.text.toString(),
                        sLangAdapter.mHearList.get(sLang2.getSelectedItemPosition()),
                        sLangAdapter.mHearList.get(sLang1.getSelectedItemPosition()))
            }
            actionSwap.id -> {
                val f_ref = sLang1.getSelectedItemPosition()
                val l_ref = sLang2.getSelectedItemPosition()
                sLang1.setSelection(0)
                sLang2.setSelection(f_ref)
                sLang1.setSelection(l_ref)


                // within same type
                val tmp = ime2
                ime2 = ime1
                ime1 = tmp

            }
            actionSay.id -> {
                val saylocale = Locale(sLangAdapter.mHearList.get(sLang2.getSelectedItemPosition()))
                tts?.setLanguage(saylocale)
                tts?.speak(mText.getText().toString(), TextToSpeech.QUEUE_FLUSH, null)
            }
            */
            }
    }


    override fun onStop() {
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val extras = data.extras
            val imageBitmap = extras!!.get("data") as Bitmap
            val decode = decode(imageBitmap)
            edit.setText(decode)
            previewLayout.visibility = View.GONE
        }
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == Activity.RESULT_OK) {
                actionHear.visibility = View.VISIBLE
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                edit.setText(result[0])
            }
        }
        if (requestCode == REQUEST_CHECK_TTS_DATA) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                tts = TextToSpeech(this, ttsListener)
            } else {
                // No engine found, go to store
                val installIntent = Intent()
                installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                startActivity(installIntent)
            }
        }
    }

    /**
     * Detect texts in image
     * should download dependencies automatically
     * may need a restart?
     */
    private fun decode(imageBitmap: Bitmap): String {
        var decode = ""
        val textRecognizer = TextRecognizer.Builder(this).build()
        val frame = Frame.Builder().setBitmap(imageBitmap).build()
        val texts = textRecognizer.detect(frame)
        if (!textRecognizer.isOperational) {
            val lowstorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = registerReceiver(null, lowstorageFilter) != null

            if (hasLowStorage) {
                Toast.makeText(this, R.string.main_low_storage_error, Toast.LENGTH_LONG).show()
            }
        }
        for (i in 0 until texts.size()) {
            val text = texts.valueAt(i)
            decode = decode + text.value + "\n"
        }
        return decode
    }

    /**
     * Sends textToTranslate to translate to google trad, sets answer in view
     *
     * @param textToTranslate to translate
     */
    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun translate(textToTranslate: String, from: String, to: String) {

        web.settings.javaScriptEnabled = true
        web.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                web.evaluateJavascript(
                        "(function() { return (document.getElementsByClassName('t0')[0].innerHTML); })();",
                        { html ->
                            actionTranslate.visibility = View.VISIBLE
                            if (!html.contentEquals("ul"))
                                text.text = html.substring(1, html.length - 1)
                            else
                                Toast.makeText(applicationContext, getString(R.string.no_net), Toast.LENGTH_SHORT).show()
                        })
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                super.onReceivedError(view, request, error)
                actionTranslate.visibility = View.VISIBLE
            }
        }
        var url: String? = null
        try {
            url = ("https://translate.google.com/m?hl=" + currentLocale!!.language
                    + "&sl=" + from + "&tl=" + to
                    + "&ie=UTF-8&prev=_m&q=" + URLEncoder.encode(textToTranslate, "utf-8"))

        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        web.loadUrl(url)
    }


    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_CODE_SPEECH_INPUT = 2
        const val REQUEST_CHECK_TTS_DATA = 3
    }

    inner class SpinAdapter(context: Context, resource: Int) : ArrayAdapter<String>(context, resource) {

        var index = ArrayList<Int>()

        inner class ViewHolder constructor(context: Context) {
            var img = ImageView(context)
            var txt = TextView(context)
        }

        private var mViewHolder: ViewHolder? = null
        private var mInflater: LayoutInflater? = null


        init {
            mInflater = LayoutInflater.from(context)
            mViewHolder = ViewHolder(context)
        }

        fun resetTo(refs: ArrayList<String>) {
            index.clear()
            refs.forEach {
                val i = allCodes!!.indexOf(it)
                if (i >= 1) index.add(i) else index.add(0)
            }
            notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return index.size
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            var view = convertView
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false)
                mViewHolder!!.img = view.findViewById(R.id.img)
                mViewHolder!!.txt = view.findViewById(R.id.txt)
                view.tag = mViewHolder
            } else {
                mViewHolder = view.tag as ViewHolder
            }
            mViewHolder!!.img.background = context.resources.getDrawable(
                    context.resources.getIdentifier(allFlags!![index[position]],
                            "drawable", context.packageName))
            mViewHolder!!.txt.text = resources.getStringArray(R.array.country_code)[index[position]].toString()
            return view!!
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return getView(position, convertView, parent)
        }
    }

}
