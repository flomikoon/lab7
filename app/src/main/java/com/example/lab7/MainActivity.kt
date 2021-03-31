package com.example.lab7

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.example.lab7.databinding.DownloadBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.lang.Exception
import java.net.URL

class DownloadService : Service() {
    private lateinit var job: Job

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("URL")
        if (url != null){
            job = CoroutineScope(Dispatchers.IO).launch {
                println(Thread.currentThread().name)
               val place =  downloadImage(url)?.saveImage()
                Log.i("Downloader" , "Place: $place")
                sendBroadcast(Intent("com.example.lab7.DOWNLOADER").apply {
                    putExtra("PLACE" , place)
                })
                stopSelf(startId)
            }
        } else stopSelf(startId)

        return START_NOT_STICKY
    }

    private fun downloadImage(url: String): Bitmap? {
        var image: Bitmap? = null
        try {
            val inputStream: InputStream = URL(url).openStream()
            image = BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception){
            Log.e("Error" , e.message.toString())
        }
        return image
    }

    private fun Bitmap.saveImage(): String{
        val filename = "img_started_service.png"
        openFileOutput(filename , MODE_PRIVATE).use{
            compress(Bitmap.CompressFormat.PNG , 100 , it)
        }
        return File(filesDir , filename).absolutePath
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            startService(Intent(this , DownloadService::class.java).apply {
                putExtra("URL" , "https://i.ibb.co/8cvn9LZ/dinosaur-5995333-1920.png" )
            })
        }
    }
}