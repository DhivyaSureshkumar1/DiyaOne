package com.naminfo.ui.main.chat.model

import androidx.annotation.WorkerThread
import org.linphone.core.EventLog

class EventLogModel
    @WorkerThread
    constructor(
    val eventLog: EventLog,
    isFromGroup: Boolean = false,
    isGroupedWithPreviousOne: Boolean = false,
    isGroupedWithNextOne: Boolean = false,
    currentFilter: String = "",
    onContentClicked: ((fileModel: FileModel) -> Unit)? = null,
    onSipUriClicked: ((uri: String) -> Unit)? = null,
    onJoinConferenceClicked: ((uri: String) -> Unit)? = null,
    onWebUrlClicked: ((url: String) -> Unit)? = null,
    onContactClicked: ((friendRefKey: String) -> Unit)? = null,
    onRedToastToShow: ((pair: Pair<Int, Int>) -> Unit)? = null,
    onVoiceRecordingPlaybackEnded: ((id: String) -> Unit)? = null,
    onFileToExportToNativeGallery: ((path: String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "[Event Log Model]"
    }

    val type: EventLog.Type = eventLog.type

    val isEvent = type != EventLog.Type.ConferenceChatMessage

    val model: Any = if (isEvent) {
        EventModel(eventLog)
    } else {
        val chatMessage = eventLog.chatMessage!!
        MessageModel(
            chatMessage,
            isFromGroup,
            isGroupedWithPreviousOne,
            isGroupedWithNextOne,
            currentFilter,
            onContentClicked,
            onSipUriClicked,
            onJoinConferenceClicked,
            onWebUrlClicked,
            onContactClicked,
            onRedToastToShow,
            onVoiceRecordingPlaybackEnded,
            onFileToExportToNativeGallery
        )
    }

    val notifyId = eventLog.notifyId

    @WorkerThread
    fun destroy() {
        (model as? MessageModel)?.destroy()
    }
}
