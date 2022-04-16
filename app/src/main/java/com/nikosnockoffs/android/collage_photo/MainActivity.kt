package com.nikosnockoffs.android.collage_photo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.SimpleAdapter
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val TAG = "COLLAGE_ACTIVITY"
class MainActivity : AppCompatActivity() {

//    private lateinit var imageButton1: ImageButton
    private lateinit var imageButtons: List<ImageButton>

    private lateinit var mainView: View

//    private var newPhotoPath: String? = null
//    private var visibleImagePath: String? = null

    // using arrayList because can't save List in saved instance state but can save arrayList
    private var photoPaths: ArrayList<String?> = arrayListOf(null, null, null, null)

    private var whichImageIndex: Int? = null

    // which photo path the user is currently using
    private var currentPhotoPath: String? = null

//    private val NEW_PHOTO_PATH_KEY = "new photo path key"
//    private val VISIBLE_IMAGE_PATH_KEY = "visible image path key"

    private val PHOTO_PATH_LIST_ARRAY_KEY = "photo path list key"
    private val IMAGE_INDEX_KEY = "image index key"
    private val CURRENT_PHOTO_PATH_KEY = "current photo path key"

    private val cameraActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        handleImage(result)
    }

    private fun handleImage(result: ActivityResult) {
        // check result - see what user did when the camera app opened
        when (result.resultCode) {
            RESULT_OK -> {
                Log.d(TAG, "Result ok, image at $currentPhotoPath")
                whichImageIndex?.let { index ->
                    photoPaths[index] = currentPhotoPath
                }
            }
            RESULT_CANCELED -> {
                Log.d(TAG, "Result cancelled, no picture taken")
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(PHOTO_PATH_LIST_ARRAY_KEY, photoPaths)
        outState.putString(CURRENT_PHOTO_PATH_KEY, currentPhotoPath)
        // within let function, lambda argument is non-null value of whichImageIndex
        // let function only runs if whichImageIndex is not null
        whichImageIndex?.let { index -> outState.putInt(IMAGE_INDEX_KEY, index)}
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d(TAG, "on window focus changed $hasFocus visible image at $currentPhotoPath")
        if (hasFocus) {
//            visibleImagePath?.let { imagePath ->
//                loadImage(imageButton1, imagePath) }
    // zip function loops over both lists at once
            imageButtons.zip(photoPaths) { imageButton, photoPath ->
                photoPath?.let {
                    loadImage(imageButton, photoPath)
                }
            }
        }
    }

    private fun loadImage(imageButton: ImageButton, imagePath: String) {
        // want to fit image into the image button size but can vary based on device/orientation
        Picasso.get()
            .load(File(imagePath))
            .error(android.R.drawable.stat_notify_error) // displayed if issue with loading image
            .fit()
            .centerCrop()
            .into(imageButton, object: Callback {
                override fun onSuccess() {
                    Log.d(TAG, "Loaded image $imagePath")
                }

                override fun onError(e: Exception?) {
                    Log.e(TAG, "Error loading image $imagePath", e)
                }
            })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        whichImageIndex = savedInstanceState?.getInt(IMAGE_INDEX_KEY)
        currentPhotoPath = savedInstanceState?.getString(CURRENT_PHOTO_PATH_KEY)
        photoPaths = savedInstanceState?.getStringArrayList(PHOTO_PATH_LIST_ARRAY_KEY) ?: arrayListOf(null, null, null, null)

//        newPhotoPath = savedInstanceState?.getString(NEW_PHOTO_PATH_KEY)
//        visibleImagePath = savedInstanceState?.getString(VISIBLE_IMAGE_PATH_KEY)

        mainView = findViewById(R.id.content)

        imageButtons = listOf<ImageButton>(
            findViewById(R.id.imageButton1),
            findViewById(R.id.imageButton2),
            findViewById(R.id.imageButton3),
            findViewById(R.id.imageButton4)
        )

//        imageButton1 = findViewById(R.id.imageButton1)
//        imageButton1.setOnClickListener {
//            takePicture()
//        }

        for (imageButton in imageButtons) {
            imageButton.setOnClickListener { ib ->
                takePictureFor(ib as ImageButton)
            }
        }

    }

    private fun takePictureFor(imageButton: ImageButton) {

        val index = imageButtons.indexOf(imageButton)
        whichImageIndex = index
        // implicit intent to launch camera app when this function is called by pressing the button
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // create reference to file want image to be saved in
        val (photoFile, photoFilePath) = createImageFile()
        if (photoFile != null) {
            currentPhotoPath = photoFilePath
            // create Uri for new photofile created - bc camera app works with Uris
            val photoUri = FileProvider.getUriForFile(
                this,
                "com.nikosnockoffs.android.collage_photo.fileprovider",
                photoFile
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            cameraActivityLauncher.launch(takePictureIntent)
        }

    }

    private fun createImageFile(): Pair<File?, String?> {
        try {
            val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val imageFileName = "COLLAGE_${dateTime}"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            // file is reference to file itself
            val file = File.createTempFile(imageFileName, ".jpg", storageDir)
            // filePath is string location of where the file is on the device
            val filePath = file.absolutePath
            return file to filePath

        } catch (ex: IOException) {
            // return pair with "to"
            return null to null
        }
    }
}