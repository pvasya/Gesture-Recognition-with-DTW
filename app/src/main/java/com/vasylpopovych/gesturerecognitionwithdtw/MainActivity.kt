package com.vasylpopovych.gesturerecognitionwithdtw

import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var xValue by mutableStateOf(0f)
    private var yValue by mutableStateOf(0f)
    private var zValue by mutableStateOf(0f)

    private val xData = mutableStateListOf<Float>()
    private val yData = mutableStateListOf<Float>()
    private val zData = mutableStateListOf<Float>()

    private var timerRunning by mutableStateOf(false)
    private var elapsedTime by mutableStateOf(0)

    private var showGestureDialog by mutableStateOf(false)
    private var showSaveDialog by mutableStateOf(false)
    private var newGestureName by mutableStateOf("")

    private val recordedX = mutableListOf<Float>()
    private val recordedY = mutableListOf<Float>()
    private val recordedZ = mutableListOf<Float>()

    private val gestures = mutableStateListOf<Gesture>()

    private val DTWCalculator = DTWCalculator();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        sensorManager = ContextCompat.getSystemService(this, SensorManager::class.java)!!
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            Toast.makeText(this, R.string.no_accelerometer, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            GestureAppUI()
        }
    }

    @Composable
    fun GestureAppUI() {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(color = colorResource(id = R.color.gray))
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    DisplayAccelerometerValues()
                    AccelerationGraph(
                        xData = xData,
                        yData = yData,
                        zData = zData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                }

                Button(
                    onClick = { showGestureDialog = true },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 12.dp)
                        .height(50.dp)
                        .width(200.dp)
                ) {
                    Text(getString(R.string.list), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = formatTime(elapsedTime),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                val context = LocalContext.current
                Button(
                    onClick = {
                        if (timerRunning) {
                            Toast.makeText(context, getString(R.string.wait), Toast.LENGTH_SHORT)
                                .show()
                            timerRunning = false
                            showSaveDialog = true
                        } else {
                            resetTimerAndData()
                            timerRunning = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 16.dp)
                        .height(50.dp)
                        .width(200.dp)
                ) {
                    Text(
                        if (timerRunning) getString(R.string.stop) else getString(R.string.start),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                TimerControl()
                if (showGestureDialog) GestureListDialog()
                if (showSaveDialog) SaveGestureDialog()
            }
        }
    }

    private fun resetTimerAndData() {
        elapsedTime = 0
        recordedX.clear()
        recordedY.clear()
        recordedZ.clear()
    }


    @Composable
    fun TimerControl() {
        if (timerRunning) {
            LaunchedEffect(Unit) {
                while (timerRunning) {
                    delay(10)
                    elapsedTime += 10
                    recordedX.add(xValue)
                    recordedY.add(yValue)
                    recordedZ.add(zValue)
                    if (elapsedTime >= 15000) {
                        timerRunning = false
                        showSaveDialog = gestures.isEmpty()
                    }
                }
            }
        }
    }

    @Composable
    fun GestureListDialog() {
        Dialog(onDismissRequest = { showGestureDialog = false }) {
            Surface {
                Column(Modifier.padding(16.dp)) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(gestures) { gesture ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    gesture.name,
                                    Modifier
                                        .weight(1f)
                                        .padding(top = 14.dp)
                                )
                                Button(onClick = { gestures.remove(gesture) }) {
                                    Text(getString(R.string.delete))
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(onClick = { showGestureDialog = false }) {
                            Text(getString(R.string.back))
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SaveGestureDialog() {
        Dialog(onDismissRequest = { showSaveDialog = false }) {
            Surface {
                Column(Modifier.padding(16.dp)) {
                    if (gestures.isEmpty()) {
                        Text(
                            getString(R.string.empty),
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            getString(R.string.save_to_list),
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        var similarGesture = "";
                        var minDistance = Double.MAX_VALUE;
                        for (gesture in gestures) {
                            DTWCalculator.calculateDTW(recordedX, recordedY, recordedZ, gesture)
                                .let {
                                    if (it < minDistance) {
                                        minDistance = it
                                        similarGesture = gesture.name
                                    }
                                }
                        }
                        Log.i("GestureSimilarity", "The gesture is similar to ${similarGesture}")
                        Text(
                            getString(R.string.similar_gesture) + " ${similarGesture}",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            getString(R.string.save_add_to_list),
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text(getString(R.string.gesture_name))
                    TextField(value = newGestureName, onValueChange = { newGestureName = it })
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Button(onClick = {
                            saveGesture(); newGestureName = ""
                        }) { Text(getString(R.string.save)) }
                        Button(onClick = {
                            showSaveDialog = false
                        }) { Text(getString(R.string.cancle)) }
                    }
                }
            }
        }
    }

    private fun saveGesture() {
        if (gestures.none { it.name == newGestureName.replace("\n", "").replace("\r", "") } && newGestureName.isNotEmpty() && newGestureName.length <= 20) {
            gestures.add(
                Gesture(
                    newGestureName.replace("\n", "").replace("\r", ""),
                    recordedX.toList(),
                    recordedY.toList(),
                    recordedZ.toList()
                )
            )
            showSaveDialog = false
        } else {
            if (newGestureName.isEmpty())
                Toast.makeText(this, getString(R.string.enter_gesture_name), Toast.LENGTH_SHORT)
                    .show()
            else if (newGestureName.length > 20)
                Toast.makeText(this, getString(R.string.name_too_long), Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(this, getString(R.string.name_already_exists), Toast.LENGTH_SHORT)
                    .show()
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            xValue = it.values[0]
            yValue = it.values[1]
            zValue = it.values[2]

            if (xData.size >= 100) xData.removeAt(0)
            if (yData.size >= 100) yData.removeAt(0)
            if (zData.size >= 100) zData.removeAt(0)

            xData.add(xValue)
            yData.add(yValue)
            zData.add(zValue)
        }
    }


    @Composable
    fun DisplayAccelerometerValues() {
        Text(
            getString(R.string.accelerometer_values),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(16.dp))
        Row {
            Text(
                "X: ${xValue.format(2)}",
                color = Color.Red,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Row {
            Text(
                "Y: ${yValue.format(2)}",
                color = Color.Blue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Row {
            Text(
                "Z: ${zValue.format(2)}",
                color = Color.Green,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun Float.format(digits: Int) = "%.${digits}f".format(this)

    private fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val millis = (milliseconds % 1000) / 10
        return String.format("%02d.%02d", seconds, millis)
    }

    @Composable
    fun AccelerationGraph(
        xData: List<Float>,
        yData: List<Float>,
        zData: List<Float>,
        modifier: Modifier = Modifier
    ) {
        Canvas(modifier = modifier) {
            drawRect(color = Color.Black)

            val widthStep = size.width / (xData.size.coerceAtLeast(1))
            val heightCenter = size.height / 2
            val scaleFactor = 20

            drawLine(
                color = Color.White,
                start = androidx.compose.ui.geometry.Offset(0f, heightCenter),
                end = androidx.compose.ui.geometry.Offset(size.width, heightCenter),
                strokeWidth = 1f
            )

            val pathX = Path().apply {
                moveTo(0f, heightCenter)
                xData.forEachIndexed { index, value ->
                    val x = index * widthStep
                    val y = heightCenter - (value * scaleFactor)
                    lineTo(x, y)
                }
            }

            val pathY = Path().apply {
                moveTo(0f, heightCenter)
                yData.forEachIndexed { index, value ->
                    val x = index * widthStep
                    val y = heightCenter - (value * scaleFactor)
                    lineTo(x, y)
                }
            }

            val pathZ = Path().apply {
                moveTo(0f, heightCenter)
                zData.forEachIndexed { index, value ->
                    val x = index * widthStep
                    val y = heightCenter - (value * scaleFactor)
                    lineTo(x, y)
                }
            }

            drawPath(pathX, color = Color.Red, style = Stroke(width = 2f))
            drawPath(pathY, color = Color.Blue, style = Stroke(width = 2f))
            drawPath(pathZ, color = Color.Green, style = Stroke(width = 2f))
        }
    }
}