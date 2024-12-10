package com.example.prizoscope.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prizoscope.data.model.Item
import com.example.prizoscope.data.repository.ItemRepository
import kotlinx.coroutines.launch

class ItemViewModel(private val itemRepository: ItemRepository) : ViewModel() {

    private val _items = MutableLiveData<List<Item>>()
    val items: LiveData<List<Item>> = _items

    fun loadItems() {
        viewModelScope.launch {
            val allItems = itemRepository.fetchAllItems()
            _items.postValue(allItems)
        }
    }

    fun addItem(item: Item) {
        itemRepository.addItem(item)
    }
}
