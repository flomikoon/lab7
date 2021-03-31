package com.example.boundservice

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.core.content.pm.ShortcutInfoCompatSaver
import com.example.boundservice.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.lang.Exception
import java.lang.ref.WeakReference
import java.net.URL
import kotlin.coroutines.CoroutineContext

class TASK{
    companion object{
        const val DOWNLOAD = 0
        const val PLACE = 1
    }
}

class DownloaderHandler(service: DownloadService) : Handler(service.mainLooper){
    private val serviceReference = WeakReference(service)

    override fun handleMessage(msg: Message) {
        if (msg.what == TASK.DOWNLOAD){
            Log.i("DownloaderHandler" , "Download msg")

            val url = msg.obj ?: return
            val replyTo = msg.replyTo ?: return

            serviceReference.get()?.savesImage(url as String){
                Log.i("DownloaderHandler" , "PLACE: $it")
                replyTo.send(Message.obtain().apply {
                    obj = it
                    what = TASK.PLACE
                })
            }
        } else super.handleMessage(msg)
    }
}

class ResponseHandler(activity: MainActivity): Handler(activity.mainLooper){
    private val activityRef = WeakReference(activity)

    override fun handleMessage(msg: Message) {
        if(msg.what == TASK.PLACE){
            Log.d("ResponseHandler" , "Save msg incoming")

            val  place = msg.obj?: return

            activityRef.get()?.changeText(place as String)?: return
        } else super.handleMessage(msg)
    }
}


class DownloadService : Service() {
    private lateinit var job: Job

    fun savesImage(url: String , saver: suspend (String) -> Unit){
        job = CoroutineScope(Dispatchers.IO).launch {
            downloadImage(url)?.saveImage().let {
                if (it != null) {
                    saver(it)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("URL")
        if (url != null){
            savesImage(url){
                Log.i("Downloader" , "PLACE: $it")
                sendBroadcast(Intent("com.example.lab7.DOWNLOADER").apply {
                    putExtra("PLACE" , it)
                })
                stopSelf(startId)
            }
        } else stopSelf(startId)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return Messenger(DownloaderHandler(this)).binder
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
        val filename = "download_img.png"
        openFileOutput(filename , MODE_PRIVATE).use{
            compress(Bitmap.CompressFormat.PNG , 100 , it)
        }
        return File(filesDir , filename).absolutePath
    }

    override fun onDestroy() {
        job.cancel()
    }
}
class MainActivity : AppCompatActivity() {

    private lateinit var binding:ActivityMainBinding

    private lateinit var messenger: Messenger

    private val connect = object : ServiceConnection{
        var messenger: Messenger? = null

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i("MainActivity" , "Service connected")
            messenger = Messenger(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i("MainActivity" , "Service disconnected")
            messenger = null
        }
    }

    fun changeText(place: String){
        binding.text.text = place
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startService.setOnClickListener {
            startService(Intent(this , DownloadService::class.java).apply {
                putExtra("URL" , "https://i.ibb.co/8cvn9LZ/dinosaur-5995333-1920.png" )
            })
        }

        val Intent = Intent(this , DownloadService::class.java)
        bindService(Intent , connect , Context.BIND_AUTO_CREATE)

        messenger = Messenger(ResponseHandler(this))

        binding.requetService.setOnClickListener {

            val message = Message.obtain().apply {
                obj = "https://i.ibb.co/8cvn9LZ/dinosaur-5995333-1920.png"
                replyTo = messenger
                what = TASK.DOWNLOAD
            }

            try {
                connect.messenger?.send(message)
            } catch (e: RemoteException){
                e.printStackTrace()
            }
        }
    }
}
