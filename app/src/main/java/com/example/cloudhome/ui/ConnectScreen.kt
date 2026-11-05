package com.example.cloudhome.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.cloudhome.data.ServerConfig

@Composable
fun ConnectScreen(
    saved: ServerConfig?,
    loading: Boolean,
    error: String?,
    onConnect: (ServerConfig) -> Unit,
) {
    var host by remember { mutableStateOf(saved?.host ?: "") }
    var username by remember { mutableStateOf(saved?.username ?: "") }
    var password by remember { mutableStateOf(saved?.password ?: "") }
    var share by remember { mutableStateOf(saved?.share ?: "Public") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .imePadding(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Connect to your WD My Cloud Home", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Device IP address") },
            placeholder = { Text("192.168.1.50") },
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username (WD account email)") },
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            enabled = !loading,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = share,
            onValueChange = { share = it },
            label = { Text("Share name") },
            placeholder = { Text("Public") },
            singleLine = true,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        )

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onConnect(ServerConfig(host.trim(), username.trim(), password, share.trim())) },
            enabled = !loading && host.isNotBlank() && username.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Connect")
            }
        }
    }
}
