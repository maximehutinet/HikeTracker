package iti.hepia.ch.malandroid

import androidx.appcompat.app.AppCompatActivity
import android.app.AlertDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.view.View
import android.widget.EditText
import android.widget.Toast
import android.util.Log
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.getActivity
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import androidx.core.app.ActivityCompat
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.LocationManager
import android.text.format.DateUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.content.ContextCompat
import iti.hepia.ch.malandroid.models.DatabaseHandler
import iti.hepia.ch.malandroid.models.PointOfInterest
import iti.hepia.ch.malandroid.models.Treks
import kotlinx.android.synthetic.main.activity_maps.*
import java.time.LocalDateTime
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_maps.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private var locationManager: LocationManager? = null
    private val REFRESH_TIME: Long = 1000L
    private val REFRESH_DISTANCE: Float = 10.0f
    private var latitude: Double? = -34.0
    private var longitude: Double? = 151.0
    private var isRecording : Boolean = false
    private var markerPosition : Marker? = null
    private var tracks : Polyline? = null
    private val DEFAULT_MAP_ZOOM : Float = 15.3f
    private val CLOSE_UP_ZOOM : Float = 20.0f
    private val FAR_ZOOM : Float = 10.0f
    private lateinit var btnStartStop: FloatingActionButton
    private lateinit var  btnAddIntPoint: FloatingActionButton
    private lateinit var  btnCurrLoc: FloatingActionButton
    private lateinit var  btnListPrevRoute: FloatingActionButton
    private var startTime: Long = 0L
    private var endTime: Long = 0L
    val PROX_ALERT_INTENT: String = "malandroid"
    val PROX_ALERT_RADIUS = 6000F
    private lateinit var mMap: GoogleMap
    val APP_AUTHORITY: String = "iti.hepia.ch.malandroid.fileprovider"
    val REQUEST_TAKE_PHOTO = 123
    private lateinit var currentPhotoPath: String
    var cameraSubscribers = mutableListOf<CameraSubscriber>()
    private var reqCode = 0
    private lateinit var currInterestPoint : MutableList<PointOfInterest>
    var currTrekID = 0
    private lateinit var DB: DatabaseHandler
    private lateinit var btnRmvStep: ImageView
    lateinit var loadedPOI: List<PointOfInterest>
    var polyGhost :Polyline? = null
    private lateinit var markers: MutableList<Marker>
    private lateinit var markersGhost: MutableList<Marker>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_maps)

        map_view.onCreate(savedInstanceState)
        map_view.onResume()
        map_view.getMapAsync(this)

        DB = DatabaseHandler(this)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?

        // Request permission to use location
        requestPermissions()
        activateLocationUpdate()

        setUpBtnListener()

        currInterestPoint = mutableListOf<PointOfInterest>()
        markers = mutableListOf<Marker>()
        markersGhost = mutableListOf<Marker>()

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener(this);
        markerPosition = addMarker(latitude!!, longitude!!, R.drawable.ic_gps, -1)

        centerMap(latitude, longitude, DEFAULT_MAP_ZOOM)
    }

    /*----------------------------------------------------------------------------------------------
     * LOCATION
     ---------------------------------------------------------------------------------------------*/

    /**
     * Check if the location is enabled
     */
    private fun isLocationEnabled(): Boolean {
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)!! || locationManager?.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )!!
    }

    /**
     * Check if the user accepted the location usage
     */
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    /**
     * Request permission to use location
     */
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            1
        )
    }

    /**
     * Listener for the location
     */
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(loc: Location?) {
            Log.i("Location", "Changed")
            Log.i("Latitude", loc?.latitude.toString())
            Log.i("Longitude", loc?.longitude.toString())
            updateCurrentLocation(loc?.latitude, loc?.longitude)
            updateCurrPositionMarker(latitude, longitude)
            centerMap(latitude, longitude, DEFAULT_MAP_ZOOM)
            if(isRecording) { updateTracks() }
        }

        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
        override fun onProviderEnabled(p0: String?) {}
        override fun onProviderDisabled(p0: String?) {}
    }

    /**
     * Used in InterstedPointFrag
     */
    fun getLatitude(): Double {
        return latitude!!
    }
    fun getLongitude(): Double {
        return longitude!!
    }

    /*----------------------------------------------------------------------------------------------
    * CAMERA
    ---------------------------------------------------------------------------------------------*/

    /**
     * Create an empty image file location
     * from https://developer.android.com/training/camera/photobasics
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File ?= getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply { currentPhotoPath = absolutePath }
    }

    /**
     * Open camera and save image in external storage
     * from https://developer.android.com/training/camera/photobasics
     */
    fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Toast.makeText(this, "Capture error", Toast.LENGTH_SHORT).show()
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(this, APP_AUTHORITY, it )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            }
        }
    }

    /**
     * Manage results of external activities
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // results from camera activity
        if( resultCode == Activity.RESULT_OK && requestCode == REQUEST_TAKE_PHOTO ) {
            Log.e("CAMERA", currentPhotoPath)
            cameraSubscribers.forEach { x -> x.onImageReceived(currentPhotoPath) }
        }
    }

    /*----------------------------------------------------------------------------------------------
    * INTEREST POINTS
    ---------------------------------------------------------------------------------------------*/
//
    fun createInterestPoint() {
        hideBtn()
        val transaction = supportFragmentManager.beginTransaction()
            .add( R.id.main_container, InterestPointFrag() )
            .addToBackStack("MAIN")
        transaction.commit()
    }

    fun viewInterestPoint( poi: PointOfInterest ) {
        hideBtn()
        val frag: Fragment = InterestPointViewFrag.newInstance(poi.id!!, poi.name, poi.comment, poi.pic_uri)
        val transaction = supportFragmentManager.beginTransaction()
            .add( R.id.main_container, frag )
            .addToBackStack("MAIN")
        transaction.commit()
    }

    fun onInterestPointCreated(p: PointOfInterest) {
        showBtn()
        p.trek_id = currTrekID
        p.id = currInterestPoint.lastIndex + 1
        currInterestPoint.add(p)
        addMarker(p.lat, p.lon, R.drawable.ic_warning, p.id!!)
        supportFragmentManager.popBackStack()
    }

    fun onBackToMap( ) {
        showBtn()
        supportFragmentManager.popBackStack()
    }
    /*----------------------------------------------------------------------------------------------
    * INTEREST POINTS
    ---------------------------------------------------------------------------------------------*/

    fun openPreviousTrecks() {
        hideBtn()
        var transaction = supportFragmentManager.beginTransaction()
            .add( R.id.main_container, PreviousTrecksFrag() )
            .addToBackStack("MAIN")
        transaction.commit()
    }

    /*----------------------------------------------------------------------------------------------
    * PREVIOUS TREK
    ---------------------------------------------------------------------------------------------*/

    fun onPreviousTrekLoaded( poi: List<PointOfInterest>, coo: List<LatLng> ) {
        showBtn()
        supportFragmentManager.popBackStack()
        polyGhost?.remove()
        loadTrek( poi, coo, Color.RED )
    }

    fun onClearGhost() {
        showBtn()
        supportFragmentManager.popBackStack()
        polyGhost?.remove()
        markersGhost.filter{ m -> m.tag != -1}.forEach { m -> m.remove() }
    }


    fun onDeleteMarker(id: Int) {
        if(DB.getPointIntData(id).id != 0 ){
            if(DB.deletePointIntData(id) != -1){
                deleteMarker(id)
            }
        }
        else{
            currInterestPoint = currInterestPoint.filter { x -> x.id != id }.toMutableList()
            deleteMarker(id)
        }

    }


    /*----------------------------------------------------------------------------------------------
     * PROXIMITY ALERT
     ---------------------------------------------------------------------------------------------*/

    /**
     * Create a proximity alert
     * @param lat : Latitude
     * @param long : Longitude
     * @param radius : Radius in meter
     * @param piID : Unique identifier for the interest point
     */
    @SuppressLint("MissingPermission")
    private fun addProximityAlert(lat: Double?, long: Double?, radius: Float?, poi: PointOfInterest) {
        Log.e("addProximityAlert", poi.id.toString())
        val intent = Intent(PROX_ALERT_INTENT)
        intent.putExtra("id", poi.id)
        val proximityIntent = PendingIntent.getBroadcast(this, reqCode, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        locationManager?.addProximityAlert(lat!!, long!!, radius!!, -1, proximityIntent)
        val filter = IntentFilter(PROX_ALERT_INTENT)
        registerReceiver(ProximityAlertReceiver(), filter)

        reqCode += 1
    }

    /**
     * Class used to receive alerts on proximity alert
     */
    inner class ProximityAlertReceiver : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            val entering = p1?.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false)
            val id = p1?.extras?.get("id")
            if(entering!!){
                Log.e("Proximity Alert", "Entering area close to interest point " + id.toString())
                this@MapsActivity.loadedPOI.filter { x -> x.id == id }.forEach { x -> this@MapsActivity.viewInterestPoint(x) }
            }
        }
    }

    /*----------------------------------------------------------------------------------------------
     * ALERT BOX METHODS
     ---------------------------------------------------------------------------------------------*/

    /**
     * Reset the start stop button to its original style
     */
    private fun resetStartStopBtn(){
        btnStartStop.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")))
        btnStartStop.setImageResource(R.drawable.ic_play)
    }

    /**
     * Change the style of the start stop button to running
     */
    private fun activateStartStopBtn(){
        btnStartStop.setBackgroundTintList(ColorStateList.valueOf(Color.RED))
        btnStartStop.setImageResource(R.drawable.ic_stop)
    }

    /**
     * Handler if the user decides to save the route he just did
     */
    private fun saveRoute(timeElapsed: Long, dist: Float){
        if( tracks != null ) saveRouteModal(timeElapsed, dist)
        else Toast.makeText(this, "No data, no save ;)", Toast.LENGTH_LONG).show()

        isRecording = false
        resetStartStopBtn()
        resetMesures()
    }

    /**
     * Handler if user doesn't want to save route
     */
    private fun dontSaveRoute(){
        isRecording = false
        resetStartStopBtn()
        resetMesures()
    }

    /**
     * Display an alert box
     */
    private fun showSaveAlertBox(timeElapsed: Long, dist: Float ){
        val savingAlert = AlertDialog.Builder(this)
        savingAlert.setTitle("Do you want to save this route ?")
        savingAlert.setMessage("You did $dist km in " + DateUtils.formatElapsedTime(timeElapsed))
        savingAlert.setPositiveButton("OK"){ _,_ -> saveRoute(timeElapsed, dist) }
        savingAlert.setNegativeButton("No"){ _,_ -> dontSaveRoute() }
        savingAlert.setNeutralButton("Cancel"){
                _, _ -> Log.e("savingAlert", "Cancel" )
        }
        val dialog: AlertDialog = savingAlert.create()
        dialog.show()
    }
    /**
     * Display a modal window to save your current trek.
     */
    private fun saveRouteModal(timeElapsed: Long, dist: Float){
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.save_route_modal, null)
        dialogBuilder.setView(dialogView)
        //val edtId = dialogView.findViewById(R.id.trek_id) as EditText
        val edtName = dialogView.findViewById(R.id.trek_name) as EditText
        val edtDescription = dialogView.findViewById(R.id.trek_desc) as EditText
        val trek_path = tracks?.points.toString()
        val trek_dist = dist.toDouble()
        val time_total = timeElapsed.toDouble()
        val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now()
        } else {
            val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
            sdf.format(Date())
        }

        dialogBuilder.setTitle("Save your Trek")
        dialogBuilder.setMessage("Enter data below")
        dialogBuilder.setPositiveButton("Save", DialogInterface.OnClickListener { _, _ ->
            val createName = edtName.text.toString()
            val createDescription = edtDescription.text.toString()

            // Creating the instance of DatabaseHandler class
            val databaseHandler = DatabaseHandler(this)
            if(createName.trim()!=""){

                // Calling the updateData method of DatabaseHandler class to update record
                val status = databaseHandler.putTrekData(Treks(null,createName, createDescription, trek_path, trek_dist, time_total, created.toString()))

                if(status > -1){
                    Toast.makeText(this,"Your trek has been saved.", Toast.LENGTH_LONG).show()
                    currTrekID = status.toInt()
                    Log.d("SaveTrek", "Current ID => $currTrekID")
                    currInterestPoint.forEach{ x ->
                        Log.d("SaveTrek", "Before trek id assignation -> Values: %s".format(x.id, x.comment, x.name, x.trek_id))
                        x.trek_id = currTrekID
                        x.id = null
                        Log.d("SaveTrek", "After trek id assignation -> Values: %s".format(x.id, x.comment, x.name, x.trek_id))
                        DB.putPointIntData(x)
                    }
                }

            } else {
                Toast.makeText(this,"Please fill all input fields", Toast.LENGTH_LONG).show()
                Log.e("SaveTrek", "Something went wrong, please fill all input fields.")
            }
        })
        dialogBuilder.setNegativeButton("Cancel", { dialog, which ->
            //pass
        })
        val b = dialogBuilder.create()
        b.show()
    }

    /*----------------------------------------------------------------------------------------------
     * LISTENER
     ---------------------------------------------------------------------------------------------*/

    private fun hideBtn() {
        btnStartStop.visibility = View.INVISIBLE
        btnAddIntPoint.visibility = View.INVISIBLE
        btnCurrLoc.visibility = View.INVISIBLE
        btnListPrevRoute.visibility = View.INVISIBLE
        btnRmvStep.visibility = View.INVISIBLE
    }
    private fun showBtn() {
        btnStartStop.visibility = View.VISIBLE
        btnAddIntPoint.visibility = View.VISIBLE
        btnCurrLoc.visibility = View.VISIBLE
        btnListPrevRoute.visibility = View.VISIBLE
        btnRmvStep.visibility = View.VISIBLE
    }

    /**
     * Set up all the button listener
     */
    private fun setUpBtnListener(){
        btnStartStop = findViewById(R.id.btn_start_stop)
        btnStartStop.setOnClickListener{
            if(isRecording){
                btnRmvStep.visibility = View.INVISIBLE
                // Construct and display a confirmation alert box
                endTime = SystemClock.elapsedRealtime()
                val timeElapsed = (endTime - startTime) / 1000L
                val dist = computeDistance(tracks)
                showSaveAlertBox(timeElapsed, dist)
            }
            else{
                isRecording = true
                startTime = SystemClock.elapsedRealtime()
                activateStartStopBtn()
                btnRmvStep.visibility = View.VISIBLE
            }
        }

        // Button add interest point handler
        btnAddIntPoint = findViewById(R.id.btn_add_int_point)
        btnAddIntPoint.setOnClickListener{
            if(isRecording){
                createInterestPoint()
            }
            else{
                Toast.makeText(this, "You have to record your tracks to do that !", Toast.LENGTH_LONG).show()
            }

        }

        // Button current location handler
        btnCurrLoc = findViewById(R.id.btn_current_loc)
        btnCurrLoc.setOnClickListener{
            showCurrentLocation()
        }

        // Button list previous route handler
        btnListPrevRoute = findViewById(R.id.btn_list_prev_route)
        btnListPrevRoute.setOnClickListener{
            openPreviousTrecks()
            /*Log.e("btnListPrevRoute", "Clicked !")

            loadedPOI = ArrayList<PointOfInterest>()

            // TEST LOAD PREVIOUS TREK
            var listPOI = ArrayList<PointOfInterest>()
            listPOI.add(PointOfInterest(lat=47.1, lon=7.1, name="Tree", comment="Watch out you have to turn left", id= 1, trek_id = 1))
            listPOI.add(PointOfInterest(lat=47.2, lon=7.2, name="Tree", comment="Watch out you have to turn left", id= 2, trek_id = 1))
            listPOI.add(PointOfInterest(lat=47.2, lon=7.3, name="Tree", comment="Watch out you have to turn left", id= 3, trek_id = 1))

            loadedPOI = listPOI

            loadedPOI.filter { x -> x.id == 1 }.forEach { x -> Log.e("Found", x.id.toString()) }

            var listCOO = ArrayList<LatLng>()
            listCOO.add(LatLng(47.1, 7.1))
            listCOO.add(LatLng(47.2, 7.2))
            listCOO.add(LatLng(47.2, 7.3))

            loadTrek(listPOI, listCOO, Color.RED)*/


        }

        btnRmvStep = findViewById(R.id.btnRmvStep)
        btnRmvStep.setOnClickListener{
            removeLastStep()
        }
        btnRmvStep.visibility = View.INVISIBLE

    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if(this::loadedPOI.isInitialized){
            loadedPOI.filter { x -> x.id == marker.tag }.forEach { x -> viewInterestPoint(x) }
        }
        currInterestPoint.filter { x -> x.id == marker.tag }.forEach { x -> viewInterestPoint(x) }
        return true
    }
    /*----------------------------------------------------------------------------------------------
     * LOCATION & MAP METHODS
     ---------------------------------------------------------------------------------------------*/

    /**
     * Add marker on the map
     * @param lat : Latitude
     * @param long : Longitude
     * @param icon : Icon
     * @return Marker object
     */
    private fun addMarker(lat: Double, long: Double, icon: Int, tag: Int?=null) : Marker {
        val markOpt : MarkerOptions = MarkerOptions().position(LatLng(lat, long)).icon(generateBitmapDescriptorFromRes(this, icon))
        var marker = mMap.addMarker(markOpt)
        if(tag != null){ marker.tag = tag }
        markers.add(marker)
        return marker
    }
    private fun addGhostMarker(lat: Double, long: Double, icon: Int, tag: Int?=null) : Marker {
        val markOpt : MarkerOptions = MarkerOptions().position(LatLng(lat, long)).icon(generateBitmapDescriptorFromRes(this, icon))
        var marker = mMap.addMarker(markOpt)
        if(tag != null){ marker.tag = tag }
        markersGhost.add(marker)
        return marker
    }

    /**
     * Delete a marker from the map
     * @param id : POI id
     */
    private fun deleteMarker(id: Int){
        markers.filter { m -> m.tag == id }.forEach { m -> m.remove() }
        markers = markers.filter { m -> m.tag != id }.toMutableList()
    }


    /**
     * Center the map on the current location and update the position marker
     */
    private fun showCurrentLocation(){
        updateCurrPositionMarker(latitude, longitude)
        centerMap(latitude, longitude, CLOSE_UP_ZOOM)
    }

    /**
     * Center the map
     * @param lat : Latitude
     * @param long : Longitude
     */
    private fun centerMap(lat : Double?, long : Double?, zoom: Float?){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat!!, long!!), zoom!!))
    }

    /**
     * Update marker current position
     * @param lat : Latitude
     * @param long : Longitude
     */
    private fun updateCurrPositionMarker(lat : Double?, long : Double?){
        markerPosition?.position = LatLng(lat!!, long!!)
    }

    /**
     * Update current location
     */
    fun updateCurrentLocation(lat : Double?, long : Double?){
        latitude = lat
        longitude = long
    }

    /**
     * Update the tracks on the map
     */
    private fun updateTracks(){
        if(tracks == null){
            tracks = mMap.addPolyline(PolylineOptions().add(LatLng(latitude!!, longitude!!)))
        }
        else{
            val listPoints : MutableList<LatLng>? = tracks?.points
            listPoints?.add(LatLng(latitude!!, longitude!!))
            tracks?.points = listPoints
        }
    }

    /**
     * Remove last step from the current polyline
     */
    fun removeLastStep(){
        val listPoints : MutableList<LatLng>? = tracks?.points
        tracks?.points = listPoints?.dropLast(1)
    }

    /**
     * Load a previous trek on the map
     * @param poi : List of POI
     * @param coo : List of coordinates
     * @param color : Color of the polyline
     */
    private fun loadTrek(poi: List<PointOfInterest>, coo: List<LatLng>, color: Int){
        loadedPOI = ArrayList<PointOfInterest>()
        loadedPOI = poi
        poi.forEach { p -> addGhostMarker(p.lat, p.lon, R.drawable.ic_warning, p.id!!) }
        poi.forEach { p -> addProximityAlert(p.lat, p.lon, PROX_ALERT_RADIUS, p) }
        polyGhost = mMap.addPolyline(PolylineOptions().color(color).addAll(coo))

        Log.e("Latitude", coo.first().latitude.toString())
        Log.e("Longitude", coo.first().longitude.toString())
        centerMap(coo.first().latitude, coo.first().longitude, FAR_ZOOM)
    }


    /**
     * Reset the time and distance mesures
     */
    private fun resetMesures(){
        startTime = 0L
        endTime = 0L
        tracks?.remove()
        polyGhost?.remove()
        markers.filter{ m -> m.tag != -1 }.forEach { m -> m.remove() }
        if(this::markersGhost.isInitialized) {
            markersGhost.filter { m -> m.tag != -1 }.forEach { m -> m.remove() }
        }
    }

    /**
     * Activate the location update
     */
    private fun activateLocationUpdate(){
        // If the user accepted the location
        if (checkPermissions() && isLocationEnabled()) {
            Log.e("Location", "Accepted")

            // Set up the location updates
            try {
                locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, REFRESH_TIME, REFRESH_DISTANCE, locationListener)
            } catch (e: SecurityException) {
                Log.e("Location", "Security exception")
            }
        } else {
            Toast.makeText(applicationContext, "Oh no, we can't access your location !", Toast.LENGTH_LONG).show()
        }
    }

    /*----------------------------------------------------------------------------------------------
     * UTILS
     ---------------------------------------------------------------------------------------------*/

    /**
     * Return the total distance of a polyline in km
     * @param p : Polyline
     */
    private fun computeDistance(p: Polyline?): Float{
        if(p == null) return 0F
        val listPoints : List<LatLng>? = p.points
        var tmp = FloatArray(1)
        var sum = 0f

        Log.e("Size", (listPoints!!.size-1).toString())

        for (i in 0 until listPoints.size-1){
            Location.distanceBetween(
                listPoints[i].latitude,
                listPoints[i].longitude,
                listPoints[i+1].latitude,
                listPoints[i+1].longitude,
                tmp)
            sum += tmp[0]
            Log.e(i.toString(), sum.toString())
        }
        return sum/1000
    }

    /**
     * Convert a vector file into bitmap
     * @param context : Application context
     * @param resId : Ressource id of the file
     */
    private fun generateBitmapDescriptorFromRes(context: Context, resId: Int): BitmapDescriptor {
        val drawable : Drawable? = ContextCompat.getDrawable(context, resId);
        drawable?.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight())
        val bitmap : Bitmap = Bitmap.createBitmap(drawable!!.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


}
