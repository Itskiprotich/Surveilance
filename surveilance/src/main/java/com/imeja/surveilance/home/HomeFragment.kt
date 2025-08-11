package com.imeja.surveilance.home

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.imeja.surveilance.R

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = ""//resources.getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(false)
        }
        setOnClicks()
    }

    private fun setOnClicks() {
        requireView().findViewById<CardView>(R.id.item_search).setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToPatientList())
        }
        requireView().findViewById<CardView>(R.id.item_sync).setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToSyncFragment())
        }
        requireView().findViewById<CardView>(R.id.item_periodic_sync).setOnClickListener {
            findNavController()
                .navigate(HomeFragmentDirections.actionHomeFragmentToPeriodicSyncFragment())
        }
        requireView().findViewById<CardView>(R.id.item_crud).setOnClickListener {
            findNavController()
                .navigate(HomeFragmentDirections.actionHomeFragmentToCrudOperationFragment())
        }
    }
}