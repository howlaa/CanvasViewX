package com.wangfeng.canvastest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.wangfeng.canvasx.CanvasViewX

class MainActivity : AppCompatActivity() {
    private lateinit var canvas: CanvasViewX
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        canvas = findViewById(R.id.canvas)
    }

}