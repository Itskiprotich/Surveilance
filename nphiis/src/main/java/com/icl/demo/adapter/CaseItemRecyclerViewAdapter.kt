package com.icl.demo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.icl.demo.databinding.CaseListItemViewBinding
import com.icl.demo.viewholder.CaseItemViewHolder
import com.icl.demo.viewmodels.CaseListViewModel


class CaseItemRecyclerViewAdapter(
    private val onItemClicked: (CaseListViewModel.PatientItem) -> Unit,
    private val listingTitle: String,
    private val context: Context
) : ListAdapter<CaseListViewModel.PatientItem, CaseItemViewHolder>(
    PatientItemDiffCallback()
) {
    // Keep a full copy of the unfiltered list
    private var fullList: List<CaseListViewModel.PatientItem> = emptyList()

    class PatientItemDiffCallback : DiffUtil.ItemCallback<CaseListViewModel.PatientItem>() {
        override fun areItemsTheSame(
            oldItem: CaseListViewModel.PatientItem,
            newItem: CaseListViewModel.PatientItem
        ): Boolean = oldItem.resourceId == newItem.resourceId

        override fun areContentsTheSame(
            oldItem: CaseListViewModel.PatientItem,
            newItem: CaseListViewModel.PatientItem
        ): Boolean = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CaseItemViewHolder {
        return CaseItemViewHolder(
            CaseListItemViewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CaseItemViewHolder, position: Int) {
        val item = currentList[position]
        holder.bindTo(item, onItemClicked, listingTitle, context)
    }

    /**
     * Store full list and display it
     */
    fun setData(list: List<CaseListViewModel.PatientItem>) {
        fullList = list
        submitList(list)
    }

    /**
     * Filter patients by query text
     */
    fun filter(query: String) {
        val filteredList = if (query.isBlank()) {
            fullList
        } else {
            fullList.filter { patient ->
                patient.epid.contains(query, ignoreCase = true) ||
                        patient.name.contains(query, ignoreCase = true)
            }
        }
        submitList(filteredList)
    }
}
