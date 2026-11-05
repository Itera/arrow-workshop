package workshop.quoting

import arrow.core.raise.Raise
import workshop.domain.DeliveryOptions
import workshop.domain.Money
import workshop.providers.LegacyZoneLookup
import workshop.validation.Shipment

const val INSURANCE_RATE_PERCENT = 2
private const val PERCENT = 100
val PICKUP_FEE = Money(4_900)

/** Absence is local and natural here, so a nullable type says everything that is needed. */
fun insuranceSurcharge(options: DeliveryOptions): Money =
    options.insuranceAmount
        ?.let { Money(it.cents * INSURANCE_RATE_PERCENT / PERCENT) }
        ?: Money.ZERO

fun pickupFee(options: DeliveryOptions): Money = if (options.pickupWindow != null) PICKUP_FEE else Money.ZERO

context(_: Raise<QuoteError>)
fun surcharges(
    shipment: Shipment,
    zones: LegacyZoneLookup,
): Money {
    val options = shipment.request.options
    return zones.zoneFor(shipment.request.recipient).surcharge + insuranceSurcharge(options) + pickupFee(options)
}
