package com.example.savepicturelocal

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.savepicturelocal.databinding.ActivityMainBinding
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    var photoFile: File? = null
    val CAPTURE_IMAGE_REQUEST = 1
    var mCurrentPhotoPath: String? = null
    var imageView: ImageView? = null

    private var latestTmpUri: Uri? = null

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            // There are no request codes
//            val data: Intent? = result.data
            latestTmpUri?.let { uri ->
                imageView?.setImageURI(uri)

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        imageView = binding.imageView

        binding.fab.setOnClickListener { view ->
//            takeImage()
//            takePictureNew()
//            takePictureOld()
            saveBitmap(imageView?.drawToBitmap())
        }
    }

    private fun saveBitmap(bitmap: Bitmap?) {
        bitmap.let {
            saveImage(it!!, context = applicationContext, "Picture")
        }
    }

    /// @param folderName can be your app's name
    private fun saveImage(bitmap: Bitmap, context: Context, folderName: String) {
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val values = contentValues()
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + folderName)
            values.put(MediaStore.Images.Media.IS_PENDING, true)
            // RELATIVE_PATH and IS_PENDING are introduced in API 29.

            val uri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                saveImageToStream(bitmap, context.contentResolver.openOutputStream(uri))
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                context.contentResolver.update(uri, values, null, null)
            }
        } else {
            val directory = File(Environment.getExternalStorageDirectory().toString() + separator + folderName)
            // getExternalStorageDirectory is deprecated in API 29

            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = System.currentTimeMillis().toString() + ".png"
            val file = File(directory, fileName)
            saveImageToStream(bitmap, FileOutputStream(file))
            if (file.absolutePath != null) {
                val values = contentValues()
                values.put(MediaStore.Images.Media.DATA, file.absolutePath)
                // .DATA is deprecated in API 29
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }
        }
    }

    private fun contentValues() : ContentValues {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        return values
    }

    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun takePictureNew() {
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ),
                    0
                )
            } else {
                try {
                    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    takeImage()
//                    resultLauncher.launch(takePictureIntent)



                //
//                    val imagesFolder = File(Environment.getExternalStorageDirectory(), "Camera")
//                    imagesFolder.mkdirs()
//                    val random = Random()
//                    val n = random.nextInt(10000)
//                    val image = File(imagesFolder, "YourApplication$n.jpg")
//                    val photoUri = Uri.fromFile(image)
//                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri); // set the image file name
                } catch (ex: Exception) {
                    // Error occurred while creating the File
                    displayMessage(baseContext, ex.message.toString())
                }
            }
        } catch (ex: Exception) {
            Log.d("Error", "this is an error")
        }
    }

    private fun takePictureOld() {
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ),
                    0
                )
            } else {
                try {
                    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    photoFile = createImageFile()
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        val photoURI = FileProvider.getUriForFile(
                            this,
                            "com.example.savepicturelocal.fileprovider",
                            photoFile!!,
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST)
                    }
                } catch (ex: Exception) {
                    // Error occurred while creating the File
                    displayMessage(baseContext, ex.message.toString())
                }
            }
        } catch (ex: Exception) {
            Log.d("Error", "this is an error")
        }
    }

    private fun displayMessage(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
//            val myBitmap = BitmapFactory.decodeFile(photoFile!!.absolutePath)
//            imageView?.setImageBitmap(myBitmap)
//
//        } else {
//            displayMessage(baseContext, "Request cancelled or something went wrong.")
//        }
//    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(collection.toString())
        val image = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        return image
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun takeImage() {
        lifecycleScope.launchWhenStarted {
            getTmpFileUri().let { uri ->
                latestTmpUri = uri
                resultLauncher.launch(uri)
            }
        }
    }
    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", getExternalFilesDir(Environment.DIRECTORY_PICTURES)).apply {
            createNewFile()
            deleteOnExit()
        }

        val uri =  FileProvider.getUriForFile(applicationContext, "${BuildConfig.APPLICATION_ID}.fileprovider", tmpFile)


        val photoUri = Uri.fromFile(tmpFile)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri); // set the image file name


        return  uri
    }
}