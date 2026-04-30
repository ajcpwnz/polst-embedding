package com.polst.sdk.offline.replay

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface ReplayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ReplayEntity)

    @Query("SELECT * FROM replay_entries ORDER BY createdAt ASC")
    suspend fun listOrderedByCreated(): List<ReplayEntity>

    @Query("DELETE FROM replay_entries WHERE idempotencyKey = :key")
    suspend fun deleteByKey(key: String): Int

    @Query(
        "UPDATE replay_entries SET attemptCount = attemptCount + 1, " +
            "lastAttemptAt = :nowMs, lastErrorClass = :errorClass " +
            "WHERE idempotencyKey = :key",
    )
    suspend fun bumpAttempt(key: String, nowMs: Long, errorClass: String): Int

    @Query("DELETE FROM replay_entries WHERE createdAt < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long): Int

    @Query("SELECT COUNT(*) FROM replay_entries")
    suspend fun count(): Int
}
