package workshop

import arrow.core.getOrElse
import workshop.domain.Address
import workshop.domain.Country
import workshop.domain.DeliveryOptions
import workshop.domain.DeliveryService
import workshop.domain.Dimensions
import workshop.domain.Parcel
import workshop.domain.ShipmentRequest
import workshop.validation.Shipment
import workshop.validation.validate

fun address(
    name: String = "Ada",
    postcode: String = "0150",
    country: Country = Country.NO,
) = Address(name = name, street = "Storgata 1", postcode = postcode, country = country)

fun parcel(
    weightGrams: Int = 1_000,
    dimensions: Dimensions = Dimensions(lengthMm = 300, widthMm = 200, heightMm = 100),
) = Parcel(weightGrams = weightGrams, dimensions = dimensions)

fun shipmentRequest(
    sender: Address = address(name = "Sender", postcode = "0150"),
    recipient: Address = address(name = "Recipient", postcode = "5003"),
    parcels: List<Parcel> = listOf(parcel()),
    service: DeliveryService = DeliveryService.STANDARD,
    options: DeliveryOptions = DeliveryOptions(),
) = ShipmentRequest(sender, recipient, parcels, service, options)

fun shipment(request: ShipmentRequest = shipmentRequest()): Shipment =
    validate(request).getOrElse { error("Fixture request is invalid: $it") }
