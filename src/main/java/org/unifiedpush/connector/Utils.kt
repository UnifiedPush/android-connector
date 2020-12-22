package org.unifiedpush.connector

import android.content.Context
import android.util.Log

fun logi(msg: String){
    Log.i("UP-lib",msg)
}

fun logw(msg: String){
    Log.w("UP-lib",msg)
}

fun registerDistributorIdInSharedPref(context: Context, id: Int){
    // Trust only the app we registered to
    // We don't trust other apps
    context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE).edit().putInt(PREF_MASTER_KEY_ID, id).commit()
}

fun getDistributorIdInSharedPref(context: Context): Int {
    return context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)?.getInt(
            PREF_MASTER_KEY_ID, 0
    )!!
}