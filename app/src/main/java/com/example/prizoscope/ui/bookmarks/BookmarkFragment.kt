package com.example.prizoscope.ui.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prizoscope.data.model.Item
import com.example.prizoscope.data.repository.ItemRepository
import com.example.prizoscope.databinding.FragmentBookmarksBinding

class BookmarkFragment : Fragment() {

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!

    private lateinit var itemRepository: ItemRepository
    private val bookmarkedItems = mutableListOf<Item>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        itemRepository = ItemRepository(requireContext())

        bookmarkedItems.addAll(loadBookmarkedItems())

        if (bookmarkedItems.isEmpty()) {
            binding.noBookmarksMessage.visibility = View.VISIBLE
            binding.bookmarksRecyclerView.visibility = View.GONE
        } else {
            binding.noBookmarksMessage.visibility = View.GONE
            binding.bookmarksRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = BookmarkAdapter(bookmarkedItems)
                visibility = View.VISIBLE
            }
        }
    }

    private fun loadBookmarkedItems(): List<Item> {
        return listOf()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
