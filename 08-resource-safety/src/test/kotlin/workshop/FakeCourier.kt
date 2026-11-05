package workshop

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.delay
import workshop.domain.Courier
import workshop.domain.CourierQuote
import workshop.domain.Money
import workshop.quoting.CourierDecline
import workshop.quoting.CourierProvider
import workshop.validation.Shipment
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

/**
 * A configurable fake: waits [latency], then answers with [respond]. It records how often it
 * was called and whether it was cancelled while waiting.
 */
class FakeCourier(
    name: String,
    private val latency: Duration = Duration.ZERO,
    private val respond: suspend FakeCourier.(attempt: Int) -> Either<CourierDecline, CourierQuote>,
) : CourierProvider {
    override val courier = Courier(name)

    private val attempts = AtomicInteger()
    val calls: Int get() = attempts.get()

    @Volatile
    var wasCancelled = false
        private set

    override suspend fun quote(shipment: Shipment): Either<CourierDecline, CourierQuote> {
        val attempt = attempts.incrementAndGet()
        try {
            delay(latency)
        } catch (e: CancellationException) {
            wasCancelled = true
            throw e
        }
        return respond(attempt)
    }

    fun price(
        cents: Long,
        deliveryDays: Int = 2,
    ): Either<CourierDecline, CourierQuote> = CourierQuote(courier, Money(cents), deliveryDays).right()

    fun noService(): Either<CourierDecline, CourierQuote> = CourierDecline.NoService(courier).left()

    fun prohibited(reason: String): Either<CourierDecline, CourierQuote> =
        CourierDecline.Prohibited(courier, reason).left()
}

fun quotingCourier(
    name: String,
    cents: Long,
    deliveryDays: Int = 2,
    latency: Duration = Duration.ZERO,
) = FakeCourier(name, latency) { price(cents, deliveryDays) }

fun decliningCourier(
    name: String,
    latency: Duration = Duration.ZERO,
) = FakeCourier(name, latency) { noService() }
