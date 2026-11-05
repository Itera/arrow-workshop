package workshop.resilience

import arrow.core.Either
import arrow.core.left
import arrow.resilience.CircuitBreaker
import arrow.resilience.Schedule
import arrow.resilience.retry
import workshop.domain.CourierQuote
import workshop.quoting.CourierDecline
import workshop.quoting.CourierProvider
import workshop.validation.Shipment
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** A technical failure that may succeed if tried again: a timeout, a dropped connection, a 503. */
class TransientProviderFailure(
    message: String,
) : RuntimeException(message)

/**
 * Exponential backoff (100 ms, 200 ms, ...), at most [maxRetries] retries. `and` continues only
 * while both schedules continue, so the retry count bounds the backoff.
 */
fun transientRetries(
    maxRetries: Long = 2,
    base: Duration = 100.milliseconds,
): Schedule<TransientProviderFailure, *> =
    Schedule.exponential<TransientProviderFailure>(base).and(Schedule.recurs(maxRetries))

/**
 * Retries only [TransientProviderFailure]. A decline is a successful *answer*, a `Left` value,
 * so it is never retried, and any other exception is rethrown at once.
 *
 * Quoting is read-only and so safe to repeat. A booking call that charges money is not; do not
 * wrap it like this unless the courier API is idempotent, for example with an idempotency key.
 */
class RetryingProvider(
    private val delegate: CourierProvider,
    private val schedule: Schedule<TransientProviderFailure, *> = transientRetries(),
) : CourierProvider by delegate {
    override suspend fun quote(shipment: Shipment): Either<CourierDecline, CourierQuote> =
        schedule.retry { delegate.quote(shipment) }
}

/**
 * Counts exceptions, not declines: a courier that keeps saying "no service" is healthy.
 * While the circuit is open, calls fail fast with [CircuitBreaker.ExecutionRejected].
 */
class CircuitBreakingProvider(
    private val delegate: CourierProvider,
    private val breaker: CircuitBreaker,
) : CourierProvider by delegate {
    override suspend fun quote(shipment: Shipment): Either<CourierDecline, CourierQuote> =
        breaker.protectOrThrow { delegate.quote(shipment) }
}

/**
 * The provider edge: any technical failure that survives retries and the breaker becomes
 * [CourierDecline.Unavailable]. `Either.catch` still rethrows cancellation.
 */
class FailSoftProvider(
    private val delegate: CourierProvider,
) : CourierProvider by delegate {
    override suspend fun quote(shipment: Shipment): Either<CourierDecline, CourierQuote> =
        Either.catch { delegate.quote(shipment) }.fold(
            ifLeft = { CourierDecline.Unavailable(courier, it).left() },
            ifRight = { it },
        )
}

/** `OpeningStrategy.Count(maxFailures)` opens once the failure count *exceeds* `maxFailures`. */
fun providerCircuitBreaker(
    maxFailures: Int = 3,
    resetTimeout: Duration = 30.seconds,
    timeSource: TimeSource = TimeSource.Monotonic,
): CircuitBreaker =
    CircuitBreaker(
        resetTimeout = resetTimeout,
        openingStrategy = CircuitBreaker.OpeningStrategy.Count(maxFailures),
        timeSource = timeSource,
    )

/**
 * Retry sits inside the breaker, so one logical quote counts as at most one failure, and an
 * open circuit is never retried. Unexpected failures surface as `Unavailable`.
 */
fun CourierProvider.resilient(
    breaker: CircuitBreaker,
    schedule: Schedule<TransientProviderFailure, *> = transientRetries(),
): CourierProvider = FailSoftProvider(CircuitBreakingProvider(RetryingProvider(this, schedule), breaker))
