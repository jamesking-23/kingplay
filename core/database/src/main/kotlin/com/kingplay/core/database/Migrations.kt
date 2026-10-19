package com.kingplay.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Every schema change gets a migration here. Nothing is ever wiped: playlists and favorites live in this database. */
object Migrations {
    /** Version 2 adds imported lyrics. */
    val V1_TO_V2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `imported_lyrics` (" +
                    "`lyricsKey` TEXT NOT NULL, `lrc` TEXT NOT NULL, `updatedAtMs` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`lyricsKey`))",
            )
        }
    }

    /** Version 3 adds the play history behind the monthly listening report. */
    val V2_TO_V3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `play_events` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `trackId` INTEGER NOT NULL, " +
                    "`playedAtMs` INTEGER NOT NULL)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_playedAtMs` ON `play_events` (`playedAtMs`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_trackId` ON `play_events` (`trackId`)")
        }
    }

    val ALL = arrayOf(V1_TO_V2, V2_TO_V3)
}
