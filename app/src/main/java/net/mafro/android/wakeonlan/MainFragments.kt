package net.mafro.android.wakeonlan

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.EditText
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import net.mafro.android.wakeonlan.WakeOnLanActivity.*
import net.mafro.android.wakeonlan.databinding.HistoryFragmentBinding
import net.mafro.android.wakeonlan.databinding.WakeFragmentBinding

class HistoryFragment : Fragment() {
    private lateinit var binding: HistoryFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.history_fragment, container, false)
        return binding.root
    }

    private var sort_mode: Int = 0
    private lateinit var histHandler: HistoryListHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // preferences
        val settings = requireContext().getSharedPreferences(TAG, Context.MODE_PRIVATE)

        // load our sort mode
        sort_mode = settings.getInt(SORT_MODE_PREFS_KEY, WakeOnLanActivity.CREATED)

        // grab the history ListView
        val lv = binding.history

        // load history handler (deals with cursor and history ListView)
        histHandler = HistoryListHandler(requireActivity(), lv)
        histHandler.bind(sort_mode)

        // add listener to get on click events
        histHandler.addHistoryListClickListener { item -> onHistoryItemClick(item) }

        // register main Activity as context menu handler
        registerForContextMenu(lv)
    }

    private fun onHistoryItemClick(item: HistoryItem) {
        val mac = sendPacket(item, requireContext())
        if (mac != null) {
            histHandler.incrementHistory(item.id.toLong())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        menu ?: return
        inflater ?: return

        inflater.inflate(R.menu.main_menu, menu)

        var mi: MenuItem? = null

        when (sort_mode) {
            CREATED -> mi = menu.findItem(R.id.menu_created)
            LAST_USED -> mi = menu.findItem(R.id.menu_lastused)
            USED_COUNT -> mi = menu.findItem(R.id.menu_usedcount)
        }

        // toggle menuitem
        if (mi != null) mi.isChecked = true
    }

    override fun onOptionsItemSelected(mi: MenuItem): Boolean {
        when (mi.itemId) {
            R.id.menu_created -> sort_mode = CREATED
            R.id.menu_lastused -> sort_mode = LAST_USED
            R.id.menu_usedcount -> sort_mode = USED_COUNT
            R.id.menu_sortby -> return false
        }

        // toggle menuitem
        mi.isChecked = true

        // save to preferences
        requireContext().getSharedPreferences(TAG, Context.MODE_PRIVATE).edit()
                .putInt(SORT_MODE_PREFS_KEY, sort_mode)
                .apply()

        // rebind the history list
        histHandler.bind(sort_mode)
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
                val result = sendPacket(item, requireContext())
                // update used count in DB
                if (result != null) {
                    histHandler.incrementHistory(item.id.toLong())
                }
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
        val formattedMac = sendPacket(requireContext(), title, mac, ip, port)
        // return on sending failed
       formattedMac ?: return@OnClickListener

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
        // clear the form
        binding.title.text = null
        binding.mac.text = null
        binding.mac.error = null
        binding.ip.text = null
        binding.port.text = null
    }

    private val saveClickListener: View.OnClickListener = View.OnClickListener {
        //TODO: clear form and persist history record
        //histHandler.addToHistory(title, formattedMac, ip, port)
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