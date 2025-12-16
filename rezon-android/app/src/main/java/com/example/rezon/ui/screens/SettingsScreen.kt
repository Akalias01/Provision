package com.example.rezon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.rezon.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val wifiOnly by viewModel.wifiOnly.collectAsState()
    val keepService by viewModel.keepServiceActive.collectAsState()
    val stopOnClose by viewModel.stopOnClose.collectAsState()
    val showCover by viewModel.showCoverOnLock.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF050505))
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF050505))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsSection("DOWNLOADS")
            SettingsSwitch("Download over Wi-Fi only", wifiOnly) { viewModel.toggleWifiOnly(it) }

            Divider(Modifier.padding(vertical = 16.dp), color = Color.DarkGray)

            SettingsSection("PLAYER")
            SettingsSwitch("Keep playback service active on pause", keepService) { viewModel.toggleKeepService(it) }
            SettingsSwitch("Stop playback on close", stopOnClose) { viewModel.toggleStopOnClose(it) }
            SettingsSwitch("Show cover on lock screen", showCover) { viewModel.toggleShowCover(it) }

            Divider(Modifier.padding(vertical = 16.dp), color = Color.DarkGray)

            SettingsSection("DEBUGGING")
            Text("Version 1.0.0 (Build 1)", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        color = Color(0xFF00E5FF),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00E5FF),
                checkedTrackColor = Color(0xFF004D40)
            )
        )
    }
}
