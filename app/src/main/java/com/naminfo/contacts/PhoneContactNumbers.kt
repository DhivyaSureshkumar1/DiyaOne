// Matching only: never use the normalized key to rewrite a SIP destination.

package com.naminfo.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract

object PhoneContactNumbers {
    fun normalize(number: String): String {
        val digits = number.filter(Char::isDigit)
        return when {
            digits.length == 14 && digits.startsWith("0091") -> digits.drop(4)
            digits.length == 12 && digits.startsWith("91") -> digits.drop(2)
            digits.length == 11 && digits.startsWith("0") -> digits.drop(1)
            else -> digits
        }
    }

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED

    // Call on Dispatchers.IO. This function owns and closes its own cursor.
    fun read(context: Context): Set<String> {
        if (!hasPermission(context)) {
            throw SecurityException("Contacts permission required")
        }

        val numbers = mutableSetOf<String>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            null,
            null,
            null
        ) ?: error("Unable to read phone contacts")

        cursor.use {
            val column = it.getColumnIndexOrThrow(
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            while (it.moveToNext()) {
                val number = normalize(it.getString(column).orEmpty())
                if (number.isNotEmpty()) numbers.add(number)
            }
        }
        return numbers
    }
}
