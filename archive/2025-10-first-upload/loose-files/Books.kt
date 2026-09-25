package org.darulhuda.udupi.model

data class BooksFeed(val updated_at: String, val items: List<BookItem>)
data class BookItem(
    val id: String, val title: String, val author: String?,
    val language: String, val cover: String, val pdf: String,
    val pages: Int?, val size_mb: Double?, val share_url: String
)
