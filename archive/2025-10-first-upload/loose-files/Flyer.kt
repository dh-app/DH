package org.darulhuda.udupi.core.model

data class Flyer(
    val title: String,
    val description: String? = null,
    val fileUrl: String,
    val thumbnailUrl: String? = null,
    val language: String? = null
)
