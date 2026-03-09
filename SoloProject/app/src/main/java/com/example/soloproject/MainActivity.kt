package com.example.soloproject

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.soloproject.ui.theme.SoloProjectTheme

private enum class Screen { Main, History }

class MainActivity : ComponentActivity() {
//this is viewmodel for the fruits
    private val viewModel: FruitViewModel by viewModels()

    //This is the current screen showing
    private var currentScreen by mutableStateOf(Screen.Main)

    //This is true when the device is shaking
    private var isShaking by mutableStateOf(false)

    //This is true when the app has permission
    private var hasNotificationPermission by mutableStateOf(false)

    //This is for sensor
    private lateinit var sensorManager: SensorManager

    //This detects shaking
    private lateinit var shakeDetector: ShakeDetector

    private val requestPermissionLauncher = registerForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            NotificationScheduler.scheduleBananaReminder(context = this)
        }
    }

    private fun requestNotificationPermissionAndSchedule() {
        NotificationScheduler.createNotificationChannel(this)
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
            PackageManager.PERMISSION_GRANTED -> {
                hasNotificationPermission = true
                NotificationScheduler.scheduleBananaReminder(this)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //This gets the sensor manager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        //This creates the shake detector
        shakeDetector = ShakeDetector(
            onShakeStarted = { isShaking = true },
            onShakeStopped = {
                lifecycleScope.launch {
                    delay(1_000)
                    isShaking = false
                    viewModel.selectFruit(fruits.filter { it != viewModel.currentFruit }.random())
                }
            }
        )

        //This sets the content to either main or history.
        setContent {
            SoloProjectTheme {
                val history by viewModel.history.collectAsState()
                when (currentScreen) {
                    Screen.Main -> MainScreen(
                        fruit = viewModel.currentFruit,
                        carbs = viewModel.carbs,
                        isShaking = isShaking,
                        hasNotificationPermission = hasNotificationPermission,
                        onHistoryClick = { currentScreen = Screen.History }
                    )
                    Screen.History -> HistoryView(
                        history = history,
                        onBack = { currentScreen = Screen.Main }
                    )
                }
            }
        }

        //This triggers the notification
        requestNotificationPermissionAndSchedule()
    }

    override fun onResume() {
        super.onResume()

        //This checks the notification permission
        hasNotificationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        //This gets carbs
        if (viewModel.carbs == null) {
            viewModel.fetchCarbs()
        }

        //This gets the default accelerometer sensor and starts to listen to it
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()

        //Stop listening to shake if app is paused
        sensorManager.unregisterListener(shakeDetector)
    }
}

//This is main screen with navigation
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    fruit: Fruit,
    carbs: Int?,
    isShaking: Boolean,
    hasNotificationPermission: Boolean,
    onHistoryClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    TextButton(onClick = onHistoryClick) {
                        Text("History")
                    }
                }
            )
        }
    ) { innerPadding ->
        FruitView(
            fruit = fruit,
            carbs = carbs,
            isShaking = isShaking,
            hasNotificationPermission = hasNotificationPermission,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

//This is fruit screen
@Composable
fun FruitView(
    fruit: Fruit,
    carbs: Int?,
    isShaking: Boolean = false,
    hasNotificationPermission: Boolean = false,
    modifier: Modifier = Modifier
) {
    val message = stringResource(R.string.fruit_carb_message, fruit.name, carbs?.toString() ?: "??")

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(
                if (hasNotificationPermission) R.string.notification_permission_granted
                else R.string.notification_permission_denied
            ),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (hasNotificationPermission) Color(0xFF2E7D32) else Color(0xFFC62828),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                //This shows ? or fruit icon
                Text(
                    text = if (isShaking) "❓" else fruit.icon,
                    fontSize = 120.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                //This shows shaking text or fruit carbs
                Text(
                    text = if (isShaking) stringResource(R.string.shaking_message) else message,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
