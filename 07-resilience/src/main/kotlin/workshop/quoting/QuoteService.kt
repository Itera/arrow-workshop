package workshop.quoting

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import workshop.domain.DeliveryQuote
import workshop.domain.ShipmentRequest
import workshop.providers.LegacyZoneLookup
import workshop.validation.Shipment
import workshop.validation.validate

class QuoteService(
    private val zones: LegacyZoneLookup,
    private val providers: List<CourierProvider>,
) {
    /** The public boundary returns `Either`, so callers can see that quoting can fail. */
    suspend fun quoteShipment(request: ShipmentRequest): Either<QuoteShipmentError, DeliveryQuote> =
        either {
            val shipment = validate(request).mapLeft(QuoteError::InvalidRequest).bind()
            quoteValidShipment(shipment)
        }

    /** Inside, the flow reads as direct Kotlin: check, call, return. */
    context(_: Raise<QuoteError>)
    private suspend fun quoteValidShipment(shipment: Shipment): DeliveryQuote {
        checkQuotable(shipment)
        val surcharges = surcharges(shipment, zones)
        val quotes = requestQuotes(shipment, providers)
        return selectQuote(quotes, shipment.request.service, surcharges)
    }
}
