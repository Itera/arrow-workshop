package workshop.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import workshop.parcel
import workshop.shipmentRequest

class DomainTest :
    FunSpec({
        test("changing the recipient postcode leaves the original request unchanged") {
            val request = shipmentRequest()

            val updated = request.withRecipientPostcode("7010")

            updated.recipient.postcode shouldBe "7010"
            updated.sender shouldBe request.sender
            request.recipient.postcode shouldBe "5003"
        }

        test("changing one parcel weight leaves other parcels and the original unchanged") {
            val request = shipmentRequest(parcels = listOf(parcel(100), parcel(200), parcel(300)))

            val updated = request.withParcelWeight(index = 1, weightGrams = 250)

            updated.parcels.map(Parcel::weightGrams) shouldBe listOf(100, 250, 300)
            request.parcels.map(Parcel::weightGrams) shouldBe listOf(100, 200, 300)
        }

        test("money cannot be negative") {
            shouldThrow<IllegalArgumentException> { Money(-1) }
        }
    })
