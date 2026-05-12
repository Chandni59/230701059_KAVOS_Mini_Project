package com.chandni.kavos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ManageGuardiansActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_guardians)
        supportActionBar?.hide()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { goHome() }

        findViewById<FloatingActionButton>(R.id.fabAddGuardian).setOnClickListener {
            showAddDialog()
        }

        loadGuardians()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goHome()
            }
        })
    }

    private fun goHome() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    private fun loadGuardians() {
        val container = findViewById<LinearLayout>(R.id.guardianContainer)
        val llEmpty = findViewById<View>(R.id.llEmptyState)
        val db = AppDatabase.getDatabase(this)

        Thread {
            val contacts = db.contactDao().getAllContactsForUser(UserScope.uid())
            runOnUiThread {
                // Remove all views except the empty-state container
                val emptyState = container.findViewById<View>(R.id.llEmptyState)
                container.removeAllViews()
                if (emptyState != null) container.addView(emptyState)

                if (contacts.isEmpty()) {
                    llEmpty.visibility = View.VISIBLE
                } else {
                    llEmpty.visibility = View.GONE
                    for (contact in contacts) {
                        val row = layoutInflater.inflate(R.layout.item_guardian_row, container, false)

                        val tvInitial = row.findViewById<TextView>(R.id.tvInitial)
                        tvInitial.text = if (contact.name.isNotEmpty()) contact.name.first().uppercase() else "?"

                        row.findViewById<TextView>(R.id.tvGName).text = contact.name
                        row.findViewById<TextView>(R.id.tvGPhone).text = contact.phoneNumber
                        row.findViewById<TextView>(R.id.tvGMsg).text =
                            if (contact.customMessage.isNotEmpty()) contact.customMessage
                            else "Default SOS message will be sent."

                        row.findViewById<ImageButton>(R.id.btnDeleteGuardian).setOnClickListener {
                            confirmDelete(contact)
                        }

                        container.addView(row)
                    }
                }
            }
        }.start()
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_guardian, null)

        AlertDialog.Builder(this)
            .setTitle("Add New Guardian")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogView.findViewById<EditText>(R.id.etDName).text.toString().trim()
                val phone = dialogView.findViewById<EditText>(R.id.etDPhone).text.toString().trim()
                val msg = dialogView.findViewById<EditText>(R.id.etDMessage).text.toString().trim()

                if (name.isEmpty() || phone.length < 10) {
                    Toast.makeText(this, "Enter a valid name and phone", Toast.LENGTH_SHORT).show()
                } else {
                    saveGuardianLocally(name, phone, msg)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveGuardianLocally(name: String, phone: String, msg: String) {
        val db = AppDatabase.getDatabase(this)
        val newContact = Contact(name = name, phoneNumber = phone, customMessage = msg, userId = UserScope.uid())

        Thread {
            db.contactDao().insertContact(newContact)
            runOnUiThread {
                loadGuardians()
                Toast.makeText(this, "$name added!", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun confirmDelete(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle("Remove Guardian")
            .setMessage("Remove ${contact.name}?")
            .setPositiveButton("Delete") { _, _ ->
                val db = AppDatabase.getDatabase(this)
                Thread {
                    db.contactDao().deleteContact(contact)
                    runOnUiThread {
                        loadGuardians()
                        Toast.makeText(this, "Guardian removed", Toast.LENGTH_SHORT).show()
                    }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
