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

    val ALL = arrayOf(V1_TO_V2)
}
