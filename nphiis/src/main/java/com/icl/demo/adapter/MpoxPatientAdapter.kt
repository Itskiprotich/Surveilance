package com.icl.demo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.icl.demo.R
import com.icl.demo.adapter.CaseItemRecyclerViewAdapter.PatientItemDiffCallback
import com.icl.demo.databinding.CaseListItemViewBinding
import com.icl.demo.viewmodels.CaseListViewModel


class MpoxPatientAdapter(
    private val onItemClicked: (CaseListViewModel.PatientItem) -> Unit,
    private val listingTitle: String,
    private val context: Context
) : ListAdapter<CaseListViewModel.PatientItem, MpoxPatientAdapter.PatientViewHolder>(
    PatientDiffCallback()
) {

    inner class PatientViewHolder(
        private val binding: CaseListItemViewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(patientItem: CaseListViewModel.PatientItem) = with(binding) {
            root.setOnClickListener { onItemClicked(patientItem) }

            name.text = patientItem.name
            epid.text = patientItem.epid
            county.text = patientItem.county
            subCounty.text = patientItem.subCounty
            dateReported.text = patientItem.caseOnsetDate

            labResults.text = patientItem.labResults

            if (!patientItem.epid.startsWith("KEN")) {
                lnCountyLabels.visibility = View.GONE
                lnCountyDetails.visibility = View.GONE
                labResults.text = patientItem.subCounty
            }

            lnFinalClassification.visibility = View.GONE

            tvDateLabel.text = "Occupation"
            tvLabLabel.text = "Vaccination Center"
            labResults.text = patientItem.vaccinationCenter
            dateReported.text = patientItem.occupation
            tvEpidLabel.text = "Serial Number"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = CaseListItemViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PatientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun appendPatients(newItems: List<CaseListViewModel.PatientItem>) {
        val updated = currentList.toMutableList()
        updated.addAll(newItems)
        submitList(updated)
    }
}

// DiffUtil callback
class PatientDiffCallback : DiffUtil.ItemCallback<CaseListViewModel.PatientItem>() {
    override fun areItemsTheSame(
        oldItem: CaseListViewModel.PatientItem,
        newItem: CaseListViewModel.PatientItem
    ) = oldItem.epid == newItem.epid

    override fun areContentsTheSame(
        oldItem: CaseListViewModel.PatientItem,
        newItem: CaseListViewModel.PatientItem
    ) = oldItem == newItem
}
