package com.imeja.surveilance.viewholders

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.imeja.surveilance.R
import com.imeja.surveilance.databinding.LabHolderBinding
import com.imeja.surveilance.viewmodels.PatientListViewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class LabItemViewHolder(binding: LabHolderBinding) : RecyclerView.ViewHolder(binding.root) {
    private val dateSpecimenReceivedView: TextView = binding.tvDateSpecimenReceived
    private val specimenConditionView: TextView = binding.tvSpecimenCondition
    private val measlesIgmView: TextView = binding.tvMeaslesIgm
    private val rubellaIgmView: TextView = binding.tvRubellaIgm
    private val dateLabSentResultsView: TextView = binding.tvDateLabSentResults
    private val finalClassificationView: TextView = binding.tvFinalClassification

    fun bindTo(
        patientItem: PatientListViewModel.CaseLabResultsData,
        onItemClicked: (PatientListViewModel.CaseLabResultsData) -> Unit,
    ) {
        this.dateSpecimenReceivedView.text = handleDate(patientItem.dateSpecimenReceived)
        this.specimenConditionView.text = patientItem.specimenCondition
        this.measlesIgmView.text = patientItem.measlesIgM
        this.rubellaIgmView.text = patientItem.rubellaIgM
        this.dateLabSentResultsView.text = handleDate(patientItem.dateLabSentResults)
        this.finalClassificationView.text = patientItem.finalClassification
        if (patientItem.finalClassification.trim() == "Confirmed by lab") {

            this.finalClassificationView.setTextColor(this.finalClassificationView.context.getColor(
                R.color.red))
        }
        this.itemView.setOnClickListener { onItemClicked(patientItem) }
    }

    private fun handleDate(string: String): String {
        return try {
            val offsetDateTime = OffsetDateTime.parse(string)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val data = offsetDateTime.format(formatter)
            data
        } catch (e: Exception) {
            string
        }
    }
}
