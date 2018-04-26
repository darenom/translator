package org.darenom.translate

import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.android.synthetic.main.activity_translate.*
import org.darenom.translate.databinding.ActivityTranslateBinding
import org.darenom.translate.ocr.OcrCaptureActivity
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*

import kotlin.collections.ArrayList

class TranslateActivity : AppCompatActivity(), View.OnClickListener {

    data class Refs(var say: String? = null, var hear: String? = null)

    private lateinit var vm: TranslateViewModel
    private lateinit var binding: ActivityTranslateBinding

    private var tts: TextToSpeech? = null
    private var currentLocale: Locale? = null
    private val ttsListener = TextToSpeech.OnInitListener {
        if (it == TextToSpeech.SUCCESS) {
            if (!vm.isConsolidated.value!!) {
                tts?.availableLanguages?.forEach {
                    if (!vm.inputList!!.containsKey(it.country))
                        if (null == vm.inputList!![it.country])
                            vm.inputList!![it.country] = Refs(it.language, null)
                        else
                            vm.inputList!![it.country] = Refs(it.language, vm.inputList!![it.country]!!.hear)
                    else
                        vm.inputList!![it.country] = Refs(it.language, vm.inputList!![it.country]!!.hear)
                }
                vm.consolidateList()
            }
            tts?.setOnUtteranceProgressListener(ttsUtteranceProgressListener)
        }
    }
    private val itemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            // do nothing
        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            when (parent?.id) {
                spin_lang_1.id -> vm.sp1.value = position
                spin_lang_2.id -> vm.sp2.value = position
            }
        }
    }
    private val ttsUtteranceProgressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            binding.loading = false
        }

        override fun onDone(utteranceId: String?) {
            binding.loading = false
        }

        @Suppress("OverridingDeprecatedMember")
        override fun onError(utteranceId: String?) {
            binding.loading = false
        }
    }
    private val isNetworkAvailable: Boolean
        get() {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_translate)
        vm = ViewModelProviders.of(this).get(TranslateViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()
        if (!checkPlayServices()){
            Toast.makeText(this, getString(R.string.no_gg), Toast.LENGTH_LONG).show()
            finish()
        } else {

            if (intent.hasExtra("text"))
                vm.edt.value = intent.getStringExtra("text")

            vm.isConsolidated.observe(this, Observer {
                if (null != it)
                    if (it)
                        onReady()
            })

            setUp()
        }
    }

    private fun checkPlayServices(): Boolean {
        val s = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        return when (s) {
            ConnectionResult.SUCCESS -> true
            else -> false
        }
    }



    private fun setUp() {

        currentLocale = Locale.getDefault()

        if (!vm.isConsolidated.value!!) {
            vm.allCodes = resources.getStringArray(R.array.country_code)
            vm.allFlags = resources.getStringArray(R.array.country_drawable)

            // all languages that can be heard
            val detailsIntent = Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS)
            sendOrderedBroadcast(
                    detailsIntent,
                    null,
                    object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {

                            // get
                            var voiceList = ArrayList<String>()
                            val results = getResultExtras(true)
                            if (results.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES))
                                voiceList = results.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)

                            // map
                            voiceList.forEach {
                                val t = it.split("-")
                                val key = t[t.size - 1]
                                if (!vm.inputList!!.containsKey(key))
                                    if (null == vm.inputList!![key])
                                        vm.inputList!![key] = Refs(null, it)
                            }

                            // then
                            // all languages that can be said
                            if (null == tts)
                                startActivityForResult(
                                    Intent().setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA),
                                    REQUEST_CHECK_TTS_DATA)

                        }
                    },
                    null,
                    Activity.RESULT_OK,
                    null,
                    null)
        } else {
            if (null == tts)
                startActivityForResult(
                    Intent().setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA),
                    REQUEST_CHECK_TTS_DATA)
        }
    }

    private fun onReady() {

        spin_lang_1.adapter = SpinAdapter(this, R.layout.spinner_item, vm.sortedList.keys.toTypedArray(), vm.allCodes!!, vm.allFlags!!)
        spin_lang_2.adapter = SpinAdapter(this, R.layout.spinner_item, vm.sortedList.keys.toTypedArray(), vm.allCodes!!, vm.allFlags!!)

        spin_lang_1.onItemSelectedListener = itemSelectedListener
        spin_lang_2.onItemSelectedListener = itemSelectedListener

        edit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                vm.edt.value = s.toString()
                if (vm.edt.value!!.isEmpty())
                    vm.txt.value = ""
            }
        })
        text.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                vm.txt.value = s.toString()
            }

        })

        vm.sp1.observe(this, android.arch.lifecycle.Observer {
            if (null != it) {
                binding.sp1 = it
            }
        })
        vm.sp2.observe(this, android.arch.lifecycle.Observer {
            if (null != it) {
                binding.sp2 = it
            }
        })
        vm.edt.observe(this, android.arch.lifecycle.Observer {
            if (null != it) {
                binding.two = it
            }
        })
        vm.txt.observe(this, android.arch.lifecycle.Observer {
            if (null != it) {
                binding.one = it
            }
        })

        spin_lang_1.setSelection(vm.sp1.value!!)
        spin_lang_2.setSelection(vm.sp2.value!!)
    }

    override fun onClick(v: View?) {
        if (null != v)
            when (v.id) {
                actionClear.id -> {
                    vm.edt.value = ""
                    vm.txt.value = ""
                }
                actionKeyboard.id -> {
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .showInputMethodPicker()
                }
                actionCamera.id -> {

                    val i = Intent(this, OcrCaptureActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(OcrCaptureActivity.AutoFocus, true)
                            .putExtra(OcrCaptureActivity.UseFlash, false)
                    startActivity(i)
                }
                actionHear.id -> {
                    binding.loading = true
                    val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    i.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.main_speech_prompt))
                    i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, vm.sortedList.values.elementAt(vm.sp2.value!!).hear)
                    try {
                        startActivityForResult(i, REQUEST_CODE_SPEECH_INPUT)
                    } catch (a: ActivityNotFoundException) {
                        Toast.makeText(applicationContext,
                                getString(R.string.main_speech_not_supported), Toast.LENGTH_SHORT).show()
                    }

                }
                actionTranslate.id -> {
                    translate(
                            vm.edt.value!!,
                            vm.sortedList.values.elementAt(vm.sp2.value!!).say
                                    ?: vm.sortedList.values.elementAt(vm.sp2.value!!).hear!!.split("-")[0],
                            vm.sortedList.values.elementAt(vm.sp1.value!!).say
                                    ?: vm.sortedList.values.elementAt(vm.sp1.value!!).hear!!.split("-")[0])
                }
                actionSwap.id -> {
                    val r1 = spin_lang_1.selectedItemPosition
                    val r2 = spin_lang_2.selectedItemPosition
                    spin_lang_2.setSelection(r1)
                    spin_lang_1.setSelection(r2)

                }
                actionSay.id -> {

                    if (null == vm.sortedList.values.elementAt(vm.sp1.value!!).say) {

                        val b = Bundle()
                        b.putString("action", "com.android.settings.TTS_SETTINGS")
                        b.putInt("message", R.string.missing_language)

                        val d = GotoDialog()
                        d.arguments = b
                        d.show(supportFragmentManager, this.javaClass.simpleName)

                    } else {

                        binding.loading = true

                        tts?.language = Locale(
                                vm.sortedList.values.elementAt(vm.sp1.value!!).say
                                        ?: vm.sortedList.values.elementAt(vm.sp1.value!!).hear!!.split("-")[0])

                        tts?.speak(vm.txt.value!!, TextToSpeech.QUEUE_FLUSH, null, vm.txt.value!!.hashCode().toString())
                    }
                }

            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == Activity.RESULT_OK) {
                if (null != data) {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    vm.edt.value = result[0]
                }
            }
            binding.loading = false
        }
        if (requestCode == REQUEST_CHECK_TTS_DATA) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                tts = TextToSpeech(this@TranslateActivity, ttsListener)
            } else {
                // No engine found, go to store
                val b = Bundle()
                b.putString("action", TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                b.putInt("message", R.string.missing_tts)

                val d = GotoDialog()
                d.arguments = b
                d.show(supportFragmentManager, this.javaClass.simpleName)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        vm.stamp()
    }

    override fun onStop() {
        super.onStop()
        tts?.stop()
        tts?.shutdown()
    }

    /**
     * use GoogleTranslate through webview
     * todo api
     */
    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun translate(textToTranslate: String, from: String, to: String) {

        if (isNetworkAvailable) {
            binding.loading = true

            val wizz = WebView(this)
            wizz.settings.javaScriptEnabled = true
            wizz.webViewClient = object : WebViewClient() {

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    wizz.evaluateJavascript(
                            "(function() { return (document.getElementsByClassName('t0')[0].innerHTML); })();",
                            { html ->
                                vm.txt.value = html.substring(1, html.length - 1)
                                wizz.clearCache(true)
                                wizz.destroy()
                            })
                    binding.loading = false
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    super.onReceivedError(view, request, error)
                    Log.e("Translate", "webview - onReceivedError")
                    binding.loading = false
                }
            }

            var url: String? = null
            try {
                url = ("https://translate.google.com/m?hl=" + currentLocale!!.language
                        + "&sl=" + from + "&tl=" + to
                        + "&ie=UTF-8&prev=_m&q=" + URLEncoder.encode(textToTranslate, "utf-8"))

            } catch (e: UnsupportedEncodingException) {
                Log.e("translate", e.message)
            } finally {
                wizz.loadUrl(url)
            }
        } else {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    companion object {
        const val REQUEST_CODE_SPEECH_INPUT = 102
        const val REQUEST_CHECK_TTS_DATA = 103
    }
}