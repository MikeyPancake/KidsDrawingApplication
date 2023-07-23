package com.udemy.kidsdrawingapplication

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception

/**
 * TODO - Save function does not work on android device but works on emulator
 */

class MainActivity : AppCompatActivity() {

    // variable for calling the drawing view
    private var drawingView : DrawingView? = null
    private var mImageButtonCurrentPaint : ImageButton? = null // A variable for current color is picked from color pallet.

    // Create launcher for getting images
    private val openGalleryLauncher : ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            // Checks result type
            if (result.resultCode == RESULT_OK && result.data != null){
                // Assigns image to background image
                val imageBackground : ImageView = findViewById(R.id.iv_background)
                // Assigns result image location to imageBackground
                imageBackground.setImageURI(result.data?.data)
        }

        }

    /** create an ActivityResultLauncher with MultiplePermissions since we are requesting
     * both read and write
     */
    private val requestPermission : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissions ->
                permissions.entries.forEach {
                    val permissionName = it.key
                    val isGranted = it.value

                        // if permission is granted show a toast and perform operation
                        if (isGranted) {
                                Toast.makeText(this@MainActivity, "Storage Access Approved",
                                Toast.LENGTH_LONG
                                ).show()

                            // Intent that allows you to go to your media once access is approved
                            val pickIntent = Intent(Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            // Sets image once access is granted and image is picked
                            openGalleryLauncher.launch(pickIntent)
                        // Perform operation
                        } else {
                            // Displaying another toast if permission is not granted and this time focus on Read external storage
                            if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE)
                                Toast.makeText(
                                    this@MainActivity,
                                    "Storage Access was Denied",
                                    Toast.LENGTH_LONG
                                ).show()
                        }
                }
        }

    // Progress bar variable
    var customProgressDialog : Dialog? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*
        Paint functionality
         */
        // Sets the drawing view from xml
        drawingView = findViewById(R.id.drawing_view)
        // Initially sets brush size
        drawingView?.setSizeForBrush(20.toFloat())

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)

        mImageButtonCurrentPaint = linearLayoutPaintColors[0] as ImageButton

        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed))

        // Action when undo icon is pressed
        val ibUndo : ImageButton = findViewById(R.id.ib_undo)
        //set onclick listener
        ibUndo.setOnClickListener {
            drawingView?.onClickUndo()
        }

        // Action when brush icon is pressed
        val ibBrush : ImageButton = findViewById(R.id.ib_brush)
        //set onclick listener
        ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        // Action when save icon is pressed
        val ibSave : ImageButton = findViewById(R.id.ib_save)
        //set onclick listener
        ibSave.setOnClickListener {
            //check if permission is allowed
            if (isReadStorageAllowed()){
                // Calls show progress dialog method
                showProgressDialog()
                // Launches Coroutine
                lifecycleScope.launch{
                    // Finds drawing view which holds background, color, and canvas
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                    //Save the image to the device
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }else{
                // UI Toast if access is denied
                Toast.makeText(
                    this@MainActivity,
                    "File saved unsuccessfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Adding an click event to image button for selecting the image from gallery.)
        val ibGallery : ImageButton = findViewById(R.id.ib_gallery)
        //set onclick listener
        ibGallery.setOnClickListener{
            requestStoragePermission()
        }

        // Action when redo icon is pressed
        val ibRedo : ImageButton = findViewById(R.id.ib_redo)
        //set onclick listener
        ibRedo.setOnClickListener {
            drawingView?.onClickRedo()
        }
    }

    /**
     * Method is used to launch the dialog to select different brush sizes.
     */
    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")

        val smallBtn : ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            // Closes the brush dialog once a brush is chosen
            brushDialog.dismiss()
        }
        val mediumBtn : ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener(View.OnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        })

        val largeBtn : ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener(View.OnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        })
        brushDialog.show()
    }

    /**
     * Method is used to select paint color if not default value (mImageButtonCurrentPaint).
     */
    fun paintClicked(colorClicked : View){
        if(colorClicked !== mImageButtonCurrentPaint){
            val imageButton = colorClicked as ImageButton
            // Reads color tage from activity_main.xml which reads from the colors.xml
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            // Assigns the color pressed to the image button
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )
            // The unselected color will then be set to the pallet normal
            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            // variable will hold current selected color
            mImageButtonCurrentPaint = colorClicked
        }
    }

    /**
     * Method is used to create a rationale dialog
     * Shows rationale dialog for displaying why the app needs permission
     * Only shown if the user has denied the permission request previously
     */
    private fun showRationaleDialog(title: String, message: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){dialog, _ -> dialog.dismiss()
            }
        builder.create().show()
    }

    /**
    *Methods is used to launch the storage access and chose an image
     * I had to change Read to Manage to work on my android device.
     * The read permission worked on the emmulator but not on the device
     */
    /**
     * Update. The saving error was with the isReadStorageAllowed function.
     * ChatGPT Explination:
     * For devices running Android 11 (API level 30) and above, the function checks if the app has
     * "All Files Access" permission using Environment.isExternalStorageManager().
     * This method returns true if the app has the necessary permission, and false otherwise.
     * For devices running Android 10 (API level 29) and below, the function uses the checkSelfPermission
     * method to check if the app has the READ_EXTERNAL_STORAGE permission. If the permission is granted,
     * it returns true; otherwise, it returns false.
     * By using this updated function, the app should properly check for the required permissions,
     * and you should be able to save images to the gallery without any issues.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun isReadStorageAllowed(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Method is used to request storage permission on the device
     */
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // Permission already granted
                val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGalleryLauncher.launch(pickIntent)
            } else {
                // Permission not granted, request it
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
                Toast.makeText(this, "Please grant permission to access external storage.", Toast.LENGTH_LONG).show()
            }
        } else {
            // Android 10 or lower, request the permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                showRationaleDialog("Drawing App", "Drawing App needs to Access your External Storage")
            } else {
                requestPermission.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    /**
     * Method is used for Getting and bitmap Exporting the image to your phone storage
     * Create bitmap from view and returns it
     */
    private fun getBitmapFromView(view : View) : Bitmap{
        //Define a bitmap with the same size as the view.
        // CreateBitmap : Returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        //Get the view's background
        val bgDrawable = view.background
        // If there is a background, it is drawn onto canvas
        if (bgDrawable != null){
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas)
        }else{
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE)
        }
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }

    /**
     * Method is used to create a bitmap (image) from view so it can be saved
     */
    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""

        /*
        This is run on the background thread (Coroutine)
         */
        /**
         * CHATGPT Update: Step by Step explination
         * 1.The function is marked as suspend because it performs some asynchronous operations (I/O operations) using coroutines.
         * The suspend modifier allows the function to be called from a coroutine or another suspend function.

        2.The function takes a Bitmap as input, which represents the image that you want to save.

        3.Inside the function, a variable result is declared and initialized to an empty string.
        This variable will be used to store the result of the image-saving operation.

        4.The function uses the withContext(Dispatchers.IO) block to switch to the I/O dispatcher.
        This is because the function performs I/O operations (e.g., writing the image to external storage),
        which should not be done on the main (UI) thread to avoid blocking it.

        5.Inside the withContext(Dispatchers.IO) block, it checks if the mBitmap parameter is not null.
        If it is null, there is no image to save, so the function will return an empty string as a result.

        6.If mBitmap is not null, it proceeds to save the image to the device's gallery.

        7.The function creates a ContentValues object, which is used to specify the details of the
        image that will be saved, such as the file name, MIME type, and the directory path where the
        image will be saved. In this case, it uses the Environment.DIRECTORY_PICTURES directory to save the image.

        8.It then uses the contentResolver to insert the image into the MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        which is the content URI for images in the external storage. This is how the image is actually
        saved to the device's gallery.

        9.If the image insertion is successful, it obtains the Uri of the newly saved image
        and opens an output stream to that Uri.

        10.The function compresses the Bitmap to PNG format with a compression quality of 90 and
        writes it to the output stream, effectively saving the image to the specified location.

        11.After the image is saved, the result variable is set to the Uri of the saved image as a string.

        12.The function then switches back to the main (UI) thread using runOnUiThread to show a
        toast indicating whether the image saving was successful or not.

        13.If the image was saved successfully, it calls the shareImage method to share the image
        using the Uri.

        14. Finally, the function returns the result, which contains the Uri of the saved image as a string.

        Note: The suspend function saveBitmapFile is designed to be called from a coroutine.
        If you are calling it from the main thread (e.g., inside a click listener),
        make sure you use lifecycleScope.launch or any other coroutine builder to launch a coroutine
        before calling this function. Otherwise, you might encounter concurrency issues or blocking the UI thread.
         */
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val contentResolver = contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "KidsDrawingApp_${System.currentTimeMillis()}.png")
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                    // Insert the image into the MediaStore.
                    val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                    imageUri?.let {
                        val outputStream: OutputStream? = contentResolver.openOutputStream(it)
                        outputStream?.let { stream ->
                            mBitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
                            stream.close()
                            result = it.toString()
                            //We switch from IO to UI thread to show a toast
                            runOnUiThread {
                                // Cancels progress dialog after a brief display
                                cancelProgressDialog()
                                if (result.isNotEmpty()) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "File saved successfully: $result",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Calls share image method
                                    shareImage(result)
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "File saved failed: $result",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    /**
     * Method is used to show the Custom Progress Dialog.
     */
    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        customProgressDialog?.show()
    }

    /**
     * Method is used to dismiss the progress dialog if it is visible to user.
     */
    private fun cancelProgressDialog(){
        if (customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    /**
    * Method is used to share image file via email or other applications
     */
    private fun shareImage(result: String){
        // TODO (Step 1 - Sharing the downloaded Image file)
        // START

        /*MediaScannerConnection provides a way for applications to pass a
        newly created or downloaded media file to the media scanner service.
        The media scanner service will read metadata from the file and add
        the file to the media content provider.
        The MediaScannerConnectionClient provides an interface for the
        media scanner service to return the Uri for a newly scanned file
        to the client of the MediaScannerConnection class.*/

        /*scanFile is used to scan the file when the connection is established with MediaScanner.*/
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            path, uri ->
            // This is used for sharing the image after it has being stored in the storage.
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(
                Intent.EXTRA_STREAM,
                uri
            )
            // A content: URI holding a stream of data associated with the Intent, used to supply the data being sent.
            shareIntent.type = "image/png" // The MIME type of the data being handled by this intent.
            startActivity(Intent.createChooser
                (shareIntent,
                "Share"
                )
            )// Activity Action: Display an activity chooser,
            // allowing the user to pick what they want to before proceeding.
            // This can be used as an alternative to the standard activity picker
            // that is displayed by the system when you try to start an activity with multiple possible matches,
            // with these differences in behavior:
        }
        // END
    }

}