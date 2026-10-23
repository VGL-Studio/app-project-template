package com.vglstudio.template.config

import com.vglstudio.template.core.ads.AdsSettings
import com.vglstudio.template.core.config.FeatureFlag
import com.vglstudio.template.core.config.RemoteKey

/**
 * Every Remote Config key of the app, by owning module (ADR-0023). A new core module with keys is added here.
 * :app is the only module that sees every core module (ADR-0001). docs/remote-config-keys.md and
 * docs/remote-config-template.json are generated from this list by RemoteConfigKeysTest.
 */
object AppRemoteKeys {
    val byModule: Map<String, List<RemoteKey<*>>> = linkedMapOf(
        ":core:config" to FeatureFlag.entries.map { it.remoteKey },
        ":core:ads" to AdsSettings.all(),
    )
}
