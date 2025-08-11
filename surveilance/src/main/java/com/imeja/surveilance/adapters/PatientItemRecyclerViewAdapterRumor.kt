package com.imeja.surveilance.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.imeja.surveilance.databinding.RumorItemViewBinding
import com.imeja.surveilance.viewholders.RumorItemViewHolder
import com.imeja.surveilance.viewmodels.PatientListViewModel


class PatientItemRecyclerViewAdapterRumor(
    private val onItemClicked: (PatientListViewModel.RumorItem) -> Unit,
) :
    ListAdapter<PatientListViewModel.RumorItem, RumorItemViewHolder>(
        PatientItemDiffCallback()
    ) {

    class PatientItemDiffCallback : DiffUtil.ItemCallback<PatientListViewModel.RumorItem>() {
        override fun areItemsTheSame(
            oldItem: PatientListViewModel.RumorItem,
            newItem: PatientListViewModel.RumorItem,
        ): Boolean = oldItem.resourceId == newItem.resourceId

        override fun areContentsTheSame(
            oldItem: PatientListViewModel.RumorItem,
            newItem: PatientListViewModel.RumorItem,
        ): Boolean = oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RumorItemViewHolder {
        return RumorItemViewHolder(
            RumorItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )
    }

    override fun onBindViewHolder(holder: RumorItemViewHolder, position: Int) {
        val item = currentList[position]
        holder.bindTo(item, onItemClicked)
    }
}
