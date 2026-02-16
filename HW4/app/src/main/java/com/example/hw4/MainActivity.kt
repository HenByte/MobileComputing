package com.example.hw4

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.hw4.ui.theme.HW4Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val requestPermissionOnLauncher = registerForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationScheduler.scheduleBananaReminder(context = this)
        }
    }
    private fun requestNotificationPermissionAndSchedule() {
        NotificationScheduler.createNotificationChannel(this)
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
            PackageManager.PERMISSION_GRANTED -> {
                NotificationScheduler.scheduleBananaReminder(this)
            }
            else -> {
                requestPermissionOnLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HW4Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BananaView(modifier = Modifier.padding(innerPadding))
                }
            }
        }

        //This triggers the notification
        requestNotificationPermissionAndSchedule()
    }
}
@Composable
fun BananaView(
    modifier: Modifier = Modifier
) {
    var carbs by remember { mutableStateOf<Int?>(null) }
    //This is the ID for banana in Fineli.fi.
    val foodId = 28934L
    LaunchedEffect(foodId) {
        try {

            //This uses the Fineli to get the banana carbs.
            val response = withContext(Dispatchers.IO) {
                FineliModule.fineliApi.getFood(foodId)
            }

            //This is response for the fetch.
            carbs = response.carbohydratePer100g
        } catch (_: Exception) {
            carbs = null
        }
    }
    //This gets the message and adds the carbs or question marks
    val message = stringResource(R.string.banana_carb_message, carbs?.toString() ?: "??")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        //This shows a big banana emoji.
        Text(
            text = "\uD83C\uDF4C",
            fontSize = 120.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        // This shows the text
        Text(
            text = message,
            fontSize = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}