package workshop.providers

import workshop.domain.Money

data class Zone(
    val name: String,
    val surcharge: Money,
)

class UnknownPostcodeException(
    val postcode: String,
) : Exception("Unknown postcode: $postcode")

/**
 * An exception-oriented API we do not own. It reports an unknown postcode as
 * [UnknownPostcodeException] and a technical problem as any other exception.
 */
fun interface LegacyZoneLookup {
    fun lookup(postcode: String): Result<Zone>
}
