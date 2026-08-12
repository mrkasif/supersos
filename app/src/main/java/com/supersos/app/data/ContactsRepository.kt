package com.supersos.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Simple SharedPreferences-backed store for the emergency contacts.
 * Enforces the "max 3 contacts" rule from the app concept.
 *
 * Swap for Room if you need more structure later.
 */
class ContactsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun list(): List<Contact> {
        val raw = prefs.getString(KEY_CONTACTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        Contact(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            phone = o.getString("phone"),
                            relationship = o.optString("relationship")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun add(contact: Contact): Boolean {
        if (list().size >= MAX_CONTACTS) return false
        val json = JSONArray().apply {
            list().forEach { put(it.toJson()) }
            put(contact.toJson())
        }
        prefs.edit().putString(KEY_CONTACTS, json.toString()).apply()
        return true
    }

    fun remove(id: String) {
        val json = JSONArray().apply {
            list().filterNot { it.id == id }.forEach { put(it.toJson()) }
        }
        prefs.edit().putString(KEY_CONTACTS, json.toString()).apply()
    }

    fun count(): Int = list().size

    private fun Contact.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("phone", phone)
        put("relationship", relationship)
    }

    companion object {
        const val MAX_CONTACTS = 3
        private const val PREFS_NAME = "supersos"
        private const val KEY_CONTACTS = "emergency_contacts"
    }
}
