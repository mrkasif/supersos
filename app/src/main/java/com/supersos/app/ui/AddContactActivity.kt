package com.supersos.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.supersos.app.R
import com.supersos.app.data.Contact
import com.supersos.app.data.ContactsRepository
import java.util.UUID

class AddContactActivity : AppCompatActivity() {

    private val repo by lazy { ContactsRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contact)

        val nameInput = findViewById<TextInputEditText>(R.id.contactNameInput)
        val phoneInput = findViewById<TextInputEditText>(R.id.contactPhoneInput)
        val relationshipInput = findViewById<TextInputEditText>(R.id.contactRelationshipInput)

        findViewById<MaterialButton>(R.id.saveButton).setOnClickListener {
            val name = nameInput.text?.toString()?.trim().orEmpty()
            val phone = phoneInput.text?.toString()?.trim().orEmpty()
            val relationship = relationshipInput.text?.toString()?.trim().orEmpty()

            when {
                name.isEmpty() -> nameInput.error = getString(R.string.error_required)
                !phone.matches(PHONE_PATTERN) -> phoneInput.error = getString(R.string.error_phone)
                else -> {
                    val added = repo.add(
                        Contact(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            phone = phone,
                            relationship = relationship
                        )
                    )
                    if (added) {
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this, R.string.max_contacts, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        findViewById<MaterialButton>(R.id.cancelButton).setOnClickListener { finish() }
    }

    companion object {
        private val PHONE_PATTERN = Regex("^\\+?[0-9][0-9()\\-\\s]{5,19}$")
    }
}
