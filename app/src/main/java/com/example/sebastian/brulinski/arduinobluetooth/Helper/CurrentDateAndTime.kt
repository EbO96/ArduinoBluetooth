package com.example.sebastian.brulinski.arduinobluetooth.Helper

import java.text.SimpleDateFormat
import java.util.*

fun getCurrentDateAndTime(): String
        = SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss a").format(Calendar.getInstance().time)

fun getCurrenDate(): String
        = SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().time)

fun getCurrentTime(): String
        = SimpleDateFormat("HH:mm:ss a").format(Calendar.getInstance().time)