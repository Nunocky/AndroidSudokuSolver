package org.nunocky.sudokusolver.adapter

import android.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import org.nunocky.sudokusolver.BuildConfig

class AboutListAdapter : BaseAdapter() {
    override fun getCount() = 3
    override fun getItem(position: Int) = position
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val inflater = LayoutInflater.from(parent?.context)

        return when (position) {
            0 -> {
                inflater.inflate(R.layout.simple_list_item_2, parent, false).apply {
                    findViewById<TextView>(R.id.text1).text =
                        context.getString(org.nunocky.sudokusolver.R.string.current_version)
                    findViewById<TextView>(R.id.text2).text = "${BuildConfig.VERSION_CODE}"
                }
            }
            1 -> {
                inflater.inflate(R.layout.simple_list_item_1, parent, false).apply {
                    findViewById<TextView>(R.id.text1).text =
                        context.getString(org.nunocky.sudokusolver.R.string.open_source_license)
                }
            }
            else -> {
                inflater.inflate(R.layout.simple_list_item_1, parent, false).apply {
                    findViewById<TextView>(R.id.text1).text =
                        context.getString(org.nunocky.sudokusolver.R.string.github_page)
                }
            }
        }
    }
}