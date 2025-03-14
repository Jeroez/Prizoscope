package com.example.prizoscope.utils

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.prizoscope.data.model.Item

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, Constants.DATABASE_NAME, null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE,
                password TEXT
            )
        """.trimIndent()
        db.execSQL(createUsersTable)

        val createBookmarksTable = """
            CREATE TABLE bookmarks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                item_id INTEGER,
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
        """.trimIndent()
        db.execSQL(createBookmarksTable)

        val createItemsTable = """
            CREATE TABLE items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                price REAL,
                discount_price INTEGER,
                duration_hours INTEGER,
                imageLink TEXT,
                ratings REAL,
                purchaseLink TEXT
            )
        """.trimIndent()
        db.execSQL(createItemsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS bookmarks")
        db.execSQL("DROP TABLE IF EXISTS items")
        onCreate(db)
    }

    fun addUser(username: String, password: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("username", username)
            put("password", password)
        }

        return try {
            db.insert("users", null, values) > 0
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error adding user: ${e.message}")
            false
        } finally {
            db.close()
        }
    }

    fun validateUser(username: String, password: String): Boolean {
        val db = this.readableDatabase
        val cursor: Cursor? = db.rawQuery(
            "SELECT * FROM users WHERE username = ? AND password = ?",
            arrayOf(username, password)
        )
        val exists = (cursor?.count ?: 0) > 0
        cursor?.close()
        db.close()
        return exists
    }

    fun getUserId(username: String): Int? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT id FROM users WHERE username = ?", arrayOf(username))
        return if (cursor.moveToFirst()) {
            val userId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            cursor.close()
            db.close()
            userId
        } else {
            cursor.close()
            db.close()
            null
        }
    }

    fun getAllItems(): List<Item> {
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM items", null)

        val items = mutableListOf<Item>()
        if (cursor.moveToFirst()) {
            do {
                val item = Item(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                    discount_price = cursor.getIntOrNull("discount_price"),
                    duration_hours = cursor.getIntOrNull("duration_hours"),
                    img_url = cursor.getString(cursor.getColumnIndexOrThrow("imageLink")),
                    rating = cursor.getFloatOrNull("ratings"),
                    url = cursor.getString(cursor.getColumnIndexOrThrow("purchaseLink"))
                )
                items.add(item)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return items
    }

    fun addBookmark(userId: Int, itemId: Int): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("user_id", userId)
            put("item_id", itemId)
        }

        return try {
            db.insert("bookmarks", null, values) > 0
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error adding bookmark: ${e.message}")
            false
        } finally {
            db.close()
        }
    }

    fun getBookmarkedItems(userId: Int): List<Item> {
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT items.* FROM items INNER JOIN bookmarks ON items.id = bookmarks.item_id WHERE bookmarks.user_id = ?",
            arrayOf(userId.toString())
        )

        val items = mutableListOf<Item>()
        if (cursor.moveToFirst()) {
            do {
                val item = Item(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                    discount_price = cursor.getIntOrNull("discount_price"),
                    duration_hours = cursor.getIntOrNull("duration_hours"),
                    img_url = cursor.getString(cursor.getColumnIndexOrThrow("imageLink")),
                    rating = cursor.getFloatOrNull("ratings"),
                    url = cursor.getString(cursor.getColumnIndexOrThrow("purchaseLink"))
                )
                items.add(item)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return items
    }

    fun getUserDetails(username: String): Map<String, String>? {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM users WHERE username = ?",
            arrayOf(username)
        )

        return if (cursor.moveToFirst()) {
            val userDetails = mutableMapOf<String, String>()
            userDetails["id"] = cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString()
            userDetails["username"] = cursor.getString(cursor.getColumnIndexOrThrow("username"))
            cursor.close()
            db.close()
            userDetails
        } else {
            cursor.close()
            db.close()
            null
        }
    }

    // SQLite helper functions for nullable numbers
    private fun Cursor.getIntOrNull(columnName: String): Int? {
        val index = getColumnIndexOrThrow(columnName)
        return if (!isNull(index)) getInt(index) else null
    }

    private fun Cursor.getFloatOrNull(columnName: String): Float? {
        val index = getColumnIndexOrThrow(columnName)
        return if (!isNull(index)) getFloat(index) else null
    }
}
