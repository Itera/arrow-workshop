package workshop.saga

import arrow.resilience.saga
import arrow.resilience.transact
import workshop.domain.Money

data class Booking(
    val reservationId: String,
    val chargeId: String,
    val pickupId: String,
)

/** Each step has an action and a compensation that undoes it. */
interface BookingSteps {
    suspend fun reserveCapacity(): String

    suspend fun releaseCapacity(reservationId: String)

    suspend fun charge(amount: Money): String

    suspend fun refund(chargeId: String)

    suspend fun arrangePickup(): String

    suspend fun cancelPickup(pickupId: String)
}

/**
 * If a later step throws, the compensations of the steps that completed run in reverse order,
 * and then the original exception is rethrown.
 */
suspend fun book(
    steps: BookingSteps,
    amount: Money,
): Booking =
    saga {
        val reservation = saga({ steps.reserveCapacity() }) { steps.releaseCapacity(it) }
        val charge = saga({ steps.charge(amount) }) { steps.refund(it) }
        val pickup = saga({ steps.arrangePickup() }) { steps.cancelPickup(it) }
        Booking(reservation, charge, pickup)
    }.transact()
