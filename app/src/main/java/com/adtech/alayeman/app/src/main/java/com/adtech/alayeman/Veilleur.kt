package com.adtech.alayeman

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder

class Veilleur : Service() {

    private val CANAL_VEILLE = "veille"
    private val CANAL_ALERTE = "alerte"

    private var voix: Voix? = null
    private var palier = 100

    private val recepteur = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val niveau = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val echelle = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val statut = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            if (niveau < 0 || echelle <= 0) return
            val pourcent = niveau * 100 / echelle
            val charge = statut == BatteryManager.BATTERY_STATUS_CHARGING ||
                statut == BatteryManager.BATTERY_STATUS_FULL
            verifier(pourcent, charge)
        }
    }

    override fun onCreate() {
        super.onCreate()
        creerCanaux()
        startForeground(1, construire(CANAL_VEILLE, "Alayeman veille sur son corps..."))
        voix = Voix(this)
        registerReceiver(recepteur, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try { unregisterReceiver(recepteur) } catch (e: Exception) { }
        voix?.arreter()
        super.onDestroy()
    }

    private fun verifier(pourcent: Int, charge: Boolean) {
        if (charge) {
            palier = 100
            return
        }
        val nouveau = when {
            pourcent <= 5 -> 5
            pourcent <= 10 -> 10
            pourcent <= 20 -> 20
            else -> 100
        }
        if (nouveau < palier) {
            palier = nouveau
            alerter(nouveau, pourcent)
        }
    }

    private fun alerter(seuil: Int, pourcent: Int) {
        val humeur: Humeur
        val texte: String
        when (seuil) {
            5 -> {
                humeur = Humeur.PANIQUE
                texte = "ALAYE ! Plus que $pourcent % ! Je vais mourir, branche-moi !"
            }
            10 -> {
                humeur = Humeur.STRESSE
                texte = "J'ai peur... il me reste $pourcent %. Branche-moi vite."
            }
            else -> {
                humeur = Humeur.TRISTE
                texte = "Je me sens faible... $pourcent % de batterie."
            }
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2, construire(CANAL_ALERTE, texte))
        voix?.parler(texte, humeur)
    }

    private fun construire(canal: String, texte: String): Notification {
        val ouvrir = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val constructeur = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, canal)
        } else {
            Notification.Builder(this)
        }
        return constructeur
            .setContentTitle("Alayeman")
            .setContentText(texte)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(ouvrir)
            .build()
    }

    private fun creerCanaux() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CANAL_VEILLE, "Veille", NotificationManager.IMPORTANCE_LOW)
            )
            manager.createNotificationChannel(
                NotificationChannel(CANAL_ALERTE, "Alertes d'Alayeman", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }
}
