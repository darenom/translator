package org.darenom.visualtralslator

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

 class SpinAdapter(context: Context, resource: Int,
                   refs: Array<String>,
                   private val allCodes: Array<String>,
                   private val allFlags: Array<String>
 ) : ArrayAdapter<String>(context, resource) {

    private var index = ArrayList<Int>()
    private var mInflater: LayoutInflater? = null

    init {
        mInflater = LayoutInflater.from(context)

        index.clear()
        refs.forEach {
            val i = allCodes.indexOf(it)
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
                        allFlags[index[position]],
                        "drawable",
                        context.packageName), null)

        view?.findViewById<TextView>(R.id.txt)?.setCompoundDrawablesWithIntrinsicBounds(drw, null, null, null)

        view?.findViewById<TextView>(R.id.txt)?.text = allCodes[index[position]]

        return view!!
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return getView(position, convertView, parent)
    }
}
