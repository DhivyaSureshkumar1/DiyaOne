package com.naminfo.ui.main.contacts.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.naminfo.R
import com.naminfo.databinding.ContactFavouriteListCellBinding
import com.naminfo.databinding.ContactListCellBinding
import com.naminfo.ui.main.contacts.model.ContactAvatarModel
import com.naminfo.utils.Event

class ContactsListAdapter(
    private val favourites: Boolean = false,
    private val disableLongClick: Boolean = false
) : ListAdapter<ContactAvatarModel, RecyclerView.ViewHolder>(ContactDiffCallback()) {
    var selectedAdapterPosition = -1

    val contactClickedEvent: MutableLiveData<Event<ContactAvatarModel>> by lazy {
        MutableLiveData()
    }

    val contactLongClickedEvent: MutableLiveData<Event<ContactAvatarModel>> by lazy {
        MutableLiveData()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (favourites) {
            val binding: ContactFavouriteListCellBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.contact_favourite_list_cell,
                parent,
                false
            )
            val viewHolder = FavouriteViewHolder(binding)
            binding.apply {
                lifecycleOwner = parent.findViewTreeLifecycleOwner()

                setOnClickListener {
                    contactClickedEvent.value = Event(model!!)
                }

                setOnLongClickListener {
                    selectedAdapterPosition = viewHolder.bindingAdapterPosition
                    root.isSelected = true
                    contactLongClickedEvent.value = Event(model!!)
                    true
                }
            }
            return viewHolder
        } else {
            val binding: ContactListCellBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.contact_list_cell,
                parent,
                false
            )
            val viewHolder = ViewHolder(binding)
            binding.apply {
                lifecycleOwner = parent.findViewTreeLifecycleOwner()

                setOnClickListener {
                    contactClickedEvent.value = Event(model!!)
                }

                if (!disableLongClick) {
                    setOnLongClickListener {
                        selectedAdapterPosition = viewHolder.bindingAdapterPosition
                        root.isSelected = true
                        contactLongClickedEvent.value = Event(model!!)
                        true
                    }
                }
            }
            return viewHolder
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (favourites) {
            (holder as FavouriteViewHolder).bind(getItem(position))
        } else {
            (holder as ViewHolder).bind(getItem(position))
        }
    }

    fun resetSelection() {
        notifyItemChanged(selectedAdapterPosition)
        selectedAdapterPosition = -1
    }

    inner class ViewHolder(
        val binding: ContactListCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        @UiThread
        fun bind(contactModel: ContactAvatarModel) {
            with(binding) {
                model = contactModel

                binding.root.isSelected = bindingAdapterPosition == selectedAdapterPosition

                val previousItem = bindingAdapterPosition - 1
                val previousLetter = if (previousItem >= 0 && !getItem(previousItem).sortingName.isNullOrEmpty()) {
                    getItem(previousItem).sortingName?.get(0).toString()
                } else {
                    ""
                }

                val currentLetter = if (!contactModel.sortingName.isNullOrEmpty()) {
                    contactModel.sortingName?.get(0).toString()
                } else {
                    ""
                }
                val displayLetter = previousLetter.isEmpty() || currentLetter != previousLetter
                firstContactStartingByThatLetter = displayLetter

                executePendingBindings()
            }
        }
    }

    inner class FavouriteViewHolder(
        val binding: ContactFavouriteListCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        @UiThread
        fun bind(contactModel: ContactAvatarModel) {
            with(binding) {
                model = contactModel

                binding.root.isSelected = bindingAdapterPosition == selectedAdapterPosition

                executePendingBindings()
            }
        }
    }

    private class ContactDiffCallback : DiffUtil.ItemCallback<ContactAvatarModel>() {
        override fun areItemsTheSame(oldItem: ContactAvatarModel, newItem: ContactAvatarModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ContactAvatarModel, newItem: ContactAvatarModel): Boolean {
            // A replacement model has new LiveData/listeners even when its name and photo match.
            // Rebind so the badge observes the friend that receives current presence updates.
            return oldItem === newItem && newItem.compare(oldItem)
        }
    }
}
