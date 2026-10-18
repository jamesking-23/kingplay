package com.kingplay.core.designsystem.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionPolicyTest {
    @Test fun normalSettingsKeepMotion() = assertFalse(effectiveLowPower(setting = false, animatorScale = 1f))

    @Test fun inAppSwitchReducesMotion() = assertTrue(effectiveLowPower(setting = true, animatorScale = 1f))

    @Test fun systemAnimationsOffReducesMotion() = assertTrue(effectiveLowPower(setting = false, animatorScale = 0f))

    @Test fun slowedAnimationsAreStillMotion() = assertFalse(effectiveLowPower(setting = false, animatorScale = 2f))
}
