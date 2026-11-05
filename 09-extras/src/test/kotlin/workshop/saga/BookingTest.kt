package workshop.saga

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import workshop.domain.Money

private class PickupUnavailable : RuntimeException("no pickup slot")

private class RecordingSteps(
    private val pickupFails: Boolean,
) : BookingSteps {
    val log = mutableListOf<String>()

    override suspend fun reserveCapacity() = "R1".also { log += "reserve" }

    override suspend fun releaseCapacity(reservationId: String) {
        log += "release $reservationId"
    }

    override suspend fun charge(amount: Money) = "C1".also { log += "charge ${amount.cents}" }

    override suspend fun refund(chargeId: String) {
        log += "refund $chargeId"
    }

    override suspend fun arrangePickup(): String {
        if (pickupFails) {
            log += "pickup failed"
            throw PickupUnavailable()
        }
        return "P1".also { log += "pickup" }
    }

    override suspend fun cancelPickup(pickupId: String) {
        log += "cancel $pickupId"
    }
}

class BookingTest :
    FunSpec({
        test("a successful booking runs no compensations") {
            val steps = RecordingSteps(pickupFails = false)

            book(steps, Money(400)) shouldBe Booking("R1", "C1", "P1")
            steps.log shouldBe listOf("reserve", "charge 400", "pickup")
        }

        test("a pickup failure after payment refunds, then releases, in reverse order") {
            val steps = RecordingSteps(pickupFails = true)

            shouldThrow<PickupUnavailable> { book(steps, Money(400)) }

            steps.log shouldBe listOf("reserve", "charge 400", "pickup failed", "refund C1", "release R1")
        }
    })
