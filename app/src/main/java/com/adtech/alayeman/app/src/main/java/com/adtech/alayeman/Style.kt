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

    val NOIR = Color.parseColor("#0B0418")
    val VERT = Color.parseColor("#00FF66")
    val VIOLET = Color.parseColor("#A855F7")
    val OR = Color.parseColor("#FFC93C")
    val BLANC = Color.WHITE
    val GRIS = Color.parseColor("#B7A9D6")

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

    // Cadre lumineux : un halo large et doux, puis un trait net par-dessus
    private fun neon(ctx: Context, fond: Int, contour: Int, halo: Int, rayon: Int): Drawable {
        val aureole = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = dp(ctx, rayon).toFloat()
            setStroke(dp(ctx, 5), avecAlpha(halo, 0x66))
        }
        val principal = forme(ctx, fond, contour, rayon - 2, 2)
        val marge = dp(ctx, 2)
        val calques: Array<Drawable> = arrayOf(aureole, principal)
        return LayerDrawable(calques).apply {
            setLayerInset(1, marge, marge, marge, marge)
        }
    }

    // Message d'Alayeman : trait violet lumineux
    fun bulleJoyeux(ctx: Context): Drawable =
        neon(ctx, Color.parseColor("#E6120628"), VIOLET, VIOLET, 18)

    // Message de l'utilisateur : trait doré
    fun bulleUtilisateur(ctx: Context): Drawable =
        neon(ctx, Color.parseColor("#E62A0F57"), OR, OR, 18)

    // Champ de saisie : trait doré, halo violet
    fun champSaisie(ctx: Context): Drawable =
        neon(ctx, Color.parseColor("#E60B0418"), OR, VIOLET, 28)

    // Boutons de commande : fond sombre, trait doré, halo violet
    fun boutonCommande(ctx: Context): Drawable =
        neon(ctx, Color.parseColor("#D9160A33"), OR, VIOLET, 14)

    // Bouton d'envoi : rond doré
    fun boutonEnvoi(): Drawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(Color.parseColor("#FFE27A"), Color.parseColor("#C9951A"))
    ).apply { shape = GradientDrawable.OVAL }

    // En-tête : fond violet profond, bord doré
    fun entete(ctx: Context): Drawable {
        val bas = dp(ctx, 24).toFloat()
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#F02A0F57"), Color.parseColor("#F00B0418"))
        ).apply {
            cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, bas, bas, bas, bas)
            setStroke(dp(ctx, 2), OR)
        }
    }

    fun rond(couleur: Int): Drawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(couleur)
    }

    fun avatarContour(ctx: Context): Drawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.TRANSPARENT)
        setStroke(dp(ctx, 3), OR)
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
