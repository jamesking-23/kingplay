package com.kingplay.core.model

import com.kingplay.core.model.about.AboutInfo
import com.kingplay.core.model.about.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class AboutInfoTest {
    @Test fun copyrightLineHasTheSymbolCompanyAndYear() {
        assertEquals("© 2026 KKINGG Corporation. All rights reserved.", AboutInfo.copyright(2026))
    }

    @Test fun sessionIdLooksLikeAnIdentifierAndDiffersEachTime() {
        val a = AboutInfo.newSessionId()
        assertTrue(Regex("[A-Za-z0-9_-]{22}\\.0").matches(a))
        assertNotEquals(a, AboutInfo.newSessionId())
        assertEquals(AboutInfo.newSessionId(Random(7)), AboutInfo.newSessionId(Random(7)))
    }

    @Test fun acknowledgementsListOnlyThePlatformsOwnSoftwareSortedByName() {
        val desktop = AboutInfo.acknowledgements(Platform.DESKTOP)
        assertTrue(desktop.contains("vlcj") && desktop.contains("JAudiotagger"))
        assertTrue(!desktop.contains("Media3"))
        val android = AboutInfo.acknowledgements(Platform.ANDROID)
        assertTrue(android.contains("Media3") && !android.contains("vlcj"))
        val names = AboutInfo.thirdParty.filter { Platform.DESKTOP in it.platforms }.map { it.name }
            .sortedBy { it.lowercase() }
        val positions = names.map { desktop.indexOf("\n$it\n") }
        assertTrue(positions.all { it >= 0 })
        assertEquals(positions.sorted(), positions)
    }

    @Test fun legalTextsNameTheCompanyAndLinksAreWebAddresses() {
        assertTrue(AboutInfo.licenseTerms.contains("KKINGG Corporation"))
        assertTrue(AboutInfo.privacyPolicy.contains("nothing about you"))
        listOf(AboutInfo.FEEDBACK_URL, AboutInfo.helpUrl(Platform.ANDROID), AboutInfo.helpUrl(Platform.DESKTOP))
            .forEach { assertTrue(it.startsWith("https://")) }
    }
}
