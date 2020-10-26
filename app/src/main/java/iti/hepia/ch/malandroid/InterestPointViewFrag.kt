package iti.hepia.ch.malandroid

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import java.io.File
import iti.hepia.ch.malandroid.models.DatabaseHandler

private const val ARG_ID = "id"
private const val ARG_NAME = "nameArg"
private const val ARG_CONTENT = "contentArg"
private const val ARG_PATH = "pathArg"


class InterestPointViewFrag : Fragment() {
    private var idArg: Int? = null
    private var nameArg: String? = null
    private var contentArg: String? = null
    private var pathArg: String? = null
    private lateinit var myActivity: MapsActivity
    private lateinit var name: TextView
    private lateinit var description: TextView
    private lateinit var photo: ImageView
    private lateinit var deleteBtn: Button
    private lateinit var backBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            idArg = it.getInt(ARG_ID)
            nameArg = it.getString(ARG_NAME)
            contentArg = it.getString(ARG_CONTENT)
            pathArg = it.getString(ARG_PATH)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_interest_point_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if( savedInstanceState == null ) {
            name = view.findViewById( R.id.name_view_pi )
            name.text = nameArg
            description = view.findViewById( R.id.desc_view_pi )
            description.text = contentArg
            photo = view.findViewById( R.id.photo_view_pi )
            if( pathArg != null ) {
                Log.e("File path", pathArg!!)
                val photoFile = File(pathArg!!)
                val uri: Uri = FileProvider.getUriForFile(myActivity, myActivity.APP_AUTHORITY, photoFile)
                photo.setImageURI( uri )
            }
            backBtn = view.findViewById( R.id.back_btn_view_pi )
            backBtn.setOnClickListener { back() }
            deleteBtn = view.findViewById( R.id.delete_btn_view_pi )
            deleteBtn.setOnClickListener { deletePI() }
        }
    }

    private fun back() {
        myActivity.onBackToMap()
    }

    private fun deletePI() {
        myActivity.onDeleteMarker(idArg!!)
        back()
    }

    /**
     * Get context attachment
     */
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        myActivity = context as MapsActivity
    }


    companion object {
        fun newInstance(idArg: Int, nameArg: String, contentArg: String, pathArg: String?) =
            InterestPointViewFrag().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID, idArg)
                    putString(ARG_NAME, nameArg)
                    putString(ARG_CONTENT, contentArg)
                    putString(ARG_PATH, pathArg)
                }
            }
    }


}
