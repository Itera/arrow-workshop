@file:OptIn(ExperimentalCoroutinesApi::class)

package workshop.quoting

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import workshop.FakeCourier
import workshop.decliningCourier
import workshop.domain.Courier
import workshop.quotingCourier
import workshop.shipment
import kotlin.time.Duration.Companion.seconds

class LiveUpdatesTest :
    FunSpec({
        val shipment = shipment()

        test("emits Received, Declined, and Failed values for each provider") {
            runTest {
                val providers =
                    listOf(
                        quotingCourier("north", 400),
                        decliningCourier("fjord"),
                        FakeCourier("aurora") { error("connection reset") },
                    )

                val updates = liveQuoteUpdates(shipment, providers).toList()

                updates.map { it::class } shouldContainExactlyInAnyOrder
                    listOf(QuoteUpdate.Received::class, QuoteUpdate.Declined::class, QuoteUpdate.Failed::class)
                updates
                    .single { it.courier == Courier("aurora") }
                    .shouldBeInstanceOf<QuoteUpdate.Failed>()
                    .cause
                    .message shouldBe "connection reset"
            }
        }

        test("a slow first provider does not delay a later provider with unordered mapping") {
            runTest {
                val providers = listOf(quotingCourier("slow", 100, latency = 5.seconds), quotingCourier("fast", 200))

                liveQuoteUpdates(shipment, providers).first().courier shouldBe Courier("fast")
                currentTime shouldBe 0
            }
        }

        test("ordered mapping keeps provider order and waits for the slow provider") {
            runTest {
                val providers = listOf(quotingCourier("slow", 100, latency = 5.seconds), quotingCourier("fast", 200))

                orderedQuoteUpdates(shipment, providers).map { it.courier }.toList() shouldBe
                    listOf(Courier("slow"), Courier("fast"))
                currentTime shouldBe 5_000
            }
        }

        test("cancelling collection cancels providers still in flight") {
            runTest {
                val slow = quotingCourier("slow", 100, latency = 5.seconds)
                val providers = listOf(slow, quotingCourier("fast", 200))

                liveQuoteUpdates(shipment, providers).first()

                slow.wasCancelled shouldBe true
            }
        }
    })
