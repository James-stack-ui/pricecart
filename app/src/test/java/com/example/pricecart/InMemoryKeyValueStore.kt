package com.example.pricecart

import com.example.pricecart.data.KeyValueStore

class InMemoryKeyValueStore : KeyValueStore {
    private val entries = linkedMapOf<String, Any?>()

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return entries[key] as? Boolean ?: defaultValue
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return entries[key] as? String ?: defaultValue
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return entries[key] as? Long ?: defaultValue
    }

    override fun putBoolean(key: String, value: Boolean) {
        entries[key] = value
    }

    override fun putString(key: String, value: String?) {
        if (value == null) {
            entries.remove(key)
        } else {
            entries[key] = value
        }
    }

    override fun putLong(key: String, value: Long) {
        entries[key] = value
    }

    override fun remove(key: String) {
        entries.remove(key)
    }

    override fun allEntries(): Map<String, Any?> {
        return entries.toMap()
    }
}
