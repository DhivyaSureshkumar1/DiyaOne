package com.naminfo.ui.main.meetings.viewmodel

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import java.util.TimeZone
import com.naminfo.DiyaOneApplication.Companion.coreContext
import org.linphone.core.ConferenceInfo
import org.linphone.core.Factory
import org.linphone.core.Participant
import org.linphone.core.tools.Log
import com.naminfo.ui.main.meetings.model.ParticipantModel
import com.naminfo.ui.main.meetings.model.TimeZoneModel
import com.naminfo.utils.Event
import com.naminfo.utils.TimestampUtils

class MeetingViewModel
    @UiThread
    constructor() : CancelMeetingViewModel() {
    companion object {
        private const val TAG = "[Meeting ViewModel]"
    }

    val showBackButton = MutableLiveData<Boolean>()

    val isBroadcast = MutableLiveData<Boolean>()

    val isEditable = MutableLiveData<Boolean>()

    val subject = MutableLiveData<String>()

    val sipUri = MutableLiveData<String>()

    val dateTime = MutableLiveData<String>()

    val timezone = MutableLiveData<String>()

    val hasNotStartedYet = MutableLiveData<Boolean>()

    val description = MutableLiveData<String>()

    val speakers = MutableLiveData<ArrayList<ParticipantModel>>()

    val participants = MutableLiveData<ArrayList<ParticipantModel>>()

    val isCancelled = MutableLiveData<Boolean>()

    val conferenceInfoFoundEvent = MutableLiveData<Event<Boolean>>()

    val startTimeStamp = MutableLiveData<Long>()
    val endTimeStamp = MutableLiveData<Long>()

    val conferenceInfoDeletedEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }

    private lateinit var conferenceInfo: ConferenceInfo

    @UiThread
    fun findConferenceInfo(meeting: ConferenceInfo?, uri: String) {
        coreContext.postOnCoreThread { core ->
            if (meeting != null && ::conferenceInfo.isInitialized && meeting == conferenceInfo) {
                Log.i("$TAG ConferenceInfo object already in memory, skipping")
                conferenceInfoFoundEvent.postValue(Event(true))
                return@postOnCoreThread
            }

            val address = Factory.instance().createAddress(uri)

            if (meeting != null && (!::conferenceInfo.isInitialized || conferenceInfo != meeting)) {
                if (address != null && meeting.uri?.equal(address) == true) {
                    Log.i("$TAG ConferenceInfo object available in sharedViewModel, using it")
                    conferenceInfo = meeting
                    configureConferenceInfo()
                    conferenceInfoFoundEvent.postValue(Event(true))
                    return@postOnCoreThread
                }
            }

            if (address != null) {
                val found = core.findConferenceInformationFromUri(address)
                if (found != null) {
                    Log.i("$TAG Conference info with SIP URI [$uri] was found")
                    conferenceInfo = found
                    configureConferenceInfo()
                    conferenceInfoFoundEvent.postValue(Event(true))
                } else {
                    Log.e("$TAG Conference info with SIP URI [$uri] couldn't be found!")
                    conferenceInfoFoundEvent.postValue(Event(false))
                }
            } else {
                Log.e("$TAG Failed to parse SIP URI [$uri] as Address!")
                conferenceInfoFoundEvent.postValue(Event(false))
            }
        }
    }

    @UiThread
    fun delete() {
        coreContext.postOnCoreThread { core ->
            if (::conferenceInfo.isInitialized) {
                Log.i("$TAG Deleting conference information [$conferenceInfo]")
                core.deleteConferenceInformation(conferenceInfo)
                conferenceInfoDeletedEvent.postValue(Event(true))
            }
        }
    }

    @UiThread
    fun refreshInfo(uri: String) {
        coreContext.postOnCoreThread { core ->
            Log.i("$TAG Looking for conference info with URI [$uri]")
            val address = Factory.instance().createAddress(uri)
            if (address != null) {
                val found = core.findConferenceInformationFromUri(address)
                if (found != null) {
                    Log.i("$TAG Conference info with SIP address [${address.asStringUriOnly()}] was found, updating info")
                    conferenceInfo = found
                    configureConferenceInfo()
                } else {
                    Log.w("$TAG No conference info with SIP address [${address.asStringUriOnly()}] was found")
                }
            } else {
                Log.e("$TAG Failed to parse URI [$uri] as a SIP address!")
            }
        }
    }

    @WorkerThread
    private fun configureConferenceInfo() {
        if (::conferenceInfo.isInitialized) {
            subject.postValue(conferenceInfo.subject)
            sipUri.postValue(conferenceInfo.uri?.asStringUriOnly() ?: "")
            description.postValue(conferenceInfo.description)

            val state = conferenceInfo.state
            Log.i("$TAG Conference info is in state [$state]")
            isCancelled.postValue(state == ConferenceInfo.State.Cancelled)

            val timestamp = conferenceInfo.dateTime
            val duration = conferenceInfo.duration
            val date = TimestampUtils.toString(
                timestamp,
                onlyDate = true,
                shortDate = false,
                hideYear = false
            )
            val startTime = TimestampUtils.timeToString(timestamp)
            val end = timestamp + (duration * 60)
            val endTime = TimestampUtils.timeToString(end)
            startTimeStamp.postValue(timestamp * 1000)
            endTimeStamp.postValue(end * 1000)
            val displayedTimestamp = "$date | $startTime - $endTime"
            dateTime.postValue(displayedTimestamp)
            hasNotStartedYet.postValue(timestamp * 1000 > System.currentTimeMillis())
            Log.i("$TAG Conference is scheduled for [$displayedTimestamp]")

            timezone.postValue(TimeZoneModel(TimeZone.getDefault()).toString())

            val organizerAddress = conferenceInfo.organizer
            if (organizerAddress != null) {
                val localAccount = coreContext.core.accountList.find { account ->
                    val address = account.params.identityAddress
                    address != null && organizerAddress.weakEqual(address)
                }
                val canMeetingBeEdited = localAccount != null
                isEditable.postValue(canMeetingBeEdited)
                Log.i("$TAG Conference organizer is [${organizerAddress.asStringUriOnly()}], we [${if (canMeetingBeEdited) "can" else "can't"}] edit it")
            } else {
                isEditable.postValue(false)
                Log.e(
                    "$TAG No organizer SIP URI found for: ${conferenceInfo.uri?.asStringUriOnly()}"
                )
            }

            computeParticipantsList()
        }
    }

    private fun computeParticipantsList() {
        val speakersList = arrayListOf<ParticipantModel>()
        val participantsList = arrayListOf<ParticipantModel>()

        var allSpeaker = true
        val organizer = conferenceInfo.organizer
        var organizerFound = false
        val participantsInfo = conferenceInfo.participantInfos
        Log.i("$TAG Found [${participantsInfo.size}] participants information")
        for (info in participantsInfo) {
            val participant = info.address
            val isOrganizer = organizer?.weakEqual(participant) == true
            Log.d(
                "$TAG Conference [${conferenceInfo.subject}] [${if (isOrganizer) "organizer" else "participant"}] [${participant.asStringUriOnly()}] is a [${info.role}]"
            )
            if (isOrganizer) {
                organizerFound = true
            }

            if (info.role == Participant.Role.Listener) {
                allSpeaker = false
                participantsList.add(ParticipantModel(participant, isOrganizer))
            } else {
                speakersList.add(ParticipantModel(participant, isOrganizer))
            }
        }
        Log.i(
            "$TAG Found [${speakersList.size}] speakers for conference [${conferenceInfo.uri?.asStringUriOnly()}] and [${participantsList.size}] listeners"
        )

        if (allSpeaker) {
            Log.i("$TAG All participants have Speaker role, considering it is a meeting")
            participantsList.addAll(speakersList)
        }

        if (!organizerFound && organizer != null) {
            Log.i("$TAG Organizer not found in participants list, adding it to participants list")
            participantsList.add(ParticipantModel(organizer, true))
        }

        isBroadcast.postValue(!allSpeaker)
        speakers.postValue(speakersList)
        participants.postValue(participantsList)
    }

    @UiThread
    fun cancel(sendNotification: Boolean) {
        if (::conferenceInfo.isInitialized) {
            cancelMeeting(conferenceInfo, sendNotification)
        }
    }
}
