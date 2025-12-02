package com.icl.demo.utils

import android.content.Context
import android.util.Log
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import java.util.zip.GZIPInputStream

class FhirBundleLoader(private val context: Context) {

    suspend fun loadBundleJson(fileName: String): String {
        return withContext(Dispatchers.IO) {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        }
    }

    suspend fun loadCompressedBundleJson(fileName: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open(fileName)
                GZIPInputStream(inputStream).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Log.e("TEST", "GZIP INVALID: ${e.message}")
            }.toString()
        }
    }

    suspend fun parseFhirBundle(json: String): Bundle {
        return withContext(Dispatchers.IO) {
            val parser = FhirContext.forR4().newJsonParser()
            parser.parseResource(Bundle::class.java, json)
        }
    }

    suspend fun createBundleInEngine(engine: FhirEngine, bundle: Bundle) {
        withContext(Dispatchers.IO) {
            var processed = 0
            var failed = 0
            var skipped = 0

            for (entry in bundle.entry) {
                try {
                    val existing =    engine.search<Location> {
                        filter(Resource.RES_ID, { value = of(entry.resource.idPart) })
                    }
                    if (existing.isNotEmpty()) {
                        // Already in DB → skip
                        skipped++
                        continue
                    }
                } catch (_: Exception) {
                }
                try {
                    engine.create(entry.resource)
                    processed++
                } catch (e: Exception) {
                    failed++
                    // Log the bad facility
                    Log.e("FHIR", "Failed to import ${entry.resource.id}: ${e.message}")
                }
            }

        }
    }

}