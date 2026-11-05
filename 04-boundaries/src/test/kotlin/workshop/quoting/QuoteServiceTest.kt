package workshop.quoting

import arrow.core.nonEmptyListOf
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import workshop.address
import workshop.domain.Country
import workshop.domain.DeliveryService
import workshop.parcel
import workshop.shipmentRequest
import workshop.validation.AddressField
import workshop.validation.ShipmentValidationError
import workshop.zoneLookup

class QuoteServiceTest :
    FunSpec({
        val service = QuoteService(zoneLookup())

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

        test("a quotable shipment has no quotes until couriers are added in section 5") {
            service.quoteShipment(shipmentRequest()).shouldBeLeft() shouldBe QuoteError.NoQuotesAvailable
        }
    })
