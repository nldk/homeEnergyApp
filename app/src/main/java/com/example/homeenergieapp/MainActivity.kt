package com.example.homeenergieapp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.homeenergieapp.ui.theme.HomeEnergieAppTheme
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class MainActivity : ComponentActivity() {
    @SuppressLint("UnrememberedMutableState")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var text by mutableStateOf("")
        val host = ""
        enableEdgeToEdge()
        setContent {
            var amps by remember { mutableStateOf(0) }
            var with by remember { mutableStateOf(70.dp) }
            var checked by remember { mutableStateOf(true) }
            var slider by remember { mutableStateOf(0f) }
            val network = rememberCoroutineScope()
            val client = HttpClient(CIO) {
                install(Auth) {
                    basic {
                        credentials {
                            BasicAuthCredentials(
                                username = "",
                                password = ""
                            )
                        }
                        realm = "test"
                        sendWithoutRequest { request ->
                            request.url.host == host
                        }
                    }
                }
            }

            LaunchedEffect("test") {
                network.launch {
                    while (true) {
                        try {

                            checked = client.get("http://$host:8080/isAutoOn").bodyAsText()
                                .toBoolean()
                            amps = client.get("http://$host:8080/getAmps").bodyAsText().toInt()
                        } catch (e: Exception) {
                            text = e.toString()
                        }
                        delay(1000)
                    }
                }
            }
            LaunchedEffect(checked) {
                launch {
                    if (!checked) {
                        client.get("http://$host:8080/autoOff")
                    } else if (checked) {
                        client.get("http://$host:8080/autoOn")
                    }
                }
            }
            LaunchedEffect(slider,checked) {
                launch {
                    client.post("http://192.168.129.130:8080/setAmps"){
                        setBody(slider.toString())
                    }
                }
            }
            HomeEnergieAppTheme {
                Column {
                    Spacer(Modifier.height(40.dp))
                    Column {
                        Row() {
                            Spacer(Modifier.width(10.dp))
                            Text("laadpaal")
                        }
                        Row {
                            Spacer(Modifier.width(10.dp))
                            Box(Modifier.width(with)) {
                                Text("auto", Modifier.align(Alignment.CenterStart))
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        checked = it

                                    },
                                    Modifier.align(Alignment.CenterEnd),

                                    )
                            }
                        }
                    }
                    if (!checked) {
                        Row {
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Slider(
                                    value = slider,
                                    onValueChange = { slider = it },
                                    steps = 7,
                                    valueRange = 0f..32f,
                                    modifier = Modifier.width(380.dp)
                                )
                                Text(slider.toString() + "A")
                            }
                        }
                    }else{
                        Row {
                            Spacer(Modifier.width(10.dp))
                            Text(amps.toString()+"A")
                        }
                    }
                }
            }
        }
    }
}
