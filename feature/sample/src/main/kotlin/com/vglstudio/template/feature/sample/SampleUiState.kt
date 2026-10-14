package com.vglstudio.template.feature.sample

/** Everything the sample screen needs to draw itself. */
sealed interface SampleUiState {
    data object Loading : SampleUiState

    data object Disabled : SampleUiState

    data class Ready(val count: Int) : SampleUiState
}
