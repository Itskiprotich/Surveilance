package com.imeja.surveilance.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.imeja.surveilance.databinding.ContactItemBinding
import com.imeja.surveilance.viewholders.ContactHolder
import com.imeja.surveilance.viewmodels.PatientListViewModel


class ContactsAdapter(
    private val onItemClicked: (PatientListViewModel.ContactResults) -> Unit,
) :
    ListAdapter<PatientListViewModel.ContactResults, ContactHolder>(
        PatientItemDiffCallback()) {

    class PatientItemDiffCallback : DiffUtil.ItemCallback<PatientListViewModel.ContactResults>() {
        override fun areItemsTheSame(
            oldItem: PatientListViewModel.ContactResults,
            newItem: PatientListViewModel.ContactResults,
        ): Boolean = oldItem.childId == newItem.childId

        override fun areContentsTheSame(
            oldItem: PatientListViewModel.ContactResults,
            newItem: PatientListViewModel.ContactResults,
        ): Boolean = oldItem.childId == newItem.childId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactHolder {
        return ContactHolder(
            ContactItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )
    }

    override fun onBindViewHolder(holder: ContactHolder, position: Int) {
        val item = currentList[position]
        holder.bindTo(item, onItemClicked)
    }
}
