package workshop.quoting

import arrow.core.raise.either
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import workshop.domain.DeliveryOptions
import workshop.domain.Money
import workshop.domain.PickupWindow
import workshop.providers.LegacyZoneLookup
import workshop.shipment
import workshop.shipmentRequest
import workshop.zoneLookup
import java.io.IOException

class BoundariesTest :
    FunSpec({
        test("insurance is charged only when an insured amount is present") {
            insuranceSurcharge(DeliveryOptions(insuranceAmount = Money(10_000))) shouldBe Money(200)
            insuranceSurcharge(DeliveryOptions(insuranceAmount = null)) shouldBe Money.ZERO
        }

        test("a missing pickup window means drop-off, with no pickup fee") {
            pickupFee(DeliveryOptions(pickupWindow = PickupWindow(9, 12))) shouldBe PICKUP_FEE
            pickupFee(DeliveryOptions()) shouldBe Money.ZERO
        }

        test("surcharges add the zone, insurance, and pickup amounts") {
            val shipment =
                shipment(
                    shipmentRequest(
                        options = DeliveryOptions(insuranceAmount = Money(10_000), pickupWindow = PickupWindow(9, 12)),
                    ),
                )

            either { surcharges(shipment, zoneLookup(surcharge = Money(1_000))) }.shouldBeRight() shouldBe
                Money(1_000 + 200 + 4_900)
        }

        // Documents the decision in Zones.kt: unknown postcode is a domain error.
        test("an unknown postcode becomes a typed domain error") {
            val service = QuoteService(zoneLookup(unknown = setOf("5003")))

            service.quoteShipment(shipmentRequest()).shouldBeLeft() shouldBe QuoteError.UnknownPostcode("5003")
        }

        test("a technical lookup failure stays an exception") {
            val broken = LegacyZoneLookup { Result.failure(IOException("zone service down")) }

            shouldThrow<IOException> { QuoteService(broken).quoteShipment(shipmentRequest()) }
        }
    })
