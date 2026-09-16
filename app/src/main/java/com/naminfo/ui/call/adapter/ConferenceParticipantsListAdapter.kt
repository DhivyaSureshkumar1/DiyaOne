package com.naminfo.ui.call.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.naminfo.R
import com.naminfo.databinding.CallConferenceParticipantListCellBinding
import com.naminfo.ui.call.conference.model.ConferenceParticipantModel

class ConferenceParticipantsListAdapter :
    ListAdapter<ConferenceParticipantModel, RecyclerView.ViewHolder>(ParticipantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: CallConferenceParticipantListCellBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.call_conference_participant_list_cell,
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
        val binding: CallConferenceParticipantListCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        @UiThread
        fun bind(participantModel: ConferenceParticipantModel) {
            with(binding) {
                model = participantModel

                executePendingBindings()
            }
        }
    }

    private class ParticipantDiffCallback : DiffUtil.ItemCallback<ConferenceParticipantModel>() {
        override fun areItemsTheSame(
            oldItem: ConferenceParticipantModel,
            newItem: ConferenceParticipantModel
        ): Boolean {
            return oldItem.sipUri == newItem.sipUri
        }

        override fun areContentsTheSame(
            oldItem: ConferenceParticipantModel,
            newItem: ConferenceParticipantModel
        ): Boolean {
            return false
        }
    }
}
