package workshop.optics

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import workshop.domain.Parcel
import workshop.domain.withParcelWeight
import workshop.domain.withRecipientPostcode
import workshop.parcel
import workshop.shipmentRequest

class OpticsTest :
    FunSpec({
        val request = shipmentRequest(parcels = listOf(parcel(100), parcel(200), parcel(300)))

        test("the optic and the copy chain give the same nested update") {
            request.withParcelWeightOptic(1, 250) shouldBe request.withParcelWeight(1, 250)
            request.withRecipientPostcodeOptic("7010") shouldBe request.withRecipientPostcode("7010")
        }

        test("the input request is not changed") {
            request.withParcelWeightOptic(1, 250)

            request.parcels.map(Parcel::weightGrams) shouldBe listOf(100, 200, 300)
        }

        test("an index outside the list leaves the request unchanged") {
            request.withParcelWeightOptic(7, 250) shouldBe request
        }

        test("every focuses all parcels at once") {
            request.addPackagingWeight(10).parcels.map(Parcel::weightGrams) shouldBe listOf(110, 210, 310)
        }
    })
