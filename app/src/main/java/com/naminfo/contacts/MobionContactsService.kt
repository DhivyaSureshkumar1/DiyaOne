package com.naminfo.contacts

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.StringReader
import org.linphone.core.tools.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object MobionContactsService {
    private const val TAG = "[Mobion Contacts Service]"

    private const val CONTACTS_URL =
        "https://mobionglobal.com/fs_webservice/WebService.asmx/Get_MobionNumber"

    private const val CONFERENCES_URL =
        "https://mobionglobal.com/fs_webservice/WebService.asmx/GroupSetting_View"

    data class Contact(val name: String, val mobileNumber: String)

    data class Conference(
        val id: String,
        val groupNumber: String,
        val groupName: String,
        val type: String
    )

    fun fetchContacts(): List<Contact> {
        val connection = URL(CONTACTS_URL).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.setRequestProperty("Accept", "text/xml")

        return try {
            if (connection.responseCode !in 200..299) {
                error("Mobion contacts request failed with HTTP ${connection.responseCode}")
            }

            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val responseXml = reader.readText()
                Log.i("$TAG Get_MobionNumber response:\n$responseXml")

                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(StringReader(responseXml))
                parseContacts(parser).also { contacts ->
                    Log.i("$TAG Parsed [${contacts.size}] valid contacts")
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    fun fetchConferences(registeredNumber: String): List<Conference> {
        val encodedNumber = URLEncoder.encode(registeredNumber, Charsets.UTF_8.name())
        val connection = URL("$CONFERENCES_URL?Number=$encodedNumber")
            .openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.setRequestProperty("Accept", "text/xml")

        return try {
            if (connection.responseCode !in 200..299) {
                error("Mobion conferences request failed with HTTP ${connection.responseCode}")
            }

            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val responseXml = reader.readText()
                Log.i("$TAG GroupSetting_View response:\n$responseXml")
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(StringReader(responseXml))
                parseConferences(parser).also { conferences ->
                    Log.i("$TAG Parsed [${conferences.size}] conferences for [$registeredNumber]")
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseContacts(parser: XmlPullParser): List<Contact> {
        val contacts = linkedMapOf<String, Contact>()
        var name = ""
        var mobileNumber = ""
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when {
                event == XmlPullParser.START_TAG && parser.name == "Table" -> {
                    name = ""
                    mobileNumber = ""
                }
                event == XmlPullParser.START_TAG && parser.name == "Name" -> {
                    name = parser.nextText().trim()
                }
                event == XmlPullParser.START_TAG && parser.name == "Mobile_No" -> {
                    mobileNumber = PhoneContactNumbers.normalize(parser.nextText())
                }
                event == XmlPullParser.END_TAG && parser.name == "Table" -> {
                    if (name.isNotBlank() && mobileNumber.length == 10) {
                        contacts[mobileNumber] = Contact(name, mobileNumber)
                    }
                }
            }
            event = parser.next()
        }
        return contacts.values.toList()
    }

    private fun parseConferences(parser: XmlPullParser): List<Conference> {
        val conferences = linkedMapOf<String, Conference>()
        var id = ""
        var groupNumber = ""
        var groupName = ""
        var type = ""
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when {
                event == XmlPullParser.START_TAG && parser.name == "Table" -> {
                    id = ""
                    groupNumber = ""
                    groupName = ""
                    type = ""
                }
                event == XmlPullParser.START_TAG && parser.name == "Id" -> {
                    id = parser.nextText().trim()
                }
                event == XmlPullParser.START_TAG && parser.name == "Groupnumber" -> {
                    groupNumber = parser.nextText().filter(Char::isDigit)
                }
                event == XmlPullParser.START_TAG && parser.name == "Group_Name" -> {
                    groupName = parser.nextText().trim()
                }
                event == XmlPullParser.START_TAG && parser.name == "conference" -> {
                    type = parser.nextText().trim()
                }
                event == XmlPullParser.END_TAG && parser.name == "Table" -> {
                    if (groupNumber.isNotEmpty() && groupName.isNotEmpty()) {
                        val key = id.ifEmpty { groupNumber }
                        conferences[key] = Conference(key, groupNumber, groupName, type)
                    }
                }
            }
            event = parser.next()
        }
        return conferences.values.toList()
    }
}
