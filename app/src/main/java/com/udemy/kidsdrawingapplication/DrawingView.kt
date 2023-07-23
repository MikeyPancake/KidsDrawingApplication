package com.udemy.kidsdrawingapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import java.nio.file.Paths

class DrawingView (context: Context, attrs: AttributeSet): View(context, attrs) {

    // TODO Step 1: Create application variables
    // An variable of CustomPath inner class to use it further.
    private var mDrawPath : CustomPath? = null
    // An instance of the Bitmap.
    private var mCanvasBitmap : Bitmap? = null
    // The Paint class holds the style and color information about how to draw geometries, text and bitmaps.
    private var mDrawPaint : Paint? = null
    // Instance of canvas paint view.
    private var mCanvasPaint : Paint? = null
    // A variable for stroke/brush size to draw on the canvas.
    private var mBrushSize : Float = 0.toFloat()
    // A variable to hold a color of the stroke.
    private var color = Color.BLACK

    /**
     * A variable for canvas which will be initialized later and used.
     *
     *The Canvas class holds the "draw" calls. To draw something, you need 4 basic components: A Bitmap to hold the pixels, a Canvas to host
     * the draw calls (writing into the bitmap), a drawing primitive (e.g. Rect,
     * Path, text, Bitmap), and a paint (to describe the colors and styles for the
     * drawing)
     */
    private var canvas : Canvas? =null
    private val mPaths = ArrayList<CustomPath>() // Array list for Paths
    private val mUndoPaths = ArrayList<CustomPath>() // Array list for undo'ing paths

    // Initialize values from function
    init {
        setUpDrawing()
    }

    /*
    Function that removes previous path and assigns it to the undo path array
     */
    fun onClickUndo(){
        // If mPaths has data greater than 0
        if (mPaths.size > 0){
            // Remove previous path and assign to mUndoPaths
            mUndoPaths.add(mPaths.removeAt(mPaths.size -1))
            invalidate()
        }
    }

    /*
    Function that redoes previous path if an undone path was created
     */
    fun onClickRedo(){
        if (mUndoPaths.size > 0){
            mPaths.add(mUndoPaths.removeAt(mUndoPaths.size - 1))
            invalidate()
        }
    }

    /**
     * This method initializes the attributes of the
     * ViewForDrawing class.
     */
    private fun setUpDrawing(){
        mDrawPaint = Paint()
        mDrawPath = CustomPath(color, mBrushSize)
        mDrawPaint!!.color = color
        // This is to draw a STROKE style
        mDrawPaint!!.style = Paint.Style.STROKE
        // This is for store join
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        // This is for stroke Cap
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        // Paint flag that enables dithering when blitting.
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
    }

    // This function is called once screen is changed in application
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Creates bitmap using 258 pixels
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        // Sets canvas using the mCanvasBitmap
        canvas = Canvas(mCanvasBitmap!!)
    }

    // TODO change canvas to nullable is fails
    /**
     * This method is called when a stroke is drawn on the canvas
     * as a part of the painting.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Canvas calls the draw bitmap, passes the bitmap you want to use, sets start position
        // and what to draw with
        canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)

        // adds path to the mPath variable
        for (path in mPaths){
            mDrawPaint!!.strokeWidth = path.brushThickness
            mDrawPaint!!.color = path.color
            canvas.drawPath(path, mDrawPaint!!)
        }

        // Starts canvas as long as Draw path is empty
        if(!mDrawPath!!.isEmpty){
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }

    }

    /**
     * This method acts as an event listener when a touch
     * event is detected on the device.
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            // When we press on the screen....
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.color = color
                mDrawPath!!.brushThickness = mBrushSize

                // Clear any lines and curves from the path, making it empty.
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
                // Adds path to Draw Path
                mPaths.add(mDrawPath!!)
                mDrawPath = CustomPath(color, mBrushSize)
            }
            else -> return false
        }
        // If nothing, reset
        invalidate()
        return true
    }

    fun setSizeForBrush(newSize: Float){
        // Sets brush size based on screen dimension
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            newSize, resources.displayMetrics
        )
        mDrawPaint?.strokeWidth = mBrushSize

    }

    fun setColor(newColor: String){
        // Parses colors
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color
    }

    internal inner class CustomPath (var color : Int, var brushThickness : Float) : Path() {

    }


}

/*
NOTES:
Bitmap represents pixel that we can draw something on the screen(or on coordinate system).
 */