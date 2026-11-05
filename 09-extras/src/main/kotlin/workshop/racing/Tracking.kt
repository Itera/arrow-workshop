package workshop.racing

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.merge
import arrow.core.raise.Raise
import arrow.core.raise.context.raise
import arrow.fx.coroutines.raceN
import kotlinx.coroutines.awaitCancellation
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext

data class TrackingStatus(
    val shipmentId: String,
    val status: String,
    val source: String,
)

data class TrackingFailure(
    val source: String,
    val reason: String,
)

data class TrackingUnavailable(
    val failures: List<TrackingFailure>,
)

fun interface TrackingProvider {
    suspend fun track(shipmentId: String): Either<TrackingFailure, TrackingStatus>
}

/**
 * `raceN` returns whichever side *completes* first and cancels the other. To get the first
 * *success*, a failed side must not complete: it waits, unless it is the last one to fail,
 * in which case nobody can succeed any more and it raises.
 */
context(_: Raise<TrackingUnavailable>)
suspend fun trackFirstSuccess(
    shipmentId: String,
    primary: TrackingProvider,
    secondary: TrackingProvider,
): TrackingStatus {
    val failures = mutableListOf<TrackingFailure>()
    val failed = AtomicInteger()

    suspend fun attempt(provider: TrackingProvider): TrackingStatus =
        provider.track(shipmentId).getOrElse { failure ->
            synchronized(failures) { failures += failure }
            if (failed.incrementAndGet() == 2) {
                raise(TrackingUnavailable(synchronized(failures) { failures.toList() }))
            }
            awaitCancellation()
        }

    // Like parZip, raceN moves to Dispatchers.Default unless given a context.
    return raceN(EmptyCoroutineContext, { attempt(primary) }, { attempt(secondary) }).merge()
}
