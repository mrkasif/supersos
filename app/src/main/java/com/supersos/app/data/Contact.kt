package com.supersos.app.data

/**
 * One of the up-to-3 trusted people who get your location when you go unreachable.
 */
data class Contact(
    val id: String,
    val name: String,
    val phone: String,
    val relationship: String
)
