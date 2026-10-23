package com.vglstudio.template.core.assets

import com.vglstudio.template.core.logging.AppLogger
import java.io.IOException

/** Logs a failed download as a warning under the tag "Assets", shown as VGL.Assets (ADR-0019). */
internal fun downloadFailureLogger(logger: AppLogger): (AssetPath, IOException) -> Unit = { _, _ -> }
