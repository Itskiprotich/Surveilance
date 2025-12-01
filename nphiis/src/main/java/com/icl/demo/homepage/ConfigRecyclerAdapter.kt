package com.icl.demo.homepage

import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.icl.demo.R
import com.icl.demo.models.NavigationNode
import com.icl.demo.models.ReportingAction
import com.icl.demo.models.ReportingItem

class ConfigRecyclerAdapter(
    private val onCategoryClick: (ReportingItem) -> Unit,
    private val onActionClick: (ReportingAction) -> Unit,
    private val factory: ViewHolderFactory
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<NavigationNode>()

    fun submit(list: List<NavigationNode>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is NavigationNode.Category -> factory.typeCategory
            is NavigationNode.ActionNode -> factory.typeAction
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return factory.create(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val node = items[position]) {
            is NavigationNode.Category -> (holder as BindableCategory)
                .bind(node.item, onCategoryClick)

            is NavigationNode.ActionNode -> (holder as BindableAction)
                .bind(node.action, onActionClick)
        }
        holder.itemView.startAnimation(
            AnimationUtils.loadAnimation(holder.itemView.context, R.anim.item_animation_fall_down)
        )
    }

    override fun getItemCount() = items.size
}
