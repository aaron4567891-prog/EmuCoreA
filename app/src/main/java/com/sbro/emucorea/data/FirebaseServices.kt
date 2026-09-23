package com.sbro.emucorea.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sbro.emucorea.BuildConfig

/** No Firebase SDK access at all in an unconfigured personal build. */
internal object FirebaseServices {
    const val UNAVAILABLE = "Google and cloud services are unavailable in this build."

    val auth: FirebaseAuth?
        get() = if (BuildConfig.GOOGLE_SERVICES_ENABLED) FirebaseAuth.getInstance() else null

    fun requireAuth(): FirebaseAuth = checkNotNull(auth) { UNAVAILABLE }

    fun firestore(): FirebaseFirestore {
        check(BuildConfig.GOOGLE_SERVICES_ENABLED) { UNAVAILABLE }
        return FirebaseFirestore.getInstance()
    }
}
