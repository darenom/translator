package org.darenom.translate

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog


class GotoDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val a = arguments!!.getString("action")
        val m = context!!.getString(arguments!!.getInt("message"))
        return AlertDialog.Builder(context!!)
                .setTitle(R.string.app_name)
                .setMessage(m)
                .setPositiveButton(R.string.ok, { _, _ ->
                    startActivity(Intent()
                            .setAction(a)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                })
                .create()
    }
}