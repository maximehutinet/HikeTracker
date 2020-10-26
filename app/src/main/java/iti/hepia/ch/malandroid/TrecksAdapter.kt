package iti.hepia.ch.malandroid

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import iti.hepia.ch.malandroid.models.Treks

class TrecksAdapter (private val context: Context, private val dataSource: ArrayList<Treks>) : BaseAdapter() {
    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return dataSource[position].id!!.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = inflater.inflate(R.layout.list_item_treks, parent, false)
        val trekName = rowView.findViewById(R.id.treck_name_item) as TextView
        val treckDesc = rowView.findViewById(R.id.treck_desc_item) as TextView
        val treckTime = rowView.findViewById(R.id.treck_time_item) as TextView
        val treckDist = rowView.findViewById(R.id.treck_dist_item) as TextView
        val trekIcon = rowView.findViewById(R.id.treck_ic_item) as ImageView

        val trek = getItem(position) as Treks
        trekName.text = trek.name
        treckDesc.text = trek.description
        treckTime.text = timeToString( trek.time_total )
        treckDist.text = distToString( trek.trek_distance )

        return rowView
    }

    private fun timeToString(time:Double): String {
        var str = ""
        var nextTime = time
        if( nextTime > 24*3600 ) {
            str += ( nextTime / (3600*24) ).toInt().toString() + "j"
            nextTime %= (3600 * 24)
        }
        if( nextTime > 3600 ) {
            str += ( nextTime / 3600 ).toInt().toString() + "h"
            nextTime %= 3600
        }
        if( nextTime > 60 ) {
            str += ( nextTime / 60 ).toInt().toString() + "m"
            nextTime %= 60
        }
        str +=  nextTime.toInt().toString() + "s"
        return str
    }

    private fun distToString(dist:Double): String {
        var str = ""
        var nextDist = dist * 1000
        if( nextDist > 1000 ) {
            str += ( nextDist / (1000) ).toInt().toString() + "km"
            nextDist %= 1000
        }
        str +=  nextDist.toInt().toString() + "m"
        return str
    }


}