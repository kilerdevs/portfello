package com.portfello.data.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkLog @Inject constructor() {

    private val _entries = MutableStateFlow<List<String>>(emptyList())
    val entries: StateFlow<List<String>> = _entries.asStateFlow()

    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val maxEntries = 200

    fun log(message: String) {
        val line = "${fmt.format(Date())}  $message"
        _entries.value = (_entries.value + line).takeLast(maxEntries)
    }

    fun clear() {
        _entries.value = emptyList()
    }
}
