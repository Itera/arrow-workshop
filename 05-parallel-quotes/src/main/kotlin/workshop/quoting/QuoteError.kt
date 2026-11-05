package workshop.quoting

import arrow.core.NonEmptyList
import workshop.domain.Country
import workshop.domain.Courier
import workshop.domain.DeliveryService
import workshop.validation.ShipmentValidationError

/** Why a request could not be quoted. Each case leads the caller to a different response. */
sealed interface QuoteError {
    /** The caller sent bad input: show them every problem. */
    data class InvalidRequest(
        val problems: NonEmptyList<ShipmentValidationError>,
    ) : QuoteError

    data class UnsupportedRoute(
        val from: Country,
        val to: Country,
    ) : QuoteError

    data class ParcelTooHeavy(
        val totalWeightGrams: Int,
    ) : QuoteError

    data class NoServiceFor(
        val service: DeliveryService,
        val from: Country,
        val to: Country,
    ) : QuoteError

    /** The postcode is well-formed, but no zone serves it. */
    data class UnknownPostcode(
        val postcode: String,
    ) : QuoteError

    /** A courier reported that nobody may carry this shipment. */
    data class ShipmentProhibited(
        val courier: Courier,
        val reason: String,
    ) : QuoteError

    data object NoQuotesAvailable : QuoteError
}

/** The public error type. Make it its own sealed type if the boundary ever needs extra cases. */
typealias QuoteShipmentError = QuoteError
