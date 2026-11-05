package workshop.providers

import arrow.core.Either
import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.resource
import arrow.fx.coroutines.resourceScope
import workshop.domain.Courier
import workshop.domain.CourierQuote
import workshop.quoting.CourierDecline
import workshop.quoting.CourierProvider
import workshop.validation.Shipment

/** A courier API that needs a session, for example a login token or a rate-limit permit. */
interface ProviderSession {
    suspend fun quote(shipment: Shipment): Either<CourierDecline, CourierQuote>

    suspend fun close()
}

fun interface SessionFactory {
    suspend fun open(): ProviderSession
}

/** The same acquisition and release as a value that can be composed with other resources. */
fun SessionFactory.session(): Resource<ProviderSession> =
    resource(acquire = { open() }, release = { session, _ -> session.close() })

/**
 * The session is released when [block] succeeds, raises a typed error, throws, or is cancelled.
 * Release runs in a non-cancellable context, so cleanup finishes even during cancellation.
 */
suspend fun <A> SessionFactory.withSession(block: suspend (ProviderSession) -> A): A =
    resourceScope {
        val session = session().bind()
        block(session)
    }

class SessionCourierProvider(
    override val courier: Courier,
    private val sessions: SessionFactory,
) : CourierProvider {
    override suspend fun quote(shipment: Shipment): Either<CourierDecline, CourierQuote> =
        sessions.withSession { it.quote(shipment) }
}
