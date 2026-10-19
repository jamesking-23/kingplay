package com.kingplay.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One counted play of a song. Kept without a foreign key so history survives a song leaving the library. */
@Entity(tableName = "play_events", indices = [Index(value = ["playedAtMs"]), Index(value = ["trackId"])])
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val playedAtMs: Long,
)
