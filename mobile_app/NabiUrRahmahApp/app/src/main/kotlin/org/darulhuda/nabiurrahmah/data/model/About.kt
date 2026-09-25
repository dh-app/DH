package org.darulhuda.nabiurrahmah.data.model

import kotlinx.serialization.Serializable

/** Contact details for the About screen, bundled with the app (assets/about.json). */
@Serializable
data class About(
    val organization: String = "",
    val summary: String = "",
    val phones: List<String> = emptyList(),
    val whatsapp: String? = null,
    val email: String? = null,
    val website: String? = null,
    val address: String? = null,
    val mapUrl: String? = null,
    val socials: List<SocialLink> = emptyList(),
)

@Serializable
data class SocialLink(
    val name: String,
    val url: String,
)
