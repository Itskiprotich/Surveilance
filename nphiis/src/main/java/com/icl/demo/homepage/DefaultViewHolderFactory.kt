package com.icl.demo.homepage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.icl.demo.R

class DefaultViewHolderFactory(
    private val layoutInflater: LayoutInflater
) : ViewHolderFactory {

    override val typeCategory = 1
    override val typeAction = 2

    override fun create(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            typeCategory -> {
                val view = layoutInflater.inflate(R.layout.item_category, parent, false)
                CategoryViewHolder(view)
            }

            typeAction -> {
                val view = layoutInflater.inflate(R.layout.item_action, parent, false)
                ActionViewHolder(view)
            }

            else -> error("Unknown viewType: $viewType")
        }
    }
}
