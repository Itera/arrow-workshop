package workshop.validation

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import workshop.address
import workshop.domain.Country
import workshop.domain.Dimensions
import workshop.parcel
import workshop.shipmentRequest
import workshop.validation.ShipmentValidationError.InvalidDimensions
import workshop.validation.ShipmentValidationError.InvalidPostcode
import workshop.validation.ShipmentValidationError.InvalidWeight
import workshop.validation.ShipmentValidationError.NoParcels

class ValidationTest :
    FunSpec({
        test("a valid request becomes a shipment with its total weight") {
            val request = shipmentRequest(parcels = listOf(parcel(1_000), parcel(2_500)))

            val shipment = validate(request).shouldBeRight()

            shipment.request shouldBe request
            shipment.totalWeightGrams shouldBe 3_500
        }

        test("returns every invalid field in one response") {
            val request =
                shipmentRequest(
                    sender = address(postcode = "x"),
                    recipient = address(postcode = "12"),
                    parcels =
                        listOf(
                            parcel(weightGrams = 0, dimensions = Dimensions(0, 10, 10)),
                            parcel(weightGrams = 40_000),
                        ),
                )

            validate(request).shouldBeLeft() shouldContainExactlyInAnyOrder
                listOf(
                    InvalidPostcode(AddressField.SENDER, "x"),
                    InvalidPostcode(AddressField.RECIPIENT, "12"),
                    InvalidWeight(parcelIndex = 0, weightGrams = 0),
                    InvalidDimensions(parcelIndex = 0),
                    InvalidWeight(parcelIndex = 1, weightGrams = 40_000),
                )
        }

        test("a request without parcels reports that, and only that") {
            validate(shipmentRequest(parcels = emptyList())).shouldBeLeft() shouldBe listOf(NoParcels)
        }

        test("postcode format depends on the country") {
            val swedish = address(postcode = "114 55", country = Country.SE)
            val norwegianFormatInSweden = address(postcode = "0150", country = Country.SE)

            validate(shipmentRequest(recipient = swedish)).shouldBeRight()
            validate(shipmentRequest(recipient = norwegianFormatInSweden)).shouldBeLeft() shouldBe
                listOf(InvalidPostcode(AddressField.RECIPIENT, "0150"))
        }
    })
