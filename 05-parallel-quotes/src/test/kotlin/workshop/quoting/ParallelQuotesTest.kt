@file:OptIn(ExperimentalCoroutinesApi::class)

package workshop.quoting

import arrow.core.raise.either
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import workshop.FakeCourier
import workshop.decliningCourier
import workshop.domain.Courier
import workshop.domain.CourierQuote
import workshop.domain.DeliveryService
import workshop.domain.Money
import workshop.quotingCourier
import workshop.shipment
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ParallelQuotesTest :
    FunSpec({
        val shipment = shipment()

        test("queries three providers in parallel") {
            runTest {
                val providers = List(3) { quotingCourier("c$it", cents = 100, latency = 1.seconds) }

                either { requestQuotes(shipment, providers) }.shouldBeRight().size shouldBe 3

                // Sequential calls would take three seconds of virtual time.
                currentTime shouldBe 1_000
            }
        }

        test("a fixed set of providers can be zipped in parallel") {
            runTest {
                val quotes =
                    either {
                        requestQuotes(
                            shipment,
                            quotingCourier("north", 400, latency = 1.seconds),
                            decliningCourier("fjord", latency = 1.seconds),
                            quotingCourier("aurora", 300, latency = 1.seconds),
                        )
                    }.shouldBeRight()

                quotes.map(CourierQuote::courier) shouldContainExactlyInAnyOrder
                    listOf(Courier("north"), Courier("aurora"))
                currentTime shouldBe 1_000
            }
        }

        test("never has more than three provider calls in flight") {
            runTest {
                val inFlight = AtomicInteger()
                val peak = AtomicInteger()
                val providers =
                    List(10) { index ->
                        FakeCourier("c$index") {
                            peak.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
                            delay(100.milliseconds)
                            inFlight.decrementAndGet()
                            price(1_000L + index)
                        }
                    }

                either { requestQuotes(shipment, providers) }.shouldBeRight().size shouldBe 10
                peak.get() shouldBe MAX_CONCURRENT_QUOTES
            }
        }

        test("a courier without service is skipped, not fatal") {
            runTest {
                val providers = listOf(decliningCourier("north"), quotingCourier("fjord", 300))

                either { requestQuotes(shipment, providers) }.shouldBeRight().single().courier shouldBe Courier("fjord")
            }
        }

        test("when every courier declines the caller sees NoQuotesAvailable") {
            runTest {
                val providers = listOf(decliningCourier("north"), decliningCourier("fjord"))

                either { requestQuotes(shipment, providers) }.shouldBeLeft() shouldBe QuoteError.NoQuotesAvailable
            }
        }

        test("a prohibited shipment cancels outstanding provider calls") {
            runTest {
                val slow = quotingCourier("slow", 100, latency = 10.seconds)
                val strict = FakeCourier("strict", latency = 1.seconds) { prohibited("lithium batteries") }

                either { requestQuotes(shipment, listOf(slow, strict)) }.shouldBeLeft() shouldBe
                    QuoteError.ShipmentProhibited(Courier("strict"), "lithium batteries")

                slow.wasCancelled shouldBe true
                currentTime shouldBe 1_000
            }
        }

        test("returns the cheapest quote that meets the delivery promise") {
            val quotes =
                listOf(
                    CourierQuote(Courier("cheap-but-slow"), Money(100), deliveryDays = 7),
                    CourierQuote(Courier("north"), Money(400), deliveryDays = 2),
                    CourierQuote(Courier("fjord"), Money(300), deliveryDays = 3),
                    CourierQuote(Courier("aurora"), Money(300), deliveryDays = 1),
                )

            val selected = either { selectQuote(quotes, DeliveryService.STANDARD, Money(50)) }.shouldBeRight()

            selected.selected.courier shouldBe Courier("aurora")
            selected.alternatives.map(CourierQuote::courier) shouldBe listOf(Courier("fjord"), Courier("north"))
            selected.total shouldBe Money(350)
        }

        test("no eligible quote is NoQuotesAvailable") {
            val quotes = listOf(CourierQuote(Courier("north"), Money(400), deliveryDays = 2))

            either { selectQuote(quotes, DeliveryService.EXPRESS, Money.ZERO) }.shouldBeLeft() shouldBe
                QuoteError.NoQuotesAvailable
        }
    })
