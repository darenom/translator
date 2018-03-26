package org.darenom.visualtralslator;

import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.*
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
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

    private lateinit var vm: MainViewModel
    var tts: TextToSpeech? = null

    var currentLocale: Locale? = null
    var allCodes: Array<String>? = null
    var allFlags: Array<String>? = null

    val list = ArrayList<String>()
    var voiceList = ArrayList<String>()

    var lang1: String? = null
    var lang2: String? = null

    val ttsListener = TextToSpeech.OnInitListener {
        if (it == TextToSpeech.SUCCESS) {
            if (vm.ttsList.isEmpty())
                tts?.availableLanguages?.forEach {
                    if (!vm.ttsList.containsKey(it.country))
                        vm.ttsList[it.country] = it.language
                }
            onReady()
        }
    }

    private fun onReady() {

        // languages spinners
        spin_lang_1.adapter = SpinAdapter(this, R.layout.spinner_item, vm.ttsList.keys.toTypedArray())
        spin_lang_1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                lang1 = vm.ttsList.values.elementAt(pos)
                vm.sp1.value = pos
            }

            override fun onNothingSelected(parent: AdapterView<out Adapter>?) {}
        }

        spin_lang_2.adapter = SpinAdapter(this, R.layout.spinner_item, vm.ttsList.keys.toTypedArray())
        spin_lang_2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                lang2 = vm.ttsList.values.elementAt(pos)
                vm.sp2.value = pos
            }

            override fun onNothingSelected(parent: AdapterView<out Adapter>?) {}
        }

        spin_lang_1.setSelection(vm.sp1.value!!)
        spin_lang_2.setSelection(vm.sp2.value!!)
        edit.setText(vm.edt.value!!)
        text.text = vm.txt.value!!

    }

    override fun onPause() {
        super.onPause()
        vm.stamp()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        vm = ViewModelProviders.of(this).get(MainViewModel::class.java)
        allCodes = resources.getStringArray(R.array.country_code)
        allFlags = resources.getStringArray(R.array.country_drawable)
        subscribeUI()
        setUp()

    }

    private fun subscribeUI() {

        vm.sp1.observe(this, android.arch.lifecycle.Observer {
            if (null != it) {
                spin_lang_1.setSelection(it)
            }
        })

        vm.sp2.observe(this, android.arch.lifecycle.Observer {
            if (null != it) {
                spin_lang_2.setSelection(it)
            }
        })

        vm.edt.observe(this, android.arch.lifecycle.Observer {
            if (null != it) {
                edit.setText(it)
            }
        })

        vm.txt.observe(this, android.arch.lifecycle.Observer {
            if (null != it) {
                text.text = it
            }
        })
    }

    private fun setUp() {

        currentLocale = Locale.getDefault()

        // all available languages
        if (vm.ttsList.isEmpty())
            Locale.getAvailableLocales().forEach { if (!list.contains(it.country)) list.add(it.country) }

        // all languages that can be heard
        val detailsIntent = Intent()
        if (vm.ttsList.isEmpty())
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

    override fun onClick(v: View?) {
        if (null != v)
            when (v.id) {
                actionClear.id -> {
                    vm.edt.value = ""
                    vm.txt.value = ""
                }
                actionCamera.id -> {
                    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    if (takePictureIntent.resolveActivity(packageManager) != null) {
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                    }
                }
                actionHear.id -> {
                    val hearLocale = Locale(lang1)
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
                            lang2!!,
                            lang1!!)
                }
                actionSwap.id -> {
                    val f_ref = spin_lang_1.selectedItemPosition
                    val l_ref = spin_lang_2.selectedItemPosition
                    spin_lang_1.setSelection(0)
                    spin_lang_2.setSelection(f_ref)
                    spin_lang_1.setSelection(l_ref)

                }
                actionSay.id -> {
                    val saylocale = Locale(lang1)
                    tts?.language = saylocale
                    tts?.speak(text.text.toString(), TextToSpeech.QUEUE_FLUSH, null)
                }

            }
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

    inner class SpinAdapter(context: Context, resource: Int, refs: Array<String>) : ArrayAdapter<String>(context, resource) {

        private var index = ArrayList<Int>()
        private var mInflater: LayoutInflater? = null

        init {
            mInflater = LayoutInflater.from(context)

            index.clear()
            refs.forEach {
                val i = allCodes!!.indexOf(it)
                if (i >= 1) index.add(i) else index.add(0)
            }
            index.trimToSize()

            notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return index.size
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

            val view: View? = convertView
                    ?: mInflater?.inflate(R.layout.spinner_item, parent, false)

            val drw = context.resources.getDrawable(
                    context.resources.getIdentifier(
                            allFlags!![index[position]],
                            "drawable",
                            context.packageName))

            view?.findViewById<TextView>(R.id.txt)?.setCompoundDrawablesWithIntrinsicBounds(drw, null, null, null)

            view?.findViewById<TextView>(R.id.txt)?.text = allCodes!![index[position]]

            return view!!
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return getView(position, convertView, parent)
        }
    }

}
