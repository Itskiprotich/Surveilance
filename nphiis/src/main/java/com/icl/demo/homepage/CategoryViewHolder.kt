package com.icl.demo.homepage

import android.app.Notification
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.icl.demo.R
import com.icl.demo.models.ReportingAction
import com.icl.demo.models.ReportingItem
import com.icl.demo.utils.FormatterClass

class CategoryViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView), BindableCategory {

    override fun bind(item: ReportingItem, click: (ReportingItem) -> Unit) {
        itemView.findViewById<TextView>(R.id.label).text = item.name
        itemView.setOnClickListener { click(item) }
        val iconView = itemView.findViewById<ImageView>(R.id.icon_view)

        if (item.showIcon && item.icon != null) {
            val iconRes = FormatterClass().resolveIcon(item.icon)

            if (iconRes != 0) {
                iconView.setImageResource(iconRes)
                iconView.visibility = View.VISIBLE
            } else {
                // Icon name exists but you forgot to map it – avoid crashes
                iconView.visibility = View.GONE
            }
        } else {
            iconView.visibility = View.GONE
        }
    }


}

class ActionViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView), BindableAction {

    override fun bind(action: ReportingAction, click: (ReportingAction) -> Unit) {
        itemView.findViewById<TextView>(R.id.label).text = action.label
        itemView.setOnClickListener { click(action) }
        val iconView = itemView.findViewById<ImageView>(R.id.icon_view)

        if (action.showIcon && action.icon != null) {
            val iconRes = FormatterClass().resolveIcon(action.icon)

            if (iconRes != 0) {
                iconView.setImageResource(iconRes)
                iconView.visibility = View.VISIBLE
            } else {
                // Icon name exists but you forgot to map it – avoid crashes
                iconView.visibility = View.GONE
            }
        } else {
            iconView.visibility = View.GONE
        }
    }

}
