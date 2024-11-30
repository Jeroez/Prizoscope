package com.example.prizoscope.ui.shopping

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Item
import kotlinx.android.synthetic.main.dialog_item_details.*

class DetailsDialog(private val item: Item) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_item_details)

        dialog.item_name.text = item.name
        dialog.item_price.text = item.price
        dialog.item_ratings.text = item.ratings

        dialog.bookmark_button.setOnClickListener {
            dismiss()
        }

        dialog.purchase_button.setOnClickListener {
        }

        return dialog
    }
}
