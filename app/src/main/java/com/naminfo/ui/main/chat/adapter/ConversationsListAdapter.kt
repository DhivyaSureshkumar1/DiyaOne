package com.naminfo.ui.main.chat.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.core.view.doOnPreDraw
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.naminfo.R
import org.linphone.core.Address
import org.linphone.core.Friend
import com.naminfo.databinding.ChatListCellBinding
import com.naminfo.databinding.ChatListContactSuggestionCellBinding
import com.naminfo.databinding.GenericAddressPickerListDecorationBinding
import com.naminfo.ui.main.chat.model.ConversationModel
import com.naminfo.ui.main.chat.model.ConversationModelWrapper
import com.naminfo.ui.main.model.ConversationContactOrSuggestionModel
import com.naminfo.utils.AppUtils
import com.naminfo.utils.Event
import com.naminfo.utils.HeaderAdapter
import com.naminfo.utils.startAnimatedDrawable

class ConversationsListAdapter :
    ListAdapter<ConversationModelWrapper, RecyclerView.ViewHolder>(
    ChatRoomDiffCallback()
),
    HeaderAdapter  {
    companion object {
        private const val CONVERSATION_TYPE = 0
        private const val CONTACT_TYPE = 1
        private const val SUGGESTION_TYPE = 2
    }

    var selectedAdapterPosition = -1

    val conversationClickedEvent: MutableLiveData<Event<ConversationModel>> by lazy {
        MutableLiveData()
    }

    val conversationLongClickedEvent: MutableLiveData<Event<ConversationModel>> by lazy {
        MutableLiveData()
    }

    val createConversationWithFriendClickedEvent: MutableLiveData<Event<Friend>> by lazy {
        MutableLiveData()
    }

    val createConversationWithAddressClickedEvent: MutableLiveData<Event<Address>> by lazy {
        MutableLiveData()
    }

    override fun displayHeaderForPosition(position: Int): Boolean {
        // Don't show header for call history section
        if (position == 0 && getItemViewType(0) == CONVERSATION_TYPE) {
            return false
        }

        return getItemViewType(position) != getItemViewType(position - 1)
    }

    override fun getHeaderViewForPosition(
        context: Context,
        position: Int
    ): View {
        val binding = GenericAddressPickerListDecorationBinding.inflate(
            LayoutInflater.from(context)
        )
        binding.header.text = when (getItemViewType(position)) {
            SUGGESTION_TYPE -> {
                AppUtils.getString(R.string.generic_address_picker_suggestions_list_title)
            }
            else -> {
                AppUtils.getString(R.string.generic_address_picker_contacts_list_title)
            }
        }
        return binding.root
    }

    override fun getItemViewType(position: Int): Int {
        try {
            val model = getItem(position)
            return if (model.isConversation) {
                CONVERSATION_TYPE
            } else if (model.contactModel?.friend != null) {
                CONTACT_TYPE
            } else {
                SUGGESTION_TYPE
            }
        } catch (ioobe: IndexOutOfBoundsException) {

        }
        return CONVERSATION_TYPE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            CONVERSATION_TYPE -> {
                val binding: ChatListCellBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.chat_list_cell,
                    parent,
                    false
                )
                val viewHolder = ConversationViewHolder(binding)
                binding.apply {
                    lifecycleOwner = parent.findViewTreeLifecycleOwner()

                    setOnClickListener {
                        conversationClickedEvent.value = Event(model!!)
                    }

                    setOnLongClickListener {
                        selectedAdapterPosition = viewHolder.bindingAdapterPosition
                        root.isSelected = true
                        conversationLongClickedEvent.value = Event(model!!)
                        true
                    }
                }
                viewHolder
            }
            else -> {
                val binding: ChatListContactSuggestionCellBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.chat_list_contact_suggestion_cell,
                    parent,
                    false
                )
                binding.apply {
                    lifecycleOwner = parent.findViewTreeLifecycleOwner()

                    setOnCreateConversationClickListener {
                        val friend = model?.friend
                        if (friend != null) {
                            createConversationWithFriendClickedEvent.value = Event(friend)
                        } else {
                            createConversationWithAddressClickedEvent.value = Event(model!!.address)
                        }
                    }
                }
                ContactSuggestionViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            CONVERSATION_TYPE -> (holder as ConversationViewHolder).bind(getItem(position).conversationModel!!)
            else -> (holder as ContactSuggestionViewHolder).bind(getItem(position).contactModel!!)
        }
    }

    fun resetSelection() {
        notifyItemChanged(selectedAdapterPosition)
        selectedAdapterPosition = -1
    }

    inner class ConversationViewHolder(
        val binding: ChatListCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        @UiThread
        fun bind(conversationModel: ConversationModel) {
            with(binding) {
                model = conversationModel

                binding.root.isSelected = bindingAdapterPosition == selectedAdapterPosition

                executePendingBindings()

                binding.root.doOnPreDraw {
                    binding.lastSentMessageStatus.startAnimatedDrawable()
                }
            }
        }
    }

    class ContactSuggestionViewHolder(
        val binding: ChatListContactSuggestionCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        @UiThread
        fun bind(conversationContactOrSuggestionModel: ConversationContactOrSuggestionModel) {
            with(binding) {
                model = conversationContactOrSuggestionModel

                executePendingBindings()
            }
        }
    }

    private class ChatRoomDiffCallback : DiffUtil.ItemCallback<ConversationModelWrapper>() {
        override fun areItemsTheSame(oldItem: ConversationModelWrapper, newItem: ConversationModelWrapper): Boolean {
            if (oldItem.isConversation && newItem.isConversation) {
                return oldItem.conversationModel?.id == newItem.conversationModel?.id && oldItem.conversationModel?.lastUpdateTime == newItem.conversationModel?.lastUpdateTime
            } else if (oldItem.isContactOrSuggestion && newItem.isContactOrSuggestion) {
                return oldItem.contactModel?.id == newItem.contactModel?.id
            }
            return false
        }

        override fun areContentsTheSame(oldItem: ConversationModelWrapper, newItem: ConversationModelWrapper): Boolean {
            if (oldItem.isConversation && newItem.isConversation) {
                return newItem.conversationModel?.avatarModel?.value?.id == oldItem.conversationModel?.avatarModel?.value?.id
            }
            return false
        }
    }
}
