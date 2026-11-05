package workshop.quoting

import arrow.core.raise.Raise
import arrow.core.raise.context.raise
import workshop.domain.Address
import workshop.providers.LegacyZoneLookup
import workshop.providers.UnknownPostcodeException
import workshop.providers.Zone

/**
 * The adapter classifies failures so no raw `Result` leaks into the domain.
 *
 * Decision: an unknown postcode is a *domain* error. The format already passed validation,
 * so the request is well-formed and we simply do not deliver there. Anything else is a
 * technical failure and stays an exception, which section 7 retries.
 */
context(_: Raise<QuoteError>)
fun LegacyZoneLookup.zoneFor(address: Address): Zone =
    lookup(address.postcode).getOrElse { failure ->
        when (failure) {
            is UnknownPostcodeException -> raise(QuoteError.UnknownPostcode(address.postcode))
            else -> throw failure
        }
    }
