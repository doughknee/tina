package com.tina.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert
import com.tina.app.agenda.OccurrenceKey
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * One occurrence of a repeating item, marked done or skipped. Repeating items are stored
 * once with a rule, so "3 of 7 done" has to live somewhere — this is where. Rows are keyed
 * on (item, day), so marking the same day twice just overwrites.
 */
@Entity(tableName = "occurrence_completions", primaryKeys = ["itemId", "epochDay"])
data class OccurrenceCompletion(
    val itemId: Long,
    val epochDay: Int,
    /** Skipped occurrences advance the series without counting as done. */
    val skipped: Boolean = false,
    val completedAt: Long,
)

@Dao
interface OccurrenceDao {
    @Upsert
    suspend fun upsert(completion: OccurrenceCompletion)

    @Query("DELETE FROM occurrence_completions WHERE itemId = :itemId AND epochDay = :epochDay")
    suspend fun delete(itemId: Long, epochDay: Int)

    @Query("SELECT * FROM occurrence_completions")
    fun observeAll(): Flow<List<OccurrenceCompletion>>
}

class OccurrenceRepository(private val dao: OccurrenceDao, private val clock: Clock = Clock.System) {
    /** Done occurrences drive the dot strip; skipped ones only move "next" along. */
    fun observeDone(): Flow<Set<OccurrenceKey>> =
        dao.observeAll().map { list -> list.filter { !it.skipped }.map { it.key() }.toSet() }

    fun observeSkipped(): Flow<Set<OccurrenceKey>> =
        dao.observeAll().map { list -> list.filter { it.skipped }.map { it.key() }.toSet() }

    suspend fun complete(itemId: Long, date: LocalDate) =
        dao.upsert(OccurrenceCompletion(itemId, date.toEpochDays().toInt(), false, clock.now().toEpochMilliseconds()))

    suspend fun skip(itemId: Long, date: LocalDate) =
        dao.upsert(OccurrenceCompletion(itemId, date.toEpochDays().toInt(), true, clock.now().toEpochMilliseconds()))

    /** Undo path for both complete and skip. */
    suspend fun clear(itemId: Long, date: LocalDate) = dao.delete(itemId, date.toEpochDays().toInt())

    private fun OccurrenceCompletion.key() = OccurrenceKey(itemId, epochDay)
}
