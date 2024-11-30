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
        val exists = cursor?.count ?: 0 > 0
        cursor?.close()
        db.close()
        return exists
    }

    fun getUserDetails(username: String): Map<String, String>? {
        val db = this.readableDatabase
        val cursor: Cursor? = db.rawQuery(
            "SELECT * FROM users WHERE username = ?",
            arrayOf(username)
        )

        return if (cursor != null && cursor.moveToFirst()) {
            val userDetails = mutableMapOf<String, String>()
            userDetails["id"] = cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString()
            userDetails["username"] = cursor.getString(cursor.getColumnIndexOrThrow("username"))
            cursor.close()
            db.close()
            userDetails
        } else {
            cursor?.close()
            db.close()
            null
        }
    }

    fun addItem(item: Item): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("name", item.name)
            put("price", item.price)
            put("imageLink", item.imageLink)
            put("ratings", item.ratings)
            put("purchaseLink", item.purchaseLink)
        }

        return try {
            db.insert("items", null, values) > 0
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error adding item: ${e.message}")
            false
        } finally {
            db.close()
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
                    imageLink = cursor.getString(cursor.getColumnIndexOrThrow("imageLink")),
                    ratings = cursor.getFloat(cursor.getColumnIndexOrThrow("ratings")),
                    purchaseLink = cursor.getString(cursor.getColumnIndexOrThrow("purchaseLink"))
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
            """
            SELECT items.* FROM items 
            INNER JOIN bookmarks ON items.id = bookmarks.item_id 
            WHERE bookmarks.user_id = ?
            """,
            arrayOf(userId.toString())
        )

        val items = mutableListOf<Item>()
        if (cursor.moveToFirst()) {
            do {
                val item = Item(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                    imageLink = cursor.getString(cursor.getColumnIndexOrThrow("imageLink")),
                    ratings = cursor.getFloat(cursor.getColumnIndexOrThrow("ratings")),
                    purchaseLink = cursor.getString(cursor.getColumnIndexOrThrow("purchaseLink"))
                )
                items.add(item)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return items
    }
}
