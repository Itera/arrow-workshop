@file:OptIn(ExperimentalCoroutinesApi::class)

package workshop.providers

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import arrow.fx.coroutines.ExitCase
import arrow.fx.coroutines.resourceScope
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import workshop.domain.Courier
import workshop.domain.CourierQuote
import workshop.domain.Money
import workshop.quoting.CourierDecline
import workshop.quoting.QuoteError
import workshop.quoting.requestQuotes
import workshop.shipment
import workshop.validation.Shipment
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** A fake session-based courier API that records every acquisition and release. */
private class RecordingSessions(
    name: String,
    private val latency: Duration = Duration.ZERO,
    private val answer: (Courier) -> Either<CourierDecline, CourierQuote> = {
        CourierQuote(it, Money(400), deliveryDays = 2).right()
    },
) : SessionFactory {
    val courier = Courier(name)
    val provider = SessionCourierProvider(courier, this)
    val opened = AtomicInteger()
    val closed = AtomicInteger()

    override suspend fun open(): ProviderSession {
        opened.incrementAndGet()
        return object : ProviderSession {
            override suspend fun quote(shipment: Shipment): Either<CourierDecline, CourierQuote> {
                delay(latency)
                return answer(courier)
            }

            override suspend fun close() {
                closed.incrementAndGet()
            }
        }
    }
}

class ResourceSafetyTest :
    FunSpec({
        val shipment = shipment()

        test("releases the session after success") {
            runTest {
                val sessions = RecordingSessions("north")

                sessions.provider.quote(shipment).shouldBeRight()

                sessions.opened.get() shouldBe 1
                sessions.closed.get() shouldBe 1
            }
        }

        test("releases the session when the block raises a typed error") {
            runTest {
                val sessions = RecordingSessions("north")

                either<String, Unit> { sessions.withSession { raise("declined") } }.shouldBeLeft() shouldBe "declined"

                sessions.closed.get() shouldBe 1
            }
        }

        test("release sees a typed raise as ExitCase.Cancelled") {
            runTest {
                var exitCase: ExitCase? = null

                either<String, Unit> {
                    resourceScope {
                        install({ "session" }) { _, exit -> exitCase = exit }
                        raise("declined")
                    }
                }.shouldBeLeft()

                exitCase.shouldBeInstanceOf<ExitCase.Cancelled>()
            }
        }

        test("releases the session when the block throws") {
            runTest {
                val sessions = RecordingSessions("north", answer = { error("protocol error") })

                shouldThrow<IllegalStateException> { sessions.provider.quote(shipment) }

                sessions.closed.get() shouldBe 1
            }
        }

        test("releases the session when the caller cancels") {
            runTest {
                val sessions = RecordingSessions("north", latency = 10.seconds)

                val job = launch { sessions.provider.quote(shipment) }
                advanceTimeBy(1.seconds)
                sessions.opened.get() shouldBe 1
                sessions.closed.get() shouldBe 0

                job.cancel()
                job.join()

                sessions.closed.get() shouldBe 1
            }
        }

        test("releases a provider session when a sibling quote fails") {
            runTest {
                val slow = RecordingSessions("slow", latency = 10.seconds)
                val strict =
                    RecordingSessions(
                        "strict",
                        latency = 1.seconds,
                    ) { CourierDecline.Prohibited(it, "explosives").left() }

                val result = either { requestQuotes(shipment, listOf(slow.provider, strict.provider)) }

                result.shouldBeLeft() shouldBe QuoteError.ShipmentProhibited(Courier("strict"), "explosives")
                slow.opened.get() shouldBe 1
                slow.closed.get() shouldBe 1
                strict.closed.get() shouldBe 1
            }
        }
    })
