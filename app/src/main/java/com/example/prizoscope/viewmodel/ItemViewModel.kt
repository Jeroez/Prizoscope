package com.example.prizoscope.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prizoscope.data.model.Item
import com.example.prizoscope.data.repository.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ItemViewModel(private val itemRepository: ItemRepository) : ViewModel() {
    private val _items = MutableLiveData<List<Item>>()
    val items: LiveData<List<Item>> = _items

    private val _bookmarkedItems = MutableLiveData<List<Item>>()
    val bookmarkedItems: LiveData<List<Item>> = _bookmarkedItems

    fun loadItems() {
        viewModelScope.launch(Dispatchers.IO) {
            val allItems = itemRepository.getAllItems()
            _items.postValue(allItems)
        }
    }

    fun bookmarkItem(item: Item) {
        viewModelScope.launch(Dispatchers.IO) {
            itemRepository.bookmarkItem(item)
            loadBookmarkedItems()
        }
    }

    fun loadBookmarkedItems() {
        viewModelScope.launch(Dispatchers.IO) {
            val bookmarked = itemRepository.getBookmarkedItems()
            _bookmarkedItems.postValue(bookmarked)
        }
    }
}
