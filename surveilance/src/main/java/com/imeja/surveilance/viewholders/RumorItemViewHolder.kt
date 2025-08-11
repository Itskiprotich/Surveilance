package com.imeja.surveilance.viewholders

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.imeja.surveilance.databinding.RumorItemViewBinding
import com.imeja.surveilance.viewmodels.PatientListViewModel


class RumorItemViewHolder(binding: RumorItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
    private val nameView: TextView = binding.name
    private val epid: TextView = binding.epid
    private val county: TextView = binding.county
    private val subCounty: TextView = binding.subCounty
    private val dateReported: TextView = binding.dateReported
    private val labResults: TextView = binding.labResults

    fun bindTo(
        patientItem: PatientListViewModel.RumorItem,
        onItemClicked: (PatientListViewModel.RumorItem) -> Unit,
    ) {
        this.nameView.text = patientItem.mohName
        this.epid.text = patientItem.directorate
        this.county.text = patientItem.county
        this.subCounty.text = patientItem.subCounty
        this.dateReported.text = patientItem.division
        this.labResults.text = patientItem.village

        this.itemView.setOnClickListener { onItemClicked(patientItem) }
    }


}