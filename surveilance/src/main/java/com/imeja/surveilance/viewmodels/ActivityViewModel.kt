package com.imeja.surveilance.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.imeja.surveilance.FhirApplication
import com.imeja.surveilance.extensions.isFirstLaunch
import com.imeja.surveilance.extensions.setFirstLaunchCompleted
import com.imeja.surveilance.helpers.PatientCreationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ActivityViewModel(application: Application) : AndroidViewModel(application) {
  private var fhirEngine: FhirEngine = FhirApplication.Companion.fhirEngine(application.applicationContext)

  fun createPatientsOnAppFirstLaunch() {
    viewModelScope.launch(Dispatchers.IO) {
      if (getApplication<FhirApplication>().applicationContext.isFirstLaunch()) {
        Timber.Forest.i("Creating patients on first launch")
        PatientCreationHelper.createSamplePatients().forEach { fhirEngine.create(it) }
        getApplication<FhirApplication>().applicationContext.setFirstLaunchCompleted()
        Timber.Forest.i("Patients created on first launch")
      }
    }
  }
}