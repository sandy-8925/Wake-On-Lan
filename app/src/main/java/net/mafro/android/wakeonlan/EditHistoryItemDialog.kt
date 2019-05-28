package net.mafro.android.wakeonlan

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import net.mafro.android.wakeonlan.databinding.WakeFormBinding
import java.util.concurrent.Callable

internal const val EDIT_ITEM_DIALOG_ID_KEY = "EDIT_ITEM_DIALOG_ID_KEY"

internal fun createEditDialogInstance(historyItemId : Int) : EditHistoryItemDialog = EditHistoryItemDialog().apply {
        arguments = Bundle().apply { putInt(EDIT_ITEM_DIALOG_ID_KEY, historyItemId) }
    }

internal const val EDIT_DIALOG_TAG = "EditHistoryItemDialog"

class EditHistoryItemDialog : DialogFragment() {
    private val ITEM_ID_DEFAULT : Int = -1

    private var itemId : Int = ITEM_ID_DEFAULT

    private lateinit var binding: WakeFormBinding
    private val saveClickListener = DialogInterface.OnClickListener { _, _ ->
        val item = binding.historyItem ?: HistoryIt().apply { this.id = itemId }
        item.title = binding.title.text.toString()
        item.mac = binding.mac.text.toString()
        item.ip = binding.ip.text.toString()
        item.port = binding.port.text.toString().toInt()

        historyController.updateHistoryItem(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        args?: return
        itemId = args.getInt(EDIT_ITEM_DIALOG_ID_KEY)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        binding = DataBindingUtil.inflate(inflater, R.layout.wake_form, null, false)
        binding.mac.onFocusChangeListener = MacFieldFocusChangeListener()

        val builder = AlertDialog.Builder(requireContext())
                .setTitle(R.string.menu_edit)
                .setPositiveButton(R.string.save, saveClickListener)
                .setNegativeButton(android.R.string.cancel, null)
                .setView(binding.root)

        Single.fromCallable(LoadHistoryItemTask(itemId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess {
                    binding.historyItem = it
                }
                .doOnError {
                    Toast.makeText(requireContext(), getString(R.string.item_edit_load_failed_msg), Toast.LENGTH_LONG).show()
                    dismiss()
                }
                .subscribe()

        return builder.create()
    }
}

internal class LoadHistoryItemTask(private val itemId: Int) : Callable<HistoryIt> {
    override fun call(): HistoryIt = historyDb.historyDao().historyItem(itemId)
}
