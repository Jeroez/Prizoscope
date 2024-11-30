package com.example.prizoscope.ui.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prizoscope.R
import com.example.prizoscope.data.model.Item
import com.example.prizoscope.data.repository.ItemRepository
import kotlinx.android.synthetic.main.fragment_bookmarks.*

class BookmarkFragment : Fragment() {

    private lateinit var itemRepository: ItemRepository
    private val bookmarkedItems = mutableListOf<Item>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_bookmarks, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        itemRepository = ItemRepository(requireContext())

        // Load bookmarked items
        bookmarkedItems.addAll(loadBookmarkedItems())

        bookmarksRecyclerView.layoutManager = LinearLayoutManager(context)
        bookmarksRecyclerView.adapter = BookmarkAdapter(bookmarkedItems)
    }

    private fun loadBookmarkedItems(): List<Item> {
        // Retrieve bookmarked items from a data source (e.g., SharedPreferences or a local database)
        return listOf() // Replace with actual implementation
    }
}
