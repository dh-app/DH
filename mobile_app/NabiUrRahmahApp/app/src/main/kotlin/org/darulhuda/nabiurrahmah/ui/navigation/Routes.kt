package org.darulhuda.nabiurrahmah.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class FlyersRoute(val languageCode: String)

@Serializable
data class ViewerRoute(val languageCode: String, val flyerId: String)

@Serializable
data class PlaylistRoute(val playlistId: String)

@Serializable
data class VideoRoute(val playlistId: String, val videoId: String)

@Serializable
data object AboutRoute
