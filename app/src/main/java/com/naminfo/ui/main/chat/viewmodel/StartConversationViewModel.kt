package com.naminfo.ui.main.chat.viewmodel

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.DiyaOneApplication.Companion.corePreferences
import com.naminfo.R
import com.naminfo.contacts.ContactLoader
import org.linphone.core.Address
import org.linphone.core.ChatRoom
import org.linphone.core.ChatRoomListenerStub
import org.linphone.core.Conference
import org.linphone.core.Friend
import org.linphone.core.tools.Log
import com.naminfo.ui.main.viewmodel.AddressSelectionViewModel
import com.naminfo.utils.AppUtils
import com.naminfo.utils.Event
import com.naminfo.utils.LinphoneUtils

class StartConversationViewModel
    @UiThread
    constructor() : AddressSelectionViewModel() {
    companion object {
        private const val TAG = "[Start Conversation ViewModel]"
    }

    override val showAddressSuggestions = false

    override val automaticallySelectPreferredAddress = true

    // Match the full address book before filtering to registered phone contacts.
    override val limitContactSearch = false

    override val contactSearchDomain = ""

    private var nativePhoneNumbers = emptySet<String>()

    @WorkerThread
    override fun additionalContacts(filter: String): List<Friend> {
        if (!com.naminfo.BuildConfig.DEBUG) return emptyList()
        return listOf("1001", "1002").mapNotNull { extension ->
            val uri = "sip:$extension@192.168.1.82"
            if (filter.isNotBlank() && !uri.contains(filter.trim(), ignoreCase = true)) {
                return@mapNotNull null
            }
            coreContext.core.createFriendWithAddress(uri)?.apply {
                name = extension
                refKey = "local-test:$extension"
            }
        }
    }

    @WorkerThread
    override fun beforeProcessingSearchResults() {
        val nativeFriendList = coreContext.core.getFriendListByName(
            ContactLoader.NATIVE_ADDRESS_BOOK_FRIEND_LIST
        )
        nativePhoneNumbers = nativeFriendList?.friends.orEmpty()
            .flatMap { nativeFriend -> nativeFriend.phoneNumbers.asIterable() }
            .map(::normalizePhoneNumber)
            .filter(String::isNotEmpty)
            .toSet()
    }

    @WorkerThread
    override fun shouldIncludeFriend(friend: Friend): Boolean {
        if (!friend.refKey.orEmpty().startsWith("mobion:")) return false

        return friend.phoneNumbers.any { number ->
            normalizePhoneNumber(number) in nativePhoneNumbers
        }
    }

    private fun normalizePhoneNumber(number: String): String {
        val digits = number.filter(Char::isDigit)
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    val hideGroupChatButton = MutableLiveData<Boolean>()

    val subject = MutableLiveData<String>()

    val createGroupConversationButtonEnabled = MediatorLiveData<Boolean>()

    val operationInProgress = MutableLiveData<Boolean>()

    val chatRoomCreatedEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData()
    }

    private val chatRoomListener = object : ChatRoomListenerStub() {
        @WorkerThread
        override fun onStateChanged(chatRoom: ChatRoom, newState: ChatRoom.State?) {
            val state = chatRoom.state
            if (state == ChatRoom.State.Instantiated) return

            val id = LinphoneUtils.getConversationId(chatRoom)
            Log.i("$TAG Conversation [$id] (${chatRoom.subjectUtf8}) state changed: [$state]")

            if (state == ChatRoom.State.Created) {
                Log.i("$TAG Conversation [$id] successfully created")
                chatRoom.removeListener(this)
                operationInProgress.postValue(false)
                chatRoomCreatedEvent.postValue(Event(LinphoneUtils.getConversationId(chatRoom)))
            } else if (state == ChatRoom.State.CreationFailed) {
                Log.e("$TAG Conversation [$id] creation has failed!")
                chatRoom.removeListener(this)
                operationInProgress.postValue(false)
                showRedToast(R.string.conversation_failed_to_create_toast, R.drawable.warning_circle)
            }
        }
    }

    init {
        createGroupConversationButtonEnabled.value = false
        createGroupConversationButtonEnabled.addSource(selection) {
            createGroupConversationButtonEnabled.value = it.isNotEmpty()
        }

        updateGroupChatButtonVisibility()
    }

    @WorkerThread
    override fun onSingleAddressSelected(address: Address, friend: Friend?) {
        createOneToOneChatRoomWith(address)
    }

    @UiThread
    fun createGroupChatRoom() {
        coreContext.postOnCoreThread { core ->
            val account = core.defaultAccount
            if (account == null) {
                Log.e(
                    "$TAG No default account found, can't create group conversation!"
                )
                return@postOnCoreThread
            }

            operationInProgress.postValue(true)

            val groupChatRoomSubject = subject.value.orEmpty()
            val params = coreContext.core.createConferenceParams(null)
            params.isChatEnabled = true
            params.isGroupEnabled = true
            params.subject = groupChatRoomSubject
            if (LinphoneUtils.isEndToEndEncryptedChatAvailable(core)) {
                params.securityLevel = Conference.SecurityLevel.EndToEnd
            } else {
                params.securityLevel = Conference.SecurityLevel.None
            }
            params.account = account

            val chatParams = params.chatParams ?: return@postOnCoreThread
            chatParams.ephemeralLifetime = 0 // Make sure ephemeral is disabled by default
            chatParams.backend = ChatRoom.Backend.FlexisipChat

            val participants = arrayListOf<Address>()
            for (participant in selection.value.orEmpty()) {
                participants.add(participant.address)
            }

            val participantsArray = arrayOf<Address>()
            val chatRoom = core.createChatRoom(params, participants.toArray(participantsArray))
            if (chatRoom != null) {
                if (chatParams.backend == ChatRoom.Backend.FlexisipChat) {
                    if (chatRoom.state == ChatRoom.State.Created) {
                        val id = LinphoneUtils.getConversationId(chatRoom)
                        Log.i(
                            "$TAG Group conversation [$id] ($groupChatRoomSubject) has been created"
                        )
                        operationInProgress.postValue(false)
                        chatRoomCreatedEvent.postValue(Event(LinphoneUtils.getConversationId(chatRoom)))
                    } else {
                        Log.i(
                            "$TAG Conversation [$groupChatRoomSubject] isn't in Created state yet, wait for it"
                        )
                        chatRoom.addListener(chatRoomListener)
                    }
                } else {
                    val id = LinphoneUtils.getConversationId(chatRoom)
                    Log.i("$TAG Conversation successfully created [$id] ($groupChatRoomSubject)")
                    operationInProgress.postValue(false)
                    chatRoomCreatedEvent.postValue(Event(LinphoneUtils.getConversationId(chatRoom)))
                }
            } else {
                Log.e("$TAG Failed to create group conversation [$groupChatRoomSubject]!")
                operationInProgress.postValue(false)
                showRedToast(R.string.conversation_failed_to_create_toast, R.drawable.warning_circle)
            }
        }
    }

    @WorkerThread
    fun createOneToOneChatRoomWith(selectedAddress: Address) {
        val core = coreContext.core
        val account = core.defaultAccount
        if (account == null) {
            Log.e(
                "$TAG No default account found, can't create conversation with [${selectedAddress.asStringUriOnly()}]!"
            )
            return
        }

        val remote = LinphoneUtils.canonicalizeBasicChatAddress(selectedAddress, account)

        operationInProgress.postValue(true)

        val params = coreContext.core.createConferenceParams(null)
        params.isChatEnabled = true
        params.isGroupEnabled = false
        params.subject = AppUtils.getString(R.string.conversation_one_to_one_hidden_subject)
        params.account = account

        val chatParams = params.chatParams ?: return
        chatParams.ephemeralLifetime = 0 // Make sure ephemeral is disabled by default

        val sameDomain = remote.domain == corePreferences.defaultDomain && remote.domain == account.params.domain
        if (account.params.instantMessagingEncryptionMandatory && sameDomain) {
            Log.i("$TAG Account is in secure mode & domain matches, creating an E2E encrypted conversation")
            chatParams.backend = ChatRoom.Backend.FlexisipChat
            params.securityLevel = Conference.SecurityLevel.EndToEnd
        } else if (!account.params.instantMessagingEncryptionMandatory) {
            if (LinphoneUtils.isEndToEndEncryptedChatAvailable(core)) {
                Log.i(
                    "$TAG Account is in interop mode but LIME is available, creating an E2E encrypted conversation"
                )
                chatParams.backend = ChatRoom.Backend.FlexisipChat
                params.securityLevel = Conference.SecurityLevel.EndToEnd
            } else {
                Log.i(
                    "$TAG Account is in interop mode but LIME isn't available, creating a SIP simple conversation"
                )
                chatParams.backend = ChatRoom.Backend.Basic
                params.securityLevel = Conference.SecurityLevel.None
            }
        } else {
            Log.e(
                "$TAG Account is in secure mode, can't chat with SIP address of different domain [${remote.asStringUriOnly()}]"
            )
            operationInProgress.postValue(false)
            showRedToast(R.string.conversation_invalid_participant_due_to_security_mode_toast, R.drawable.warning_circle)
            return
        }

        val participants = arrayOf(remote)
        val localAddress = account.params.identityAddress
        val existingChatRoom = core.searchChatRoom(params, localAddress, null, participants)
        if (existingChatRoom == null) {
            Log.i(
                "$TAG No existing 1-1 conversation between local account [${localAddress?.asStringUriOnly()}] and remote [${remote.asStringUriOnly()}] was found for given parameters, let's create it"
            )
            val chatRoom = core.createChatRoom(params, participants)
            if (chatRoom != null) {
                if (chatParams.backend == ChatRoom.Backend.FlexisipChat) {
                    val state = chatRoom.state
                    if (state == ChatRoom.State.Created) {
                        val id = LinphoneUtils.getConversationId(chatRoom)
                        Log.i("$TAG 1-1 conversation [$id] has been created")
                        operationInProgress.postValue(false)
                        chatRoomCreatedEvent.postValue(Event(LinphoneUtils.getConversationId(chatRoom)))
                    } else {
                        Log.i("$TAG Conversation isn't in Created state yet (state is [$state]), wait for it")
                        chatRoom.addListener(chatRoomListener)
                    }
                } else {
                    val id = LinphoneUtils.getConversationId(chatRoom)
                    Log.i("$TAG Conversation successfully created [$id]")
                    operationInProgress.postValue(false)
                    chatRoomCreatedEvent.postValue(Event(LinphoneUtils.getConversationId(chatRoom)))
                }
            } else {
                Log.e("$TAG Failed to create 1-1 conversation with [${remote.asStringUriOnly()}]!")
                operationInProgress.postValue(false)
                showRedToast(R.string.conversation_failed_to_create_toast, R.drawable.warning_circle)
            }
        } else {
            Log.w(
                "$TAG A 1-1 conversation between local account [${localAddress?.asStringUriOnly()}] and remote [${remote.asStringUriOnly()}] for given parameters already exists!"
            )
            operationInProgress.postValue(false)
            chatRoomCreatedEvent.postValue(Event(LinphoneUtils.getConversationId(existingChatRoom)))
        }
    }

    @UiThread
    fun updateGroupChatButtonVisibility() {
        coreContext.postOnCoreThread { core ->
            val hideGroupChat = !LinphoneUtils.isGroupChatAvailable(core)
            hideGroupChatButton.postValue(hideGroupChat)
        }
    }
}
