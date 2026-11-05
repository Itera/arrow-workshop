package workshop.domain

/** An amount in øre/öre, the smallest currency unit. Never negative. */
@JvmInline
value class Money(
    val cents: Long,
) : Comparable<Money> {
    init {
        require(cents >= 0) { "Money cannot be negative: $cents" }
    }

    operator fun plus(other: Money): Money = Money(cents + other.cents)

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    companion object {
        val ZERO = Money(0)
    }
}

enum class Country { NO, SE, DK }

/** Each service promises delivery within a number of days. */
enum class DeliveryService(
    val maxDeliveryDays: Int,
) {
    STANDARD(maxDeliveryDays = 5),
    EXPRESS(maxDeliveryDays = 1),
}

@JvmInline
value class Courier(
    val name: String,
)

data class Address(
    val name: String,
    val street: String,
    val postcode: String,
    val country: Country,
)

data class Dimensions(
    val lengthMm: Int,
    val widthMm: Int,
    val heightMm: Int,
)

data class Parcel(
    val weightGrams: Int,
    val dimensions: Dimensions,
)

/** Hours of the day, for example 9 to 12. */
data class PickupWindow(
    val fromHour: Int,
    val toHour: Int,
)

data class DeliveryOptions(
    val insuranceAmount: Money? = null,
    val pickupWindow: PickupWindow? = null,
)

data class ShipmentRequest(
    val sender: Address,
    val recipient: Address,
    val parcels: List<Parcel>,
    val service: DeliveryService,
    val options: DeliveryOptions = DeliveryOptions(),
)

data class CourierQuote(
    val courier: Courier,
    val price: Money,
    val deliveryDays: Int,
)

data class DeliveryQuote(
    val selected: CourierQuote,
    val alternatives: List<CourierQuote>,
    val surcharges: Money = Money.ZERO,
) {
    val total: Money get() = selected.price + surcharges
}

fun ShipmentRequest.withRecipientPostcode(postcode: String): ShipmentRequest =
    copy(recipient = recipient.copy(postcode = postcode))

// Compare this with the optics version in 09-extras.
fun ShipmentRequest.withParcelWeight(
    index: Int,
    weightGrams: Int,
): ShipmentRequest =
    copy(
        parcels =
            parcels.mapIndexed { currentIndex, parcel ->
                if (currentIndex == index) parcel.copy(weightGrams = weightGrams) else parcel
            },
    )
