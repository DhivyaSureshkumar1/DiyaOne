package com.naminfo.ui.main.history.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.naminfo.R
import com.naminfo.databinding.HistoryCallListCellBinding
import com.naminfo.ui.main.history.model.CallLogHistoryModel

class ContactHistoryListAdapter : ListAdapter<CallLogHistoryModel, RecyclerView.ViewHolder>(
    CallHistoryDiffCallback()
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: HistoryCallListCellBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.history_call_list_cell,
            parent,
            false
        )
        binding.lifecycleOwner = parent.findViewTreeLifecycleOwner()
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(getItem(position))
    }

    class ViewHolder(
        val binding: HistoryCallListCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        @UiThread
        fun bind(callLogHistoryModel: CallLogHistoryModel) {
            with(binding) {
                model = callLogHistoryModel
                executePendingBindings()
            }
        }
    }

    private class CallHistoryDiffCallback : DiffUtil.ItemCallback<CallLogHistoryModel>() {
        override fun areItemsTheSame(oldItem: CallLogHistoryModel, newItem: CallLogHistoryModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CallLogHistoryModel, newItem: CallLogHistoryModel): Boolean {
            return false
        }
    }
}
