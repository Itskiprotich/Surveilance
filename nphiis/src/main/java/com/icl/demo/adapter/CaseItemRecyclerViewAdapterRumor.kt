package com.icl.demo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.icl.demo.databinding.RumorItemViewBinding
import com.icl.demo.viewholder.RumorItemViewHolder
import com.icl.demo.viewmodels.CaseListViewModel


class CaseItemRecyclerViewAdapterRumor(
    private val onItemClicked: (CaseListViewModel.RumorItem) -> Unit,
) :
    ListAdapter<CaseListViewModel.RumorItem, RumorItemViewHolder>(
        PatientItemDiffCallback()
    ) {

    class PatientItemDiffCallback : DiffUtil.ItemCallback<CaseListViewModel.RumorItem>() {
        override fun areItemsTheSame(
            oldItem: CaseListViewModel.RumorItem,
            newItem: CaseListViewModel.RumorItem,
        ): Boolean = oldItem.resourceId == newItem.resourceId

        override fun areContentsTheSame(
            oldItem: CaseListViewModel.RumorItem,
            newItem: CaseListViewModel.RumorItem,
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