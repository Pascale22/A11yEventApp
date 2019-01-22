package com.example.a11yeventapp

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat
import android.support.v4.widget.ExploreByTouchHelper
import android.util.AttributeSet
import android.view.*
import java.util.logging.Logger
import kotlin.math.atan2

/** The application draw a pie chart included in a rectangle.
 * This app only add contentDescription for screenreader (talkback) with the use of accessibility events.
 *
 * L’application dessine un camembert de couleurs, le lecteur d’écran donne les valeurs associées aux secteurs.
 */

class MySurface @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    class MyExploreByTouchHelper(host: View, val rect: Rect, val angles: IntArray) : ExploreByTouchHelper(host) {
        val Log = Logger.getLogger(MyExploreByTouchHelper::class.java.name)
        val context : Context = host.context

        /** A x,y cursor position is translated into a sector number of the pie chart (circle chart).
         * The angle is calculated to find the sector.
         *
         * Une position x,y est traduite en un numéro de vue correspondant à un secteur. Un angle est
         * calculé pour définir le secteur correspondant.
         */
        override fun getVirtualViewAt(x: Float, y: Float): Int {
            Log.warning("x=" + x + " y=" + y)
            val xc = rect.centerX()
            val yc = rect.centerY()
            if (rect.contains(x.toInt(), y.toInt())) {
                var angle = 180 / Math.PI * atan2(y - yc, x - xc)
                if (angle < 0) {
                    angle += 360
                }
                Log.warning("angle is " + angle)
                for(i in angles.indices) {
                    if (angle < angles[i]) return i
                }
            }
            // when the clic is not in the pie chart.
            return INVALID_ID
        }

        /** Here is the list of sector views. Liste des vues secteur.
         */
        override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>?) {
            virtualViewIds?.addAll(angles.indices)
        }

        /** Actions are performed here. The clic is obtain and true is returned if it is in the rectangle
         *
         * Accès au clic en deux temps. On rend true si on gère, false si on ne gère pas.
         */

        override fun onPerformActionForVirtualView(virtualViewId: Int, action: Int, arguments: Bundle?): Boolean {
            when (action) {
                AccessibilityNodeInfoCompat.ACTION_CLICK -> {
                    return true
                }
                else -> return false
            }
        }

        /** For each view, accessibility information (content description) is added.
         * Pour chaque vue-secteur les infos d'accessibilité sont définies.
         */
        override fun onPopulateNodeForVirtualView(virtualViewId: Int, node: AccessibilityNodeInfoCompat) {
            Log.warning("onPopulate" + virtualViewId)
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
            node.addAction(AccessibilityNodeInfoCompat.ACTION_FOCUS)
            node.setBoundsInParent(rect)
            node.contentDescription = context.getString(R.string.value_area) + virtualViewId + context.getString(R.string.is_en) + (angles[virtualViewId] - (if (virtualViewId == 0) 0 else angles[virtualViewId - 1]))
        }
    }

    val Log = Logger.getLogger(MySurface::class.java.name)

    /* Modify color -- effet sur les couleurs */
    val lc = 165
    val mc = 210
    val hc = 255

    /* Array of colors to be used - Tableau de couleurs */
    val colors = arrayOf(
        Color.rgb(hc, lc, mc),
        Color.rgb( mc,hc, lc),
        Color.rgb( lc, mc, hc),
        Color.rgb(hc, mc, lc),
        Color.rgb(mc, lc, hc),
        Color.rgb(lc, hc, mc)
    )

    /* Values for sectors in the pie chart -- Valeurs pour les secteurs du camembert */
    var angles = intArrayOf(46, 60, 81, 120, 145, 190, 210, 250, 300, 310, 340, 360)

    /* Conversion -- canvas uses float but event prefers integer -- le système d'évènement préfère les entiers
     * et le canvas travaille en flottants.
     */
    var rect: Rect = Rect()
    var rectf: RectF = RectF()

    /* accessibility helper -- Le helper d’accessibilité */
    val mHelper = MyExploreByTouchHelper(this, rect, angles)

    init {
        /* callback init for surface holder -- initialisation des callbacks de surface holder */
        holder.addCallback(this)
        /* Helper must be a delegate for accessibility -- Le helper est délégué d’accessibilité.
         */
        ViewCompat.setAccessibilityDelegate(this, mHelper)
    }

    /* Helper catch events here -- Le helper se met en coupure des évènements.
     */
    public override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        return mHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)
    }


    /* Same for key events -- Idem pour les événements clavier. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return mHelper.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
    }

    /* Helper needs to know who has focus. Le helper a besoin de savoir qui a le focus */
    public override fun onFocusChanged(
        gainFocus: Boolean, direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        mHelper.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }

    /* surfaceHolder callbacks */
    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
    }

    override fun surfaceCreated(p0: SurfaceHolder?) {
        drawThePie()
    }

    /* Rectangle calculation, rectf into rect
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val r = kotlin.math.min(w, h) * .4f // radius of pie.
        val cx = w / 2f // center x of pie
        val cy = h / 2f // center y of pie
        rectf.left = cx - r
        rectf.top = cy - r
        rectf.right = cx + r
        rectf.bottom = cy + r
        rectf.round(rect)
    }

    /* Display the pie - Affiche le camembert */
    fun drawThePie() {
        if (holder.surface.isValid) {
            val canvas = holder.lockCanvas()
            val paint = Paint()
            var oldAng = 0f
            for(i in angles.indices) {
                paint.color = colors[i % colors.size]
                val ang = angles[i].toFloat()
                canvas.drawArc(rectf, oldAng, ang - oldAng, true, paint)
                oldAng = ang
            }
            holder.unlockCanvasAndPost(canvas)
        }
    }

}