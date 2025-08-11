package com.imeja.surveilance.viewholders

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.imeja.surveilance.databinding.ContactItemBinding
import com.imeja.surveilance.viewmodels.PatientListViewModel


class ContactHolder(binding: ContactItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    private val nameView: TextView = binding.valueName
    private val epid: TextView = binding.valueEdip

    fun bindTo(
        patientItem: PatientListViewModel.ContactResults,
        onItemClicked: (PatientListViewModel.ContactResults) -> Unit,
    ) {
        this.nameView.text = patientItem.name
        this.epid.text = patientItem.epid

        this.itemView.setOnClickListener { onItemClicked(patientItem) }
    }
}