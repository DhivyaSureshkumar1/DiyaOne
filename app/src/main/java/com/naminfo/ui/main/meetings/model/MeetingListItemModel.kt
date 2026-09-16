package com.naminfo.ui.main.meetings.model

import androidx.annotation.WorkerThread
import com.naminfo.utils.TimestampUtils

class MeetingListItemModel
    @WorkerThread
    constructor(
    meetingModel: MeetingModel?,
    val firstMeetingOfTheWeek: Boolean
) {
    val isToday = meetingModel == null || meetingModel.isToday

    val isTodayIndicator = meetingModel == null

    val month = meetingModel?.month ?: TimestampUtils.month(System.currentTimeMillis(), false)

    val day = meetingModel?.day ?: TimestampUtils.dayOfWeek(System.currentTimeMillis(), false)

    val dayNumber = meetingModel?.dayNumber ?: TimestampUtils.dayOfMonth(
        System.currentTimeMillis(),
        false
    )

    val weekLabel = meetingModel?.weekLabel ?: TimestampUtils.firstAndLastDayOfWeek(
        System.currentTimeMillis(),
        false
    )

    val model = meetingModel ?: TodayModel()

    init {
        meetingModel?.firstMeetingOfTheWeek?.postValue(firstMeetingOfTheWeek)
    }

    class TodayModel
}
