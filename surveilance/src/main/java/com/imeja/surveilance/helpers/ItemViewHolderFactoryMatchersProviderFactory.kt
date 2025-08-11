package com.imeja.surveilance.helpers

import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.QuestionnaireItemViewHolderFactoryMatchersProviderFactory
import com.google.android.fhir.datacapture.contrib.views.locationwidget.LocationGpsCoordinateViewHolderFactory
import com.google.android.fhir.datacapture.contrib.views.locationwidget.LocationWidgetViewHolderFactory

object ItemViewHolderFactoryMatchersProviderFactory :
    QuestionnaireItemViewHolderFactoryMatchersProviderFactory {

    const val LOCATION_WIDGET_PROVIDER = "location-widget-provider"

    override fun get(
        provider: String,
    ): QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatchersProvider =
        when (provider) {
            LOCATION_WIDGET_PROVIDER ->
                object :
                    QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatchersProvider() {
                    override fun get():
                            List<QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatcher> {
                        return listOf(
                            QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatcher(
                                factory = LocationGpsCoordinateViewHolderFactory,
                                matches = LocationGpsCoordinateViewHolderFactory::matcher,
                            ),
                            QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatcher(
                                factory = LocationWidgetViewHolderFactory,
                                matches = LocationWidgetViewHolderFactory::matcher,
                            ),
                        )
                    }
                }

            else -> throw NotImplementedError()
        }
}
