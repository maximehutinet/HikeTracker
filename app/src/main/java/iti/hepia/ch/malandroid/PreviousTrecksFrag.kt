package iti.hepia.ch.malandroid

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import com.google.android.gms.maps.model.LatLng
import iti.hepia.ch.malandroid.models.DatabaseHandler
import iti.hepia.ch.malandroid.models.Treks
import java.util.ArrayList

class PreviousTrecksFrag : Fragment() {
    private lateinit var myActivity: MapsActivity
    private lateinit var treckList: ListView
    private lateinit var databaseHandler: DatabaseHandler
    private lateinit var backBtn: Button
    private lateinit var clearBtn: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_previous_trecks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if( savedInstanceState == null ) {
            backBtn = view.findViewById(R.id.back_trecks_previous)
            backBtn.setOnClickListener { myActivity.onBackToMap() }
            clearBtn = view.findViewById(R.id.clear_trecks_previous)
            clearBtn.setOnClickListener { myActivity.onClearGhost() }
            loadListItem()
        }
    }

    private fun loadListItem() {
        treckList = view!!.findViewById(R.id.list_trecks_previous)
        databaseHandler = DatabaseHandler(myActivity)
        val treks = databaseHandler.getAlTrekslData().toCollection(ArrayList())
        val adapter = TrecksAdapter(myActivity, treks)
        treckList.adapter = adapter
        treckList.setOnItemClickListener { parent, view, position, id ->
            val item = adapter.getItem(position) as Treks
            showSaveAlertBox( item )
        }
    }

    private fun showSaveAlertBox( trek: Treks ){
        val savingAlert = AlertDialog.Builder(myActivity)
        savingAlert.setTitle("Choose your action : ")
        savingAlert.setPositiveButton("Load"){ _ ,_-> loadTrek( trek ) }
        savingAlert.setNeutralButton("Delete"){ _ ,_-> deleteTrek( trek ) }
        val dialog: AlertDialog = savingAlert.create()
        dialog.show()
    }
    //loadTrek(poi: List<PointOfInterest>, coo: List<LatLng>){
    private fun loadTrek( trek: Treks ) {
        val coo =  stringToLatLngList( trek.trek_path )
        var allPoi = databaseHandler.getAllPointIntData()
        val poi = allPoi.filter { poi -> poi.trek_id == trek.id }
        myActivity.onPreviousTrekLoaded(poi, coo)
    }

    private fun stringToLatLngList(str: String): List<LatLng> {
        val regex = "^\\[lat\\/lng: \\(|\\), lat\\/lng: \\(|\\)\\]\$".toRegex()
        var str = str.split(regex);
        var  coo: List<LatLng>? = null
        coo = str.filter { s -> s!="" }
            .map{ s -> LatLng(
                s.split(",")[0].toDouble(),
                s.split(",")[1].toDouble() )}
        return coo
    }

    private fun deleteTrek( trek: Treks ) {
        databaseHandler.deleteTrekData( trek.id!! )
        loadListItem()
    }

    /**
     * Get context attachment
     */
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        myActivity = context as MapsActivity
    }

}

