package com.example.helloworld

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Локальна історія прогнозів у SQLite (приватна база застосунку).
 * Файл бази: /data/data/com.example.helloworld/databases/cyberscope_history.db
 *
 * Зберігаються лише успішно отримані й коректні результати.
 * Помилки backend і технічні повідомлення про помилки сюди не потрапляють.
 */
class HistoryStore private constructor(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                created_at INTEGER NOT NULL,
                input_summary TEXT NOT NULL,
                result_summary TEXT NOT NULL,
                status TEXT NOT NULL,
                details_json TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Схема версії 1, міграції поки не потрібні.
    }

    /** Зберігає результат. Повертає id запису або null, якщо відповідь некоректна. */
    suspend fun add(result: PredictResponse): Long? = withContext(Dispatchers.IO) {
        if (!result.isSavable()) return@withContext null
        val values = ContentValues().apply {
            put("created_at", System.currentTimeMillis())
            put("input_summary", result.inputSummary())
            put("result_summary", result.resultSummary())
            put("status", STATUS_READY)
            put("details_json", result.toJsonString())
        }
        val id = writableDatabase.insert(TABLE, null, values)
        if (id == -1L) null else id
    }

    /** Список записів, новіші зверху. */
    suspend fun getAll(): List<HistoryItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<HistoryItem>()
        readableDatabase.query(
            TABLE,
            arrayOf("id", "created_at", "input_summary", "result_summary", "status"),
            null, null, null, null,
            "created_at DESC, id DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                items.add(
                    HistoryItem(
                        id = cursor.getLong(0),
                        createdAt = cursor.getLong(1),
                        inputSummary = cursor.getString(2),
                        resultSummary = cursor.getString(3),
                        status = cursor.getString(4)
                    )
                )
            }
        }
        items
    }

    /** Повна збережена відповідь для екрана деталей або null, якщо запису немає чи він пошкоджений. */
    suspend fun getResult(id: Long): PredictResponse? = withContext(Dispatchers.IO) {
        readableDatabase.query(
            TABLE,
            arrayOf("details_json"),
            "id = ?", arrayOf(id.toString()),
            null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) predictResponseFromJson(cursor.getString(0)) else null
        }
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE, "id = ?", arrayOf(id.toString()))
        Unit
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE, null, null)
        Unit
    }

    private fun PredictResponse.isSavable(): Boolean =
        team_a_name.isNotBlank() && team_b_name.isNotBlank() &&
            team_a_win_probability in 0.0..1.0 && team_b_win_probability in 0.0..1.0

    companion object {
        const val STATUS_READY = "готово"
        private const val DB_NAME = "cyberscope_history.db"
        private const val DB_VERSION = 1
        private const val TABLE = "history"

        @Volatile
        private var instance: HistoryStore? = null

        fun get(context: Context): HistoryStore =
            instance ?: synchronized(this) {
                instance ?: HistoryStore(context.applicationContext).also { instance = it }
            }
    }
}
