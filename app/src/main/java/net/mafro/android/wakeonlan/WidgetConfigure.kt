/*
Copyright (C) 2013-2014 Yohan Pereira, Matt Black
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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import net.mafro.android.wakeonlan.databinding.WidgetConfigureBinding

/**
 * @desc    This class is used to configure the home screen widget
 */
class WidgetConfigure : AppCompatActivity() {
    private val listObserver: Observer<List<HistoryIt>> = Observer {
        adapter.setHistoryItems(it)
    }
    private lateinit var binding: WidgetConfigureBinding
    private var widgetId: Int = 0
    private lateinit var settings: SharedPreferences
    private lateinit var adapter : BaseHistoryItemAdapter

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED)

        // get the widget id
        widgetId = getWidgetId(intent)

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            // no valid widget id; bailing
            finish()
            return
        }

        binding = DataBindingUtil.setContentView(this, R.layout.widget_configure)
        settings = getSharedPreferences(WakeOnLanActivity.TAG, 0)

        binding.widgetConfigureList.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        adapter = BaseHistoryItemAdapter(false, HistoryListDiffCallback()).apply {
            itemClickListener = object : HistoryItemListClickListener {
                override fun onClick(itemId: Int) { selected(itemId) }
            }
        }
        binding.widgetConfigureList.adapter = adapter

        val viewModel = ViewModelProviders.of(this).get(WidgetConfigureViewModel::class.java)
        viewModel.histListLiveData.observe(this, listObserver)
    }

    @UiThread
    private fun selected(itemId: Int) {
        // save selected item id to the settings.
        WidgetProvider.saveItemPref(settings, itemId, widgetId)

        // configure the widget
        Single.fromCallable(LoadHistoryItemTask(itemId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(ConfigureWidgetAction(widgetId, applicationContext))
                .subscribe()

        val resultValue = Intent().apply { putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId) }
        setResult(RESULT_OK, resultValue)
        finish()
    }

}

internal class WidgetConfigureViewModel : ViewModel() {
    val histListLiveData : LiveData<List<HistoryIt>> = createHistListLiveDataObject(WakeOnLanActivity.USED_COUNT)
}

private class ConfigureWidgetAction(private val widgetId: Int, private val context: Context) : Consumer<HistoryIt> {
    override fun accept(item: HistoryIt) {
        WidgetProvider.configureWidget(widgetId, item, context)
    }
}
