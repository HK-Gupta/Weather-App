package com.example.compressor

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.format.Formatter
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.karumi.dexter.Dexter
import com.karumi.dexter.DexterBuilder.MultiPermissionListener
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.PermissionRequestErrorListener
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.destination
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val CODE = 99099
    lateinit var img_original: ImageView
    lateinit var img_compressed: ImageView
    lateinit var txt_original: TextView
    lateinit var txt_compressed: TextView
    lateinit var txt_quality: TextView
    lateinit var txt_height: EditText
    lateinit var txt_width: EditText
    lateinit var seek_quality: SeekBar
    lateinit var btn_select: Button
    lateinit var btn_compress: Button
    lateinit var original: File
    lateinit var compressed: File
    private var filePath: String? = null
    val path =  File(Environment.getExternalStorageDirectory().absolutePath + "/myCompressor")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        askPermission()

        img_original = findViewById(R.id.img_original)
        img_compressed = findViewById(R.id.img_compressed)
        txt_original = findViewById(R.id.txt_original)
        txt_compressed = findViewById(R.id.txt_compressed)
        txt_quality = findViewById(R.id.txt_quality)
        txt_height = findViewById(R.id.txt_height)
        txt_width = findViewById(R.id.txt_width)
        seek_quality = findViewById(R.id.seek_quality)
        btn_select = findViewById(R.id.btn_select)
        btn_compress = findViewById(R.id.btn_compress)

        filePath = path.absolutePath

        if(!path.exists())
            path.mkdirs()

        seek_quality.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, i: Int, boolean: Boolean) {
                txt_quality.text = "Quality: $i"
                seekBar?.max = 100
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })

        btn_select.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            startActivityForResult(gallery, CODE)
        }

        btn_compress.setOnClickListener {
            val quality = seek_quality.progress
            val width = txt_width.text.toString().toInt()
            val height = txt_height.text.toString().toInt()

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    compressImage(width, height, quality)
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error While Compressing", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK && requestCode==CODE) {
            btn_compress.visibility = View.VISIBLE
            val imageUri = data?.data
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(imageUri!!, projection, null, null , null)

            if(cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                val filePath = cursor.getString(columnIndex)
                cursor.close()

                val imageFile = File(filePath)
                if(imageFile.exists()) {
                    try{
                        val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
                        val selectedImage = BitmapFactory.decodeStream(inputStream)
                        img_original.setImageBitmap(selectedImage)
                        original = imageFile
                        txt_original.setText("Size: " + Formatter.formatShortFileSize(this, original.length()))
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                        Toast.makeText(this, "Error opening the selected image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "No Image Selected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun askPermission() {
        Dexter.withContext(this).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {

                }
                override fun onPermissionRationaleShouldBeShown(
                    mList: MutableList<PermissionRequest>?,
                    permission: PermissionToken
                ) {
                    permission.continuePermissionRequest()
                }
            }).check()
    }
    private suspend fun compressImage(width: Int, height: Int, quality: Int) {
        try {
            compressed = Compressor.compress(this, original) {
                resolution(width, height)
                quality(quality)
                format(Bitmap.CompressFormat.JPEG)
                destination(original)
            }
            val finalFile = File(filePath, original.name)
            val finalBitMap = BitmapFactory.decodeFile(finalFile.absolutePath)
            img_compressed.setImageBitmap(finalBitMap)
            txt_compressed.text = "Size: " + Formatter.formatShortFileSize(this, finalFile.length())
            Toast.makeText(this, "Compressed Successfully", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Error While Compressing", Toast.LENGTH_SHORT).show()
            throw RuntimeException(e)
        }
    }
}