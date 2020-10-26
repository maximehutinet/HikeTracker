package iti.hepia.ch.malandroid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.icu.text.CaseMap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import iti.hepia.ch.malandroid.models.PointOfInterest
import java.io.File


class InterestPointFrag : Fragment(), CameraSubscriber {
    private lateinit var myActivity: MapsActivity
    private lateinit var name: EditText
    private lateinit var description: EditText
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var photo: ImageView
    private lateinit var photoIcon: ImageView
    private lateinit var cancelBtn: Button
    private lateinit var saveBtn: Button
    private var photoFile: File? = null
    private var photoPath: String? = null

    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View {
        return inflater.inflate( R.layout.fragment_interest_point, container, false )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if( savedInstanceState == null ) {
            name = view.findViewById( R.id.name_pi )
            description = view.findViewById( R.id.desc_pi )
            photo = view.findViewById( R.id.photo_pi )
            photoIcon = view.findViewById( R.id.photo_ic_pi )
            photoIcon.setOnClickListener { takePhoto() }
            saveBtn = view.findViewById( R.id.save_btn_pi )
            saveBtn.setOnClickListener { saveData() }
            cancelBtn = view.findViewById( R.id.cancel_btn_pi )
            cancelBtn.setOnClickListener { cancelData() }
        }
    }

    /**
     * Camera button listener
     * Call main activity function for camera management
     */
    private fun takePhoto() {
        if( !checkCameraPermissions() ) {
            val message = "You can't use these feature, please check your permissions"
            Toast.makeText( myActivity, message, Toast.LENGTH_SHORT ).show()
        } else myActivity.dispatchTakePictureIntent()
    }

    /**
     * Delete photo from screen and from app external storage
     */
    private fun deletePhoto() {
        photo.setImageResource( 0 )
        photoIcon.setImageResource( R.drawable.ic_camera )
        photoIcon.setOnClickListener{ takePhoto() }
        photoFile?.delete()
        photoFile = null
    }

    /**
     * Cancel Btn listener
     * Remove all data and go back to map
     */
    private fun cancelData() {
        Log.e("TEST", "START CANCEL")
        if( photoFile != null ) deletePhoto()
        myActivity.onBackToMap()
    }

    /**
     * Save Btn Listener
     * Create InterestPoint object and return it to map
     */
    private fun saveData() {
        latitude = myActivity.getLatitude()
        longitude = myActivity.getLongitude()

        val p = PointOfInterest(name = name.text.toString(),
                                comment = description.text.toString(),
                                pic_uri = photoPath,
                                lat = latitude,
                                lon = longitude)
        myActivity.onInterestPointCreated(p)
    }

    /**
     * Receipts MainActivity camera results
     * Display user image on screen
     */
    override fun onImageReceived( imgPath: String ) {

        photoPath = imgPath
        photoFile = File( imgPath )
        val uri: Uri =  FileProvider.getUriForFile( myActivity, myActivity.APP_AUTHORITY, photoFile!! )
        photo.setImageURI( uri )
        photoIcon.setImageResource( R.drawable.ic_delete_black_18dp )
        photoIcon.setOnClickListener{ deletePhoto() }
    }

    /**
     * Check if the user accepted the Camera usage and external data access
     */
    private fun checkCameraPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission( myActivity, Manifest.permission.CAMERA ) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission( myActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission( myActivity, Manifest.permission.READ_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get context attachment
     */
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        myActivity = context as MapsActivity
        myActivity.cameraSubscribers.add(this)
    }
}
