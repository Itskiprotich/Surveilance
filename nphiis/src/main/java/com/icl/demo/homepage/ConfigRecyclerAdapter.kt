package com.icl.demo.homepage

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
        val node = items[position]
        when {
            node is NavigationNode.Category -> (holder as BindableCategory)
                .bind(node.item, onCategoryClick)

            node is NavigationNode.ActionNode -> (holder as BindableAction)
                .bind(node.action, onActionClick)
        }
    }

    override fun getItemCount() = items.size
}
