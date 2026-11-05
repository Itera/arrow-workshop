package workshop.quoting

import arrow.core.nonEmptyListOf
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import workshop.address
import workshop.decliningCourier
import workshop.domain.Country
import workshop.domain.Courier
import workshop.domain.CourierQuote
import workshop.domain.DeliveryOptions
import workshop.domain.DeliveryService
import workshop.domain.Money
import workshop.parcel
import workshop.quotingCourier
import workshop.shipmentRequest
import workshop.validation.AddressField
import workshop.validation.ShipmentValidationError
import workshop.zoneLookup

class QuoteServiceTest :
    FunSpec({
        val service = QuoteService(zoneLookup(), listOf(quotingCourier("north", 400)))

        test("invalid input is reported as InvalidRequest with every problem") {
            val request = shipmentRequest(sender = address(postcode = "x"), parcels = emptyList())

            val error = service.quoteShipment(request).shouldBeLeft()

            error shouldBe
                QuoteError.InvalidRequest(
                    nonEmptyListOf(
                        ShipmentValidationError.InvalidPostcode(AddressField.SENDER, "x"),
                        ShipmentValidationError.NoParcels,
                    ),
                )
        }

        test("an unsupported route is a domain error, not a validation error") {
            val request = shipmentRequest(recipient = address(postcode = "8000", country = Country.DK))

            service.quoteShipment(request).shouldBeLeft() shouldBe
                QuoteError.UnsupportedRoute(Country.NO, Country.DK)
        }

        test("parcels that are each valid can still be too heavy together") {
            val request = shipmentRequest(parcels = List(2) { parcel(weightGrams = 30_000) })

            service.quoteShipment(request).shouldBeLeft() shouldBe QuoteError.ParcelTooHeavy(60_000)
        }

        test("express is only offered domestically") {
            val request =
                shipmentRequest(
                    recipient = address(postcode = "114 55", country = Country.SE),
                    service = DeliveryService.EXPRESS,
                )

            service.quoteShipment(request).shouldBeLeft() shouldBe
                QuoteError.NoServiceFor(DeliveryService.EXPRESS, Country.NO, Country.SE)
        }

        test("validation short-circuits the flow before any business rule runs") {
            val request =
                shipmentRequest(
                    recipient = address(postcode = "bad", country = Country.DK),
                )

            service.quoteShipment(request).shouldBeLeft().shouldBeInstanceOf<QuoteError.InvalidRequest>()
        }

        test("quotes a valid shipment end to end with surcharges") {
            val quoting =
                QuoteService(
                    zoneLookup(surcharge = Money(1_000)),
                    listOf(
                        quotingCourier("north", 40_000),
                        quotingCourier("fjord", 30_000),
                        decliningCourier("aurora"),
                    ),
                )
            val request = shipmentRequest(options = DeliveryOptions(insuranceAmount = Money(50_000)))

            val quote = quoting.quoteShipment(request).shouldBeRight()

            quote.selected.courier shouldBe Courier("fjord")
            quote.alternatives.map(CourierQuote::courier) shouldBe listOf(Courier("north"))
            quote.total shouldBe Money(30_000 + 1_000 + 1_000)
        }

        test("express needs a courier that delivers the next day") {
            val quoting =
                QuoteService(
                    zoneLookup(),
                    listOf(
                        quotingCourier("north", 400, deliveryDays = 2),
                        quotingCourier("fjord", 900, deliveryDays = 1),
                    ),
                )

            val quote = quoting.quoteShipment(shipmentRequest(service = DeliveryService.EXPRESS)).shouldBeRight()

            quote.selected.courier shouldBe Courier("fjord")
        }

        test("with no couriers there are no quotes") {
            QuoteService(zoneLookup(), emptyList()).quoteShipment(shipmentRequest()).shouldBeLeft() shouldBe
                QuoteError.NoQuotesAvailable
        }
    })
