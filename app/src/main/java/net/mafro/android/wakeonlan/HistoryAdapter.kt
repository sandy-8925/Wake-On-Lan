/*
Copyright (C) 2008-2014 Matt Black
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
* Neither the name of the author nor the names of its contributors may be used
  to endorse or promote products derived from this software without specific
  prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package net.mafro.android.wakeonlan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.TextView
import androidx.annotation.UiThread
import net.mafro.android.widget.StarButton


/**
 * @desc    Custom adapter to aid in UI binding
 */
class HistoryAdapter internal constructor(private val showStars: Boolean) : OnCheckedChangeListener, BaseAdapter() {

    private var historyItems = emptyList<HistoryIt>()

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        // extract record's _ID from tag
        val id = buttonView.tag as Int

        if (isChecked) {
            setIsStarred(id, 1)
        } else {
            setIsStarred(id, 0)
        }
    }

    private fun setIsStarred(id: Int, value: Int) {
        val historyItem = historyDb.historyDao().historyItem(id.toLong())
        historyItem.starred = value
        historyDb.historyDao().updateItem(historyItem)
    }

    override fun getCount(): Int = historyItems.size

    override fun getItem(position: Int): Any = historyItems[position]

    override fun getItemId(position: Int): Long = historyItems[position].id.toLong()

    override fun hasStableIds(): Boolean = true

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.history_row, parent, false)
        val vtitle = view.findViewById<TextView>(R.id.history_row_title)
        val vmac = view.findViewById<TextView>(R.id.history_row_mac)
        val vip = view.findViewById<TextView>(R.id.history_row_ip)
        val vport = view.findViewById<TextView>(R.id.history_row_port)
        val star = view.findViewById<StarButton>(R.id.history_row_star)

        val item = getItem(position) as HistoryIt

        // bind the cursor data to the form items
        vtitle.text = item.title
        vmac.text = item.mac
        vip.text = item.ip
        vport.text = Integer.toString(item.port)

        if (this.showStars) {
            // remove click handler to prevent recursive calls
            star.setOnCheckedChangeListener(null)

            // change the star state if different
            val starred = item.starred != 0    // non-zero == true
            star.isChecked = starred
            star.render()

            // add event listener to star button
            star.setOnCheckedChangeListener(this)

            // save our record _ID in the star's tag
            star.tag = item.id

        } else {
            // disable the star button
            star.isClickable = false
            star.noRender = true
            star.render()
        }
        return view
    }

    @UiThread
    fun setHistoryItems(historyItems: List<HistoryIt>?) {
        this.historyItems = historyItems ?: emptyList()
        notifyDataSetChanged()
    }
}
