/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.icl.demo

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import com.google.android.fhir.DatabaseErrorStrategy.RECREATE_AT_OPEN
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineConfiguration
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.NetworkConfiguration
import com.google.android.fhir.ServerConfiguration
import com.google.android.fhir.datacapture.DataCaptureConfig
import com.google.android.fhir.datacapture.XFhirQueryResolver
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.PeriodicSyncConfiguration
import com.google.android.fhir.sync.RepeatInterval
import com.google.android.fhir.sync.Sync
import com.google.android.fhir.sync.remote.HttpLogger
import com.icl.demo.data.DemoFhirSyncWorker
import com.icl.demo.location.ContribQuestionnaireItemViewHolderFactoryMatchersProviderFactory
import com.icl.demo.utils.Constants.BASE_URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

class FhirApplication : Application(), DataCaptureConfig.Provider {
    // Only initiate the FhirEngine when used for the first time, not when the app is created.
    private val fhirEngine: FhirEngine by lazy { constructFhirEngine() }

    private var dataCaptureConfig: DataCaptureConfig? = null

    private val dataStore by lazy { DemoDataStore(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        FhirEngineProvider.init(
            FhirEngineConfiguration(
                enableEncryptionIfSupported = false,
                RECREATE_AT_OPEN,
                ServerConfiguration(
                    BASE_URL,
                    httpLogger =
                        HttpLogger(
                            HttpLogger.Configuration(
                                if (BuildConfig.DEBUG) HttpLogger.Level.BODY else HttpLogger.Level.BASIC,
                            ),
                        ) {
                            Timber.tag("App-HttpLog").d(it)
                        },
                    networkConfiguration = NetworkConfiguration(uploadWithGzip = false),
                ),
            ),
        )

        dataCaptureConfig =
            DataCaptureConfig().apply {
                urlResolver = ReferenceUrlResolver(this@FhirApplication as Context)
                questionnaireItemViewHolderFactoryMatchersProviderFactory =
                    ContribQuestionnaireItemViewHolderFactoryMatchersProviderFactory
                xFhirQueryResolver =
                    XFhirQueryResolver { it -> fhirEngine.search(it).map { it.resource } }
            }
        setupPeriodicSync()
    }

    private fun setupPeriodicSync() {
        appScope.launch {
            try {
                Sync.periodicSync<DemoFhirSyncWorker>(
                    this@FhirApplication,
                    periodicSyncConfiguration = PeriodicSyncConfiguration(
                        syncConstraints = Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                        repeat = RepeatInterval(interval = 15, timeUnit = TimeUnit.MINUTES)
                    )

                ).catch { throwable ->
                    Log.e(
                        "FHIR_SYNC",
                        "Error setting up periodic sync: ${throwable.message}",
                        throwable
                    )
                }
                    .collect {
                    }
            } catch (e: Exception) {
                Log.e("FHIR_SYNC", "Error setting up periodic sync: ${e.message}", e)
            }
        }
    }
    private fun constructFhirEngine(): FhirEngine {
        return FhirEngineProvider.getInstance(this)
    }

    companion object {
        fun fhirEngine(context: Context) =
            (context.applicationContext as FhirApplication).fhirEngine

        fun dataStore(context: Context) = (context.applicationContext as FhirApplication).dataStore
    }

    override fun getDataCaptureConfig(): DataCaptureConfig =
        dataCaptureConfig ?: DataCaptureConfig()
}
