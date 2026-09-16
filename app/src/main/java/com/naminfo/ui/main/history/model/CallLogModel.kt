package com.naminfo.ui.main.history.model

import androidx.annotation.IntegerRes
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.R
import org.linphone.core.CallLog
import org.linphone.core.tools.Log
import com.naminfo.ui.main.contacts.model.ContactAvatarModel
import com.naminfo.utils.AppUtils
import com.naminfo.utils.LinphoneUtils
import com.naminfo.utils.TimestampUtils

class CallLogModel
    @WorkerThread
    constructor(private val callLog: CallLog) {
    companion object {
        private const val TAG = "[Call Log Model]"
    }

    val id = callLog.callId ?: callLog.refKey

    val timestamp = callLog.startDate

    val address = callLog.remoteAddress

    val sipUri = address.asStringUriOnly()

    val displayedAddress: String

    val avatarModel: ContactAvatarModel

    val wasConference: Boolean

    @IntegerRes
    val iconResId: Int

    val dateTime: String

    val friendRefKey: String?

    var friendExists: Boolean = false

    init {
        val date = if (TimestampUtils.isToday(timestamp)) {
            AppUtils.getString(R.string.today)
        } else if (TimestampUtils.isYesterday(timestamp)) {
            AppUtils.getString(R.string.yesterday)
        } else {
            TimestampUtils.toString(timestamp, onlyDate = true, shortDate = true, hideYear = true)
        }
        val time = TimestampUtils.timeToString(timestamp)
        dateTime = "$date | $time"

        wasConference = callLog.wasConference()
        if (wasConference) {
            val conferenceInfo = callLog.conferenceInfo
            if (conferenceInfo != null) {
                avatarModel = coreContext.contactsManager.getContactAvatarModelForConferenceInfo(
                    conferenceInfo
                )
            } else {
                Log.w("$TAG Failed to retrieve conference info attached to call log!")
                val fakeFriend = coreContext.core.createFriend()
                fakeFriend.address = address
                fakeFriend.name = LinphoneUtils.getDisplayName(address)
                avatarModel = ContactAvatarModel(fakeFriend)
                avatarModel.forceConferenceIcon.postValue(true)
            }

            friendRefKey = null
            friendExists = false
        } else {
            avatarModel = coreContext.contactsManager.getContactAvatarModelForAddress(address)
            val friend = avatarModel.friend
            friendRefKey = friend.refKey
            friendExists = coreContext.contactsManager.isContactAvailable(friend)
        }
        displayedAddress = address.username.orEmpty()

        iconResId = LinphoneUtils.getCallIconResId(callLog.status, callLog.dir)
    }

    @UiThread
    fun delete() {
        coreContext.postOnCoreThread { core ->
            core.removeCallLog(callLog)
        }
    }
}
