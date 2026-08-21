package com.example.data

/**
 * Pro subscription and purchase tiers supported by Stripe payment links / product IDs.
 */
enum class ProPlanTier(
    val id: String,
    val title: String,
    val priceDisplay: String,
    val billingPeriod: String,
    val stripeProductId: String,
    val stripePriceId: String,
    val stripeBuyUrl: String,
    val isPopular: Boolean = false
) {
    MONTHLY(
        id = "pro_monthly",
        title = "Monthly Pro",
        priceDisplay = "$2.99",
        billingPeriod = "/ month",
        stripeProductId = "prod_V6pUi3ISMMawiH",
        stripePriceId = "price_1U6bpfCeNOeBc4iF929RWZY5",
        stripeBuyUrl = "https://buy.stripe.com/eVq00igRT6yG0yFfXod7q0j"
    ),
    YEARLY(
        id = "pro_yearly",
        title = "Yearly Pro",
        priceDisplay = "$19.99",
        billingPeriod = "/ year",
        stripeProductId = "prod_V6pWUoMyHpLhSf",
        stripePriceId = "price_1U6brRCeNOeBc4iF0b4vGUc8",
        stripeBuyUrl = "https://buy.stripe.com/7sYcN43138GO5SZdPgd7q0i",
        isPopular = true
    ),
    LIFETIME(
        id = "pro_lifetime",
        title = "Lifetime License",
        priceDisplay = "$39.99",
        billingPeriod = "one-time",
        stripeProductId = "prod_V6pXwCqkNwRXtu",
        stripePriceId = "price_1U6bsRCeNOeBc4iFxXfDniti",
        stripeBuyUrl = "https://buy.stripe.com/4gMbJ08lne180yF4eGd7q0h"
    );

    companion object {
        fun fromId(id: String): ProPlanTier = entries.find { it.id == id } ?: YEARLY
    }
}
