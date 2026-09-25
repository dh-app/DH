package org.darulhuda.nabiurrahmah.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object FlyerLanguagesRoute

@Serializable
data class FlyersRoute(val languageCode: String)

@Serializable
data class ViewerRoute(val languageCode: String, val flyerId: String)

@Serializable
data object VideosRoute

@Serializable
data class PlaylistRoute(val playlistId: String)

@Serializable
data class VideoRoute(val playlistId: String, val videoId: String)

@Serializable
data class ShelfRoute(val shelf: String)

@Serializable
data class BookRoute(val shelf: String, val bookId: String)

@Serializable
data class ReaderRoute(val url: String, val title: String)

@Serializable
data object AboutRoute
