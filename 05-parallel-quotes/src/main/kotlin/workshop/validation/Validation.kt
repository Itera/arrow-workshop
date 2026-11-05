package workshop.validation

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.mapOrAccumulate
import arrow.core.raise.zipOrAccumulate
import workshop.domain.Address
import workshop.domain.Country
import workshop.domain.Parcel
import workshop.domain.ShipmentRequest

const val MAX_PARCEL_WEIGHT_GRAMS = 30_000

enum class AddressField { SENDER, RECIPIENT }

sealed interface ShipmentValidationError {
    data object NoParcels : ShipmentValidationError

    data class InvalidPostcode(
        val field: AddressField,
        val value: String,
    ) : ShipmentValidationError

    data class InvalidWeight(
        val parcelIndex: Int,
        val weightGrams: Int,
    ) : ShipmentValidationError

    data class InvalidDimensions(
        val parcelIndex: Int,
    ) : ShipmentValidationError
}

/**
 * A request that passed validation. The constructor is internal, so code outside this module
 * must go through [validate] to get one: parse, don't validate.
 */
@ConsistentCopyVisibility
data class Shipment internal constructor(
    val request: ShipmentRequest,
    val totalWeightGrams: Int,
)

/**
 * Sender, recipient, and parcels are independent, so `zipOrAccumulate` runs all three and
 * raises every problem together. `withNel` lets a block that already raises a
 * `NonEmptyList` take part in the accumulation.
 */
fun validate(request: ShipmentRequest): Either<NonEmptyList<ShipmentValidationError>, Shipment> =
    either {
        zipOrAccumulate(
            { validatePostcode(request.sender, AddressField.SENDER) },
            { validatePostcode(request.recipient, AddressField.RECIPIENT) },
            { withNel { validateParcels(request.parcels) } },
        ) { _, _, parcels -> Shipment(request, parcels.sumOf(Parcel::weightGrams)) }
    }

/** One rule, one possible error: a plain `Raise` is enough. */
fun Raise<ShipmentValidationError>.validatePostcode(
    address: Address,
    field: AddressField,
): Address {
    ensure(address.postcode.matches(postcodeFormat(address.country))) {
        ShipmentValidationError.InvalidPostcode(field, address.postcode)
    }
    return address
}

/** Every parcel is checked, so this can fail with several errors at once. */
fun Raise<NonEmptyList<ShipmentValidationError>>.validateParcels(parcels: List<Parcel>): List<Parcel> {
    ensure(parcels.isNotEmpty()) { nonEmptyListOf(ShipmentValidationError.NoParcels) }
    return mapOrAccumulate(parcels.withIndex()) { (index, parcel) ->
        withNel { validateParcel(parcel, index) }
    }
}

/** An invalid weight must not hide invalid dimensions on the same parcel. */
fun Raise<NonEmptyList<ShipmentValidationError>>.validateParcel(
    parcel: Parcel,
    index: Int,
): Parcel =
    zipOrAccumulate(
        {
            ensure(parcel.weightGrams in 1..MAX_PARCEL_WEIGHT_GRAMS) {
                ShipmentValidationError.InvalidWeight(index, parcel.weightGrams)
            }
        },
        {
            val dimensions = parcel.dimensions
            ensure(dimensions.lengthMm > 0 && dimensions.widthMm > 0 && dimensions.heightMm > 0) {
                ShipmentValidationError.InvalidDimensions(index)
            }
        },
    ) { _, _ -> parcel }

private val norwegianOrDanishPostcode = Regex("""\d{4}""")
private val swedishPostcode = Regex("""\d{3} ?\d{2}""")

private fun postcodeFormat(country: Country): Regex =
    when (country) {
        Country.NO, Country.DK -> norwegianOrDanishPostcode
        Country.SE -> swedishPostcode
    }
