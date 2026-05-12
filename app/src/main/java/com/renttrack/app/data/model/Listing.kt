package com.renttrack.app.data.model

import java.io.Serializable

data class ListingPhoto(
    val id: String = "",
    val listingId: String = "",
    val url: String = "",
    val isCover: Boolean = false,
    val displayOrder: Int = 0
) : Serializable

data class Listing(
    val id: String = "",
    val landlordId: String = "",
    val title: String = "",
    val address: String = "",
    val city: String = "",
    val zone: String = "",
    val priceMonthly: Double = 0.0,
    val sqm: Int? = null,
    val rooms: Int? = null,
    val bathrooms: Int? = null,
    val floor: String = "",
    val furnished: Boolean = false,
    val availableFrom: String = "",
    val description: String = "",
    val contactType: String = "direct",   // "direct" | "form"
    val contactPhone: String = "",
    val contactEmail: String = "",
    val contactWhatsapp: String = "",
    val isActive: Boolean = true,
    val isAvailable: Boolean = true,        // true = libero, false = occupato
    val isFeatured: Boolean = false,        // true = annuncio promosso (a pagamento)
    val featuredUntil: String? = null,      // data scadenza promozione (ISO 8601)
    val createdAt: String = "",
    val photos: List<ListingPhoto> = emptyList()
) : Serializable {
    val coverUrl: String
        get() = photos.firstOrNull { it.isCover }?.url
            ?: photos.minByOrNull { it.displayOrder }?.url
            ?: ""
}

data class Inquiry(
    val id: String = "",
    val listingId: String = "",
    val seekerName: String = "",
    val seekerPhone: String = "",
    val seekerEmail: String = "",
    val message: String = "",
    val createdAt: String = ""
) : Serializable
