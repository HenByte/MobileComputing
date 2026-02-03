package com.example.mobilecomputing

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.mobilecomputing.ui.theme.MobileComputingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //This defines the visual appearance of the mobile application.
        val dataBase = HippoDatabase.getDatabase(context = this)
        setContent {
            MobileComputingTheme {
                ApplicationNavigation(dataBase = dataBase)
            }
        }
    }
}
//This handles all the navigation of the application
@Composable
fun ApplicationNavigation(dataBase: HippoDatabase){
    val navController = rememberNavController()

    // haetaan tietokanta rajapinnat käyttöön
    val userDao = dataBase.userDao()
    val messageDao = dataBase.messageDao()

    //This defines all the possible views in the application and navigation between them.
    // Home defines the firts page.
    NavHost(navController = navController, startDestination = "Hippo"){
        //this names the view.
        composable(route = "Hippo"){
            HippoScreen(
                userDao = userDao,
                messageDao = messageDao,
                onHistoryClick = { userName -> navController.navigate(route="conversation/$userName")}
            )
        }

        composable(route = "conversation/{userName}"){ data ->
            //get the username from the arguments
            val userName = data.arguments?.getString("userName") ?: ""
            Conversation(userName, userDao, messageDao, onBack = {navController.popBackStack()})
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HippoScreen(userDao: UserDao, messageDao: MessageDao, onHistoryClick: (String) -> Unit) {
    val currentUser by userDao.getCurrent().collectAsState(initial = null)
    //This defines the structure of the view.
    Scaffold(
        topBar = {
            TopAppBar(
                title = {Text(text = "Hippo View")},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Magenta,
                    titleContentColor = Color.Black
                )
            )
        }
        //This tells where the elemetns should go.
    ) { innerPadding ->
        val user = currentUser
        if (user == null) {
            NoUser(userDao = userDao)
        } else {
            val coroutineScope = rememberCoroutineScope()
            var messageText by remember { mutableStateOf( value = "")  }
            Column(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                //This aligns the elements to the middle of the screen.
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                //This shows the profilepicture.
                AsyncImage(
                    model = user.imageUri.toImageUri(),
                    contentDescription = "Contact profile picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)


                )

                //Users name
                Text(
                    text = user.name,
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(height = 8.dp))

                // This is the messagefield.
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Message") },
                    modifier = Modifier.width(300.dp),
                    maxLines = 3
                )
                //This is the button to save the message.
                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            coroutineScope.launch {
                                val message = Message(
                                    userId = user.id,
                                    text = messageText
                                )
                                messageDao.insert(message)
                                messageText = ""
                            }
                        }
                    },
                    enabled = messageText.isNotBlank()
                ) {
                    Text("Save Message")
                }

                Spacer(modifier = Modifier.height(16.dp))

                //Button to show messages.
                Button(
                    onClick = {
                        onHistoryClick(user.name)
                    }
                ) {
                    Text("History")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // This button is to delete user
                Button(
                    onClick = {
                        coroutineScope.launch {
                            messageDao.deleteMessagesByUser(user.id)
                            userDao.delete(user)
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Delete User")
                }
            }
        }
    }

}

@Composable
fun NoUser(userDao: UserDao) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedImageUri by remember { mutableStateOf<Uri?>(value = null) }
    var userName by remember { mutableStateOf( value = "") }

    // this open a activity content picker that returns path to image
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }



    Column(
        modifier = Modifier.fillMaxSize(),
        //This aligns the elements to the middle of the screen.
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {

        // This defines placeholder for the profile image and when the picture is not picked it is just a button.
        Box(
            modifier = Modifier
                .size(120.dp)
                .border(3.dp, Color.Gray, CircleShape)
                .clip(CircleShape)
                .clickable {
                    // This lauches the image launcher.
                    imagePickerLauncher.launch("image/*")
                },
            contentAlignment = Alignment.Center
        ) {
            //If the picture is pick from the picker this shows it.
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Profile image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                //This shows the cirle to the for the profileimagepicker.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pick Image",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    // This is the text to add the username.
        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text("Name") },
            modifier = Modifier.width(300.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
//This is the save button.
        Button(
            onClick = {
                //This is to copy the image in the background in the appstorage.
                if (userName.isNotBlank() && selectedImageUri != null) {
                    val uri = selectedImageUri!!

                    // lunch a background task
                    coroutineScope.launch {
                        val copiedPath = withContext(Dispatchers.IO) {
                            context.copyImageToAppStorage(uri)
                        }
                        if (copiedPath != null) {

                            val user = User(
                                name = userName,
                                imageUri = copiedPath
                            )

                            //This is used to save the user.
                            userDao.insert(user)
                            selectedImageUri = null
                            userName = ""
                        }
                    }
                }
            },
            enabled = userName.isNotBlank() && selectedImageUri != null
        ) {
            Text("Save User")
        }
    }

}
// this copies image to apps storage
// NOTE: original idea from Chat GPT but this is  modified using the ideas from it
fun Context.copyImageToAppStorage(contentUri: Uri): String? {
    // use profileImages folder or create it
    val dir = File(filesDir, "profileImages").apply {
        mkdirs()
    }
    val extension = contentResolver
        .getType(contentUri)?.substringAfter("/") ?: "jpg"
    val file = File(dir, "${UUID.randomUUID()}.$extension")
    var path: String? = null
    try {
        // this part was modified based from Chat GTP, how to copy the image to the
        // app's storage
        contentResolver.openInputStream(contentUri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
            path = file.absolutePath
        }
    } catch (e: Exception) {

    }
    return path
}
fun String?.toImageUri(): Uri? {
    if (this.isNullOrBlank()) return null
    return if (startsWith("/")) {
        Uri.parse("file://$this")
    } else {
        Uri.parse(this)
    }
}
@Composable
    fun MessageCard(msg: Message, authorName: String, userImageUri: String?) {
        var isExpanded by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier
                .padding(all = 8.dp)
                // This makes the card clickable.
                .clickable { isExpanded = !isExpanded }
        ) {
            Row {
                //This shows the profile picture.
                AsyncImage(
                    model = userImageUri.toImageUri(),
                    contentDescription = "Contact profile picture",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop

                )
                Spacer(modifier = Modifier.width(16.dp))

                Column {

                    Spacer(modifier = Modifier.height(8.dp))
                    //This creates the text element for the author.
                    Text(
                        text = authorName,
                        color = Color.Black,
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // This creates the text element for the message.
                    Text(
                        text = msg.text,
                        color = Color(0xFFB39DDB),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Conversation(userName: String, userDao: UserDao, messageDao: MessageDao, onBack: () -> Unit) {
        var user by remember {mutableStateOf<User?>(null)}
        LaunchedEffect(Unit) {
            user = userDao.getCurrentOnce()
        }
        val userId = user?.id
        val messages = if (userId != null) {
            messageDao.getMessagesByUser(userId).collectAsState(initial = emptyList())
        } else {
            remember { mutableStateOf(emptyList())}
        }


        //This defines the structure of the view.
        Scaffold(
            //This is the navigation element which is on top of the page.
            topBar = {
                TopAppBar(
                    title = { Text(text = "Hippo View") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Magenta,
                        titleContentColor = Color.Black
                    ),
                    navigationIcon = {
                        //This is the back arrow button
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
            //This tells where the elemetns should go.
        ) { innerPadding ->
            LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize(),) {
                //This creates a scrollable list of messages.
                items(messages.value) { message ->
                    MessageCard(message, userName, user?.imageUri)
                }
            }
        }
    }

