package com.naminfo.ui.assistant.viewmodel

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import java.util.Locale
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
import org.linphone.core.TransportType
import org.linphone.core.tools.Log
import com.naminfo.ui.GenericViewModel
import com.naminfo.utils.AppUtils
import com.naminfo.utils.Event

class ThirdPartySipAccountLoginViewModel
    @UiThread
    constructor() : GenericViewModel() {
    companion object {
        private const val TAG = "[Third Party SIP Account Login ViewModel]"
    }

    val username = MutableLiveData<String>()

    val authId = MutableLiveData<String>()

    val domain = MutableLiveData<String>()

    val displayName = MutableLiveData<String>()

    val transport = MutableLiveData<String>()

    val countryDialPlans = MutableLiveData<List<org.linphone.core.DialPlan>>()

    val internationalPrefix = MutableLiveData("91")

    val internationalPrefixIsoCountryCode = MutableLiveData("IN")

    val expandAdvancedSettings = MutableLiveData<Boolean>()

    val proxy = MutableLiveData<String>()

    val outboundProxy = MutableLiveData<String>()

    val loginEnabled = MediatorLiveData<Boolean>()

    val registrationInProgress = MutableLiveData<Boolean>()

    val accountLoggedInEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }

    val accountLoginErrorEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData()
    }

    val defaultTransportIndexEvent: MutableLiveData<Event<Int>> by lazy {
        MutableLiveData()
    }

    val availableTransports = arrayListOf<String>()

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
                    // Persist the account and authentication immediately. The account
                    // will be restored and re-registered on later app launches until
                    // the user explicitly signs out from Settings.
                    core.config.sync()
                    accountLoggedInEvent.postValue(Event(true))
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
        expandAdvancedSettings.value = false
        registrationInProgress.value = false

        loginEnabled.addSource(username) {
            loginEnabled.value = isLoginButtonEnabled()
        }
        loginEnabled.addSource(domain) {
            loginEnabled.value = isLoginButtonEnabled()
        }

        // TODO: handle formatting errors ?

        availableTransports.add(TransportType.Udp.name.uppercase(Locale.getDefault()))
        availableTransports.add(TransportType.Tcp.name.uppercase(Locale.getDefault()))
        availableTransports.add(TransportType.Tls.name.uppercase(Locale.getDefault()))

        coreContext.postOnCoreThread {
            countryDialPlans.postValue(Factory.instance().dialPlans.toList())
            domain.postValue(
                corePreferences.thirdPartySipAccountDefaultDomain.ifBlank {
                    corePreferences.defaultDomain
                }
            )

            val defaultTransport = corePreferences.thirdPartySipAccountDefaultTransport.uppercase(
                Locale.getDefault()
            )
            val udpIndex = availableTransports.indexOf(
                TransportType.Udp.name.uppercase(Locale.getDefault())
            )
            val savedTransportIndex = availableTransports.indexOf(defaultTransport)
            val index = savedTransportIndex.takeIf { it >= 0 } ?: udpIndex
            defaultTransportIndexEvent.postValue(Event(index))
        }
    }

    @UiThread
    fun login() {
        coreContext.postOnCoreThread { core ->
            core.loadConfigFromXml(corePreferences.thirdPartyDefaultValuesPath)

            // Remove sip: in front of domain, just in case...
            val domainValue = domain.value.orEmpty().trim()
            val domainWithoutSip = if (domainValue.startsWith("sip:")) {
                domainValue.substring("sip:".length)
            } else {
                domainValue
            }
            val domainAddress = Factory.instance().createAddress("sip:$domainWithoutSip")
            val port = domainAddress?.port ?: -1
            if (port != -1) {
                Log.w("$TAG It seems a port [$port] was set in the domain [$domainValue], removing it from SIP identity but setting it to proxy server URI")
            }
            val domain = domainAddress?.domain ?: domainWithoutSip

            // Allow to enter SIP identity instead of simply username
            // in case identity domain doesn't match proxy domain
            var user = username.value.orEmpty().trim()
            if (user.startsWith("sip:")) {
                user = user.substring("sip:".length)
            } else if (user.startsWith("sips:")) {
                user = user.substring("sips:".length)
            }
            if (user.contains("@")) {
                user = user.split("@")[0]
            }

            val userId = authId.value.orEmpty().trim()

            Log.i("$TAG Parsed username is [$user], user ID [$userId] and domain [$domain]")
            val identity = "sip:$user@$domain"
            val identityAddress = Factory.instance().createAddress(identity)
            if (identityAddress == null) {
                Log.e("$TAG Can't parse [$identity] as Address!")
                showRedToast(R.string.assistant_login_cant_parse_address_toast, R.drawable.warning_circle)
                return@postOnCoreThread
            }
            Log.i("$TAG Computed SIP identity is [${identityAddress.asStringUriOnly()}]")

            val accounts = core.accountList
            val found = accounts.find {
                it.params.identityAddress?.weakEqual(identityAddress) == true
            }
            if (found != null) {
                Log.w("$TAG An account with the same identity address [${found.params.identityAddress?.asStringUriOnly()}] already exists, do not add it again!")
                showRedToast(R.string.assistant_account_login_already_connected_error, R.drawable.warning_circle)
                return@postOnCoreThread
            }

            newlyCreatedAuthInfo = Factory.instance().createAuthInfo(
                user,
                userId,
                user,
                null,
                null,
                domainAddress?.domain ?: domainValue
            )
            core.addAuthInfo(newlyCreatedAuthInfo)

            val accountParams = core.createAccountParams()

            if (displayName.value.orEmpty().isNotEmpty()) {
                identityAddress.displayName = displayName.value.orEmpty().trim()
            }
            val profileSection = "saved_profile_${identityAddress.asStringUriOnly()}"
            if (identityAddress.displayName.isNullOrBlank()) {
                identityAddress.displayName = core.config.getString(profileSection, "display_name", null)
            }
            accountParams.pictureUri = core.config.getString(profileSection, "picture_uri", null)
            accountParams.identityAddress = identityAddress

            val outboundProxyValue = outboundProxy.value.orEmpty().trim()
            val outboundProxyAddress = if (outboundProxyValue.isNotEmpty()) {
                val server = if (outboundProxyValue.startsWith("sip:")) {
                    outboundProxyValue
                } else {
                    "sip:$outboundProxyValue"
                }
                Factory.instance().createAddress(server)
            } else {
                null
            }
            if (outboundProxyAddress != null) {
                outboundProxyAddress.transport = when (transport.value.orEmpty().trim()) {
                    TransportType.Tcp.name.uppercase(Locale.getDefault()) -> TransportType.Tcp
                    TransportType.Tls.name.uppercase(Locale.getDefault()) -> TransportType.Tls
                    else -> TransportType.Udp
                }
                Log.i("$TAG Created outbound proxy server SIP address [${outboundProxyAddress.asStringUriOnly()}]")
                accountParams.setRoutesAddresses(arrayOf(outboundProxyAddress))
            }

            val proxyServerValue = proxy.value.orEmpty().trim()
            val proxyServerAddress = if (proxyServerValue.isNotEmpty()) {
                val server = if (proxyServerValue.startsWith("sip:")) {
                    proxyServerValue
                } else {
                    "sip:$proxyServerValue"
                }
                Factory.instance().createAddress(server)
            } else {
                outboundProxyAddress ?: domainAddress ?: Factory.instance().createAddress("sip:$domainWithoutSip")
            }
            proxyServerAddress?.transport = when (transport.value.orEmpty().trim()) {
                TransportType.Tcp.name.uppercase(Locale.getDefault()) -> TransportType.Tcp
                TransportType.Tls.name.uppercase(Locale.getDefault()) -> TransportType.Tls
                else -> TransportType.Udp
            }
            Log.i("$TAG Created proxy server SIP address [${proxyServerAddress?.asStringUriOnly()}]")
            accountParams.serverAddress = proxyServerAddress
            Log.i("$TAG Is outbound proxy enabled ? [${accountParams.isOutboundProxyEnabled}]")

            // The login country selection must not add a prefix to dialpad calls.
            accountParams.internationalPrefix = ""
            accountParams.internationalPrefixIsoCountryCode = ""

            newlyCreatedAccount = core.createAccount(accountParams)

            registrationInProgress.postValue(true)
            core.addListener(coreListener)
            core.addAccount(newlyCreatedAccount)
        }
    }

    @UiThread
    private fun isLoginButtonEnabled(): Boolean {
        return username.value.orEmpty().isNotBlank() && domain.value.orEmpty().isNotBlank()
    }

   /* @UiThread
    private fun isLoginButtonEnabled(): Boolean {
        val mobileNumber = username.value.orEmpty().trim()
        return true // mobileNumber.matches(Regex("[6-9]\\d{9}")) && domain.value.orEmpty().isNotEmpty()
    }*/

    @UiThread
    fun toggleAdvancedSettingsExpand() {
        expandAdvancedSettings.value = expandAdvancedSettings.value == false
    }
}
