package com.naminfo.ui.main.viewmodel

import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.naminfo.DiyaOneApplication.Companion.coreContext
import org.linphone.core.Account
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.GlobalState
import org.linphone.core.tools.Log
import com.naminfo.ui.GenericViewModel
import com.naminfo.utils.Event

open class DefaultAccountChangedViewModel : GenericViewModel() {
    companion object {
        private const val TAG = "[Default Account Changed ViewModel]"
    }

    val defaultAccountChangedEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }

    private val coreListener = object : CoreListenerStub() {
        @WorkerThread
        override fun onDefaultAccountChanged(core: Core, account: Account?) {
            defaultAccountChangedEvent.postValue(Event(true))
        }

        @WorkerThread
        override fun onGlobalStateChanged(core: Core, state: GlobalState?, message: String) {
            if (core.globalState == GlobalState.On) {
                Log.i("$TAG Global state is [${core.globalState}], reload default account")
                defaultAccountChangedEvent.postValue(Event(true))
            }
        }
    }

    init {
        coreContext.postOnCoreThread { core ->
            core.addListener(coreListener)
        }
    }

    override fun onCleared() {
        coreContext.postOnCoreThread { core ->
            core.removeListener(coreListener)
        }

        super.onCleared()
    }
}
