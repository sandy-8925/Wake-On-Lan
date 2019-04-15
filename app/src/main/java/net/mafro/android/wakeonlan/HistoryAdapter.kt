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
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.mafro.android.wakeonlan.databinding.HistoryRowBinding


/**
 * @desc    Custom adapter to aid in UI binding
 */
internal class HistoryAdapter internal constructor(private val showStars: Boolean) : OnCheckedChangeListener, RecyclerView.Adapter<HistoryCellViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryCellViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<HistoryRowBinding>(inflater, R.layout.history_row, parent, false)
        return HistoryCellViewHolder(binding)
    }

    override fun getItemCount(): Int  = historyItems.size

    private val rowClickListener : View.OnClickListener = View.OnClickListener { view ->
        val itemId = view.getTag(R.id.hist_cell_position_tag) as Int
        historyController.sendWakePacket(itemId.toLong())
    }

    override fun onBindViewHolder(holder: HistoryCellViewHolder, position: Int) {
        val item = historyItems[position]

        // bind the cursor data to the form items
        holder.binding.historyRowTitle.text = item.title
        holder.binding.historyRowMac.text = item.mac
        holder.binding.historyRowIp.text = item.ip
        holder.binding.historyRowPort.text = Integer.toString(item.port)

        holder.binding.root.setTag(R.id.hist_cell_position_tag, item.id)
        holder.binding.root.setOnClickListener(rowClickListener)

        val star = holder.binding.historyRowStar

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
    }

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

    @UiThread
    internal fun setHistoryItems(historyItems: List<HistoryIt>?) {
        this.historyItems = historyItems ?: emptyList()
        notifyDataSetChanged()
    }
}

internal class HistoryCellViewHolder(val binding : HistoryRowBinding) : RecyclerView.ViewHolder(binding.root)