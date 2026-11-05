@file:OptIn(ExperimentalCoroutinesApi::class)

package workshop.resilience

import arrow.resilience.CircuitBreaker
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import workshop.FakeCourier
import workshop.domain.Courier
import workshop.quoting.CourierDecline
import workshop.quoting.QuoteService
import workshop.quotingCourier
import workshop.shipment
import workshop.shipmentRequest
import workshop.zoneLookup
import kotlin.time.Duration.Companion.seconds

class ResilienceTest :
    FunSpec({
        val shipment = shipment()

        test("a courier that fails twice and then answers is retried with backoff") {
            runTest {
                val flaky =
                    FakeCourier("flaky") { attempt ->
                        if (attempt <= 2) throw TransientProviderFailure("timeout") else price(400)
                    }

                RetryingProvider(flaky)
                    .quote(shipment)
                    .shouldBeRight()
                    .price.cents shouldBe 400

                flaky.calls shouldBe 3
                currentTime shouldBe 100 + 200
            }
        }

        test("does not retry a courier that declines the route") {
            runTest {
                val declining = FakeCourier("declining") { noService() }

                RetryingProvider(declining).quote(shipment).shouldBeLeft()

                declining.calls shouldBe 1
            }
        }

        test("does not retry an exception that is not transient") {
            runTest {
                val broken = FakeCourier("broken") { error("bug in the adapter") }

                shouldThrow<IllegalStateException> { RetryingProvider(broken).quote(shipment) }

                broken.calls shouldBe 1
            }
        }

        test("gives up after the retry budget and rethrows the last failure") {
            runTest {
                val down = FakeCourier("down") { throw TransientProviderFailure("503") }

                shouldThrow<TransientProviderFailure> { RetryingProvider(down).quote(shipment) }

                down.calls shouldBe 3
            }
        }

        test("the circuit opens after the configured failures and then fails fast") {
            runTest {
                val breaker = providerCircuitBreaker(maxFailures = 2, resetTimeout = 30.seconds, testTimeSource)
                var healthy = false
                val courier = FakeCourier("unstable") { if (healthy) price(400) else error("down") }
                val guarded = CircuitBreakingProvider(courier, breaker)

                repeat(3) { shouldThrow<IllegalStateException> { guarded.quote(shipment) } }
                breaker.state().shouldBeInstanceOf<CircuitBreaker.State.Open>()

                shouldThrow<CircuitBreaker.ExecutionRejected> { guarded.quote(shipment) }
                courier.calls shouldBe 3

                // After the reset timeout one trial call is let through; success closes the circuit.
                advanceTimeBy(31.seconds)
                healthy = true
                guarded.quote(shipment).shouldBeRight()
                breaker.state().shouldBeInstanceOf<CircuitBreaker.State.Closed>()
            }
        }

        test("declines do not open the circuit") {
            runTest {
                val breaker = providerCircuitBreaker(maxFailures = 1, timeSource = testTimeSource)
                val guarded = CircuitBreakingProvider(FakeCourier("declining") { noService() }, breaker)

                repeat(5) { guarded.quote(shipment).shouldBeLeft() }

                breaker.state().shouldBeInstanceOf<CircuitBreaker.State.Closed>()
            }
        }

        test("an unreachable courier becomes Unavailable after its retries") {
            runTest {
                val down = FakeCourier("down") { throw TransientProviderFailure("503") }
                val resilient = down.resilient(providerCircuitBreaker(timeSource = testTimeSource))

                resilient
                    .quote(shipment)
                    .shouldBeLeft()
                    .shouldBeInstanceOf<CourierDecline.Unavailable>()
                    .cause
                    .shouldBeInstanceOf<TransientProviderFailure>()
                down.calls shouldBe 3
            }
        }

        test("one courier being down does not stop the quote") {
            runTest {
                val down = FakeCourier("down") { throw TransientProviderFailure("503") }
                val service =
                    QuoteService(
                        zoneLookup(),
                        listOf(
                            down.resilient(providerCircuitBreaker(timeSource = testTimeSource)),
                            quotingCourier("north", 400).resilient(providerCircuitBreaker(timeSource = testTimeSource)),
                        ),
                    )

                service
                    .quoteShipment(shipmentRequest())
                    .shouldBeRight()
                    .selected.courier shouldBe Courier("north")
            }
        }
    })
