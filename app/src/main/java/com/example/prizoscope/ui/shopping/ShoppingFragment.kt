package com.example.prizoscope.ui.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prizoscope.R
import com.example.prizoscope.data.repository.ItemRepository
import kotlinx.android.synthetic.main.fragment_shopping.*

class ShoppingFragment : Fragment() {

    private lateinit var itemRepository: ItemRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_shopping, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        itemRepository = ItemRepository(requireContext())

        val items = itemRepository.getItems()

        shoppingRecyclerView.layoutManager = LinearLayoutManager(context)
        shoppingRecyclerView.adapter = ItemAdapter(items)
    }
}
