package com.example.imeiscanner.utilits


import android.app.AlertDialog
import android.content.SharedPreferences
import android.widget.ImageView
import com.example.imeiscanner.MainActivity

lateinit var MAIN_ACTIVITY: MainActivity
lateinit var DIALOG_BUILDER: AlertDialog.Builder
lateinit var sharedPreferences: SharedPreferences
lateinit var editor: SharedPreferences.Editor
lateinit var check:ImageView
const val LANG = "Lang"
const val STATE = "state"
const val RC_SiGN_IN = 1
const val TAG = "TAG1212"
const val GOOGLE_PROVIDER_ID = "google"
const val PHONE_PROVIDER_ID = "phone"
const val DATA_FROM_MAIN_FRAGMENT = "data_from_main_fragment"
const val POSITION_ITEM = "position"
const val DATA_FROM_PHONE_INFO_FRAGMENT = "data_from_phone_info_fragment"
