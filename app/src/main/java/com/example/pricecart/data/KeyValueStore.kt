package com.example.pricecart.data

interface KeyValueStore {
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun getString(key: String, defaultValue: String? = null): String?
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun putBoolean(key: String, value: Boolean)
    fun putString(key: String, value: String?)
    fun putLong(key: String, value: Long)
    fun remove(key: String)
    fun allEntries(): Map<String, Any?>
}
