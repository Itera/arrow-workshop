package workshop.quoting

import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.context.ensure
import arrow.core.raise.context.ensureNotNull
import arrow.core.raise.context.raise
import arrow.fx.coroutines.parMapNotNull
import arrow.fx.coroutines.parZip
import workshop.domain.CourierQuote
import workshop.domain.DeliveryQuote
import workshop.domain.DeliveryService
import workshop.domain.Money
import workshop.validation.Shipment
import kotlin.coroutines.EmptyCoroutineContext

const val MAX_CONCURRENT_QUOTES = 3

/**
 * The error policy, decided per decline:
 * - `NoService` concerns one courier only, so skip it and keep the others running.
 * - `Prohibited` concerns the shipment itself, so raise. The raise cancels every sibling
 *   still in flight, because the parallel calls run inside the `Raise` scope.
 *
 * `Raise` is not a filter: skipping uses `null`, aborting uses `raise`.
 */
context(_: Raise<QuoteError>)
suspend fun quoteOrSkip(
    provider: CourierProvider,
    shipment: Shipment,
): CourierQuote? =
    provider.quote(shipment).getOrElse { decline ->
        when (decline) {
            is CourierDecline.NoService -> null
            is CourierDecline.Prohibited -> raise(QuoteError.ShipmentProhibited(decline.courier, decline.reason))
        }
    }

/** Any number of providers, at most [concurrency] in flight at a time. */
context(_: Raise<QuoteError>)
suspend fun requestQuotes(
    shipment: Shipment,
    providers: List<CourierProvider>,
    concurrency: Int = MAX_CONCURRENT_QUOTES,
): List<CourierQuote> {
    val quotes = providers.parMapNotNull(concurrency = concurrency) { quoteOrSkip(it, shipment) }
    ensure(quotes.isNotEmpty()) { QuoteError.NoQuotesAvailable }
    return quotes
}

/**
 * A fixed set of independent calls, each with its own type if needed: `parZip`.
 *
 * Without a context argument `parZip` switches to `Dispatchers.Default`, unlike `parMap`, which
 * inherits the caller's context. Passing `EmptyCoroutineContext` keeps it on the caller's
 * dispatcher, which also lets tests control time.
 */
context(_: Raise<QuoteError>)
suspend fun requestQuotes(
    shipment: Shipment,
    first: CourierProvider,
    second: CourierProvider,
    third: CourierProvider,
): List<CourierQuote> {
    val quotes =
        parZip(
            EmptyCoroutineContext,
            { quoteOrSkip(first, shipment) },
            { quoteOrSkip(second, shipment) },
            { quoteOrSkip(third, shipment) },
        ) { a, b, c -> listOfNotNull(a, b, c) }
    ensure(quotes.isNotEmpty()) { QuoteError.NoQuotesAvailable }
    return quotes
}

/** Cheapest quote that meets the service's delivery promise; ties go to the faster courier. */
context(_: Raise<QuoteError>)
fun selectQuote(
    quotes: List<CourierQuote>,
    service: DeliveryService,
    surcharges: Money,
): DeliveryQuote {
    val eligible =
        quotes
            .filter { it.deliveryDays <= service.maxDeliveryDays }
            .sortedWith(compareBy(CourierQuote::price, CourierQuote::deliveryDays))
    val selected = ensureNotNull(eligible.firstOrNull()) { QuoteError.NoQuotesAvailable }
    return DeliveryQuote(selected = selected, alternatives = eligible.drop(1), surcharges = surcharges)
}
