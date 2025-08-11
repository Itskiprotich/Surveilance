package com.imeja.surveilance.viewholders

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.imeja.surveilance.databinding.CaseDetailBinding
import com.imeja.surveilance.viewmodels.PatientListViewModel

class PatientDetailItemViewHolder(binding: CaseDetailBinding) :
    RecyclerView.ViewHolder(binding.root) {
  private val status: TextView = binding.status
  private val reason: TextView = binding.reason

  fun bindTo(
      patientItem: PatientListViewModel.EncounterItem,
      onItemClicked: (PatientListViewModel.EncounterItem) -> Unit,
  ) {
    this.status.text = patientItem.status
    this.reason.text = patientItem.reasonCode
    this.itemView.setOnClickListener { onItemClicked(patientItem) }
  }
}
