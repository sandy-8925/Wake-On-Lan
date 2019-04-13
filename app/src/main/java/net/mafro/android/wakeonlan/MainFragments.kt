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
import android.widget.EditText
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Single
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import net.mafro.android.wakeonlan.WakeOnLanActivity.Companion.CREATED
import net.mafro.android.wakeonlan.WakeOnLanActivity.Companion.LAST_USED
import net.mafro.android.wakeonlan.WakeOnLanActivity.Companion.SORT_MODE_PREFS_KEY
import net.mafro.android.wakeonlan.WakeOnLanActivity.Companion.TAG
import net.mafro.android.wakeonlan.WakeOnLanActivity.Companion.USED_COUNT
import net.mafro.android.wakeonlan.WakeOnLanActivity.Companion.notifyUser
import net.mafro.android.wakeonlan.databinding.HistoryFragmentBinding
import net.mafro.android.wakeonlan.databinding.WakeFragmentBinding
import java.util.concurrent.Callable

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
        val settings = requireContext().getSharedPreferences(TAG, Context.MODE_PRIVATE)
        histViewModel.sortMode = settings.getInt(SORT_MODE_PREFS_KEY, WakeOnLanActivity.CREATED)
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

    private fun onHistoryItemClick(item: HistoryItem) {
        MagicPacket.createSendPacketSingle(requireContext(), item).doOnSuccess {
            histHandler.incrementHistory(item.id.toLong())
        }.subscribe()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        menu ?: return
        inflater ?: return

        inflater.inflate(R.menu.main_menu, menu)

        var mi: MenuItem? = null

        when (histViewModel.sortMode) {
            CREATED -> mi = menu.findItem(R.id.menu_created)
            LAST_USED -> mi = menu.findItem(R.id.menu_lastused)
            USED_COUNT -> mi = menu.findItem(R.id.menu_usedcount)
        }

        // toggle menuitem
        if (mi != null) mi.isChecked = true
    }

    override fun onOptionsItemSelected(mi: MenuItem): Boolean {
        when (mi.itemId) {
            R.id.menu_created -> setNewSortMode(CREATED)
            R.id.menu_lastused -> setNewSortMode(LAST_USED)
            R.id.menu_usedcount -> setNewSortMode(USED_COUNT)
            R.id.menu_sortby -> return false
        }

        // toggle menuitem
        mi.isChecked = true

        // save to preferences
        requireContext().getSharedPreferences(TAG, Context.MODE_PRIVATE).edit()
                .putInt(SORT_MODE_PREFS_KEY, histViewModel.sortMode)
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
                    histHandler.incrementHistory(item.id.toLong())
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
        return historyDb.historyDao().getHistoryList(sortColumnName)
    }

    var sortMode: Int = WakeOnLanActivity.CREATED
        set(value) {
            histListLiveData = createHistListLiveDataObject()
            field = value
        }
}

class WakeFragment : Fragment() {
    private lateinit var binding: WakeFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.wake_fragment, container, false)
        return binding.root
    }

    private val macFocusChangeListener: View.OnFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
        // validate mac address on field exit
        if (!hasFocus) {
            val vmac = v as EditText
            try {
                // validate our mac address
                var mac = vmac.text.toString()
                if (mac.isNotEmpty()) {
                    mac = MagicPacket.cleanMac(mac)
                    vmac.setText(mac)
                }
                vmac.error = null

            } catch (iae: IllegalArgumentException) {
                vmac.error = getString(R.string.invalid_mac)
            }

        }
    }

    private val testClickListener: View.OnClickListener = View.OnClickListener {
        val vtitle = binding.title
        val vmac = binding.mac
        val vip = binding.ip
        val vport = binding.port

        val title = vtitle.text.toString().trim { it <= ' ' }
        val mac = vmac.text.toString().trim { it <= ' ' }

        // default IP and port unless set on form
        var ip = MagicPacket.BROADCAST
        if (vip.text.toString().trim { it <= ' ' } != "") {
            ip = vip.text.toString().trim { it <= ' ' }
        }

        var port = MagicPacket.PORT
        if (vport.text.toString().trim { it <= ' ' } != "") {
            try {
                port = Integer.valueOf(vport.text.toString().trim { it <= ' ' })
            } catch (nfe: NumberFormatException) {
                notifyUser("Bad port number", requireContext())
                return@OnClickListener
            }
        }

        // update form with cleaned variables
        vtitle.setText(title)
        vmac.setText(mac)
        vip.setText(ip)
        vport.setText(Integer.toString(port))

        // send the magic packet
        MagicPacket.sendPacket(requireContext(), title, mac, ip, port)

        //TODO: Move this block of code over to history fragment
//            else {
//                val formattedMac: String
//
//                try {
//                    // validate and clean our mac address
//                    formattedMac = MagicPacket.cleanMac(mac)
//
//                } catch (iae: IllegalArgumentException) {
//                    notifyUser(iae.message, requireContext())
//                    return
//                }
//
//                // update existing history entry
//                histHandler.updateHistory(_editModeID, title, formattedMac, ip, port)
//
//                // reset now edit mode complete
//                _editModeID = 0
//            }

        // finished typing (either send or edit)
//            typingMode = false

        // switch back to the history tab
//            if (WakeOnLanActivity.isTablet) {
//                th.setCurrentTab(0)
//            }
    }

    private val clearClickListener: View.OnClickListener = View.OnClickListener {
        clearForm()
    }

    @UiThread
    private fun clearForm() {
        // clear the form
        binding.title.text = null
        binding.mac.text = null
        binding.mac.error = null
        binding.ip.setText(MagicPacket.BROADCAST)
        binding.port.setText(MagicPacket.PORT.toString())
    }

    private val saveClickListener: View.OnClickListener = View.OnClickListener {
        val title = binding.title.text.toString()
        val formattedMac = binding.mac.text.toString()
        val ip = binding.ip.text.toString()
        val port = Integer.parseInt(binding.port.text.toString())

        clearForm()

        Single.fromCallable(ItemAlreadyPresent(formattedMac, ip, port))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnSuccess(WakePacketSaveAction(title, formattedMac, ip, port))
                .subscribe()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // set defaults on Wake tab
        binding.ip.setText(MagicPacket.BROADCAST)
        binding.port.setText(Integer.toString(MagicPacket.PORT))

        // register self as listener for wake button
        binding.testWake.setOnClickListener(testClickListener)
        binding.clearWake.setOnClickListener(clearClickListener)
        binding.saveWake.setOnClickListener(saveClickListener)

        // register self as mac address field focus change listener
        binding.mac.onFocusChangeListener = macFocusChangeListener
    }
}

internal class ItemAlreadyPresent(private val mac: String, private val ip: String, private val port: Int) : Callable<Boolean> {
    override fun call(): Boolean {
        return historyDb.historyDao().getNumRows(mac, ip, port) > 0
    }
}

internal class WakePacketSaveAction(private val title: String, private val mac: String, private val ip: String, private val port: Int) : Consumer<Boolean> {
    override fun accept(itemAlreadyPresent: Boolean) {
        if(itemAlreadyPresent) return
        val newHistoryIt = HistoryIt().also {
            it.title = title
            it.mac = mac
            it.ip = ip
            it.port = port
        }
        historyDb.historyDao().addNewItem(newHistoryIt)
    }
}