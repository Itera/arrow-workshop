package workshop.stm

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

@Suppress("InjectDispatcher") // Real threads on purpose, so the transactions genuinely contend.
class PickupSlotsTest :
    FunSpec({
        test("concurrent reservations never take more slots than exist") {
            val slots = PickupSlots.create(capacity = 10)

            val results =
                withContext(Dispatchers.Default) {
                    List(1_000) { async { slots.tryReserve() } }.awaitAll()
                }

            results.count { it } shouldBe 10
            slots.available() shouldBe 0
        }

        test("a released slot can be reserved again") {
            val slots = PickupSlots.create(capacity = 1)

            slots.tryReserve() shouldBe true
            slots.tryReserve() shouldBe false
            slots.release()
            slots.tryReserve() shouldBe true
        }
    })
