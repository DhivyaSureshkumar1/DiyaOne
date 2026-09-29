package com.naminfo.utils

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.R
import com.naminfo.ui.main.MainActivity
import org.linphone.core.Address

class PhoneContactEditor(private val fragment: Fragment) {
    // Register during fragment construction so results survive recreation.
    private val launcher = fragment.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val activity = fragment.activity as? MainActivity
        if (activity != null && ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Some contact editors return RESULT_CANCELED even after saving.
            // Reload actual provider data instead of assuming a result means saved.
            coreContext.contactsManager.loadContacts(activity)
        }
    }

    fun open(address: Address, displayName: String) {
        val username = address.username.orEmpty()
        val sipUri = address.asStringUriOnly()
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            putExtra("finishActivityOnSaveCompleted", true)
            if (displayName.isNotBlank() && displayName != username && displayName != sipUri) {
                putExtra(ContactsContract.Intents.Insert.NAME, displayName)
            }
            if (username.matches(Regex("[+]?[0-9]+"))) {
                putExtra(ContactsContract.Intents.Insert.PHONE, username)
            }
            val sipRow = ContentValues().apply {
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE)
                put(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS, sipUri.removePrefix("sip:").removePrefix("sips:"))
                put(ContactsContract.CommonDataKinds.SipAddress.TYPE, ContactsContract.CommonDataKinds.SipAddress.TYPE_OTHER)
            }
            putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, arrayListOf(sipRow))
        }
        try {
            launcher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            showUnavailable()
        } catch (_: SecurityException) {
            showUnavailable()
        }
    }

    private fun showUnavailable() {
        Toast.makeText(fragment.requireContext(), R.string.phone_contact_editor_unavailable, Toast.LENGTH_LONG).show()
    }
}
