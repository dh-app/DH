package org.darulhuda.udupi.model

data class AboutPage(val title: String, val html: String, val share_url: String)
data class ContactPage(
    val phone: String?, val whatsapp: String?, val email: String?, val address: String?,
    val map_url: String?, val website: String?, val share_url: String?
)
