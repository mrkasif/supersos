package com.supersos.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.supersos.app.R
import com.supersos.app.data.Contact

class ContactAdapter(
    private val onRemove: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    private val items = mutableListOf<Contact>()

    fun submit(contacts: List<Contact>) {
        items.clear()
        items.addAll(contacts)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.contactName)
        val relationship: TextView = view.findViewById(R.id.contactRelationship)
        val phone: TextView = view.findViewById(R.id.contactPhone)
        val remove: TextView = view.findViewById(R.id.contactRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = items[position]
        holder.name.text = contact.name
        holder.relationship.text = contact.relationship
        holder.phone.text = contact.phone
        holder.remove.setOnClickListener { onRemove(contact) }
    }

    override fun getItemCount(): Int = items.size
}
