package workshop.quoting

import arrow.core.Either
import workshop.domain.Courier
import workshop.domain.CourierQuote
import workshop.validation.Shipment

/** A courier's business answer when it will not quote. Technical failures are exceptions instead. */
sealed interface CourierDecline {
    val courier: Courier

    /** This courier cannot carry the shipment; another one may. */
    data class NoService(
        override val courier: Courier,
    ) : CourierDecline

    /** No courier may carry the shipment, for example restricted goods. Stops all quoting. */
    data class Prohibited(
        override val courier: Courier,
        val reason: String,
    ) : CourierDecline
}

interface CourierProvider {
    val courier: Courier

    suspend fun quote(shipment: Shipment): Either<CourierDecline, CourierQuote>
}
