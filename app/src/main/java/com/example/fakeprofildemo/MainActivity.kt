package com.example.fakeprofildemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Profile(
    val name: String,
    val email: String,
    val username: String,
    val phone: String,
    val city: String,
    val country: String,
    val picture: String
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                ProfileScreen()
            }
        }
    }
}

private suspend fun fetchProfile(): Profile =
    withContext(Dispatchers.IO) {

        val url = URL("https://randomuser.me/api/")
        val connection =
            url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val response = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            val json = JSONObject(response)
            val user = json.getJSONArray("results")
                .getJSONObject(0)

            val name = user.getJSONObject("name")
            val location = user.getJSONObject("location")
            val login = user.getJSONObject("login")
            val picture = user.getJSONObject("picture")

            Profile(
                name = "${name.getString("first")} " +
                        name.getString("last"),
                email = user.getString("email"),
                username = login.getString("username"),
                phone = user.getString("phone"),
                city = location.getString("city"),
                country = location.getString("country"),
                picture = picture.getString("large")
            )
        } finally {
            connection.disconnect()
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {

    var profile by remember {
        mutableStateOf<Profile?>(null)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()

    fun loadProfile() {
        scope.launch {
            loading = true
            error = null

            try {
                profile = fetchProfile()
            } catch (e: Exception) {
                error = "Could not load the profile. Check your internet connection and try again."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Demo Profile") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            when {
                loading -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Loading profile...")
                }

                error != null -> {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(onClick = { loadProfile() }) {
                        Text("Try Again")
                    }
                }

                profile != null -> {
                    val user = profile!!

                    AsyncImage(
                        model = user.picture,
                        contentDescription = "Profile picture of ${user.name}",
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(24.dp))

                    ProfileDetail("Email", user.email)
                    ProfileDetail("Phone", user.phone)
                    ProfileDetail("City", user.city)
                    ProfileDetail("Country", user.country)

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { loadProfile() },
                        enabled = !loading
                    ) {
                        Text("Generate Another Profile")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDetail(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}