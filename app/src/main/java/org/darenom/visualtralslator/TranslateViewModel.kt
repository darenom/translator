package org.darenom.visualtralslator

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.content.SharedPreferences


/**
 * Created by adm on 24/03/2018.
 */
class TranslateViewModel(app: Application) : AndroidViewModel(app) {

    private var settings: SharedPreferences? = null

    var allCodes: Array<String>? = null
    var allFlags: Array<String>? = null
    var mList = HashMap<String, TranslateActivity.Refs>()
    var isConsolidated = false

    var sp1 = MutableLiveData<Int>()
    var sp2 = MutableLiveData<Int>()

    var edt = MutableLiveData<String>()
    var txt = MutableLiveData<String>()

    init {

        settings = getApplication<Application>()
                .getSharedPreferences(getApplication<Application>().packageName, 0)

        sp1.value = settings!!.getInt(SP1_KEY, 0)
        sp2.value = settings!!.getInt(SP2_KEY, 0)
        edt.value = settings!!.getString(EDT_KEY, "")
        txt.value = settings!!.getString(TXT_KEY, "")

    }


    fun consolidateList(){

        val listSay = ArrayList<String>()
        mList.keys.forEach {
            if (!listSay.contains(mList[it]!!.say))
                if (null != mList[it]!!.say)
                    listSay.add(mList[it]!!.say!!)
        }

        mList.keys.forEach {
            if (null == mList[it]!!.say)
                if (listSay.contains(mList[it]!!.hear!!.split("-")[0]))
                    mList[it]!!.say = mList[it]!!.hear!!.split("-")[0]
        }

        isConsolidated = true
    }

    fun stamp(){
        val editor = settings!!.edit()
        editor.putInt(SP1_KEY, sp1.value!!)
        editor.putInt(SP2_KEY, sp2.value!!)
        editor.putString(EDT_KEY, edt.value!!)
        editor.putString(TXT_KEY, txt.value!!)
        editor.apply()
    }

    companion object {
        const val SP1_KEY = "Sp1Key"
        const val SP2_KEY = "Sp2Key"
        const val EDT_KEY = "EdtKey"
        const val TXT_KEY = "TxtKey"

    }
}
