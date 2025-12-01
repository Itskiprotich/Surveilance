package com.icl.demo.homepage

import android.app.Notification
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.icl.demo.models.ReportingAction
import com.icl.demo.models.ReportingItem

interface ViewHolderFactory {
    val typeCategory: Int
    val typeAction: Int

    fun create(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder
}

interface BindableCategory {
    fun bind(item: ReportingItem, click: (ReportingItem) -> Unit)
}

interface BindableAction {
    fun bind(action: ReportingAction, click: (ReportingAction) -> Unit)
}
