package com.example.app2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.app2.databinding.ActivityMainBinding

class DownloadReceiver: BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("DownloadReceiver" , "Receiver intent")
        val place = intent?.getStringExtra("PLACE")

        val resultIntent = Intent(context , MainActivity::class.java).apply {
            putExtra("PLACE" , place)
        }

        context?.startActivity(resultIntent)
    }
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filter = IntentFilter()
        filter.addAction("com.example.lab7.DOWNLOADER")
        registerReceiver(DownloadReceiver() , filter)

        val place = intent?.getStringExtra("PLACE")

        if(place != null) {
            binding.textView2.text = place
        }
    }

}

