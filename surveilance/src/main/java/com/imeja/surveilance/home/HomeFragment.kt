package com.imeja.surveilance.home

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.imeja.surveilance.AddPatientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.imeja.surveilance.R
import com.imeja.surveilance.helpers.FormatterClass

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = ""//resources.getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(false)
        }
        val name = getUserNameFromDetails()
        val time = FormatterClass().getTimeOfDay()
        val fullText = "$time, \n\n$name"

        requireView().findViewById<TextView>(R.id.greetingText).text = time
        requireView().findViewById<TextView>(R.id.usernameText).text = name

        setOnClicks()
    }

    private fun getUserNameFromDetails(): String {
        val user = FormatterClass().getSharedPref("fullNames", requireContext())
        return user ?: "John Mdoe"
    }

    private fun handleClick(stage: String, title: String) {
        val bundle =
            Bundle().apply { putString(QUESTIONNAIRE_FILE_PATH_KEY, "add-vl.json") }

        FormatterClass()
            .saveSharedPref(
                "stage", stage,
                requireContext()
            )
        FormatterClass()
            .saveSharedPref(
                "title", title,
                requireContext()
            )

        findNavController().navigate(
            R.id.action_home_fragment_to_childFragment,
            bundle
        )
    }

    private fun setOnClicks() {
        requireView().findViewById<CardView>(R.id.item_search).setOnClickListener {
            handleClick("0", "Notifiable Diseases")
        }
        requireView().findViewById<CardView>(R.id.item_sync).setOnClickListener {
            handleClick("1", "Mass Immunization")
        }
        requireView().findViewById<CardView>(R.id.item_periodic_sync).setOnClickListener {
            handleClick("2", "Case Management")
        }
        requireView().findViewById<CardView>(R.id.item_crud).setOnClickListener {
            handleClick("3", "RCCE Tools")
        }
        requireView().findViewById<CardView>(R.id.item_assessments).setOnClickListener {
            Toast.makeText(requireContext(), "Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }
}