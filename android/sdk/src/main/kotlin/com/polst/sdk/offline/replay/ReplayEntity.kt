package com.polst.sdk.offline.replay

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Note: data class equals() compares ByteArray by reference; acceptable for internal repository use.
@Entity(tableName = "replay_entries")
internal data class ReplayEntity(
    @PrimaryKey val idempotencyKey: String,
    val endpoint: String,
    val method: String,
    val body: ByteArray?,
    @ColumnInfo(index = true) val createdAt: Long,
    @ColumnInfo(index = true) val lastAttemptAt: Long?,
    val attemptCount: Int,
    val lastErrorClass: String?,
)
