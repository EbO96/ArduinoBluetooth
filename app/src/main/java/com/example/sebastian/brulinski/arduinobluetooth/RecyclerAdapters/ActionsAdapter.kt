package com.example.sebastian.brulinski.arduinobluetooth.RecyclerAdapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.sebastian.brulinski.arduinobluetooth.Models.ActionsItem
import com.example.sebastian.brulinski.arduinobluetooth.R

class ActionsAdapter(val actionsItems: ArrayList<ActionsItem>, val listItemClickListener: View.OnClickListener): RecyclerView.Adapter<ActionsAdapter.MyViewHolder>() {

    override fun onBindViewHolder(holder: MyViewHolder?, position: Int) {
        holder?.itemView?.setOnClickListener(listItemClickListener)

        val objectFromArray = actionsItems[position]

        holder?.actionTitle?.text = objectFromArray.itemTitle
        holder?.actionDescription?.text = objectFromArray.itemDescription
        holder?.actionIcon?.setImageDrawable(objectFromArray.itemImage)
    }

    override fun getItemCount(): Int = actionsItems.size

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): MyViewHolder =
         MyViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.action_item, parent, false))

    inner class MyViewHolder(itemView: View?): RecyclerView.ViewHolder(itemView) {
        val actionTitle = itemView!!.findViewById<TextView>(R.id.actionTitleTextView)
        val actionIcon = itemView!!.findViewById<ImageView>(R.id.actionIconImageView)
        val actionDescription = itemView!!.findViewById<TextView>(R.id.actionDescriptionTextView)
    }
}