package com.nyora.hasan72341.mihon.parsers.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface NavDestination {
    @Serializable
    data object Library : NavDestination
    @Serializable
    data object Updates : NavDestination
    @Serializable
    data object History : NavDestination
    @Serializable
    data object Explore : NavDestination
    @Serializable
    data object Settings : NavDestination
}
