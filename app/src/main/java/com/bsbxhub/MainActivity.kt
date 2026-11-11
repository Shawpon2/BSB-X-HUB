package com.bsbxhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val DEFAULT_SERVER = "https://YOUR_BACKEND_HOST"
private val http = OkHttpClient.Builder()
    .connectTimeout(12, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { HubScreen() } }
    }
}

@Composable
fun HubScreen() {
    var server by remember { mutableStateOf(DEFAULT_SERVER) }
    var token by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Not connected") }
    var createdToken by remember { mutableStateOf("") }
    var pendingId by remember { mutableStateOf("") }
    var permissions by remember { mutableStateOf(setOf("File transfer")) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun request(path: String, body: JSONObject, done: (JSONObject) -> Unit) {
        scope.launch {
            busy = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = server.trimEnd('/') + path
                    require(url.startsWith("https://")) { "Use an HTTPS backend URL." }
                    val req = Request.Builder().url(url)
                        .post(body.toString().toRequestBody("application/json".toMediaType())).build()
                    http.newCall(req).execute().use { response ->
                        val raw = response.body?.string().orEmpty()
                        if (!response.isSuccessful) error("Server ${response.code}: $raw")
                        JSONObject(raw)
                    }
                }
                done(result)
            } catch (e: Exception) { status = e.message ?: "Request failed" }
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("BSB-X-HUB", style = MaterialTheme.typography.headlineLarge)
        Text("Consent-based Android device pairing", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(server, { server = it }, label = { Text("Backend HTTPS URL") }, modifier = Modifier.fillMaxWidth())
        HorizontalDivider()
        Text("1 · Share Your Device", style = MaterialTheme.typography.titleLarge)
        Text("Choose capabilities. The device owner must approve each sensitive Android system permission separately.")
        listOf("File transfer", "Screen sharing (system consent required)", "Microphone (runtime consent required)", "Location (runtime consent required)").forEach { item ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(item in permissions, { checked -> permissions = if (checked) permissions + item else permissions - item })
                Text(item)
            }
        }
        Button(enabled = !busy && server.startsWith("https://") && permissions.isNotEmpty(), onClick = {
            val p = JSONObject().put("permissions", permissions.joinToString(","))
            request("/api/tokens", p) { result ->
                createdToken = result.optString("token")
                status = "Token created. Share it with the other device."
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Please wait…" else "CREATE NEW TOKEN") }
        if (createdToken.isNotBlank()) {
            Text("Active token: $createdToken", style = MaterialTheme.typography.titleMedium)
            Text("Treat this token like a password. It expires and can be used once.")
        }
        HorizontalDivider()
        Text("2 · Connect New Device", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(token, { token = it.uppercase() }, label = { Text("Enter active token") }, modifier = Modifier.fillMaxWidth())
        Button(enabled = !busy && token.isNotBlank() && server.startsWith("https://"), onClick = {
            request("/api/pair", JSONObject().put("token", token)) { result ->
                pendingId = result.optString("requestId")
                status = "Pairing request sent. The sharing device must approve it."
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("REQUEST CONNECTION") }
        if (pendingId.isNotBlank()) Text("Request ID: $pendingId")
        HorizontalDivider()
        Text("Session status", style = MaterialTheme.typography.titleLarge)
        Text(status)
        Text("This starter build intentionally does not provide covert access, remote power-off, silent recording, or unattended location/microphone capture. Pairing is only a request; the sharing device must approve it.")
        Text("Admin contact: blackspammerbd@gmail.com")
    }
}
