package com.icl.demo.homepage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.icl.demo.models.NavigationNode
import com.icl.demo.models.ReportingAction
import com.icl.demo.models.ReportingItem

class ConfigNavigatorViewModel : ViewModel() {

    // Kotlin-native, no Java interop surprises
    private val navStack = ArrayDeque<List<NavigationNode>>()

    private val _currentNodes = MutableLiveData<List<NavigationNode>>()
    val currentNodes: LiveData<List<NavigationNode>> = _currentNodes
    fun canGoBack(): Boolean = navStack.size > 1
    fun isComingSoon(item: ReportingItem): Boolean = item.comingSoon
    fun isComingSoon(action: ReportingAction): Boolean = action.comingSoon

    fun start(root: List<ReportingItem>) {
        val nodes = root.map { NavigationNode.Category(it) }
        navStack.clear()
        navStack.addLast(nodes)      // push()
        _currentNodes.value = nodes
    }

    fun navigateInto(item: ReportingItem): Boolean {
        if (item.comingSoon) {
            return false
        }

        val children = item.children?.map { NavigationNode.Category(it) }
            ?: item.actions?.map { NavigationNode.ActionNode(it) }
            ?: emptyList()

        navStack.addLast(children)
        _currentNodes.value = children
        return true
    }

    fun goBack() {
        if (navStack.size > 1) {
            navStack.removeLast()    // pop()
            _currentNodes.value = navStack.last()  // peek()
        }
    }
}
