package com.imeja.surveilance.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.imeja.surveilance.PatientDetailItemViewHolder
import com.imeja.surveilance.databinding.CaseDetailBinding
import com.imeja.surveilance.viewmodels.PatientListViewModel

class PatientDetailsRecyclerViewAdapter(
    private val onItemClicked: (PatientListViewModel.EncounterItem) -> Unit,
) :
    ListAdapter<PatientListViewModel.EncounterItem, PatientDetailItemViewHolder>(
        PatientItemDiffCallback()
    ) {

    class PatientItemDiffCallback : DiffUtil.ItemCallback<PatientListViewModel.EncounterItem>() {
        override fun areItemsTheSame(
            oldItem: PatientListViewModel.EncounterItem,
            newItem: PatientListViewModel.EncounterItem,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: PatientListViewModel.EncounterItem,
            newItem: PatientListViewModel.EncounterItem,
        ): Boolean = oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientDetailItemViewHolder {
        return PatientDetailItemViewHolder(
            CaseDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )
    }

    override fun onBindViewHolder(holder: PatientDetailItemViewHolder, position: Int) {
        val item = currentList[position]
        holder.bindTo(item, onItemClicked)
    }
}
