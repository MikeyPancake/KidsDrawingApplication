package com.udemy.kidsdrawingapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView (context: Context, attrs: AttributeSet): View(context, attrs) {

    // TODO Step 1: Create application variables
    // Global variables
    private var mDrawPath : CustomPath? = null
    private var mCanvasBitmap : Bitmap? = null
    private var mDrawPaint : Paint? = null
    private var mCanvasPaint : Paint? = null
    private var mBrushSize : Float = 0.toFloat()
    private var color = Color.BLACK
    private var canvas : Canvas? =null

    // Initialize values from function
    init {
        setUpDrawing()
    }

    private fun setUpDrawing(){
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        mBrushSize = 20.toFloat()

    }

    // This function is called once screen is changed in application
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Creates bitmap using 258 pixels
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        // Sets canvas using the mCanvasBitmap
        canvas = Canvas(mCanvasBitmap!!)
    }

    // Function for when you draw on canvas
    // TODO change canvas to nullable is fails
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Canvas calls the draw bitmap, passes the bitmap you want to use, sets start position
        // and what to draw with
        canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)

        // Starts canvas as long as Draw path is empty
        if(!mDrawPath!!.isEmpty){
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }

    }

    // What happens when the canvas is touched
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            // When we press on the screen....
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize

                mDrawPath!!.reset()

                // Creates start point
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX, touchY)
                    }
                }
            }
            // When you move your finger on the screen.....
            MotionEvent.ACTION_MOVE -> {
                // Creates a line as you move your finger
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.lineTo(touchX, touchY)
                    }
                }
            }
            // When you lift your finger up it creates the custom path
            MotionEvent.ACTION_UP -> {
                mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false
        }
        // If nothing, reset
        invalidate()

        return true


    }

    internal inner class CustomPath (var color : Int, var brushThickness : Float) : Path() {

    }


}

/*
NOTES:
Bitmap represents pixel that we can draw something on the screen(or on coordinate system).
 */