package com.naminfo.ui.main.contacts.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.utils.Event
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.linphone.core.Core
import org.linphone.core.RegistrationState
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class ConferenceCallViewModel : ViewModel() {
    val inProgress = MutableLiveData(false)
    val errorEvent = MutableLiveData<Event<String>>()

    private data class Caller(
        val identity: String,
        val number: String,
        val domain: String
    )

    fun sendToConnect(selectedNumbers: List<String>, video: Boolean) {
        if (inProgress.value == true) return
        inProgress.value = true

        viewModelScope.launch {
            try {
                val caller = onCoreThread { core ->
                    val account = core.defaultAccount
                        ?: error("Please sign in first")

                    check(account.state == RegistrationState.Ok) {
                        "Wait until your account is registered"
                    }

                    val identity = account.params.identityAddress
                        ?: error("Account SIP address is missing")

                    val number = identity.username.orEmpty()
                    val domain = identity.domain.orEmpty()

                    check(number.isNotBlank() && domain.isNotBlank()) {
                        "Account number or SIP domain is missing"
                    }

                    Caller(identity.asStringUriOnly(), number, domain)
                }

                val participants = selectedNumbers
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .distinct()
                    .filterNot {
                        matchingNumber(it) == matchingNumber(caller.number)
                    }

                check(participants.isNotEmpty()) {
                    "Select at least one other contact"
                }

                val conferenceId =
                    caller.number + if (video) "_tcvideo" else "_tcaudio"

                val response = withContext(Dispatchers.IO) {
                    postConference(
                        domain = caller.domain,
                        from = caller.number,
                        to = participants.joinToString(","),
                        conferenceId = conferenceId
                    )
                }

                check(response.equals("Insert successful", ignoreCase = true)) {
                    response.ifBlank {
                        "The conference API returned an empty response"
                    }
                }

                onCoreThread { core ->
                    val account = core.defaultAccount
                        ?: error("You signed out before the conference started")

                    val identity = account.params.identityAddress
                        ?: error("Account SIP address is missing")

                    check(
                        identity.asStringUriOnly() == caller.identity &&
                                account.state == RegistrationState.Ok
                    ) {
                        "Your account changed or disconnected. Start again."
                    }

                    // Clone the identity to preserve its SIP domain.
                    val destination = identity.clone()
                    destination.username = conferenceId

                    if (video) {
                        coreContext.startVideoCall(
                            destination,
                            localAddress = identity
                        )
                    } else {
                        coreContext.startAudioCall(
                            destination,
                            localAddress = identity
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorEvent.value = Event(
                    e.message ?: "Unable to start the conference"
                )
            } finally {
                inProgress.value = false
            }
        }
    }

    private fun matchingNumber(value: String): String {
        val digits = value.filter(Char::isDigit)
        return when {
            digits.length == 14 && digits.startsWith("0091") ->
                digits.drop(4)
            digits.length == 12 && digits.startsWith("91") ->
                digits.drop(2)
            digits.length == 11 && digits.startsWith("0") ->
                digits.drop(1)
            else -> digits
        }
    }

    private fun postConference(
        domain: String,
        from: String,
        to: String,
        conferenceId: String
    ): String {
        val baseUrl = when (domain) {
            "203.95.216.68" -> "https://www.mobionglobal.com"
            "103.16.202.169" -> "https://fswebrtc.co.in"
            else -> "http://$domain"
        }

        val parameters = linkedMapOf(
            "From" to from,
            "To" to to,
            "ConferenceId" to conferenceId
        )

        val body = parameters.entries.joinToString("&") {
            "${URLEncoder.encode(it.key, "UTF-8")}=" +
                    URLEncoder.encode(it.value, "UTF-8")
        }.toByteArray(Charsets.UTF_8)

        val connection = URL(
            "$baseUrl/fs_webservice/WebService.asmx/TakeConference"
        ).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8"
            )
            connection.setFixedLengthStreamingMode(body.size)

            connection.outputStream.use { it.write(body) }

            check(connection.responseCode in 200..299) {
                "Conference API failed: HTTP ${connection.responseCode}"
            }

            val xml = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val parser = XmlPullParserFactory.newInstance()
                .newPullParser()
            parser.setInput(xml.reader())

            val result = StringBuilder()
            var event = parser.eventType

            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.TEXT) {
                    val text = parser.text.trim()
                    if (text.isNotEmpty()) {
                        if (result.isNotEmpty()) result.append(' ')
                        result.append(text)
                    }
                }
                event = parser.next()
            }

            return result.toString().trim()
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun <T> onCoreThread(block: (Core) -> T): T =
        suspendCancellableCoroutine { continuation ->
            coreContext.postOnCoreThread { core ->
                if (continuation.isActive) {
                    continuation.resumeWith(runCatching { block(core) })
                }
            }
        }
}
