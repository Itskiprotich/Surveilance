package com.imeja.surveilance.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.imeja.surveilance.databinding.PatientListItemViewBinding
import com.imeja.surveilance.viewholders.PatientItemViewHolder
import com.imeja.surveilance.viewmodels.PatientListViewModel

class PatientItemRecyclerViewAdapter(
    private val onItemClicked: (PatientListViewModel.PatientItem) -> Unit,
    private val listingTitle: String,
    private val context: Context
) :
    ListAdapter<PatientListViewModel.PatientItem, PatientItemViewHolder>(
        PatientItemDiffCallback()
    ) {

    class PatientItemDiffCallback : DiffUtil.ItemCallback<PatientListViewModel.PatientItem>() {
        override fun areItemsTheSame(
            oldItem: PatientListViewModel.PatientItem,
            newItem: PatientListViewModel.PatientItem,
        ): Boolean = oldItem.resourceId == newItem.resourceId

        override fun areContentsTheSame(
            oldItem: PatientListViewModel.PatientItem,
            newItem: PatientListViewModel.PatientItem,
        ): Boolean = oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientItemViewHolder {
        return PatientItemViewHolder(
            PatientListItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )
    }

    override fun onBindViewHolder(holder: PatientItemViewHolder, position: Int) {
        val item = currentList[position]
        holder.bindTo(item, onItemClicked, listingTitle, context)
    }
}
