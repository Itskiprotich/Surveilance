package com.icl.demo.homepage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.icl.demo.models.NavigationNode
import com.icl.demo.models.ReportingAction
import com.icl.demo.models.ReportingItem

class ConfigNavigatorViewModel : ViewModel() {

    // Kotlin-native, no Java interop surprises
    data class NavLevel(
        val title: String,
        val nodes: List<NavigationNode>
    )

    private val navStack = ArrayDeque<NavLevel>()

    private val _currentNodes = MutableLiveData<List<NavigationNode>>()
    val currentNodes: LiveData<List<NavigationNode>> = _currentNodes

    private val _currentTitle = MutableLiveData<String>()
    val currentTitle: LiveData<String> = _currentTitle

    fun canGoBack(): Boolean = navStack.size > 1
    fun isComingSoon(item: ReportingItem): Boolean = item.comingSoon
    fun isComingSoon(action: ReportingAction): Boolean = action.comingSoon

    fun currentTitle(): String {
        return navStack.lastOrNull()?.title ?: ""
    }


    fun start(root: List<ReportingItem>) {
        val nodes = root.map { NavigationNode.Category(it) }
        navStack.clear()
        navStack.addLast(NavLevel(title = "Home", nodes = nodes))
        _currentNodes.value = nodes
        _currentTitle.value = "Home"
    }


    fun navigateInto(item: ReportingItem): Boolean {
        if (item.comingSoon) return false

        val children = item.children?.map { NavigationNode.Category(it) }
            ?: item.actions?.map { NavigationNode.ActionNode(it) }
            ?: emptyList()

        navStack.addLast(NavLevel(item.name, children))
        _currentNodes.value = children
        _currentTitle.value = item.name
        return true
    }


    fun goBack() {
        if (navStack.size > 1) {
            navStack.removeLast()
            val level = navStack.last()
            _currentNodes.value = level.nodes
            _currentTitle.value = level.title
        }
    }

}
