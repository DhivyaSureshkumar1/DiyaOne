package com.naminfo.ui.call.adapter

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
import com.naminfo.databinding.CallListCellBinding
import com.naminfo.ui.call.model.CallModel
import com.naminfo.utils.Event

class CallsListAdapter(private val showTransferIconInsteadOfCallState: Boolean = false) :
    ListAdapter<CallModel, RecyclerView.ViewHolder>(CallDiffCallback()) {
    var selectedAdapterPosition = -1

    val callClickedEvent: MutableLiveData<Event<CallModel>> by lazy {
        MutableLiveData()
    }

    val callLongClickedEvent: MutableLiveData<Event<CallModel>> by lazy {
        MutableLiveData()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: CallListCellBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.call_list_cell,
            parent,
            false
        )
        val viewHolder = ViewHolder(binding)
        binding.apply {
            lifecycleOwner = parent.findViewTreeLifecycleOwner()
            showTransferIcon = showTransferIconInsteadOfCallState

            setOnClickListener {
                callClickedEvent.value = Event(model!!)
            }

            setOnLongClickListener {
                selectedAdapterPosition = viewHolder.bindingAdapterPosition
                root.isSelected = true
                callLongClickedEvent.value = Event(model!!)
                true
            }
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(getItem(position))
    }

    fun resetSelection() {
        notifyItemChanged(selectedAdapterPosition)
        selectedAdapterPosition = -1
    }

    inner class ViewHolder(
        val binding: CallListCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        @UiThread
        fun bind(callModel: CallModel) {
            with(binding) {
                model = callModel

                binding.root.isSelected = bindingAdapterPosition == selectedAdapterPosition

                executePendingBindings()
            }
        }
    }

    private class CallDiffCallback : DiffUtil.ItemCallback<CallModel>() {
        override fun areItemsTheSame(oldItem: CallModel, newItem: CallModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CallModel, newItem: CallModel): Boolean {
            return false
        }
    }
}
