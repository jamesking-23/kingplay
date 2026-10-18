package com.kingplay.core.common

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

enum class Entitlement { PRO }

/** Premium gate. Phase 1 binds [ProUnlockedEntitlements]; :core:billing replaces the binding later. */
interface Entitlements {
    fun isUnlocked(entitlement: Entitlement = Entitlement.PRO): Boolean

    fun observe(entitlement: Entitlement = Entitlement.PRO): Flow<Boolean>
}

@Singleton
class ProUnlockedEntitlements @Inject constructor() : Entitlements {
    override fun isUnlocked(entitlement: Entitlement): Boolean = true

    override fun observe(entitlement: Entitlement): Flow<Boolean> = flowOf(true)
}
