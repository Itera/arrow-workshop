package workshop.quoting

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.context.raise
import arrow.core.raise.either
import workshop.domain.DeliveryQuote
import workshop.domain.ShipmentRequest
import workshop.providers.LegacyZoneLookup
import workshop.validation.Shipment
import workshop.validation.validate

class QuoteService(
    private val zones: LegacyZoneLookup,
) {
    /** The public boundary returns `Either`, so callers can see that quoting can fail. */
    fun quoteShipment(request: ShipmentRequest): Either<QuoteShipmentError, DeliveryQuote> =
        either {
            val shipment = validate(request).mapLeft(QuoteError::InvalidRequest).bind()
            quoteValidShipment(shipment)
        }

    /** Inside, the flow reads as direct Kotlin: check, call, return. */
    context(_: Raise<QuoteError>)
    private fun quoteValidShipment(shipment: Shipment): DeliveryQuote {
        checkQuotable(shipment)
        // Looking up the zone can already fail with UnknownPostcode. The amount is used in section 5.
        surcharges(shipment, zones)
        // There are no couriers yet. Section 5 replaces this with parallel provider quotes.
        raise(QuoteError.NoQuotesAvailable)
    }
}
