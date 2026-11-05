package workshop.optics

import arrow.optics.dsl.every
import arrow.optics.dsl.index
import workshop.domain.ShipmentRequest
import workshop.domain.parcels
import workshop.domain.postcode
import workshop.domain.recipient
import workshop.domain.weightGrams

/**
 * The `@optics` compiler plugin generates a lens per property on each annotated class's
 * companion. Lenses compose with `.`, so a deep update reads as a path.
 */
fun ShipmentRequest.withRecipientPostcodeOptic(postcode: String): ShipmentRequest =
    ShipmentRequest.recipient.postcode.set(this, postcode)

/** `index` focuses one list element. An out-of-range index leaves the request unchanged. */
fun ShipmentRequest.withParcelWeightOptic(
    index: Int,
    weightGrams: Int,
): ShipmentRequest =
    ShipmentRequest.parcels
        .index(index)
        .weightGrams
        .set(this, weightGrams)

/** `modify` takes a function, which `copy` chains make especially noisy. */
fun ShipmentRequest.addPackagingWeight(grams: Int): ShipmentRequest =
    ShipmentRequest.parcels.every.weightGrams
        .modify(this) { it + grams }
