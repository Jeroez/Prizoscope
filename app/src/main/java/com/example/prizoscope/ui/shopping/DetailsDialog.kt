package com.example.prizoscope.ui.shopping

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.prizoscope.databinding.DialogItemDetailsBinding
import com.example.prizoscope.data.model.Item

class DetailsDialog(private val item: Item) : DialogFragment() {

    private lateinit var binding: DialogItemDetailsBinding

    @SuppressLint("DefaultLocale")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        binding = DialogItemDetailsBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        binding.itemName.text = item.name
        binding.itemPrice.text = String.format("$%.2f", item.price)
        binding.itemRatings.text = String.format("%.1f/5.0", item.ratings)

        binding.bookmarkButton.setOnClickListener {
            dismiss()
        }

        binding.purchaseButton.setOnClickListener {
        }

        return dialog
    }
}
