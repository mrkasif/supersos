package com.supersos.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.supersos.app.R
import com.supersos.app.data.AppPrefs
import com.supersos.app.data.Contact
import com.supersos.app.data.ContactsRepository
import com.supersos.app.monitor.EmergencyAlertService

class MainActivity : AppCompatActivity() {

    private val contactsRepo by lazy { ContactsRepository(this) }
    private lateinit var adapter: ContactAdapter
    private lateinit var statusView: TextView
    private lateinit var statusDetail: TextView
    private lateinit var guardSwitch: MaterialSwitch

    private val addContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) refreshContacts()
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.none { granted -> granted }) {
                Toast.makeText(this, R.string.permission_needed, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusView = findViewById(R.id.statusText)
        statusDetail = findViewById(R.id.statusDetail)
        guardSwitch = findViewById(R.id.guardSwitch)

        adapter = ContactAdapter(onRemove = ::removeContact)
        findViewById<RecyclerView>(R.id.contactList).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        findViewById<FloatingActionButton>(R.id.addContact).setOnClickListener {
            addContactLauncher.launch(Intent(this, AddContactActivity::class.java))
        }

        guardSwitch.isChecked = AppPrefs.isGuardEnabled(this)
        guardSwitch.setOnCheckedChangeListener { _, checked ->
            AppPrefs.setGuardEnabled(this, checked)
            if (checked) {
                EmergencyAlertService.start(this)
                requestMissingPermissions()
            } else {
                EmergencyAlertService.stop(this)
            }
        }

        refreshContacts()
        requestMissingPermissions()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun refreshContacts() {
        adapter.submit(contactsRepo.list())
    }

    private fun removeContact(contact: Contact) {
        contactsRepo.remove(contact.id)
        refreshContacts()
    }

    private fun updateStatus() {
        val running = AppPrefs.isGuardEnabled(this)
        statusView.setText(if (running) R.string.status_running else R.string.status_stopped)
        statusDetail.text = getString(R.string.status_detail, contactsRepo.count())
    }

    private fun requestMissingPermissions() {
        val missing = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }

    private fun requiredPermissions(): Array<String> {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        return perms.toTypedArray()
    }
}
