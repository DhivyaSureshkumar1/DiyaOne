package com.naminfo.ui.main.chat.viewmodel

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.DiyaOneApplication.Companion.corePreferences
import com.naminfo.R
import com.naminfo.contacts.ContactsManager
import org.linphone.core.Address
import org.linphone.core.ChatMessage
import org.linphone.core.ChatRoom
import org.linphone.core.ChatRoomListenerStub
import org.linphone.core.Conference
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Friend
import org.linphone.core.tools.Log
import com.naminfo.ui.main.chat.model.ConversationModel
import com.naminfo.ui.main.chat.model.ConversationModelWrapper
import com.naminfo.ui.main.viewmodel.AbstractMainViewModel
import com.naminfo.utils.AppUtils
import com.naminfo.utils.Event
import com.naminfo.utils.LinphoneUtils

class ConversationsListViewModel
    @UiThread
    constructor() : AbstractMainViewModel() {
    companion object {
        private const val TAG = "[Conversations List ViewModel]"
    }

    val conversations = MutableLiveData<ArrayList<ConversationModelWrapper>>()

    val fetchInProgress = MutableLiveData<Boolean>()

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
                fetchInProgress.postValue(false)
                chatRoomCreatedEvent.postValue(Event(LinphoneUtils.getConversationId(chatRoom)))
            } else if (state == ChatRoom.State.CreationFailed) {
                Log.e("$TAG Conversation [$id] creation has failed!")
                chatRoom.removeListener(this)
                fetchInProgress.postValue(false)
                showRedToast(R.string.conversation_failed_to_create_toast, R.drawable.warning_circle)
            }
        }
    }

    private val coreListener = object : CoreListenerStub() {
        @WorkerThread
        override fun onAccountRegistrationStateChanged(
            core: Core, account: org.linphone.core.Account,
            state: org.linphone.core.RegistrationState?, message: String
        ) {
            if (state == org.linphone.core.RegistrationState.Ok && account == core.defaultAccount) {
                computeChatRoomsList(currentFilter)
            }
        }

        @WorkerThread
        override fun onChatRoomStateChanged(
            core: Core,
            chatRoom: ChatRoom,
            state: ChatRoom.State?
        ) {
            Log.i(
                "$TAG Conversation [${LinphoneUtils.getConversationId(chatRoom)}] state changed [$state]"
            )

            when (state) {
                ChatRoom.State.Created -> addChatRoom(chatRoom)
                ChatRoom.State.Deleted -> removeChatRoom(chatRoom)
                else -> {}
            }
        }

        @WorkerThread
        override fun onMessageSent(core: Core, chatRoom: ChatRoom, message: ChatMessage) {
            val id = LinphoneUtils.getConversationId(chatRoom)
            val found = conversations.value.orEmpty().find {
                it.conversationModel?.id == id
            }
            if (found == null) {
                Log.i("$TAG Message sent for a conversation not yet in the list (probably was empty), adding it")
                addChatRoom(chatRoom)
            } else {
                Log.i("$TAG Message sent for an existing conversation, re-order them")
                reorderChatRooms()
            }
        }

        @WorkerThread
        override fun onMessagesReceived(
            core: Core,
            chatRoom: ChatRoom,
            messages: Array<out ChatMessage>
        ) {
            val id = LinphoneUtils.getConversationId(chatRoom)
            val found = conversations.value.orEmpty().find {
                it.conversationModel?.id == id
            }
            if (found == null) {
                Log.i("$TAG Message(s) received for a conversation not yet in the list (probably was empty), adding it")
                addChatRoom(chatRoom)
            } else {
                Log.i("$TAG Message(s) received for an existing conversation, re-order them")
                reorderChatRooms()
            }
        }
    }

    private val contactsListener = object : ContactsManager.ContactsListener {
        @WorkerThread
        override fun onContactsLoaded() {
            Log.i("$TAG Contacts have been (re)loaded, updating list")
            for (model in conversations.value.orEmpty()) {
                if (model.isConversation) {
                    model.conversationModel?.computeParticipants()
                    model.conversationModel?.updateLastMessage()
                }
            }
        }

        @WorkerThread
        override fun onContactFoundInRemoteDirectory(friend: Friend) { }
    }

    init {
        fetchInProgress.value = true

        coreContext.postOnCoreThread { core ->
            coreContext.contactsManager.addListener(contactsListener)
            core.addListener(coreListener)

            computeChatRoomsList(currentFilter)
        }
    }

    @UiThread
    override fun onCleared() {
        super.onCleared()

        coreContext.postOnCoreThread { core ->
            conversations.value.orEmpty().forEach(ConversationModelWrapper::destroy)
            coreContext.contactsManager.removeListener(contactsListener)
            core.removeListener(coreListener)
        }
    }

    @UiThread
    override fun filter() {
        coreContext.postOnCoreThread {
            computeChatRoomsList(currentFilter)
        }
    }

    @WorkerThread
    private fun computeChatRoomsList(filter: String) {
        conversations.value.orEmpty().forEach(ConversationModelWrapper::destroy)

        if (conversations.value.orEmpty().isEmpty()) {
            fetchInProgress.postValue(true)
        }

        val list = arrayListOf<ConversationModelWrapper>()

        val account = LinphoneUtils.getDefaultAccount()
        val chatRooms = if (filter.isEmpty()) {
            account?.chatRooms
        } else {
            account?.filterChatRooms(filter)
        }
        for (chatRoom in chatRooms.orEmpty()) {
            val model = ConversationModel(chatRoom)
            list.add(ConversationModelWrapper(model))
        }

        conversations.postValue(list)
        fetchInProgress.postValue(false)
    }

    @WorkerThread
    private fun addChatRoom(chatRoom: ChatRoom) {
        val identifier = chatRoom.identifier
        val chatRoomAccount = chatRoom.account
        val defaultAccount = LinphoneUtils.getDefaultAccount()
        if (defaultAccount == null || chatRoomAccount == null || chatRoomAccount != defaultAccount) {
            Log.w(
                "$TAG Chat room with identifier [$identifier] was created but not displaying it because it doesn't belong to currently default account"
            )
            return
        }

        val conferenceInfo = chatRoom.conferenceInfo
        if (conferenceInfo != null) {
            Log.w(
                "$TAG Chat room with identifier [$identifier] was created but not displaying it because it is related to a conference"
            )
            return
        }

        val hideEmptyChatRooms = coreContext.core.config.getBool("misc", "hide_empty_chat_rooms", true)
        // Hide empty chat rooms only applies to 1-1 conversations
        if (hideEmptyChatRooms && !LinphoneUtils.isChatRoomAGroup(chatRoom) && chatRoom.lastMessageInHistory == null) {
            Log.w("$TAG Chat room with identifier [$identifier] is empty, not adding it to match Core setting")
            return
        }

        val currentList = conversations.value.orEmpty()
        val found = currentList.find {
            it.conversationModel?.chatRoom?.identifier == identifier
        }
        if (found != null) {
            Log.w("$TAG Created chat room with identifier [$identifier] is already in the list, skipping")
            return
        }

        if (currentFilter.isNotEmpty()) {
            val filteredRooms = defaultAccount.filterChatRooms(currentFilter)
            val found = filteredRooms.find {
                it == chatRoom
            }
            if (found == null) return
        }

        val newList = arrayListOf<ConversationModelWrapper>()
        val model = ConversationModel(chatRoom)
        newList.add(ConversationModelWrapper(model))
        newList.addAll(currentList)
        Log.i("$TAG Adding chat room with identifier [$identifier] to list")
        conversations.postValue(newList)
    }

    @WorkerThread
    private fun removeChatRoom(chatRoom: ChatRoom) {
        val currentList = conversations.value.orEmpty()
        val identifier = chatRoom.identifier
        val found = currentList.find {
            it.conversationModel?.chatRoom?.identifier == identifier
        }
        if (found != null) {
            val newList = arrayListOf<ConversationModelWrapper>()
            newList.addAll(currentList)
            newList.remove(found)
            found.destroy()
            Log.i("$TAG Removing chat room with identifier [$identifier] from list")
            conversations.postValue(newList)
        } else {
            Log.w(
                "$TAG Failed to find item in list matching deleted chat room identifier [$identifier]"
            )
        }

        showGreenToast(R.string.conversation_deleted_toast, R.drawable.chat_teardrop_text)
    }

    @WorkerThread
    private fun reorderChatRooms() {
        if (currentFilter.isNotEmpty()) {
            Log.w("$TAG List filter isn't empty, do not re-order list")
            return
        }

        Log.i("$TAG Re-ordering conversations")
        val sortedList = arrayListOf<ConversationModelWrapper>()
        sortedList.addAll(conversations.value.orEmpty())
        sortedList.sortByDescending {
            it.conversationModel?.chatRoom?.lastUpdateTime
        }
        conversations.postValue(sortedList)
    }

    @WorkerThread
    fun createOneToOneChatRoomWith(remote: Address) {
        val core = coreContext.core
        val account = core.defaultAccount
        if (account == null) {
            Log.e(
                "$TAG No default account found, can't create conversation with [${remote.asStringUriOnly()}]!"
            )
            return
        }

        fetchInProgress.postValue(true)

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
            fetchInProgress.postValue(false)
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
                        fetchInProgress.postValue(false)
                        chatRoomCreatedEvent.postValue(Event(LinphoneUtils.getConversationId(chatRoom)))
                    } else {
                        Log.i("$TAG Conversation isn't in Created state yet (state is [$state]), wait for it")
                        chatRoom.addListener(chatRoomListener)
                    }
                } else {
                    val id = LinphoneUtils.getConversationId(chatRoom)
                    Log.i("$TAG Conversation successfully created [$id]")
                    fetchInProgress.postValue(false)
                    chatRoomCreatedEvent.postValue(Event(LinphoneUtils.getConversationId(chatRoom)))
                }
            } else {
                Log.e("$TAG Failed to create 1-1 conversation with [${remote.asStringUriOnly()}]!")
                fetchInProgress.postValue(false)
                showRedToast(R.string.conversation_failed_to_create_toast, R.drawable.warning_circle)
            }
        } else {
            Log.w(
                "$TAG A 1-1 conversation between local account [${localAddress?.asStringUriOnly()}] and remote [${remote.asStringUriOnly()}] for given parameters already exists!"
            )
            fetchInProgress.postValue(false)
            chatRoomCreatedEvent.postValue(Event(LinphoneUtils.getConversationId(existingChatRoom)))
        }
    }
}
