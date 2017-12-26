package com.example.sebastian.brulinski.arduinobluetooth.RecyclerAdapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.sebastian.brulinski.arduinobluetooth.Interfaces.ViewBindInterface
import com.example.sebastian.brulinski.arduinobluetooth.Models.MyBluetoothDevice
import com.example.sebastian.brulinski.arduinobluetooth.R

class DevicesAdapter(private var devices: ArrayList<MyBluetoothDevice>, private val context: Context
                     , private val deviceClickListener: View.OnClickListener)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val TYPE_PAIRED = 1
        private val TYPE_FOUND = 2
        private val TYPE_LABEL = 3
    }

    fun setDevice(devices: ArrayList<MyBluetoothDevice>) {
        this.devices = devices
        notifyDataSetChanged()
    }


    override fun getItemViewType(position: Int): Int {
        return when (devices[position].type) {
            MyBluetoothDevice.Companion.DeviceType.PAIRED -> TYPE_PAIRED
            MyBluetoothDevice.Companion.DeviceType.FOUND -> TYPE_FOUND
            else -> TYPE_LABEL
        }
    }

    override fun getItemCount(): Int = devices.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        (holder as ViewBindInterface).bindViews(device = devices[position], position = position)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {

            TYPE_PAIRED -> DeviceViewHolder(LayoutInflater.from(parent?.context)
                    .inflate(R.layout.device_item, parent, false))

            TYPE_FOUND -> DeviceViewHolder(LayoutInflater.from(parent?.context)
                    .inflate(R.layout.device_item, parent, false))

            else -> LabelViewHolder(LayoutInflater.from(parent?.context)
                    .inflate(R.layout.device_type_label_item, parent, false))
        }
    }

    inner class DeviceViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView), ViewBindInterface {
        val name = itemView?.findViewById<TextView>(R.id.deviceNameTextView)
        val mac = itemView?.findViewById<TextView>(R.id.deviceMacTextView)
        val connectedImageView = itemView?.findViewById<ImageView>(R.id.connectedImageView)

        override fun bindViews(device: MyBluetoothDevice, position: Int) {
            name?.text = device.device?.name
            mac?.text = device.device?.address

            connectedImageView?.visibility = if (device.connected) View.VISIBLE
            else View.INVISIBLE
            itemView.setOnClickListener(deviceClickListener)
        }
    }

    inner class LabelViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView), ViewBindInterface {
        val lable = itemView?.findViewById<TextView>(R.id.deviceStateLabel)

        override fun bindViews(device: MyBluetoothDevice, position: Int) {
            lable?.text = device.label

            //itemView.setOnClickListener(deviceClickListener)
        }
    }
}