package com.imeja.surveilance.adapters

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.imeja.surveilance.databinding.LandingPageItemBinding
import com.imeja.surveilance.viewmodels.HomeViewModel


class DiseasesRecyclerViewAdapter(
    private val onItemClick: (HomeViewModel.Diseases) -> Unit,

    ) :
    ListAdapter<HomeViewModel.Diseases, DiseaseViewHolder>(DiseaseDiffUtil()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiseaseViewHolder {
        return DiseaseViewHolder(
            LandingPageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onItemClick,
        )
    }

    override fun onBindViewHolder(holder: DiseaseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class DiseaseViewHolder(
    val binding: LandingPageItemBinding,
    private val onItemClick: (HomeViewModel.Diseases) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(layout: HomeViewModel.Diseases) {
        try {
            val desiredHeightInDp = 140f
            // Use your helper function
            val desiredHeightInPixels = dpToPx(desiredHeightInDp, binding.root.context)

            val layoutParams = binding.cardHolder.layoutParams
            layoutParams.height = desiredHeightInPixels // Your dpToPx already returns Int
            binding.cardHolder.layoutParams = layoutParams
            // I also need to align the textview to center

        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding.iconView.apply {
            setImageResource(layout.iconId)
            visibility = View.GONE
        }

        binding.textView.text =
            binding.textView.context.getString(layout.textId)
        binding.root.setOnClickListener { onItemClick(layout) }
    }

    private fun dpToPx(dp: Float, context: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }
}

class DiseaseDiffUtil : DiffUtil.ItemCallback<HomeViewModel.Diseases>() {
    override fun areItemsTheSame(
        oldLayout: HomeViewModel.Diseases,
        newLayout: HomeViewModel.Diseases,
    ) = oldLayout === newLayout

    override fun areContentsTheSame(
        oldLayout: HomeViewModel.Diseases,
        newLayout: HomeViewModel.Diseases,
    ) = oldLayout == newLayout
}

