package com.imeja.surveilance.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.imeja.surveilance.cases.child.GroupFragment
import com.imeja.surveilance.models.OutputGroup

class GroupPagerAdapter(
    fa: FragmentActivity,
    private val groups: List<OutputGroup>,
    private val customFragments: List<Pair<String, Fragment>> = emptyList()
) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = groups.size + customFragments.size

    override fun createFragment(position: Int): Fragment {
        return if (position < groups.size) {
            val group = groups[position]
            GroupFragment.newInstance(group)
        } else {
            customFragments[position - groups.size].second
        }
    }

    fun getTabTitle(position: Int): String {
        return if (position < groups.size) {
            groups[position].text
        } else {
            customFragments[position - groups.size].first
        }
    }
}

