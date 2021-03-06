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
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.mafro.android.wakeonlan.databinding.HistoryRowBinding
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils

/**
 * @desc    Custom adapter to aid in UI binding
 */
internal open class BaseHistoryItemAdapter internal constructor(private val showStars: Boolean, diffCallback: DiffUtil.ItemCallback<HistoryIt>) : OnCheckedChangeListener, ListAdapter<HistoryIt, HistoryCellViewHolder>(diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryCellViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<HistoryRowBinding>(inflater, R.layout.history_row, parent, false)
        if(showStars) {
            binding.historyRowStar.isClickable = true
            binding.historyRowStar.visibility = View.VISIBLE
        } else {
            binding.historyRowStar.isClickable = false
            binding.historyRowStar.visibility = View.INVISIBLE
        }
        binding.root.setOnClickListener(rowClickListener)
        return HistoryCellViewHolder(binding)
    }

    private val rowClickListener : View.OnClickListener = View.OnClickListener { view ->
        val itemId = view.getTag(R.id.hist_cell_position_tag) as Int
        itemClickListener?.onClick(itemId)
    }

    internal var itemClickListener : HistoryItemListClickListener? = null

    override fun onBindViewHolder(holder: HistoryCellViewHolder, position: Int) {
        val item = getItem(position)

        holder.binding.root.setTag(R.id.hist_cell_position_tag, item.id)
        holder.binding.historyItem = item

        if (this.showStars) {
            val star = holder.binding.historyRowStar

            // remove click handler to prevent recursive calls
            star.setOnCheckedChangeListener(null)

            // change the star state if different
            star.isChecked = BooleanUtils.toBoolean(item.starred)

            // add event listener to star button
            star.setOnCheckedChangeListener(this)

            // save our record _ID in the star's tag
            star.setTag(R.id.hist_cell_itemid_tag, item.id)
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        // extract record's _ID from tag
        val id = buttonView.getTag(R.id.hist_cell_itemid_tag) as Int
        val starredVal = BooleanUtils.toInteger(isChecked)
        historyController.setIsStarred(id, starredVal)
    }

    @UiThread
    internal fun setHistoryItems(historyItems: List<HistoryIt>?) {
        submitList(historyItems ?: emptyList())
    }
}

internal class HistoryCellViewHolder(val binding : HistoryRowBinding) : RecyclerView.ViewHolder(binding.root)

internal class HistoryListDiffCallback : DiffUtil.ItemCallback<HistoryIt>() {
    override fun areItemsTheSame(oldItem: HistoryIt, newItem: HistoryIt): Boolean = (oldItem.id == newItem.id)

    override fun areContentsTheSame(oldItem: HistoryIt, newItem: HistoryIt): Boolean {
        return StringUtils.equals(oldItem.title, newItem.title) &&
                StringUtils.equals(oldItem.mac, newItem.mac) &&
                StringUtils.equals(oldItem.ip, newItem.ip) &&
                oldItem.port == newItem.port &&
                oldItem.starred == newItem.starred
    }
}

interface HistoryItemContextMenuCreator {
    fun createContextMenu(menu : Menu, view : View, historyItemId : Int)
}

interface HistoryItemListClickListener {
    fun onClick(itemId : Int)
}

internal class HistoryAdapter : BaseHistoryItemAdapter(true, HistoryListDiffCallback()) {
    internal var contextMenuCreator : HistoryItemContextMenuCreator? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryCellViewHolder {
        val holder = super.onCreateViewHolder(parent, viewType)
        if(contextMenuCreator!=null) holder.binding.root.setOnCreateContextMenuListener(contextMenuListener)
        return holder
    }

    private val contextMenuListener = View.OnCreateContextMenuListener { menu, v, _ ->
        val historyItemId = v.getTag(R.id.hist_cell_position_tag) as Int
        contextMenuCreator?.createContextMenu(menu, v, historyItemId)
    }
}