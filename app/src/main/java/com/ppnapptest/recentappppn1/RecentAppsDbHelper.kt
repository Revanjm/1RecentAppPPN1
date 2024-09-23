package com.ppnapptest.recentappppn1

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class RecentAppsDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_PATH, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_PATH = "/storage/emulated/0/.recentappppn1/.db/main.db"
        const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Таблица создается заранее, код создания удален
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Обновление базы данных (не используется)
    }

    fun getRecentApps(): List<String> {
        Log.d("RecentAppsDbHelper", "Database path: ${readableDatabase.path}")
        val db = readableDatabase
        val projection = arrayOf(
            "ra1", "ra2", "ra3", "ra4", "ra5", "ra6", "ra7", "ra8", "ra9", "ra10",
            "ra11", "ra12", "ra13", "ra14", "ra15", "ra16", "ra17", "ra18", "ra19", "ra20",
            "ra21", "ra22", "ra23", "ra24", "ra25", "ra26", "ra27", "ra28", "ra29", "ra30"
        )
        val cursor = db.query(
            "main", projection, null, null, null, null, null
        )

        val recentAppsList = mutableListOf<String>()
        with(cursor) {
            while (moveToNext()) {
                for (i in projection.indices) {
                    val packageName = getString(getColumnIndexOrThrow(projection[i]))
                    if (!packageName.isNullOrEmpty()) {
                        recentAppsList.add(packageName)
                        Log.d("RecentAppsDbHelper", "packageName: $packageName")
                    }
                }
            }
        }
        cursor.close()
        return recentAppsList
    }
}
