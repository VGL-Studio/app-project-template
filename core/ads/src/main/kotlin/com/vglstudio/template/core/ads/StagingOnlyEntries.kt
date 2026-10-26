package com.vglstudio.template.core.ads

// SKELETON (plan T-036, step 3): bodies come in step 4.

internal fun AdPlacementDeclaration.acceptsStagingAppOpen(): Boolean = false

internal fun rewardedPolicyOf(declaration: AdPlacementDeclaration, override: RewardedNoAdPolicy?): RewardedNoAdPolicy =
    declaration.effectiveNoAdPolicy
