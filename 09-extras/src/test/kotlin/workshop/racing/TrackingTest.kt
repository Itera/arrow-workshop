@file:OptIn(ExperimentalCoroutinesApi::class)

package workshop.racing

import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private class FakeTracking(
    private val source: String,
    private val latency: Duration,
    private val succeeds: Boolean,
) : TrackingProvider {
    var wasCancelled = false
        private set

    override suspend fun track(shipmentId: String) =
        try {
            delay(latency)
            if (succeeds) {
                TrackingStatus(shipmentId, "in transit", source).right()
            } else {
                TrackingFailure(source, "timeout").left()
            }
        } catch (e: CancellationException) {
            wasCancelled = true
            throw e
        }
}

class TrackingTest :
    FunSpec({
        test("returns the faster success and cancels the losing request") {
            runTest {
                val fast = FakeTracking("fast", 1.seconds, succeeds = true)
                val slow = FakeTracking("slow", 5.seconds, succeeds = true)

                either { trackFirstSuccess("S1", slow, fast) }.shouldBeRight().source shouldBe "fast"

                slow.wasCancelled shouldBe true
                currentTime shouldBe 1_000
            }
        }

        test("a fast failure does not prevent a slower success") {
            runTest {
                val fastFailure = FakeTracking("fast", 1.seconds, succeeds = false)
                val slowSuccess = FakeTracking("slow", 5.seconds, succeeds = true)

                either { trackFirstSuccess("S1", fastFailure, slowSuccess) }.shouldBeRight().source shouldBe "slow"
                currentTime shouldBe 5_000
            }
        }

        test("when both fail the caller sees both failures") {
            runTest {
                val result =
                    either {
                        trackFirstSuccess(
                            "S1",
                            FakeTracking("a", 1.seconds, succeeds = false),
                            FakeTracking("b", 2.seconds, succeeds = false),
                        )
                    }

                result.shouldBeLeft().failures.map(TrackingFailure::source) shouldContainExactlyInAnyOrder
                    listOf("a", "b")
            }
        }
    })
