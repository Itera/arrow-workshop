package workshop.quoting

import arrow.core.raise.Raise
import arrow.core.raise.context.ensure
import workshop.domain.Country
import workshop.domain.DeliveryService
import workshop.validation.Shipment

const val MAX_SHIPMENT_WEIGHT_GRAMS = 50_000

private val crossBorderRoutes =
    setOf(
        Country.NO to Country.SE,
        Country.SE to Country.NO,
        Country.SE to Country.DK,
        Country.DK to Country.SE,
    )

/**
 * Business rules for a shipment that is already valid. These fail fast: once the route is
 * unsupported there is nothing useful to say about weight, so there is nothing to accumulate.
 */
context(_: Raise<QuoteError>)
fun checkQuotable(shipment: Shipment) {
    val from = shipment.request.sender.country
    val to = shipment.request.recipient.country
    val domestic = from == to

    ensure(domestic || (from to to) in crossBorderRoutes) { QuoteError.UnsupportedRoute(from, to) }
    ensure(shipment.totalWeightGrams <= MAX_SHIPMENT_WEIGHT_GRAMS) {
        QuoteError.ParcelTooHeavy(shipment.totalWeightGrams)
    }
    ensure(domestic || shipment.request.service != DeliveryService.EXPRESS) {
        QuoteError.NoServiceFor(shipment.request.service, from, to)
    }
}
