package iti.hepia.ch.malandroid.models

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper


class DatabaseHandler(context: Context?): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){
    companion object {
        val DATABASE_VERSION = 2                        // This number must be changed each time you change the database
        val DATABASE_NAME = "malandroid_treks.db"

        // Table Treks
        val TABLE_TREKS = "treks"
        val T_ID = "id"
        val T_NAME = "name"
        val T_DESC = "description"
        val T_TREK_PATH = "trek_path"
        val T_TREK_DISTANCE = "trek_distance"
        val T_TIME = "time_total"
        val T_SAVED = "saved_at"

        // Table POI
        val TABLE_POI = "points_of_interest"
        val P_ID = "id"
        val P_NAME = "name"
        val P_COMMENT = "comments"
        val P_PIC_URI = "pic_uri"
        val P_LAT = "latitude"
        val P_LON = "longitude"
        val P_TREK_ID = "trek_id"

        /**
         * SQLITE Documentation (DataTypes)
         * https://www.w3resource.com/sqlite/sqlite-data-types.php
         */
        // Create database Treks
        private val CREATE_TREKS_TABLE = ("CREATE TABLE $TABLE_TREKS ("+
                "$T_ID INTEGER PRIMARY KEY AUTOINCREMENT, "+
                "$T_NAME TEXT NOT NULL, "+
                "$T_DESC TEXT NOT NULL, "+
                "$T_TREK_PATH TEXT NOT NULL, " +
                "$T_TREK_DISTANCE FLOAT NOT NULL," +
                "$T_TIME FLOAT NOT NULL,"+
                "$T_SAVED TEXT NOT NULL)")

        // Create database point of interest
        private val CREATE_POI_TABLE = ("CREATE TABLE $TABLE_POI ("+
                "$P_ID INTEGER PRIMARY KEY AUTOINCREMENT, "+
                "$P_NAME TEXT NOT NULL, "+
                "$P_COMMENT TEXT,"+
                "$P_PIC_URI TEXT,"+
                "$P_LAT FLOAT NOT NULL, "+
                "$P_LON FLOAT NOT NULL,"+
                "$P_TREK_ID INTEGER NOT NULL, "+
                "FOREIGN KEY($P_TREK_ID) REFERENCES $TABLE_TREKS($T_ID) ON DELETE CASCADE)")
    }
    /**
     * OnCreate we setup the database.
     */
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_TREKS_TABLE)
        db?.execSQL(CREATE_POI_TABLE)
    }

    /**
     * On db upgrade we destroy the old db and make sure a new clean db is setup
     */
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $TABLE_TREKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POI")
        onCreate(db)
    }

    /**
     * On downgrade we just apply onUpgrade with the old version
     */
    override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    /* ---------------------------------------------------------------------------------------------
        Methods
    -------------------------------------------------------------------------------------------- */
    /**
     * Put data method (Treks)
     */
    fun putTrekData(treks_info: Treks): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(T_ID, treks_info.id)
        contentValues.put(T_NAME, treks_info.name)
        contentValues.put(T_DESC, treks_info.description)
        contentValues.put(T_TREK_PATH, treks_info.trek_path)
        contentValues.put(T_TREK_DISTANCE, treks_info.trek_distance)
        contentValues.put(T_TIME, treks_info.time_total)
        contentValues.put(T_SAVED, treks_info.saved)

        // Insert data
        val success = db.insert(TABLE_TREKS, null, contentValues)
        db.close()
        return success
    }
    /**
     * Custom SQL exec
     */
    fun getCurrentTrekId(cmd: String): Treks {
        val selectQuery = "SELECT * FROM $TABLE_TREKS WHERE $cmd"
        val db = this.readableDatabase
        var trek: Treks = Treks(0,"","", "",0.0,0.0, "")
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(selectQuery, null)
        } catch (e: SQLiteException){
            db.execSQL(selectQuery)
            return trek
        }
        var id: Int
        var name: String
        var description: String
        var trek_path: String
        var trek_distance: Double
        var time_total: Double
        var saved: String
        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(cursor.getColumnIndex(T_ID))
                name = cursor.getString(cursor.getColumnIndex(T_NAME))
                description = cursor.getString(cursor.getColumnIndex(T_DESC))
                trek_path = cursor.getString(cursor.getColumnIndex(T_TREK_PATH))
                trek_distance = cursor.getDouble(cursor.getColumnIndex(T_TREK_DISTANCE))
                time_total = cursor.getDouble(cursor.getColumnIndex(T_TIME))
                saved = cursor.getString(cursor.getColumnIndex(T_SAVED))
                trek = Treks(id = id, name = name, description = description, trek_path = trek_path, trek_distance = trek_distance, time_total = time_total, saved = saved)
            } while (cursor.moveToNext())
        }
        return trek
    }

    /**
     * Update data (Treks)
     */
    fun updateTreksData(treks_info: Treks):Int{
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(T_ID, treks_info.id)                          // Treks Class Id
        contentValues.put(T_NAME, treks_info.name)                      // Treks Class name
        contentValues.put(T_DESC,treks_info.description )               // Treks Class description
        contentValues.put(T_TREK_PATH,treks_info.trek_path )            // Treks Class trek_path
        contentValues.put(T_TREK_DISTANCE,treks_info.trek_distance )    // Treks Class trek_path
        contentValues.put(T_TIME,treks_info.time_total )                // Treks Class time total
        contentValues.put(T_SAVED,treks_info.saved )                    // Treks Class saved

        // Updating Row
        val success = db.update(TABLE_TREKS, contentValues,"id="+treks_info.id,null) //2nd argument is String containing nullColumnHack

        // Closing database connection
        db.close()

        return success
    }

    /**
     * Get data (by id)
     */
    fun getTrekData(id: Int): Treks {
        val selectQuery = "SELECT * FROM $TABLE_TREKS WHERE id='$id'"
        val db = this.readableDatabase
        var trek: Treks = Treks(0,"","", "",0.0,0.0, "")
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(selectQuery, null)
        } catch (e: SQLiteException){
            db.execSQL(selectQuery)
            return trek
        }
        var id: Int
        var name: String
        var description: String
        var trek_path: String
        var trek_distance: Double
        var time_total: Double
        var saved: String
        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(cursor.getColumnIndex(T_ID))
                name = cursor.getString(cursor.getColumnIndex(T_NAME))
                description = cursor.getString(cursor.getColumnIndex(T_DESC))
                trek_path = cursor.getString(cursor.getColumnIndex(T_TREK_PATH))
                trek_distance = cursor.getDouble(cursor.getColumnIndex(T_TREK_DISTANCE))
                time_total = cursor.getDouble(cursor.getColumnIndex(T_TIME))
                saved = cursor.getString(cursor.getColumnIndex(T_SAVED))
                trek = Treks(id = id, name = name, description = description, trek_path = trek_path, trek_distance = trek_distance, time_total = time_total, saved = saved)
            } while (cursor.moveToNext())
        }
        return trek
    }
    /**
     * Get all Data (Array) (Treks)
     */
    fun getAlTrekslData(): List<Treks> {
        val treks_info: ArrayList<Treks> = ArrayList<Treks>()
        val selectQuery = "SELECT  * FROM $TABLE_TREKS"
        val db = this.readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(selectQuery, null)
        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return ArrayList()
        }
        var id: Int
        var name: String
        var description: String
        var trek_path: String
        var trek_distance: Double
        var time_total: Double
        var saved: String
        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(cursor.getColumnIndex(T_ID))
                name = cursor.getString(cursor.getColumnIndex(T_NAME))
                description = cursor.getString(cursor.getColumnIndex(T_DESC))
                trek_path = cursor.getString(cursor.getColumnIndex(T_TREK_PATH))
                trek_distance = cursor.getDouble(cursor.getColumnIndex(T_TREK_DISTANCE))
                time_total = cursor.getDouble(cursor.getColumnIndex(T_TIME))
                saved = cursor.getString(cursor.getColumnIndex(T_SAVED))
                val info = Treks(
                    id = id, name = name, description = description, trek_path = trek_path,
                    trek_distance = trek_distance, time_total = time_total, saved = saved)
                treks_info.add(info)
            } while (cursor.moveToNext())
        }
        return treks_info
    }

    /**
     * Delete Data (Treks)
     */
    fun deleteTrekData(trek_id: Int):Int{
        val db = this.writableDatabase

        // Deleting Row
        val success = db.delete(TABLE_TREKS, "id=$trek_id",null) //2nd argument is String containing nullColumnHack

        // Closing database connection
        db.close()

        return success
    }

    /**
     * Put data method (Point of Interest)
     */
    fun putPointIntData(poi_info: PointOfInterest): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(P_ID, poi_info.id)
        contentValues.put(P_NAME, poi_info.name)
        contentValues.put(P_COMMENT, poi_info.comment)
        contentValues.put(P_PIC_URI, poi_info.pic_uri)
        contentValues.put(P_LAT, poi_info.lat)
        contentValues.put(P_LON, poi_info.lon)
        contentValues.put(P_TREK_ID, poi_info.trek_id)

        // Insert data
        val success = db.insert(TABLE_POI, null, contentValues)
        db.close()
        return success
    }

    /**
     * Update data (Point of Interest)
     */
    fun updatePointIntsData(poi_info: PointOfInterest):Int{
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(P_ID, poi_info.id)                                // PointOfInterest Class Id
        contentValues.put(P_NAME, poi_info.name)                            // PointOfInterest Class name
        contentValues.put(P_COMMENT,poi_info.comment )                      // PointOfInterest Class description
        contentValues.put(P_PIC_URI,poi_info.pic_uri )                      // PointOfInterest Class pic_uri
        contentValues.put(P_LAT,poi_info.lat )                              // PointOfInterest Class rando_path
        contentValues.put(P_LON,poi_info.lon )                              // PointOfInterest Class time total
        contentValues.put(P_TREK_ID,poi_info.trek_id )                      // PointOfInterest Class saved

        // Updating Row
        val success = db.update(TABLE_POI, contentValues,"id="+poi_info.id,null) //2nd argument is String containing nullColumnHack

        // Closing database connection
        db.close()

        return success
    }

    /**
     * Get data (by id) (Point of Interest)
     */
    fun getPointIntData(p_id: Int): PointOfInterest {
        val selectQuery = "SELECT * FROM $TABLE_POI WHERE $P_ID='$p_id'"
        val db = this.readableDatabase
        var poi = PointOfInterest(0,"","", "",42.0,42.0,0)
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(selectQuery, null)
        } catch (e: SQLiteException){
            db.execSQL(selectQuery)
            return poi
        }
        var id: Int
        var name: String
        var comment: String
        var pic_uri: String
        var lat: Double
        var lon: Double
        var trek_id: Int
        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(cursor.getColumnIndex(P_ID))
                name = cursor.getString(cursor.getColumnIndex(P_NAME))
                comment = cursor.getString(cursor.getColumnIndex(P_COMMENT))
                pic_uri = cursor.getString(cursor.getColumnIndex(P_PIC_URI))
                lat = cursor.getDouble(cursor.getColumnIndex(P_LAT))
                lon = cursor.getDouble(cursor.getColumnIndex(P_LON))
                trek_id = cursor.getInt(cursor.getColumnIndex(P_TREK_ID))
                poi = PointOfInterest(id = id, name = name, comment = comment, pic_uri = pic_uri, lat = lat, lon = lon, trek_id = trek_id)
            } while (cursor.moveToNext())
        }
        return poi
    }

    /**
     * Get all Data (Array) (Point of Interest)
     */
    fun getAllPointIntData(): List<PointOfInterest> {
        val poi: ArrayList<PointOfInterest> = ArrayList<PointOfInterest>()
        val selectQuery = "SELECT  * FROM $TABLE_POI"
        val db = this.readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(selectQuery, null)
        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return ArrayList()
        }
        var id: Int
        var name: String
        var comment: String
        var pic_uri: String?
        var lat: Double
        var lon: Double
        var trek_id: Int
        //Log.e("DB", "Adding to list")
        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(cursor.getColumnIndex(P_ID))
                name = cursor.getString(cursor.getColumnIndex(P_NAME))
                comment = cursor.getString(cursor.getColumnIndex(P_COMMENT))
                pic_uri = cursor.getString(cursor.getColumnIndex(P_PIC_URI))
                lat = cursor.getDouble(cursor.getColumnIndex(P_LAT))
                lon = cursor.getDouble(cursor.getColumnIndex(P_LON))
                trek_id = cursor.getInt(cursor.getColumnIndex(P_TREK_ID))
                val info = PointOfInterest(id = id, name = name, comment = comment, pic_uri = pic_uri, lat = lat, lon = lon, trek_id = trek_id)
                poi.add(info)
            } while (cursor.moveToNext())
        }
        return poi
    }

    /**
     * Delete Data (Point of Interest)
     */
    fun deletePointIntData(poi_id: Int):Int{
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(P_ID, poi_id)

        // Deleting Row
        val success = db.delete(TABLE_POI, "id="+poi_id, null)

        // Closing database connection
        db.close()

        return success
    }
}