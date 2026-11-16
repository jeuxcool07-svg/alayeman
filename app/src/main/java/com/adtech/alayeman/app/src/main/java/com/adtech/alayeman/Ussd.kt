package com.adtech.alayeman

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager

class Ussd(private val activite: Activity) {

    companion object {
        // Le SEUL code que l'appli a le droit de composer
        const val CODE_CREDIT = "*101#"
    }

    private fun autorise(): Boolean =
        activite.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun verifierCredit(onReponse: (String) -> Unit, onErreur: (String) -> Unit) {
        if (Build.VERSION.SDK_INT < 26) {
            onErreur("Ce téléphone est trop ancien pour lire le crédit.")
            return
        }
        if (!autorise()) {
            activite.requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 4)
            onErreur("Autorise le téléphone, puis redemande mon crédit.")
            return
        }
        try {
            lancer(onReponse, onErreur)
        } catch (e: Exception) {
            onErreur("Impossible de lancer le code : ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun lancer(onReponse: (String) -> Unit, onErreur: (String) -> Unit) {
        if (Build.VERSION.SDK_INT < 26) return
        val gestionnaire = activite.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val rappel = object : TelephonyManager.UssdResponseCallback() {
            override fun onReceiveUssdResponse(
                telephonyManager: TelephonyManager,
                request: String,
                response: CharSequence
            ) {
                onReponse(response.toString())
            }

            override fun onReceiveUssdResponseFailed(
                telephonyManager: TelephonyManager,
                request: String,
                failureCode: Int
            ) {
                onErreur("Le réseau n'a pas répondu (code $failureCode). Réessaie dans un instant.")
            }
        }
        gestionnaire.sendUssdRequest(CODE_CREDIT, rappel, Handler(Looper.getMainLooper()))
    }
}
