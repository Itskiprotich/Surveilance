package com.imeja.surveilance.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.imeja.surveilance.databinding.DiseaseHolderBinding
import com.imeja.surveilance.viewholders.DiseaseItemViewHolder
import com.imeja.surveilance.viewmodels.PatientListViewModel

class PatientDiseaseRecyclerViewAdapter(
    private val onItemClicked: (PatientListViewModel.CaseDiseaseData) -> Unit,
) :
    ListAdapter<PatientListViewModel.CaseDiseaseData, DiseaseItemViewHolder>(
        PatientItemDiffCallback()) {

  class PatientItemDiffCallback : DiffUtil.ItemCallback<PatientListViewModel.CaseDiseaseData>() {
    override fun areItemsTheSame(
        oldItem: PatientListViewModel.CaseDiseaseData,
        newItem: PatientListViewModel.CaseDiseaseData,
    ): Boolean = oldItem.logicalId == newItem.logicalId

    override fun areContentsTheSame(
        oldItem: PatientListViewModel.CaseDiseaseData,
        newItem: PatientListViewModel.CaseDiseaseData,
    ): Boolean = oldItem.logicalId == newItem.logicalId
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiseaseItemViewHolder {
    return DiseaseItemViewHolder(
        DiseaseHolderBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )
  }

  override fun onBindViewHolder(holder: DiseaseItemViewHolder, position: Int) {
    val item = currentList[position]
    holder.bindTo(item, onItemClicked)
  }
}
