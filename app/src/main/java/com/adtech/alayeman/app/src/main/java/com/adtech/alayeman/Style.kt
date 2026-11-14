package com.adtech.alayeman

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.MotionEvent
import android.view.View

object Style {

    val NOIR = Color.parseColor("#05000F")
    val VERT = Color.parseColor("#00FF66")
    val VIOLET = Color.parseColor("#8B3DFF")
    val OR = Color.parseColor("#FFC93C")
    val BLANC = Color.WHITE
    val GRIS = Color.parseColor("#9AA0B4")

    fun dp(ctx: Context, valeur: Int): Int =
        (valeur * ctx.resources.displayMetrics.density + 0.5f).toInt()

    private fun avecAlpha(couleur: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(couleur), Color.green(couleur), Color.blue(couleur))

    private fun forme(
        ctx: Context, fond: Int, contour: Int, rayon: Int, epaisseur: Int
    ): GradientDrawable = GradientDrawable().apply {
        setColor(fond)
        cornerRadius = dp(ctx, rayon).toFloat()
        setStroke(dp(ctx, epaisseur), contour)
    }

    private fun neon(ctx: Context, fond: Int, couleur: Int, rayon: Int): Drawable {
        val halo = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = dp(ctx, rayon).toFloat()
            setStroke(dp(ctx, 5), avecAlpha(couleur, 0x55))
        }
        val principal = forme(ctx, fond, couleur, rayon - 2, 2)
        val marge = dp(ctx, 2)
        return LayerDrawable(arrayOf(halo, principal)).apply {
            setLayerInset(1, marge, marge, marge, marge)
        }
    }

    fun bulleJoyeux(ctx: Context): Drawable = neon(ctx, Color.parseColor("#E60A1A12"), VERT, 18)

    fun bulleUtilisateur(ctx: Context): Drawable = neon(ctx, Color.parseColor("#E61A0B3B"), VIOLET, 18)

    fun champSaisie(ctx: Context): Drawable = neon(ctx, Color.parseColor("#E605000F"), VERT, 28)

    fun boutonCommande(ctx: Context): Drawable = neon(ctx, Color.parseColor("#CC12082B"), VIOLET, 16)

    fun boutonEnvoi(): Drawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(Color.parseColor("#00FF66"), Color.parseColor("#00A843"))
    ).apply { shape = GradientDrawable.OVAL }

    fun entete(ctx: Context): Drawable {
        val bas = dp(ctx, 24).toFloat()
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#F0120830"), Color.parseColor("#F005000F"))
        ).apply {
            cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, bas, bas, bas, bas)
            setStroke(dp(ctx, 2), VERT)
        }
    }

    fun rond(couleur: Int): Drawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(couleur)
    }

    fun avatarContour(ctx: Context): Drawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.TRANSPARENT)
        setStroke(dp(ctx, 3), VERT)
    }

    fun pulser(vue: View) {
        ObjectAnimator.ofFloat(vue, "alpha", 1f, 0.25f).apply {
            duration = 900
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    fun apparaitre(vue: View, ctx: Context) {
        vue.alpha = 0f
        vue.translationY = dp(ctx, 16).toFloat()
        vue.animate().alpha(1f).translationY(0f).setDuration(250).start()
    }

    fun effetToucher(vue: View) {
        vue.setOnTouchListener { v, evenement ->
            when (evenement.action) {
                MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            false
        }
    }
}
