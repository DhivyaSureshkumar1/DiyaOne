package com.naminfo.ui.main.settings.viewmodel

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import java.util.Locale
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.DiyaOneApplication.Companion.corePreferences
import com.naminfo.R
import org.linphone.core.Account
import org.linphone.core.AccountDevice
import org.linphone.core.AccountManagerServices
import org.linphone.core.AccountManagerServicesRequest
import org.linphone.core.AccountManagerServicesRequestListenerStub
import org.linphone.core.Address
import org.linphone.core.Dictionary
import org.linphone.core.tools.Log
import com.naminfo.ui.GenericViewModel
import com.naminfo.ui.main.model.AccountModel
import com.naminfo.ui.main.model.isEndToEndEncryptionMandatory
import com.naminfo.ui.main.settings.model.AccountDeviceModel
import com.naminfo.utils.Event

class AccountProfileViewModel
    @UiThread
    constructor() : GenericViewModel() {
    companion object {
        private const val TAG = "[Account Profile ViewModel]"
    }

    val accountModel = MutableLiveData<AccountModel>()

    val sipAddress = MutableLiveData<String>()

    val displayName = MutableLiveData<String>()

    val registerEnabled = MutableLiveData<Boolean>()

    val isCurrentlySelectedModeSecure = MutableLiveData<Boolean>()

    val devices = MutableLiveData<ArrayList<AccountDeviceModel>>()

    val accountFoundEvent = MutableLiveData<Event<Boolean>>()

    val expandDetails = MutableLiveData<Boolean>()

    val expandDevices = MutableLiveData<Boolean>()

    val isOnDefaultDomain = MutableLiveData<Boolean>()

    val emptyDevices = MediatorLiveData<Boolean>()

    val devicesFetchInProgress = MutableLiveData<Boolean>()

    val hideAccountSettings = MutableLiveData<Boolean>()

    val hideSipAddresses = MutableLiveData<Boolean>()

    val deviceId = MutableLiveData<String>()

    val showDeviceId = MutableLiveData<Boolean>()

    val accountRemovedEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }

    private lateinit var account: Account

    private lateinit var accountManagerServices: AccountManagerServices

    private val accountManagerServicesListener = object : AccountManagerServicesRequestListenerStub() {
        @WorkerThread
        override fun onDevicesListFetched(
            request: AccountManagerServicesRequest,
            accountDevices: Array<out AccountDevice>
        ) {
            Log.i("$TAG Fetched [${accountDevices.size}] devices for our account")
            val devicesList = arrayListOf<AccountDeviceModel>()
            for (accountDevice in accountDevices) {
                devicesList.add(
                    AccountDeviceModel(accountDevice) { model, device ->
                        if (::accountManagerServices.isInitialized) {
                            val identityAddress = account.params.identityAddress
                            if (identityAddress != null) {
                                Log.i(
                                    "$TAG Removing device with name [${device.name}] and uuid [${device.uuid}]"
                                )
                                val deleteRequest = accountManagerServices.createDeleteDeviceRequest(
                                    identityAddress,
                                    device
                                )
                                deleteRequest.addListener(this)
                                deleteRequest.submit()

                                val newList = arrayListOf<AccountDeviceModel>()
                                newList.addAll(devices.value.orEmpty())
                                newList.remove(model)
                                devices.postValue(newList)
                            } else {
                                Log.e("$TAG Account identity address is null, can't delete device!")
                            }
                        }
                    }
                )
            }
            devices.postValue(devicesList)
            devicesFetchInProgress.postValue(false)
        }

        override fun onRequestSuccessful(request: AccountManagerServicesRequest, data: String?) {
            if (request.type == AccountManagerServicesRequest.Type.DeleteDevice) {
                Log.i("$TAG Device successfully deleted: $data")
            }
        }

        @WorkerThread
        override fun onRequestError(
            request: AccountManagerServicesRequest,
            statusCode: Int,
            errorMessage: String?,
            parameterErrors: Dictionary?
        ) {
            Log.e(
                "$TAG Request [${request.type}] returned an error with status code [$statusCode] and message [$errorMessage]"
            )
            if (!errorMessage.isNullOrEmpty()) {
                when (request.type) {
                    AccountManagerServicesRequest.Type.GetDevicesList, AccountManagerServicesRequest.Type.DeleteDevice -> {
                        showFormattedRedToast(errorMessage, R.drawable.warning_circle)
                        devicesFetchInProgress.postValue(false)
                    }
                    else -> {}
                }
            }
        }
    }

    init {
        expandDetails.value = true
        expandDevices.value = false
        showDeviceId.value = false
        devicesFetchInProgress.value = true
        isOnDefaultDomain.value = false

        emptyDevices.value = true
        emptyDevices.addSource(devices) { list ->
            emptyDevices.value = list.orEmpty().isEmpty()
        }

        coreContext.postOnCoreThread {
            hideAccountSettings.postValue(corePreferences.hideAccountSettings)
            hideSipAddresses.postValue(corePreferences.hideSipAddresses)
        }
    }

    @UiThread
    override fun onCleared() {
        super.onCleared()

        coreContext.postOnCoreThread {
            accountModel.value?.destroy()
        }
    }

    @UiThread
    fun findAccountMatchingIdentity(identity: String) {
        coreContext.postOnCoreThread { core ->
            val found = core.accountList.find {
                it.params.identityAddress?.asStringUriOnly() == identity
            }
            if (found != null) {
                Log.i("$TAG Found matching account [$found]")
                account = found
                accountModel.postValue(AccountModel(account))
                isCurrentlySelectedModeSecure.postValue(isEndToEndEncryptionMandatory())
                registerEnabled.postValue(account.params.isRegisterEnabled)

                sipAddress.postValue(account.params.identityAddress?.asStringUriOnly())
                displayName.postValue(account.params.identityAddress?.displayName)
                showDeviceId.postValue(false)

                val identityAddress = account.params.identityAddress
                if (identityAddress != null) {
                    val domain = identityAddress.domain
                    val defaultDomain = corePreferences.defaultDomain
                    isOnDefaultDomain.postValue(domain == defaultDomain)
                    if (domain == defaultDomain) {
                        requestDevicesList(identityAddress)
                    } else {
                        Log.i(
                            "$TAG Account with domain [$domain] can't get devices list, only works with [$defaultDomain] domain"
                        )
                    }
                } else {
                    Log.e("$TAG No identity address found!")
                }

                deviceId.postValue(account.contactAddress?.getUriParam("gr"))

                accountFoundEvent.postValue(Event(true))
            } else {
                accountFoundEvent.postValue(Event(false))
            }
        }
    }

    @UiThread
    fun deleteAccount() {
        coreContext.postOnCoreThread { core ->
            if (::account.isInitialized) {
                val identity = account.params.identityAddress?.asStringUriOnly()
                Log.i("$TAG Signing out account [$identity], retaining local history and profile")
                val profileSection = "saved_profile_$identity"
                core.config.setString(profileSection, "display_name", account.params.identityAddress?.displayName)
                core.config.setString(profileSection, "picture_uri", account.params.pictureUri)
                val authInfo = account.findAuthInfo()
                core.removeAccount(account)
                if (authInfo != null && core.accountList.none { it.findAuthInfo() == authInfo }) {
                    core.removeAuthInfo(authInfo)
                }
                core.config.sync()
                accountRemovedEvent.postValue(Event(true))

                if (core.accountList.isEmpty()) {
                    Log.w("$TAG No more account found in Core")
                    if (!core.provisioningUri.isNullOrEmpty()) {
                        Log.w("$TAG Removing remote provisioning URI")
                        core.provisioningUri = null
                    }
                }
            }
        }
    }

    @UiThread
    fun setNewPicturePath(path: String) {
        coreContext.postOnCoreThread {
            if (::account.isInitialized) {
                val params = account.params
                val copy = params.clone()

                if (path.isNotEmpty() && path != params.pictureUri) {
                    Log.i("$TAG New account profile picture [$path]")
                    copy.pictureUri = path
                } else {
                    Log.i("$TAG Account profile picture removed")
                    copy.pictureUri = null
                }

                // Create a new model to force UI to update
                val newModel = AccountModel(account)
                newModel.picturePath.postValue(path)
                accountModel.postValue(newModel)

                // Also update friend & contact avatar model for ourselves
                val model = coreContext.contactsManager.getContactAvatarModelForAddress(
                    params.identityAddress
                )
                model.friend.photo = path
                model.picturePath.postValue(path)

                account.params = copy
                account.refreshRegister()
            }
        }
    }

    @UiThread
    fun saveChangesWhenLeaving() {
        coreContext.postOnCoreThread {
            if (::account.isInitialized) {
                val params = account.params
                val copy = params.clone()

                val address = params.identityAddress?.clone()
                if (address != null) {
                    val newValue = displayName.value.orEmpty().trim()
                    address.displayName = newValue
                    copy.identityAddress = address
                    // This will trigger a REGISTER, so account display name will be updated by
                    // CoreListener.onAccountRegistrationStateChanged everywhere in the app
                    Log.i(
                        "$TAG Updated account [${params.identityAddress?.asStringUriOnly()}] identity address display name [$newValue]"
                    )
                }

                account.params = copy
                account.refreshRegister()
            }
        }
    }

    @UiThread
    fun toggleDetailsExpand() {
        expandDetails.value = expandDetails.value == false
    }

    @UiThread
    fun toggleDevicesExpand() {
        expandDevices.value = expandDevices.value == false
    }

    @UiThread
    fun toggleRegister() {
        coreContext.postOnCoreThread { core ->
            val params = account.params
            val copy = params.clone()
            copy.isRegisterEnabled = !params.isRegisterEnabled
            Log.i(
                "$TAG Account registration is now [${if (copy.isRegisterEnabled) "enabled" else "disabled"}] for account [${account.params.identityAddress?.asStringUriOnly()}]"
            )
            account.params = copy
            registerEnabled.postValue(account.params.isRegisterEnabled)

            if (!core.isNetworkReachable) {
                Log.w("$TAG Network is not reachable, updating registration state to reflect that")
                // To reflect the difference between Disabled & Disconnected
                accountModel.value?.updateRegistrationState()
            }
        }
    }

    @UiThread
    fun showDebugInfo(): Boolean {
        showDeviceId.value = true
        return true
    }

    @WorkerThread
    private fun requestDevicesList(identityAddress: Address) {
        Log.i(
            "$TAG Request devices list for identity address [${identityAddress.asStringUriOnly()}]"
        )
        accountManagerServices = coreContext.core.createAccountManagerServices()
        accountManagerServices.language = Locale.getDefault().language // Returns en, fr, etc...
        val request = accountManagerServices.createGetDevicesListRequest(
            identityAddress
        )
        request.addListener(accountManagerServicesListener)
        request.submit()
    }
}
