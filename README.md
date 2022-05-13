# CanvasViewX
##Android hand paint library
##android 涂鸦手绘工具库
各种图形，箭头，曲线，直线，圆形，橡皮擦等。
配上我的FloatWindowX库，可实现后台悬浮画图

#依赖
#Dependency

1.
```
        allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
 2.
```
        dependencies {
	        implementation 'com.github.howlaa:CanvasViewX:1.0'
	}
```
 
#Usage
#使用
 在你的xml文件里：
##in your xml, like this:
```
          <com.wangfeng.canvasx.CanvasViewX
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:id="@+id/canvas"
        />
```
    
## your activity:
```
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
```
# API：
```
       //setMode
        canvas.drawer = CanvasViewX.Drawer.PEN
        //bg color
        canvas.setBaseColor(Color.WHITE)
        ...以后有时间再更新
```
    
    
