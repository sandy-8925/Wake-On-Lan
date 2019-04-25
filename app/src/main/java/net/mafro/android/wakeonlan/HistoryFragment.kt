package net.mafro.android.wakeonlan

import android.content.Context
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.sqlite.db.SimpleSQLiteQuery
import net.mafro.android.wakeonlan.databinding.HistoryFragmentBinding

class HistoryFragment : Fragment() {
    private lateinit var binding: HistoryFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.history_fragment, container, false)
        return binding.root
    }

    private lateinit var histHandler: HistoryListHandler
    private lateinit var histViewModel: HistoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        histViewModel = ViewModelProviders.of(this).get(HistoryViewModel::class.java)
        val settings = requireContext().getSharedPreferences(WakeOnLanActivity.TAG, Context.MODE_PRIVATE)
        histViewModel.sortMode = settings.getInt(WakeOnLanActivity.SORT_MODE_PREFS_KEY, WakeOnLanActivity.CREATED)
    }

    @UiThread
    private fun setNewSortMode(sortMode : Int) {
        histViewModel.histListLiveData.removeObserver(listDataObserver)
        histViewModel.sortMode = sortMode
        histViewModel.histListLiveData.observe(this, listDataObserver)
    }

    private val listDataObserver: Observer<in List<HistoryIt>> = Observer {
        historyAdapter.setHistoryItems(it)
    }

    private lateinit var historyAdapter: HistoryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        // load history handler (deals with cursor and history ListView)
//        histHandler = HistoryListHandler(requireActivity(), binding.history)
//        histHandler.bind(sort_mode)
//
//        // add listener to get on click events
//        histHandler.addHistoryListClickListener { item -> onHistoryItemClick(item) }
//
//        // register main Activity as context menu handler
//        registerForContextMenu(binding.history)
        historyAdapter = HistoryAdapter(true)
        binding.history.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.history.adapter = historyAdapter
        histViewModel.histListLiveData.observe(this, listDataObserver)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        menu ?: return
        inflater ?: return

        inflater.inflate(R.menu.main_menu, menu)

        var mi: MenuItem? = null

        when (histViewModel.sortMode) {
            WakeOnLanActivity.CREATED -> mi = menu.findItem(R.id.menu_created)
            WakeOnLanActivity.LAST_USED -> mi = menu.findItem(R.id.menu_lastused)
            WakeOnLanActivity.USED_COUNT -> mi = menu.findItem(R.id.menu_usedcount)
        }

        // toggle menuitem
        if (mi != null) mi.isChecked = true
    }

    override fun onOptionsItemSelected(mi: MenuItem): Boolean {
        when (mi.itemId) {
            R.id.menu_created -> setNewSortMode(WakeOnLanActivity.CREATED)
            R.id.menu_lastused -> setNewSortMode(WakeOnLanActivity.LAST_USED)
            R.id.menu_usedcount -> setNewSortMode(WakeOnLanActivity.USED_COUNT)
            R.id.menu_sortby -> return false
        }

        // toggle menuitem
        mi.isChecked = true

        // save to preferences
        requireContext().getSharedPreferences(WakeOnLanActivity.TAG, Context.MODE_PRIVATE).edit()
                .putInt(WakeOnLanActivity.SORT_MODE_PREFS_KEY, histViewModel.sortMode)
                .apply()

        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo)
        requireActivity().menuInflater.inflate(R.menu.history_menu, menu)
    }

    override fun onContextItemSelected(mi: MenuItem): Boolean {
        // extract data about clicked item
        val info = mi.menuInfo as AdapterView.AdapterContextMenuInfo

        // extract history item
        val item = histHandler.getItem(info.position)

        when (mi.itemId) {
            R.id.menu_wake -> {
                MagicPacket.createSendPacketSingle(requireContext(), item).doOnSuccess {
                    histHandler.incrementHistory(item.id)
                }.subscribe()
                return true
            }

            R.id.menu_edit -> {
                TODO("Implement this!")
                return true
            }

            R.id.menu_delete -> {
                histHandler.deleteHistory(item.id)
                return true
            }

            else -> return super.onContextItemSelected(mi)
        }
    }
}

internal class HistoryViewModel : ViewModel() {
    var histListLiveData : LiveData<List<HistoryIt>> = createHistListLiveDataObject()
        private set

    private fun createHistListLiveDataObject() : LiveData<List<HistoryIt>> {
        val sortColumnName = when(sortMode) {
            WakeOnLanActivity.CREATED -> History.Items.CREATED_DATE
            WakeOnLanActivity.LAST_USED -> History.Items.LAST_USED_DATE
            WakeOnLanActivity.USED_COUNT -> History.Items.USED_COUNT
            else -> History.Items.CREATED_DATE
        }
        val query = SimpleSQLiteQuery("select * from ${HistoryProvider.HISTORY_TABLE_NAME} order by $sortColumnName desc")
        return historyDb.historyDao().histItemList(query)
    }

    var sortMode: Int = WakeOnLanActivity.CREATED
        set(value) {
            field = value
            histListLiveData = createHistListLiveDataObject()
        }
}