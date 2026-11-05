package workshop.stm

import arrow.fx.stm.TVar
import arrow.fx.stm.atomically

/**
 * Courier pickup capacity shared by concurrent bookings. Reading and writing the counter in one
 * `atomically` block makes check-then-decrement a single transaction, so two bookings can never
 * both take the last slot.
 */
class PickupSlots private constructor(
    private val available: TVar<Int>,
) {
    suspend fun tryReserve(): Boolean =
        atomically {
            val free = available.read()
            if (free > 0) {
                available.write(free - 1)
                true
            } else {
                false
            }
        }

    suspend fun release() = atomically { available.modify { it + 1 } }

    suspend fun available(): Int = available.unsafeRead()

    companion object {
        suspend fun create(capacity: Int): PickupSlots = PickupSlots(TVar.new(capacity))
    }
}
