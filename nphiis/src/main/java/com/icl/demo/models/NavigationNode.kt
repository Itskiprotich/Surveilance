package com.icl.demo.models

import kotlinx.serialization.Serializable

sealed class NavigationNode {
    data class Category(val item: ReportingItem) : NavigationNode()
    data class ActionNode(val action: ReportingAction) : NavigationNode()
}

@Serializable
data class ReportingConfig(
    val reporting: List<ReportingItem>
)

@Serializable
data class ReportingItem(
    val name: String,
    val code: String,
    val children: List<ReportingItem>? = null,
    val actions: List<ReportingAction>? = null,
    val comingSoon: Boolean = false,
    val layout: String? = null,
    val icon: String? = null,          // we ignore this except for future use
    val showIcon: Boolean = false
)

@Serializable
data class ReportingAction(
    val type: String,
    val label: String,
    val questionnaire: String? = null,
    val target: String? = null,
    val comingSoon: Boolean = false,
    val icon: String? = null,          // we ignore this except for future use
    val showIcon: Boolean = false
)

enum class LayoutMode {
    LINEAR,
    GRID
}
