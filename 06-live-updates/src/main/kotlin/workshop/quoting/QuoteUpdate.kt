package workshop.quoting

import arrow.core.Either
import arrow.fx.coroutines.parMap
import arrow.fx.coroutines.parMapUnordered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import workshop.domain.Courier
import workshop.domain.CourierQuote
import workshop.validation.Shipment

/** One provider outcome. Failures are data in the stream, not a `Raise` scope kept open by the flow. */
sealed interface QuoteUpdate {
    val courier: Courier

    data class Received(
        val quote: CourierQuote,
    ) : QuoteUpdate {
        override val courier: Courier get() = quote.courier
    }

    data class Declined(
        val decline: CourierDecline,
    ) : QuoteUpdate {
        override val courier: Courier get() = decline.courier
    }

    data class Failed(
        override val courier: Courier,
        val cause: Throwable,
    ) : QuoteUpdate
}

/** Emits each outcome as soon as it completes, whatever the provider order. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
fun liveQuoteUpdates(
    shipment: Shipment,
    providers: List<CourierProvider>,
    concurrency: Int = MAX_CONCURRENT_QUOTES,
): Flow<QuoteUpdate> = providers.asFlow().parMapUnordered(concurrency) { quoteUpdate(it, shipment) }

/** Same outcomes in provider order: a slow first provider holds back the rest. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
fun orderedQuoteUpdates(
    shipment: Shipment,
    providers: List<CourierProvider>,
    concurrency: Int = MAX_CONCURRENT_QUOTES,
): Flow<QuoteUpdate> = providers.asFlow().parMap(concurrency) { quoteUpdate(it, shipment) }

/**
 * `Either.catch` turns non-fatal exceptions into values but rethrows `CancellationException`.
 * `runCatching` would swallow it and keep cancelled work alive.
 */
private suspend fun quoteUpdate(
    provider: CourierProvider,
    shipment: Shipment,
): QuoteUpdate =
    Either.catch { provider.quote(shipment) }.fold(
        ifLeft = { QuoteUpdate.Failed(provider.courier, it) },
        ifRight = { response -> response.fold(QuoteUpdate::Declined, QuoteUpdate::Received) },
    )
