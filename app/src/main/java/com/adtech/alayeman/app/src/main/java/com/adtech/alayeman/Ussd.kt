package com.adtech.alayeman

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager

class Ussd(private val activite: Activity) {

    companion object {
        // Les SEULS codes que l'appli a le droit de composer
        const val CODE_CREDIT = "*101#"

        // Conversion de crédit en data : montant de crédit dépensé -> code Moov
        val CONVERSIONS: Map<Int, String> = mapOf(
            100 to "#555#3#1#1#2#1#",
            200 to "#555#3#1#1#3#1#",
            250 to "#555#3#1#1#4#1#",
            500 to "#555#3#1#2#1#",
            1000 to "#555#3#1#2#1#2#1#"
        )

        private const val DELAI_ENTRE_ACHATS = 2 * 60 * 1000L
    }

    private val prefs = activite.getSharedPreferences("alayeman_ussd", Context.MODE_PRIVATE)

    fun montantsDisponibles(): List<Int> = CONVERSIONS.keys.sorted()

    private fun autorise(): Boolean =
        activite.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    // Consulter le crédit : simple lecture, aucune confirmation nécessaire
    fun verifierCredit(onReponse: (String) -> Unit, onErreur: (String) -> Unit) {
        envoyer(CODE_CREDIT, onReponse, onErreur)
    }

    // Transformer du crédit en data : confirmation obligatoire à chaque fois
    fun convertirEnData(montant: Int, onReponse: (String) -> Unit, onErreur: (String) -> Unit) {
        val code = CONVERSIONS[montant]
        if (code == null) {
            onErreur(
                "Je n'ai pas de code pour $montant F. Montants possibles : " +
                    montantsDisponibles().joinToString(", ") + " F."
            )
            return
        }
        val dernier = prefs.getLong("dernier_achat", 0L)
        if (System.currentTimeMillis() - dernier < DELAI_ENTRE_ACHATS) {
            onErreur("Tu viens de lancer un achat de data. Attends 2 minutes avant d'en refaire un.")
            return
        }
        AlertDialog.Builder(activite)
            .setTitle("Acheter de la data (${montant} F)")
            .setMessage(
                "Tu vas dépenser $montant F de ton crédit pour acheter de la data. " +
                    "Ce crédit sera retiré de ton compte. Confirmer ?"
            )
            .setPositiveButton("Oui, acheter") { _, _ ->
                prefs.edit().putLong("dernier_achat", System.currentTimeMillis()).apply()
                envoyer(code, onReponse, onErreur)
            }
            .setNegativeButton("Non") { _, _ ->
                onErreur("D'accord, je n'ai rien acheté.")
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun envoyer(code: String, onReponse: (String) -> Unit, onErreur: (String) -> Unit) {
        if (Build.VERSION.SDK_INT < 26) {
            onErreur("Ce téléphone est trop ancien pour lancer ce code.")
            return
        }
        if (!autorise()) {
            activite.requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 4)
            onErreur("Autorise le téléphone, puis recommence.")
            return
        }
        try {
            lancer(code, onReponse, onErreur)
        } catch (e: Exception) {
            onErreur("Impossible de lancer le code : ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun lancer(code: String, onReponse: (String) -> Unit, onErreur: (String) -> Unit) {
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
        gestionnaire.sendUssdRequest(code, rappel, Handler(Looper.getMainLooper()))
    }
}
