package com.adtech.alayeman

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatterieCorps(private val context: Context) {

    private fun etat(): Intent? =
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    fun niveau(): Int {
        val i = etat()
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level < 0 || scale <= 0) -1 else level * 100 / scale
    }

    fun enCharge(): Boolean {
        val status = etat()?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun ressentir(ame: Ame): String {
        val n = niveau()
        val humeur = when {
            enCharge() -> listOf(Humeur.JOYEUX, Humeur.JOYEUX, Humeur.ENERVE).random()
            n in 0..5 -> Humeur.PANIQUE
            n in 6..15 -> Humeur.STRESSE
            n in 16..30 -> listOf(Humeur.TRISTE, Humeur.BOUDEUR, Humeur.ENERVE).random()
            else -> listOf(Humeur.JOYEUX, Humeur.BOUDEUR, Humeur.ENERVE).random()
        }
        ame.changerHumeur(humeur)
        return ame.parler()
    }
}
