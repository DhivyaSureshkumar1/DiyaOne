package com.naminfo.ui.assistant.viewmodel

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.DiyaOneApplication.Companion.corePreferences
import com.naminfo.R
import org.linphone.core.Account
import org.linphone.core.AuthInfo
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.Reason
import org.linphone.core.RegistrationState
import org.linphone.core.tools.Log
import com.naminfo.ui.GenericViewModel
import com.naminfo.utils.AppUtils
import com.naminfo.utils.Event

open class AccountLoginViewModel
    @UiThread
    constructor() : GenericViewModel() {
    companion object {
        private const val TAG = "[Account Login ViewModel]"
    }

    val showBackButton = MutableLiveData<Boolean>()

    val hideCreateAccount = MutableLiveData<Boolean>()

    val hideScanQrCode = MutableLiveData<Boolean>()

    val hideThirdPartyAccount = MutableLiveData<Boolean>()

    val sipIdentity = MutableLiveData<String>()

    val password = MutableLiveData<String>()

    val internationalPrefix = MutableLiveData<String>()

    val internationalPrefixIsoCountryCode = MutableLiveData<String>()

    val showPassword = MutableLiveData<Boolean>()

    val loginEnabled = MediatorLiveData<Boolean>()

    val registrationInProgress = MutableLiveData<Boolean>()

    val accountLoggedInEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }

    val accountLoginErrorEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData()
    }

    val skipLandingToThirdPartySipAccountEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }

    var conditionsAndPrivacyPolicyAccepted = false

    private lateinit var newlyCreatedAuthInfo: AuthInfo
    private lateinit var newlyCreatedAccount: Account

    private val coreListener = object : CoreListenerStub() {
        @WorkerThread
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String
        ) {
            if (account == newlyCreatedAccount) {
                Log.i("$TAG Newly created account registration state is [$state] ($message)")

                if (state == RegistrationState.Ok) {
                    registrationInProgress.postValue(false)
                    core.removeListener(this)

                    // Set new account as default
                    core.defaultAccount = newlyCreatedAccount
                    accountLoggedInEvent.postValue(Event(core.accountList.size == 1))
                } else if (state == RegistrationState.Failed) {
                    registrationInProgress.postValue(false)
                    core.removeListener(this)

                    val error = when (account.error) {
                        Reason.Forbidden -> {
                            AppUtils.getString(R.string.assistant_account_login_forbidden_error)
                        }
                        else -> {
                            AppUtils.getFormattedString(
                                R.string.assistant_account_login_error,
                                account.error.toString()
                            )
                        }
                    }
                    accountLoginErrorEvent.postValue(Event(error))

                    Log.e("$TAG Account failed to REGISTER [$message], removing it")
                    core.removeAuthInfo(newlyCreatedAuthInfo)
                    core.removeAccount(newlyCreatedAccount)
                }
            }
        }
    }

    init {
        coreContext.postOnCoreThread { core ->
            // Prevent user from leaving assistant if no account was configured yet
            showBackButton.postValue(core.accountList.isNotEmpty())
            hideCreateAccount.postValue(corePreferences.hideAssistantCreateAccount)
            hideScanQrCode.postValue(corePreferences.hideAssistantScanQrCode)
            hideThirdPartyAccount.postValue(corePreferences.hideAssistantThirdPartySipAccount)
            conditionsAndPrivacyPolicyAccepted = corePreferences.conditionsAndPrivacyPolicyAccepted

            if (corePreferences.assistantDirectlyGoToThirdPartySipAccountLogin) {
                skipLandingToThirdPartySipAccountEvent.postValue(Event(true))
            }
        }

        showPassword.value = false
        registrationInProgress.value = false

        loginEnabled.addSource(sipIdentity) {
            loginEnabled.value = isLoginButtonEnabled()
        }
        loginEnabled.addSource(password) {
            loginEnabled.value = isLoginButtonEnabled()
        }
    }

    @UiThread
    fun login() {
        coreContext.postOnCoreThread { core ->
            core.loadConfigFromXml(corePreferences.linphoneDefaultValuesPath)

            val userInput = sipIdentity.value.orEmpty().trim()
            val defaultDomain = corePreferences.defaultDomain
            val identity = if (userInput.startsWith("sip:")) {
                if (userInput.contains("@")) {
                    userInput
                } else {
                    "$userInput@$defaultDomain"
                }
            } else {
                if (userInput.contains("@")) {
                    "sip:$userInput"
                } else {
                    "sip:$userInput@$defaultDomain"
                }
            }
            Log.i("$TAG Computed identity is [$identity] from user input [$userInput]")

            val identityAddress = Factory.instance().createAddress(identity)
            if (identityAddress == null) {
                Log.e("$TAG Can't parse [$identity] as Address!")
                showRedToast(R.string.assistant_login_cant_parse_address_toast, R.drawable.warning_circle)
                return@postOnCoreThread
            }

            val accounts = core.accountList
            val found = accounts.find {
                it.params.identityAddress?.weakEqual(identityAddress) == true
            }
            if (found != null) {
                Log.w("$TAG An account with the same identity address [${identityAddress.asStringUriOnly()}] already exists, do not add it again!")
                showRedToast(R.string.assistant_account_login_already_connected_error, R.drawable.warning_circle)
                return@postOnCoreThread
            }

            val user = identityAddress.username
            if (user == null) {
                Log.e(
                    "$TAG Address [${identityAddress.asStringUriOnly()}] doesn't contains an username!"
                )
                showRedToast(R.string.assistant_login_address_without_username_toast, R.drawable.warning_circle)
                return@postOnCoreThread
            }

            val domain = identityAddress.domain

            newlyCreatedAuthInfo = Factory.instance().createAuthInfo(
                user,
                null,
                password.value.orEmpty().trim(),
                null,
                null,
                domain
            )
            core.addAuthInfo(newlyCreatedAuthInfo)

            val accountParams = core.createAccountParams()
            val profileSection = "saved_profile_${identityAddress.asStringUriOnly()}"
            if (identityAddress.displayName.isNullOrBlank()) {
                identityAddress.displayName = core.config.getString(profileSection, "display_name", null)
            }
            accountParams.pictureUri = core.config.getString(profileSection, "picture_uri", null)
            accountParams.identityAddress = identityAddress

            val prefix = internationalPrefix.value.orEmpty().trim()
            val isoCountryCode = internationalPrefixIsoCountryCode.value.orEmpty()
            if (prefix.isNotEmpty()) {
                val prefixDigits = if (prefix.startsWith("+")) {
                    prefix.substring(1)
                } else {
                    prefix
                }
                if (prefixDigits.isNotEmpty()) {
                    Log.i(
                        "$TAG Setting international prefix [$prefixDigits]($isoCountryCode) in account params"
                    )
                    accountParams.internationalPrefix = prefixDigits
                    accountParams.internationalPrefixIsoCountryCode = isoCountryCode
                }
            }

            newlyCreatedAccount = core.createAccount(accountParams)

            registrationInProgress.postValue(true)
            core.addListener(coreListener)
            Log.i(
                "$TAG Trying to log in account with SIP identity [${identityAddress.asStringUriOnly()}]"
            )
            core.addAccount(newlyCreatedAccount)
        }
    }

    @UiThread
    fun toggleShowPassword() {
        showPassword.value = showPassword.value == false
    }

    @UiThread
    private fun isLoginButtonEnabled(): Boolean {
        return sipIdentity.value.orEmpty().trim().isNotEmpty() && password.value.orEmpty().isNotEmpty()
    }
}
