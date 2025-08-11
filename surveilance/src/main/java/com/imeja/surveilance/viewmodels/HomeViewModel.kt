package com.imeja.surveilance.viewmodels

import android.app.Application
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.imeja.surveilance.R

class HomeViewModel(application: Application, private val state: SavedStateHandle) :
    AndroidViewModel(application) {

    fun getLayoutList(): List<Layout> {
        return Layout.values().toList()
    }

    fun getDiseasesList(int: Int): List<Diseases> {
        return Diseases.values().filter { it.count == int }.toList()
    }


    fun getNotifiableList(): List<Diseases> {
        return Diseases.values().filter { it.count == 0 }.toList()
    }

    fun getMassList(): List<Diseases> {
        return Diseases.values().filter { it.count == 1 }.toList()
    }

    fun getCaseList(): List<Diseases> {
        return Diseases.values().filter { it.count == 2 }.toList()
    }

    fun getSocialList(): List<Diseases> {
        return Diseases.values().filter { it.count == 3 }.toList()
    }

    fun getMOHList(): List<Diseases> {
        return Diseases.values().filter { it.count == 100 }.toList()
    }

    fun getAssessmentList(): List<Diseases> {
        return Diseases.values().filter { it.count == 4 }.toList()
    }


    enum class Diseases(
        @DrawableRes val iconId: Int,
        @StringRes val textId: Int,
        val count: Int,
        val level: Int
    ) {

        RUMOR_TOOL(
            R.drawable.virus,
            R.string.rumor_tool, 3, 30
        ),
        SOCIAL_INV(
            R.drawable.virus,
            R.string.social_inv, 3, 31
        ),
        VL_FORM(
            R.drawable.virus,
            R.string.vl_form, 2, 20
        ),

        // Mass

        MPOX(
            R.drawable.virus,
            R.string.mpox, 1, 13
        ),
        POLIO(
            R.drawable.virus,
            R.string.polio, 1, 10
        ),
        MEASLES_IMM(
            R.drawable.virus,
            R.string.measles_imm, 1, 11
        ),
        CHOLERA(
            R.drawable.virus,
            R.string.cholera, 1, 12
        ),


        // Weekly Reportable Diseases
        MOH_505(
            R.drawable.virus,
            R.string.moh505_form, 100, 100
        ),


        // Top Layer

        IMMEDIATE(
            R.drawable.virus,
            R.string.immediate_reportable, 0, 0
        ),
        WEEKLY(
            R.drawable.virus,
            R.string.weekly_reported, 0, 1
        ),
        MONTHLY(
            R.drawable.virus,
            R.string.monthly_reported, 0, 2
        ),


        MEASLES(
            R.drawable.virus,
            R.string.measles, 6, 0
        ),
        AFP(
            R.drawable.virus,
            R.string.afp, 6, 0
        ),


        TALLY_SHEET(
            R.drawable.virus,
            R.string.tally, 700, 0
        ),
        SUPERVISOR_CHECKLIST(
            R.drawable.virus,
            R.string.supervisor, 700, 0
        ),

        RUMOR(
            R.drawable.virus,
            R.string.rumor_tracking, 7, 0
        ),
    }

    enum class Layout(
        @DrawableRes val iconId: Int,
        @StringRes val textId: Int,
        val count: Int,
    ) {

        NOTIFIABLE(
            R.drawable.virus,
            R.string.notifiable,
            0
        ),
        MASS(
            R.drawable.syringe,
            R.string.mass, 1
        ),
        CASE(
            R.drawable.clipboard,
            R.string.case_management, 2
        ),
        SOCIAL(
            R.drawable.people,
            R.string.social, 3
        ),
        SURVEY(
            R.drawable.presentation,
            R.string.surveys, 4
        )

    }
}