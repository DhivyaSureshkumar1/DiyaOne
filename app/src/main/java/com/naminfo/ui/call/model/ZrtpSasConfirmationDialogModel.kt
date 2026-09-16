package com.naminfo.ui.call.model

import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import org.linphone.core.tools.Log
import com.naminfo.ui.GenericViewModel
import com.naminfo.utils.Event

class ZrtpSasConfirmationDialogModel
    @UiThread
    constructor(
    authTokenToRead: String,
    authTokensToListen: List<String>,
    val cacheMismatch: Boolean
) : GenericViewModel() {
    companion object {
        private const val TAG = "[ZRTP SAS Confirmation Dialog]"
    }

    val localToken = MutableLiveData<String>()
    val letters1 = MutableLiveData<String>()
    val letters2 = MutableLiveData<String>()
    val letters3 = MutableLiveData<String>()
    val letters4 = MutableLiveData<String>()

    val authTokenClickedEvent = MutableLiveData<Event<String>>()

    val skipEvent = MutableLiveData<Event<Boolean>>()

    init {
        localToken.value = authTokenToRead
        letters1.value = authTokensToListen[0]
        letters2.value = authTokensToListen[1]
        letters3.value = authTokensToListen[2]
        letters4.value = authTokensToListen[3]
    }

    @UiThread
    fun skip() {
        skipEvent.value = Event(true)
    }

    @UiThread
    fun notFound() {
        Log.e("$TAG User clicked on 'Not Found' button!")
        authTokenClickedEvent.value = Event("")
    }

    @UiThread
    fun lettersClicked(letters: MutableLiveData<String>) {
        val token = letters.value.orEmpty()
        Log.i("$TAG User clicked on [$token] letters")
        authTokenClickedEvent.value = Event(token)
    }
}
