package com.imeja.surveilance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.imeja.surveilance.databinding.LandingPageItemBinding
import com.imeja.surveilance.viewmodels.HomeViewModel


class HomeRecyclerViewAdapter(
    private val onItemClick: (HomeViewModel.Layout) -> Unit,
    private val showIcon: Boolean = false
) :
    ListAdapter<HomeViewModel.Layout, LayoutViewHolder>(LayoutDiffUtil()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayoutViewHolder {
        return LayoutViewHolder(
            LandingPageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onItemClick, showIcon
        )
    }

    override fun onBindViewHolder(holder: LayoutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class LayoutViewHolder(
    val binding: LandingPageItemBinding,
    private val onItemClick: (HomeViewModel.Layout) -> Unit,
    private val showIcon: Boolean,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(layout: HomeViewModel.Layout) {
        if (!showIcon) {
            binding.iconView.visibility = View.GONE
        }
        binding.iconView.setImageResource(layout.iconId)
        binding.textView.text = binding.textView.context.getString(layout.textId)
        binding.root.setOnClickListener { onItemClick(layout) }
    }
}

class LayoutDiffUtil : DiffUtil.ItemCallback<HomeViewModel.Layout>() {
    override fun areItemsTheSame(
        oldLayout: HomeViewModel.Layout,
        newLayout: HomeViewModel.Layout,
    ) = oldLayout === newLayout

    override fun areContentsTheSame(
        oldLayout: HomeViewModel.Layout,
        newLayout: HomeViewModel.Layout,
    ) = oldLayout == newLayout
}

