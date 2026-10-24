package com.vglstudio.template.core.ads

import android.app.Activity

/** The User Messaging Platform, behind an interface so the consent rules run on the JVM ([ConsentCoordinator]). */
internal interface ConsentPlatform {
    fun canRequestAds(): Boolean

    /** consentStatus == REQUIRED: the consent form will really show (S-16). */
    fun consentFormRequired(): Boolean

    /** privacyOptionsRequirementStatus == REQUIRED (S-14). */
    fun privacyOptionsRequired(): Boolean

    fun requestUpdate(activity: Activity, onSuccess: () -> Unit, onFailure: (errorCode: Int) -> Unit)

    fun showFormIfRequired(activity: Activity, onClosed: (errorCode: Int?) -> Unit)

    fun showPrivacyForm(activity: Activity, onClosed: (errorCode: Int?) -> Unit)

    /** S-18. */
    fun reset()
}
