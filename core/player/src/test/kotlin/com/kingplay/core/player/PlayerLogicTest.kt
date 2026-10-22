package com.kingplay.core.player

import com.kingplay.core.model.MediaRef
import com.kingplay.core.model.QueueState
import com.kingplay.core.model.RepeatMode
import com.kingplay.core.player.logic.BrowseIds
import com.kingplay.core.player.logic.BrowseNode
import com.kingplay.core.player.logic.CallerPolicy
import com.kingplay.core.player.logic.MediaIds
import com.kingplay.core.player.logic.PlayCountTracker
import com.kingplay.core.player.logic.defaultPlayThresholdMs
import com.kingplay.core.player.logic.expandToList
import com.kingplay.core.player.logic.pageOf
import com.kingplay.core.player.logic.playNextInsertIndex
import com.kingplay.core.player.logic.pruneQueue
import com.kingplay.core.player.logic.snapshotOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaIdsTest {
    @Test
    fun roundTripsAudioAndVideo() {
        assertEquals("a:42", MediaIds.of(MediaRef.Audio(42)))
        assertEquals("v:7", MediaIds.of(MediaRef.Video(7)))
        assertEquals(MediaRef.Audio(42), MediaIds.parse("a:42"))
        assertEquals(MediaRef.Video(7), MediaIds.parse("v:7"))
    }

    @Test
    fun rejectsAnythingElse() {
        assertNull(MediaIds.parse(null))
        assertNull(MediaIds.parse(""))
        assertNull(MediaIds.parse("x:1"))
        assertNull(MediaIds.parse("a:"))
        assertNull(MediaIds.parse("a:abc"))
        assertNull(MediaIds.parse("42"))
        assertNull(MediaIds.parse("kingplay_root"))
    }
}

class QueueSnapshotTest {
    @Test
    fun snapshotKeepsOrderIndexAndModes() {
        val s = snapshotOf(listOf("a:1", "v:2", "a:3"), 1, 12_000, true, RepeatMode.ALL)
        assertEquals(listOf(MediaRef.Audio(1), MediaRef.Video(2), MediaRef.Audio(3)), s.items)
        assertEquals(1, s.currentIndex)
        assertEquals(12_000L, s.positionMs)
        assertEquals(true, s.shuffleEnabled)
        assertEquals(RepeatMode.ALL, s.repeatMode)
    }

    @Test
    fun unparsableIdsAreDroppedAndIndexFollowsItsItem() {
        val s = snapshotOf(listOf("junk", "a:1", "junk", "a:2"), 3, 500, false, RepeatMode.OFF)
        assertEquals(listOf(MediaRef.Audio(1), MediaRef.Audio(2)), s.items)
        assertEquals(1, s.currentIndex)
        assertEquals(MediaRef.Audio(2), s.current)
    }

    @Test
    fun noCurrentItemMeansNoPosition() {
        val s = snapshotOf(listOf("a:1"), -1, 9_000, false, RepeatMode.OFF)
        assertEquals(-1, s.currentIndex)
        assertEquals(0L, s.positionMs)
    }

    private val queue = QueueState(
        items = listOf(MediaRef.Audio(1), MediaRef.Audio(2), MediaRef.Audio(3), MediaRef.Audio(4)),
        currentIndex = 2,
        positionMs = 40_000,
        shuffleEnabled = true,
        repeatMode = RepeatMode.ONE,
    )

    @Test
    fun pruneKeepsEverythingWhenAllExist() {
        assertEquals(queue.copy(shuffleOrder = emptyList()), pruneQueue(queue) { true })
    }

    @Test
    fun pruneBeforeCurrentShiftsIndexAndKeepsPosition() {
        val p = pruneQueue(queue) { it != MediaRef.Audio(1) }
        assertEquals(listOf(MediaRef.Audio(2), MediaRef.Audio(3), MediaRef.Audio(4)), p.items)
        assertEquals(1, p.currentIndex)
        assertEquals(MediaRef.Audio(3), p.current)
        assertEquals(40_000L, p.positionMs)
        assertEquals(RepeatMode.ONE, p.repeatMode)
    }

    @Test
    fun pruneCurrentResumesAtNextItemFromTheStart() {
        val p = pruneQueue(queue) { it != MediaRef.Audio(3) }
        assertEquals(listOf(MediaRef.Audio(1), MediaRef.Audio(2), MediaRef.Audio(4)), p.items)
        assertEquals(MediaRef.Audio(4), p.current)
        assertEquals(0L, p.positionMs)
    }

    @Test
    fun pruneCurrentAtEndFallsBackToLastItem() {
        val atEnd = queue.copy(currentIndex = 3)
        val p = pruneQueue(atEnd) { it != MediaRef.Audio(4) }
        assertEquals(MediaRef.Audio(3), p.current)
        assertEquals(0L, p.positionMs)
    }

    @Test
    fun pruneEverythingGivesEmptyQueueButKeepsModes() {
        val p = pruneQueue(queue) { false }
        assertEquals(emptyList<MediaRef>(), p.items)
        assertEquals(-1, p.currentIndex)
        assertEquals(true, p.shuffleEnabled)
        assertEquals(RepeatMode.ONE, p.repeatMode)
    }

    @Test
    fun playNextGoesAfterCurrentOrAtEnd() {
        assertEquals(3, playNextInsertIndex(2, 5))
        assertEquals(5, playNextInsertIndex(-1, 5))
        assertEquals(0, playNextInsertIndex(-1, 0))
        assertEquals(5, playNextInsertIndex(9, 5))
    }
}

class PlayCountTrackerTest {
    @Test
    fun aSongCountsAfterThreeSecondsOfRealPlaying() {
        assertEquals(3_000L, defaultPlayThresholdMs(240_000))
        assertEquals(3_000L, defaultPlayThresholdMs(0))
        assertEquals(2_000L, defaultPlayThresholdMs(2_000))
    }

    @Test
    fun countsOncePerStartAfterThreeSeconds() {
        val t = PlayCountTracker()
        t.onItemStarted("a:1")
        assertNull(t.addPlayed(1_000, 240_000))
        assertNull(t.addPlayed(1_000, 240_000))
        assertEquals("a:1", t.addPlayed(1_000, 240_000))
        assertNull(t.addPlayed(60_000, 240_000))
    }

    @Test
    fun repeatOfSameItemCountsAgain() {
        val t = PlayCountTracker()
        t.onItemStarted("a:1")
        assertEquals("a:1", t.addPlayed(3_000, 240_000))
        t.onItemStarted("a:1")
        assertEquals("a:1", t.addPlayed(3_000, 240_000))
    }

    @Test
    fun skippingBeforeThreeSecondsDoesNotCount() {
        val t = PlayCountTracker()
        t.onItemStarted("a:1")
        assertNull(t.addPlayed(2_900, 240_000))
        t.onItemStarted("a:2")
        assertNull(t.addPlayed(2_000, 240_000))
    }

    @Test
    fun ignoresTimeWithNoItemOrNonPositiveDelta() {
        val t = PlayCountTracker()
        assertNull(t.addPlayed(60_000, 240_000))
        t.onItemStarted("a:1")
        assertNull(t.addPlayed(0, 240_000))
        assertNull(t.addPlayed(-5, 240_000))
        t.onItemStarted(null)
        assertNull(t.addPlayed(60_000, 240_000))
    }
}

class BrowseIdsTest {
    @Test
    fun everyNodeSurvivesAnIdRoundTrip() {
        val nodes = BrowseIds.rootChildren + BrowseIds.smartLists + listOf(
            BrowseNode.Root,
            BrowseNode.Playlist(7),
            BrowseNode.Album(123_456_789_012L),
            BrowseNode.Artist(0),
        )
        for (node in nodes) assertEquals(node, BrowseIds.parse(BrowseIds.of(node)))
    }

    @Test
    fun playableAndGarbageIdsAreNotFolders() {
        for (id in listOf(null, "", "a:5", "v:5", "browse:", "browse:album", "browse:album:x", "browse:nope:1", "x")) {
            assertNull("accepted: $id", BrowseIds.parse(id))
        }
    }

    @Test
    fun theRootHasFourTabsAndEachIsReachable() {
        assertEquals(4, BrowseIds.rootChildren.size)
        assertTrue(BrowseIds.rootChildren.none { it == BrowseNode.Root })
        assertTrue(BrowseIds.rootChildren.all { BrowseIds.parse(BrowseIds.of(it)) == it })
    }

    @Test
    fun onlySongFoldersExpandToTheWholeList() {
        assertTrue(BrowseIds.listsSongs(BrowseNode.Album(1)))
        assertTrue(BrowseIds.listsSongs(BrowseNode.Recent))
        assertTrue(BrowseIds.listsSongs(BrowseNode.Favorites))
        assertFalse(BrowseIds.listsSongs(BrowseNode.Albums))
        assertFalse(BrowseIds.listsSongs(BrowseNode.Playlists))
    }

    @Test
    fun pagesSliceTheListAndStopAtTheEnd() {
        val items = (1..10).toList()
        assertEquals(listOf(1, 2, 3, 4), pageOf(items, 0, 4))
        assertEquals(listOf(5, 6, 7, 8), pageOf(items, 1, 4))
        assertEquals(listOf(9, 10), pageOf(items, 2, 4))
        assertEquals(emptyList<Int>(), pageOf(items, 3, 4))
        assertEquals(emptyList<Int>(), pageOf(items, -1, 4))
        assertEquals(emptyList<Int>(), pageOf(items, 0, 0))
        assertEquals(emptyList<Int>(), pageOf(items, Int.MAX_VALUE, Int.MAX_VALUE))
    }

    @Test
    fun tappingASongQueuesTheListFromThatSong() {
        assertEquals(listOf(10L, 20L, 30L) to 1, expandToList(listOf(10L, 20L, 30L), 20L))
        assertNull(expandToList(listOf(10L, 20L), 99L))
    }
}

class CallerPolicyTest {
    private val own = "com.kingplay.app"

    @Test
    fun ownAppAndTrustedSystemCallersAreAllowed() {
        assertTrue(CallerPolicy.isAllowed(own, own, trusted = false))
        assertTrue(CallerPolicy.isAllowed("android", own, trusted = true))
    }

    @Test
    fun knownCarAndAssistantPackagesAreAllowed() {
        assertTrue(CallerPolicy.isAllowed("com.google.android.projection.gearhead", own, trusted = false))
        assertTrue(CallerPolicy.isAllowed("com.google.android.googlequicksearchbox", own, trusted = false))
        assertTrue(CallerPolicy.isAllowed("com.android.bluetooth", own, trusted = false))
    }

    @Test
    fun anyOtherPackageIsRefused() {
        assertFalse(CallerPolicy.isAllowed("com.example.snoop", own, trusted = false))
        assertFalse(CallerPolicy.isAllowed("", own, trusted = false))
    }

    @Test
    fun lookAlikeNamesDoNotMatch() {
        assertFalse(CallerPolicy.isAllowed("com.google.android.projection.gearhead.evil", own, trusted = false))
        assertFalse(CallerPolicy.isAllowed("evil.com.android.bluetooth", own, trusted = false))
        assertFalse(CallerPolicy.isAllowed(own + ".other", own, trusted = false))
    }

    @Test
    fun theListIsWellFormed() {
        assertTrue(CallerPolicy.ALLOWED_PACKAGES.all { it == it.trim().lowercase() && it.contains('.') })
    }
}
