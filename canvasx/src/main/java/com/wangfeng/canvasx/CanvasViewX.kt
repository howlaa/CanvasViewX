package com.wangfeng.canvasx

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 画笔画布工具类
 * @author 王丰
 * @date 2022-3-22 11:30:02
 */
class CanvasViewX @JvmOverloads constructor(
    c: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(c, attrs, defStyleAttr) {

    //分类 画，文字，橡皮
    enum class Mode {
        DRAW, TEXT, ERASER
    }

    // 枚举 Drawer
    enum class Drawer {
        PEN,
        LINE,
        RECTANGLE,
        CIRCLE,
        ELLIPSE,
        QUADRATIC_BEZIER,
        QUBIC_BEZIER,
        ARROW
    }

    private var callBack: ((Boolean, Boolean) -> Unit?)? = null
    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private lateinit var mContext: Context
    private val pathLists = mutableListOf<Path>()
    private val paintLists = mutableListOf<Paint>()

    // for Eraser
    private var baseColor = Color.TRANSPARENT

    //Flag
    var drawer: Drawer = Drawer.PEN
    private var isDown = false
    private var hasMove = false


    //for paint
    private var paintStyle = Paint.Style.STROKE
    private var paintStrokeColor = Color.BLACK
    private var blur = 0f
    private var opacity = 255

    //for 撤销，反撤销
    private var historyPointer = 0

    private var mode = Mode.DRAW

    //for Text文字
    private var text: String? = ""
    private var textPaint = Paint()
    private var fontFamily = Typeface.DEFAULT
    private var fontSize = 32f
    private var textAlign = Paint.Align.RIGHT // fixed
    private var textX = 0f
    private var textY = 0f

    //for Drawer
    private var startX: Float = 0f
    private var startY = 0f
    private var controlX = 0f
    private var controlY = 0f

    private var paintStrokeWidth = 3F
//    private var erasePaintStrokeWidth = 3F
    private var lineCap = Paint.Cap.ROUND
    init {
        setup(c)
    }

    /**
     * 设置画板基本颜色
     */
    fun setBaseColor(color: Int) {
        this.baseColor = color
    }

    fun setCallback(callBack: (Boolean, Boolean) -> Unit?){
        this.callBack = callBack
    }


    /**
     * 设置画笔粗度
     */
    fun setPaintStrokeWidth(width: Float){
        if (width > 0 ) {
            this.paintStrokeWidth = width
        } else {
            paintStrokeWidth = 3f
        }
    }

    /**
     * 设置画笔颜色
     */
    fun setPaintStrokeColor(color:Int){
        this.paintStrokeColor = color
    }

    /**
     * 设置画笔style
     */
    fun setPaintStyle(style: Paint.Style) {
        paintStyle = style
    }

    /**
     * 设置模式
     */
    fun setMode(mode: Mode) {
        this.mode = mode
    }

    /**
     *
     */
    fun setLineCap(cap:Paint.Cap){
        lineCap = cap
    }

    /**
     * 通用的初始化
     */
    private fun setup(ctx: Context) {
        historyPointer = 0
        pathLists.clear()
        paintLists.clear()
        mContext = ctx
        pathLists.add(Path())
        paintLists.add(createPaint())
        historyPointer++
        Log.d("wangfeng","historyPointer:$historyPointer")
        textPaint.setARGB(0, 255, 255, 255)
    }

    /**
     * 这个方法是获取paint的实例
     * 此外，在这个方法中，可以设置画笔的实例
     */
    private fun createPaint(): Paint {
        val paint = Paint()
        //抗锯齿
        paint.isAntiAlias = true
        paint.style = paintStyle
        paint.strokeWidth = paintStrokeWidth
        paint.strokeCap = lineCap
        paint.strokeJoin = Paint.Join.ROUND

        //Text文字
        if (mode == Mode.TEXT) {
            paint.typeface = fontFamily
            paint.textSize = fontSize
            paint.textAlign = textAlign
            paint.strokeWidth = 0F
        }

        //橡皮擦
        if (mode == Mode.ERASER) {
            // Eraser
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            paint.setARGB(0, 0, 0, 0)
        } else {
            paint.color = paintStrokeColor
            paint.setShadowLayer(blur, 0F, 0F, paintStrokeColor)
            paint.alpha = opacity
        }
        return paint
    }

    /**
     * 这个方法，初始化Path
     * 创建Path的实例，以及移动当前的坐标点
     * @return path 返回的Path实例
     */
    private fun createPath(event: MotionEvent):Path{
        val path = Path()

        //保存按下的坐标，作为开始坐标
        startX = event.x
        startY = event.y

        path.moveTo(startX,startY)
        return path
    }

    /**
     * 当仅仅是点击，没有移动，那么就没有画图
     */
    private fun dealNoMove(){
        if (paintLists.isNotEmpty()) {
            pathLists.removeLast()
        }
        if (paintLists.isNotEmpty()) {
            paintLists.removeLast()
        }
        historyPointer--
    }

    /**
     * 这个方法更新路径和画笔
     * 撤销，清除 enable的设置也在这里
     */
    private fun updateHistory(path: Path){
        if (historyPointer == this.pathLists.size) {
            pathLists.add(path)
            paintLists.add(createPaint())
            historyPointer++
        } else {
            //处理撤销 重做

            // On the way of Undo or Redo
            pathLists[historyPointer] = path
            paintLists[historyPointer] = createPaint()
            historyPointer++

            for (i in historyPointer until paintLists.size){
                pathLists.removeAt(historyPointer)
                paintLists.removeAt(historyPointer)
            }
        }
    }

    /**
     * 获取当前路径
     */
    private fun getCurrentPath():Path{
        return pathLists[historyPointer - 1]
    }

    /**
     * 绘制Text
     */
    private fun drawText(canvas: Canvas) {
        if (text?.isEmpty() == true){
            return
        }

        if (mode == Mode.TEXT) {
            textX = startX
            textY = startY

            textPaint = createPaint()
        }

        val textX = textX
        val textY = textY

        val paintForMeasureText = Paint()

        //自动换行
        text?.let {
            val textLength = paintForMeasureText.measureText(it)
            val lengthOfChar = textLength / it.length.toFloat()
            val restWidth = canvas.width - textX // text-align : right
            val numChars =
                if (lengthOfChar <= 0) 1 else floor((restWidth / lengthOfChar).toDouble())
                    .toInt() // The number of characters at 1 line
            val modNumChars = if (numChars < 1) 1 else numChars
            var y = textY
            val len = it.length
            for (i in 0 until len step modNumChars) {
                var substring = ""
                substring = if (i + modNumChars < len) {
                    it.substring(i, i + modNumChars)
                } else {
                    it.substring(i, len)
                }

                y += fontSize

                canvas.drawText(substring, textX, y, textPaint)
            }
        }
    }


    /**
     * 手指按下
     */
    private fun onActionDown(event: MotionEvent){
        when(mode) {
            Mode.DRAW,Mode.ERASER -> {
                if ((this.drawer != Drawer.QUADRATIC_BEZIER) && (this.drawer != Drawer.QUBIC_BEZIER)) {
                    updateHistory(createPath(event))
                    this.hasMove = false
                    this.isDown = true
//                    when(drawer) {
//                        Drawer.PEN,Drawer.LINE -> {
//                            drawEvent(event)
//                        }
//                        else -> {}
//                    }
                } else {
                    if ((this.startX == 0F) && (this.startY == 0F)) {
                        // 第一次点击
                        this.updateHistory(createPath(event))
                    } else {
                        //第二次点击
                        this.controlX = event.x
                        this.controlY = event.y

                        this.isDown = true
                    }
                }
            }
            Mode.TEXT -> {
                this.startX = event.x
                this.startY = event.y
            }
        }
    }

    /**
     * 橡皮或者画刷通过坐标点画图
     */
    private fun drawEvent(event: MotionEvent){
        val eventX = event.x
        val eventY = event.y
        val path = getCurrentPath()
        when(drawer) {
            Drawer.PEN -> {
                path.lineTo(eventX,eventY)
            }
            Drawer.LINE -> {
                path.reset()
                path.moveTo(startX, startY)
                path.lineTo(eventX,eventY)
            }
            Drawer.RECTANGLE -> {
                path.reset()
                path.addRect(startX,startY,eventX,eventY,Path.Direction.CCW) //逆时针
            }
            Drawer.CIRCLE -> {
                val distanceX = abs((startX - eventX).toDouble())
                val distanceY = abs((startX - eventY).toDouble())
                val radius =
                    sqrt(distanceX.pow(2.0) + distanceY.pow(2.0))

                path.reset()
                path.addCircle(startX, startY, radius.toFloat(), Path.Direction.CCW)
            }
            //椭圆
            Drawer.ELLIPSE -> {
                val rect = RectF(startX, startY, eventX, eventY)
                path.reset()
                path.addOval(rect, Path.Direction.CCW)
            }
            //箭头
            Drawer.ARROW -> {
                path.reset()
                path.moveTo(startX, startY)
                path.lineTo(eventX,eventY)
                Log.d("wangfeng","画笔大小：$paintStrokeWidth}")
                drawArrow(path,startX,startY,toX,toY,paintStrokeWidth.toInt()*2,paintStrokeWidth.toInt()*2)
            }
            else -> {}
        }
    }


    var toX = 0f
    var toY = 0f
    /**
     * 手指移动
     */
    private fun onActionMove(event: MotionEvent){
//        Log.d("wangfeng","onActionMove")
        toX = event.x
        toY = event.y
         val x = event.x
         val y = event.y
        when(mode) {
            Mode.DRAW,Mode.ERASER -> {
                if ((this.drawer != Drawer.QUADRATIC_BEZIER) && (this.drawer != Drawer.QUBIC_BEZIER)) {
                    if (!isDown) {
                        return
                    }
                    this.hasMove = true
                    drawEvent(event)
                } else {
                    if (!isDown) {
                        return
                    }

                    val path = getCurrentPath()

                    path.reset()
                    path.moveTo(startX, startY)
                    path.quadTo(controlX, controlY, event.x, y)
                }
            }
            Mode.TEXT -> {
                this.startX = event.x
                this.startY = event.y
            }
        }
    }

    /**
     * 画箭头
     */
    private fun drawArrow(path: Path, fromX:Float, fromY:Float, toX:Float, toY: Float,height:Int,bottom:Int){
        val distance = sqrt(
            ((toX - fromX) * (toX - fromX)
                    + (toY - fromY) * (toY - fromY)).toDouble()
        ).toFloat() // 获取线段距离
        val distanceX = toX - fromX // 有正负，不要取绝对值
        val distanceY = toY - fromY // 有正负，不要取绝对值

        val dianX: Float = toX - height / distance * distanceX
        val dianY = toY - height / distance * distanceY
        //终点的箭头
        path.moveTo(toX, toY) // 此点为三边形的起点
        path.lineTo(dianX + (bottom / distance * distanceY), dianY
                - (bottom / distance * distanceX));
        path.moveTo(toX, toY)
        path.lineTo(dianX - (bottom / distance * distanceY), dianY
                + (bottom / distance * distanceX))
//        path.close()
    }

    /**
     * 手指抬起
     */
    private fun onActionUp(event: MotionEvent){
        if (isDown) {
            startX = 0F
            startY = 0F
            isDown = false
            if (mode == Mode.DRAW || mode == Mode.ERASER) {
                if ((this.drawer != Drawer.QUADRATIC_BEZIER) && (this.drawer != Drawer.QUBIC_BEZIER)) {
//                    if (drawer != Drawer.PEN && drawer != Drawer.LINE){
                        if (!hasMove) {
                            dealNoMove()
                        }
//                    }
                }
            }
        }
    }
    private var isSave = false

    /**
     * 这个方法是更新Canvas的实例
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawColor(baseColor)
        bitmap?.let {
            canvas?.drawBitmap(it, 0F,0F,null)
        }
        for (i in 0 until historyPointer) {
            val path = pathLists[i]
            val paint = paintLists[i]
            canvas?.drawPath(path, paint)
        }
        canvas?.let{
            drawText(it)
        }
        this.canvas = canvas
        callBack?.invoke(undoEnable(),redoEnable())
    }

    /**
     * 设置触摸监听
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                onActionDown(event)
                if (mode == Mode.DRAW || mode == Mode.ERASER) {
                    if ((this.drawer != Drawer.QUADRATIC_BEZIER) && (this.drawer != Drawer.QUBIC_BEZIER)) {
//                        if (drawer == Drawer.PEN || drawer == Drawer.LINE){
//                            // 重新绘制
//                            this.invalidate()
//                        }
                    } else {
                        this.invalidate()
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                onActionMove(event)
                this.invalidate()
            }
            MotionEvent.ACTION_UP -> {
                onActionUp(event)
                this.invalidate()
            }
        }


        return true
    }

    /**
     * 是否可重做
     */
    fun redoEnable():Boolean {
        return historyPointer < pathLists.size
    }

    /**
     * 重做
     */
    fun redo():Boolean {
        var result = false
        if (historyPointer < pathLists.size) {
            historyPointer++
            invalidate()
            result = true
        }
        return result
    }

    fun clearCanvas(color: Int){
        val path = Path()
        path.moveTo(0f, 0f)
        path.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CCW)
        path.close()

        val paint = Paint()
//        paint.setARGB(0, 0, 0, 0)
        paint.color = color
        paint.style = Paint.Style.FILL

        if (historyPointer == pathLists.size) {
            pathLists.add(path)
            paintLists.add(paint)
            historyPointer++
        } else {
            // 正在进行撤销或者反撤销
            pathLists[historyPointer] = path
            paintLists[historyPointer] = paint
            historyPointer++
            var i = historyPointer
            val size = paintLists.size
            while (i < size) {
                pathLists.removeAt(historyPointer)
                paintLists.removeAt(historyPointer)
                i++
            }
        }
        text = ""

        // Clear
        this.invalidate()
    }

    /**
     * 是否可撤销
     */
    fun undoEnable():Boolean {
        return historyPointer > 1
    }

    /**
     * 撤销
     */
    fun undo(): Boolean {
        return if (historyPointer > 1) {
            historyPointer--
            this.invalidate()
            true
        } else {
            false
        }
    }



    /**
     * 清空
     */
    fun clear(context: Context){
       setup(context)
        this.invalidate()
    }

}

