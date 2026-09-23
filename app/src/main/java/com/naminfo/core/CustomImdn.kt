package com.naminfo.core

import org.json.JSONObject
import org.linphone.core.ChatMessage
import org.linphone.core.ChatMessageListenerStub
import org.linphone.core.ChatRoom
import org.linphone.core.tools.Log
import java.util.UUID

/**
 * Initial implementation for one-to-one basic text chat.
 * Call all methods on the Linphone core thread.
 *
 * appdata is reserved for this helper. The inspected project
 * does not currently use it.
 */
object CustomImdn {
    private const val HEADER = "X-Diya-Message-ID"
    private const val META_PREFIX = "diya-imdn-v1:"
    private const val RECEIPT_PREFIX = "DIYA_RECEIPT_V1\n"

    private val pending = mutableSetOf<String>()
    private val observers =
        mutableSetOf<(String, ChatMessage.State) -> Unit>()

    private fun supported(room: ChatRoom): Boolean =
        room.hasCapability(ChatRoom.Capabilities.Basic.toInt()) &&
                !room.hasCapability(ChatRoom.Capabilities.Encrypted.toInt())

    private fun metadata(message: ChatMessage): JSONObject {
        val raw = message.appdata.orEmpty()

        if (raw.isEmpty()) return JSONObject()

        require(raw.startsWith(META_PREFIX)) {
            "Message appdata already belongs to another feature"
        }

        return JSONObject(raw.removePrefix(META_PREFIX))
    }

    private fun save(message: ChatMessage, data: JSONObject) {
        message.appdata = META_PREFIX + data.toString()
    }

    private fun validId(value: String?): String? {
        if (value == null) return null
        return runCatching {
            UUID.fromString(value).toString().takeIf {
                it.equals(value, ignoreCase = true)
            }
        }.getOrNull()
    }

    fun id(message: ChatMessage): String? =
        validId(metadata(message).optString("id"))

    private fun key(room: ChatRoom, id: String): String =
        "${room.localAddress.asStringUriOnly()}|" +
                "${room.peerAddress.asStringUriOnly()}|$id"

    fun key(message: ChatMessage): String? =
        id(message)?.let { key(message.chatRoom, it) }

    fun isReceipt(message: ChatMessage?): Boolean =
        message?.utf8Text?.startsWith(RECEIPT_PREFIX) == true

    /**
     * Call before sending an original message.
     * Reuses its ID when the same message is retried.
     */
    fun prepareOutgoing(message: ChatMessage) {
        if (!supported(message.chatRoom) || isReceipt(message)) return

        val data = metadata(message)
        val messageId = validId(data.optString("id"))
            ?: UUID.randomUUID().toString()

        data.put("id", messageId)
        save(message, data)

        message.removeCustomHeader(HEADER)
        message.addCustomHeader(HEADER, messageId)

        Log.i("[Custom IMDN] Sending original id=$messageId")
    }

    fun addObserver(observer: (String, ChatMessage.State) -> Unit) {
        observers.add(observer)
    }

    fun removeObserver(observer: (String, ChatMessage.State) -> Unit) {
        observers.remove(observer)
    }

    fun effectiveState(message: ChatMessage): ChatMessage.State {
        if (!message.isOutgoing) return message.state

        return when (metadata(message).optString("receivedStatus")) {
            "displayed" -> ChatMessage.State.Displayed
            "delivered" -> {
                if (message.state == ChatMessage.State.Displayed) {
                    ChatMessage.State.Displayed
                } else {
                    ChatMessage.State.DeliveredToUser
                }
            }
            else -> message.state
        }
    }

    /**
     * Process receipts before processing ordinary incoming messages.
     * Receipt messages never generate another receipt.
     */
    fun receive(room: ChatRoom, message: ChatMessage) {
        if (message.isOutgoing || !supported(room)) return

        if (isReceipt(message)) {
            try {
                consumeReceipt(room, message)
            } catch (e: Exception) {
                Log.w("[Custom IMDN] Invalid receipt: ${e.message}")
            } finally {
                // Delete only the control message, not the original.
                room.deleteMessage(message)
            }
            return
        }

        val messageId = validId(message.getCustomHeader(HEADER))
        if (messageId == null) {
            Log.w("[Custom IMDN] Original message has no valid custom ID")
            return
        }

        val data = metadata(message)
        data.put("id", messageId)
        save(message, data)

        sendReceipt(message, "delivered")

        // Handles messages already marked read before this callback.
        if (message.isRead) {
            sendReceipt(message, "displayed")
        }
    }

    private fun consumeReceipt(room: ChatRoom, receipt: ChatMessage) {
        if (!receipt.fromAddress.weakEqual(room.peerAddress)) return

        val text = receipt.utf8Text.orEmpty()
        require(text.length <= 2048) { "Receipt too large" }

        val body = JSONObject(text.removePrefix(RECEIPT_PREFIX))
        require(body.optInt("version") == 1)

        val messageId = validId(body.optString("messageId")) ?: return
        val status = body.optString("status")
        if (status != "delivered" && status != "displayed") return

        // Match only an outgoing message in this account/peer chat.
        val original = room.getHistoryMessageEvents(0)
            .mapNotNull { it.chatMessage }
            .firstOrNull {
                it.isOutgoing && !isReceipt(it) && id(it) == messageId
            }

        if (original == null) {
            Log.w("[Custom IMDN] No outgoing match for id=$messageId")
            return
        }

        val data = metadata(original)
        val previous = data.optString("receivedStatus")

        // Ignore duplicates and prevent displayed -> delivered regression.
        if (previous == "displayed" || previous == status) return

        data.put("receivedStatus", status)
        save(original, data)

        val newState = effectiveState(original)
        val messageKey = key(room, messageId)

        observers.toList().forEach {
            it(messageKey, newState)
        }

        Log.i("[Custom IMDN] Matched id=$messageId status=$status")
    }

    /**
     * Called after a conversation is marked read.
     * Persisted IDs allow read receipts after restarting the app.
     */
    fun roomRead(room: ChatRoom) {
        if (!supported(room)) return

        for (event in room.getHistoryMessageEvents(0)) {
            val message = event.chatMessage ?: continue
            if (
                !message.isOutgoing &&
                !isReceipt(message) &&
                message.isRead &&
                id(message) != null
            ) {
                sendReceipt(message, "displayed")
            }
        }
    }

    fun messageRead(message: ChatMessage) {
        if (
            !message.isOutgoing &&
            !isReceipt(message) &&
            message.isRead &&
            supported(message.chatRoom)
        ) {
            sendReceipt(message, "displayed")
        }
    }

    private fun sendReceipt(original: ChatMessage, status: String) {
        val messageId = id(original) ?: return
        val flag = "sent_$status"

        if (metadata(original).optBoolean(flag)) return

        val pendingKey = "${key(original)}|$status"
        if (!pending.add(pendingKey)) return

        val body = JSONObject()
            .put("version", 1)
            .put("messageId", messageId)
            .put("status", status)
            .toString()

        val receipt = original.chatRoom.createMessageFromUtf8(
            RECEIPT_PREFIX + body
        )

        // No original-message header: receipts cannot request receipts.
        receipt.setToBeStored(false)

        val listener = object : ChatMessageListenerStub() {
            override fun onMsgStateChanged(
                message: ChatMessage,
                state: ChatMessage.State?
            ) {
                when (state) {
                    ChatMessage.State.Delivered -> {
                        // Means FS accepted this outgoing SIP MESSAGE.
                        val data = metadata(original)
                        data.put(flag, true)
                        save(original, data)

                        pending.remove(pendingKey)
                        message.removeListener(this)
                        // This SDK may still store the outgoing control message.
                        message.chatRoom.deleteMessage(message)
                        Log.i(
                            "[Custom IMDN] FS accepted " +
                                    "id=$messageId status=$status"
                        )
                    }

                    ChatMessage.State.NotDelivered -> {
                        pending.remove(pendingKey)
                        message.removeListener(this)
                        message.chatRoom.deleteMessage(message)
                        Log.w(
                            "[Custom IMDN] Receipt send failed " +
                                    "id=$messageId status=$status"
                        )
                    }

                    else -> {}
                }
            }
        }

        receipt.addListener(listener)

        try {
            Log.i("[Custom IMDN] Sending id=$messageId status=$status")
            receipt.send()
        } catch (e: Exception) {
            pending.remove(pendingKey)
            receipt.removeListener(listener)
            Log.w("[Custom IMDN] Receipt send failed: ${e.message}")
        }
    }
}
