package net.mafro.android.wakeonlan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import io.reactivex.Single
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import net.mafro.android.wakeonlan.WakeOnLanActivity.Companion.notifyUser
import net.mafro.android.wakeonlan.databinding.WakeFragmentBinding
import java.util.concurrent.Callable

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
        val vtitle = binding.wakeForm.title
        val vmac = binding.wakeForm.mac
        val vip = binding.wakeForm.ip
        val vport = binding.wakeForm.port

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
        binding.wakeForm.title.text = null
        binding.wakeForm.mac.text = null
        binding.wakeForm.mac.error = null
        binding.wakeForm.ip.setText(MagicPacket.BROADCAST)
        binding.wakeForm.port.setText(MagicPacket.PORT.toString())
    }

    private val saveClickListener: View.OnClickListener = View.OnClickListener {
        val title = binding.wakeForm.title.text.toString()
        val formattedMac = binding.wakeForm.mac.text.toString()
        val ip = binding.wakeForm.ip.text.toString()
        val port = Integer.parseInt(binding.wakeForm.port.text.toString())

        clearForm()

        Single.fromCallable(ItemAlreadyPresent(formattedMac, ip, port))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnSuccess(WakePacketSaveAction(title, formattedMac, ip, port))
                .subscribe()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.wakeForm.executePendingBindings()

        // set defaults on Wake tab
        binding.wakeForm.ip.setText(MagicPacket.BROADCAST)
        binding.wakeForm.port.setText(Integer.toString(MagicPacket.PORT))

        // register self as listener for wake button
        binding.testWake.setOnClickListener(testClickListener)
        binding.clearWake.setOnClickListener(clearClickListener)
        binding.saveWake.setOnClickListener(saveClickListener)

        // register self as mac address field focus change listener
        binding.wakeForm.mac.onFocusChangeListener = macFocusChangeListener
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
            it.createdDate = System.currentTimeMillis()
        }
        historyDb.historyDao().addNewItem(newHistoryIt)
    }
}