package org.darulhuda.udupi.model


/**
 * 📘 Data Model for Qur'an Printing Complex (QPC)
 */
data class QpcModel(
    val title: String? = null,
    val hero_image: String? = null,
    val intro_html: String? = null,
    val callouts: List<String> = emptyList(),
    val contribution_rates: ContributionRates? = null,
    val bank_details: BankDetails? = null,
    val contacts: Contacts? = null,
    val qr_brochure_image: String? = null,
    val videos: List<Video> = emptyList(),
    val share_url: String? = null
)

data class ContributionRates(
    val per_sq_ft_inr: Double? = null,
    val per_sq_m_inr: Double? = null
)

data class BankDetails(
    val trust_name: String? = null,
    val bank: String? = null,
    val branch: String? = null,
    val account_no: String? = null,
    val ifsc: String? = null,
    val note: String? = null,
    val zakat: String? = null
)

data class Contacts(
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null
)

data class Video(
    val title: String? = null,
    val url: String? = null
)
