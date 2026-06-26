package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Schedule
import com.example.ui.theme.SoundSchedulerTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SoundSchedulerViewModel = viewModel()
            val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()

            MaterialTheme(colorScheme = currentTheme.toColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: SoundSchedulerViewModel) {
    var currentTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Smart AI, 2: Analytics, 3: Settings
    val activeNotification by viewModel.activeNotification.collectAsStateWithLifecycle()
    val activeDetails by viewModel.activeNotificationDetails.collectAsStateWithLifecycle()
    val activeType by viewModel.activeNotificationType.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = "Schedules") },
                    label = { Text("Schedules") },
                    modifier = Modifier.testTag("tab_schedules")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Smart AI") },
                    label = { Text("Smart AI") },
                    modifier = Modifier.testTag("tab_smart_ai")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                    label = { Text("Stats") },
                    modifier = Modifier.testTag("tab_stats")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile & Settings") },
                    label = { Text("Profile") },
                    modifier = Modifier.testTag("tab_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> DashboardTab(viewModel)
                1 -> SmartCalendarTab(viewModel)
                2 -> AnalyticsTab(viewModel)
                3 -> ProfileSettingsTab(viewModel)
            }

            // Real-Time Simulation Notification Overlay Card
            AnimatedVisibility(
                visible = activeNotification != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                activeNotification?.let { title ->
                    val isVibrate = activeType == "VIBRATE"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.dismissNotification() },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isVibrate) Color(0xFF2C2C2C) else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isVibrate) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isVibrate) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isVibrate) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (isVibrate) Color.LightGray else MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = activeDetails ?: "",
                                    fontSize = 14.sp,
                                    color = if (isVibrate) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.dismissNotification() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = if (isVibrate) Color.LightGray else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SoundwaveAnimatedBanner(
    isLoggedIn: Boolean,
    userName: String,
    masterPause: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "soundwave")
    
    // Animate the offset/phase of the waves
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )
    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (-2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )
    val wavePhase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase3"
    )

    // Pulse animation for the glowing node
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        // Ambient Moving Waves
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            val midY = height / 2f
            
            // Draw wave 1 (Primary)
            val path1 = Path()
            path1.moveTo(0f, midY)
            for (x in 0..width.toInt() step 5) {
                val dx = x.toFloat()
                // A dynamic amplitude that tapers towards the edges
                val factor = Math.sin((dx / width) * Math.PI).toFloat()
                val dy = midY + (Math.sin((dx * 2 * Math.PI / width) + wavePhase1).toFloat() * 25f * factor)
                path1.lineTo(dx, dy)
            }
            drawPath(
                path = path1,
                color = primaryColor.copy(alpha = 0.35f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw wave 2 (Secondary)
            val path2 = Path()
            path2.moveTo(0f, midY)
            for (x in 0..width.toInt() step 5) {
                val dx = x.toFloat()
                val factor = Math.sin((dx / width) * Math.PI).toFloat()
                val dy = midY + (Math.sin((dx * 3 * Math.PI / width) + wavePhase2).toFloat() * 15f * factor)
                path2.lineTo(dx, dy)
            }
            drawPath(
                path = path2,
                color = secondaryColor.copy(alpha = 0.25f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw wave 3 (Tertiary)
            val path3 = Path()
            path3.moveTo(0f, midY)
            for (x in 0..width.toInt() step 5) {
                val dx = x.toFloat()
                val factor = Math.sin((dx / width) * Math.PI).toFloat()
                val dy = midY + (Math.sin((dx * 1.5 * Math.PI / width) + wavePhase3).toFloat() * 10f * factor)
                path3.lineTo(dx, dy)
            }
            drawPath(
                path = path3,
                color = tertiaryColor.copy(alpha = 0.2f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Draw a glowing dynamic status light on the right edge/center
            val glowX = width - 60.dp.toPx()
            val glowY = midY
            drawCircle(
                color = if (masterPause) Color.Red.copy(alpha = 0.15f * glowScale) else primaryColor.copy(alpha = 0.2f * glowScale),
                radius = 35.dp.toPx() * glowScale,
                center = androidx.compose.ui.geometry.Offset(glowX, glowY)
            )
            drawCircle(
                color = if (masterPause) Color.Red.copy(alpha = 0.3f * glowScale) else primaryColor.copy(alpha = 0.4f * glowScale),
                radius = 20.dp.toPx() * glowScale,
                center = androidx.compose.ui.geometry.Offset(glowX, glowY)
            )
            drawCircle(
                color = if (masterPause) Color.Red else primaryColor,
                radius = 8.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(glowX, glowY)
            )
        }

        // Card Content on top
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (masterPause) Color.Red.copy(alpha = 0.12f) 
                            else primaryColor.copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (masterPause) Color.Red else primaryColor)
                        )
                        Text(
                            text = if (masterPause) "DISTRACTION FILTER PAUSED" else "OBSTACLE FILTER ACTIVE",
                            color = if (masterPause) Color.Red else primaryColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = if (isLoggedIn) "Namaste, $userName! 🙏" else "Vighnharta Smart Sound",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Acoustic peace & automated quiet hours.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.width(72.dp)) // Reserve space for the glowing dot graphic on the right
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTab(viewModel: SoundSchedulerViewModel) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val masterPause by viewModel.masterPause.collectAsStateWithLifecycle()
    val manualOverride by viewModel.manualOverride.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High quality Animated Waveform/Soundwave Banner as requested
        item {
            SoundwaveAnimatedBanner(
                isLoggedIn = isLoggedIn,
                userName = userName,
                masterPause = masterPause
            )
        }

        // Quick Master Actions Row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (masterPause) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = if (masterPause) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (masterPause) "Schedules Suspended" else "Schedules Running",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (masterPause) "Master Pause is ON" else "Auto-switching is active",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = !masterPause,
                            onCheckedChange = { viewModel.toggleMasterPause() },
                            modifier = Modifier.testTag("master_pause_switch")
                        )
                    }

                    if (manualOverride != "NONE") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Manual Mode: $manualOverride",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Button(
                                onClick = { viewModel.clearManualOverride() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Resume Auto", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Manual Switch Quick overrides (Direct sound vibrate controls)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setManualOverride("VIBRATE") },
                    colors = CardDefaults.cardColors(
                        containerColor = if (manualOverride == "VIBRATE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.VolumeMute, contentDescription = "Force Vibrate")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Force Vibrate", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Mutes sound immediately", fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setManualOverride("SOUND") },
                    colors = CardDefaults.cardColors(
                        containerColor = if (manualOverride == "SOUND") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Force Sound")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Force Sound", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Enables ringer immediately", fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // Section Title: Quick Presets
        item {
            Text(
                text = "Context-Aware Profiles (Quick Activation)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Profiles row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf("🏢 Work Profile", "🏋️ Gym Profile", "😴 Sleep Profile", "📚 Student Mode", "🎬 Movie Mode")
                presets.forEach { profile ->
                    Button(
                        onClick = { viewModel.addProfilePreset(profile) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(profile, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Section Title: Simulated Location GPS Geofencing
        item {
            Text(
                text = "📍 Simulated GPS Geofencing",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            val currentLocationName by viewModel.currentLocationName.collectAsStateWithLifecycle()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Location Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Simulating GPS coordinate stream",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentLocationName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tap to move and trigger automatic geofenced silence zones:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val locs = listOf("🏠 Home", "🏢 Office", "🏋️ Gym", "🌳 Unknown")
                        locs.forEach { loc ->
                            val isSelected = currentLocationName == loc
                            Button(
                                onClick = { viewModel.simulateLocation(loc) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(loc, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Active Schedules
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Schedules (${schedules.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.testTag("add_schedule_button")
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Add Schedule", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (schedules.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No Schedules Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Create custom triggers, or apply a Quick Profile to automate sound toggles.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(schedules) { schedule ->
                ScheduleCard(schedule = schedule, viewModel = viewModel)
            }
        }
    }

    if (showCreateDialog) {
        CreateEditScheduleDialog(
            schedule = null,
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
fun ScheduleCard(schedule: Schedule, viewModel: SoundSchedulerViewModel) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showEditDialog = true },
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = schedule.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (schedule.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.VolumeMute,
                            contentDescription = "Vibrate Time",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = " ${schedule.vibrateTime} ",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "Sound Time",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = " ${schedule.soundTime}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
                Switch(
                    checked = schedule.isEnabled,
                    onCheckedChange = { viewModel.saveSchedule(schedule.copy(isEnabled = it)) {} },
                    modifier = Modifier.testTag("toggle_schedule_${schedule.id}")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active Days display
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                    val activeDays = schedule.days.split(",")
                    allDays.forEach { day ->
                        val isActive = activeDays.contains(day)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.first().toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Custom notification text badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = schedule.customMessage,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (schedule.latitude != null && schedule.longitude != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location Trigger",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Geofencing Active: ${schedule.locationName ?: "Office"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        CreateEditScheduleDialog(
            schedule = schedule,
            viewModel = viewModel,
            onDismiss = { showEditDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditScheduleDialog(
    schedule: Schedule?,
    viewModel: SoundSchedulerViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(schedule?.name ?: "Office Hour Focus") }
    var vibrateHour by remember { mutableStateOf(schedule?.vibrateTime?.split(":")?.getOrNull(0) ?: "09") }
    var vibrateMinute by remember { mutableStateOf(schedule?.vibrateTime?.split(":")?.getOrNull(1) ?: "00") }
    var soundHour by remember { mutableStateOf(schedule?.soundTime?.split(":")?.getOrNull(0) ?: "17") }
    var soundMinute by remember { mutableStateOf(schedule?.soundTime?.split(":")?.getOrNull(1) ?: "00") }
    var customMessage by remember { mutableStateOf(schedule?.customMessage ?: "Silence is golden") }
    var selectedDays by remember { mutableStateOf(schedule?.days?.split(",")?.toSet() ?: setOf("MON", "TUE", "WED", "THU", "FRI")) }

    // Geofencing parameters
    var isGeofenceEnabled by remember { mutableStateOf(schedule?.latitude != null) }
    var locationName by remember { mutableStateOf(schedule?.locationName ?: "My Office") }
    var radiusValue by remember { mutableStateOf(schedule?.radius?.toFloat() ?: 150f) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = if (schedule == null) "New Schedule" else "Edit Schedule",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Schedule Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("schedule_name_input")
                    )
                }

                item {
                    Text("Trigger Settings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🔇 Vibrate Time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = vibrateHour,
                                    onValueChange = { if (it.length <= 2) vibrateHour = it },
                                    modifier = Modifier.width(55.dp),
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                                )
                                Text(" : ", fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = vibrateMinute,
                                    onValueChange = { if (it.length <= 2) vibrateMinute = it },
                                    modifier = Modifier.width(55.dp),
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("🔔 Sound Return", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = soundHour,
                                    onValueChange = { if (it.length <= 2) soundHour = it },
                                    modifier = Modifier.width(55.dp),
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                                )
                                Text(" : ", fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = soundMinute,
                                    onValueChange = { if (it.length <= 2) soundMinute = it },
                                    modifier = Modifier.width(55.dp),
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                                )
                            }
                        }
                    }
                }

                item {
                    Text("Select Days", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                        allDays.forEach { day ->
                            val isSelected = selectedDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.first().toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = customMessage,
                        onValueChange = { customMessage = it },
                        label = { Text("Context Popup Alert Message") },
                        placeholder = { Text("e.g. Muted for standup call") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // GPS / Location Geofencing toggle as requested by geofencing feature
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Location-Based Trigger", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                        Switch(
                            checked = isGeofenceEnabled,
                            onCheckedChange = { isGeofenceEnabled = it }
                        )
                    }
                }

                if (isGeofenceEnabled) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = locationName,
                                onValueChange = { locationName = it },
                                label = { Text("Geofence Location Label") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Geofence Radius: ${radiusValue.toInt()}m", fontSize = 12.sp)
                            Slider(
                                value = radiusValue,
                                onValueChange = { radiusValue = it },
                                valueRange = 50f..500f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (schedule != null) {
                            TextButton(
                                onClick = {
                                    viewModel.deleteSchedule(schedule)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank() && selectedDays.isNotEmpty()) {
                                    val safeSchedule = Schedule(
                                        id = schedule?.id ?: 0,
                                        name = name,
                                        vibrateTime = "$vibrateHour:$vibrateMinute",
                                        soundTime = "$soundHour:$soundMinute",
                                        days = selectedDays.joinToString(","),
                                        customMessage = customMessage,
                                        latitude = if (isGeofenceEnabled) 37.7749 else null,
                                        longitude = if (isGeofenceEnabled) -122.4194 else null,
                                        locationName = if (isGeofenceEnabled) locationName else null,
                                        radius = if (isGeofenceEnabled) radiusValue.toDouble() else null
                                    )
                                    viewModel.saveSchedule(safeSchedule) {}
                                    onDismiss()
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartCalendarTab(viewModel: SoundSchedulerViewModel) {
    val isAnalyzing by viewModel.isAnalyzingCalendar.collectAsStateWithLifecycle()
    var calendarInputText by remember { mutableStateOf("") }

    val presetAgendas = listOf(
        "Standup meeting from 10:00 AM to 11:00 AM, Mon-Fri. Gym session at 6:00 PM to 7:00 PM on Monday, Wednesday, Friday.",
        "Calculus class on Tuesday and Thursday from 2:00 PM to 3:30 PM. Group project work on Thursday night from 8:00 PM to 10:00 PM.",
        "Dentist visit on Monday 3:00 PM to 4:00 PM. Weekend yoga meditation Saturday 9:00 AM to 10:30 AM."
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Smart Calendar AI Sync",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Input meeting details or copy-paste calendar text. Vighnharta AI will automatically build custom silence schedules.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = calendarInputText,
                        onValueChange = { calendarInputText = it },
                        label = { Text("Calendar Text / Agenda Notes") },
                        placeholder = { Text("Paste here e.g., Scrum meeting 10am-11am Monday through Friday...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("calendar_agenda_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isAnalyzing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Analyzing with Gemini AI...", fontSize = 13.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.importCalendarAgenda(calendarInputText)
                                calendarInputText = ""
                            },
                            enabled = calendarInputText.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_calendar_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Extract & Schedule")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Quick Mock Templates",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(presetAgendas) { preset ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { calendarInputText = preset },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = preset,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AnalyticsTab(viewModel: SoundSchedulerViewModel) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    val totalTriggers = history.filter { it.actionType.contains("ACTIVATED") }.size
    val estimatedBatterySavings = totalTriggers * 0.12f // 0.12% saved per trigger switch
    val timeSpentVibrateMin = schedules.filter { it.isEnabled }.sumOf { sched ->
        val partsVibrate = sched.vibrateTime.split(":")
        val partsSound = sched.soundTime.split(":")
        val vMin = (partsVibrate.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (partsVibrate.getOrNull(1)?.toIntOrNull() ?: 0)
        val sMin = (partsSound.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (partsSound.getOrNull(1)?.toIntOrNull() ?: 0)
        val diff = sMin - vMin
        if (diff > 0) diff else 0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Analytics & Sound Report",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Track your silent focus habits and phone health.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Stats card row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Activations", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("$totalTriggers Times", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Est. Battery Saved", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(String.format("%.2f%%", estimatedBatterySavings), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Quiet Time/Day", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${timeSpentVibrateMin / 60}h ${timeSpentVibrateMin % 60}m", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Beautiful custom Canvas-drawn chart as per frontend-design guidelines!
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Sound Modes Trend (Mon - Sun)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val colorPrimary = MaterialTheme.colorScheme.primary
                    val colorSurface = MaterialTheme.colorScheme.surfaceVariant
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        val days = listOf("M", "T", "W", "T", "F", "S", "S")
                        val hoursMuted = listOf(4f, 6f, 5f, 7f, 3f, 1f, 2f) // Mock stats
                        val stepX = size.width / (days.size + 1)
                        val maxVal = 10f

                        // Draw Grid lines
                        for (i in 1..4) {
                            val y = size.height * (i / 5f)
                            drawLine(
                                color = colorSurface,
                                start = androidx.compose.ui.geometry.Offset(0f, y),
                                end = androidx.compose.ui.geometry.Offset(size.width, y),
                                strokeWidth = 1f
                            )
                        }

                        // Draw Bar charts
                        hoursMuted.forEachIndexed { idx, value ->
                            val x = stepX * (idx + 1)
                            val barHeight = size.height * (value / maxVal)
                            val barWidth = 24.dp.toPx()
                            val topY = size.height - barHeight

                            drawRoundRect(
                                color = colorPrimary,
                                topLeft = androidx.compose.ui.geometry.Offset(x - barWidth / 2, topY),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        days.forEach {
                            Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historical Event Logs",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = { viewModel.clearLogHistory() }) {
                    Text("Clear Logs", fontSize = 12.sp)
                }
            }
        }

        if (history.isEmpty()) {
            item {
                Text(
                    "No historic silent switches logged yet.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            items(history) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = log.scheduleName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            val dateStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                            Text(
                                text = dateStr,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = log.details,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSettingsTab(viewModel: SoundSchedulerViewModel) {
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val vipContacts by viewModel.vipContacts.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()

    var inputName by remember { mutableStateOf(userName) }
    var inputEmail by remember { mutableStateOf(userEmail) }
    var inputVip by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Profile & App Customization",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Manage your sync credentials, VIP contacts, and color theme.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Profile Section Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sync Profile Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("Cloud Sync Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { viewModel.login(inputName, inputEmail) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }

        // Premium Themes section
        item {
            Text(
                text = "Premium Themes Selection",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val listOfThemes = SoundSchedulerTheme.values()
                listOfThemes.forEach { theme ->
                    val isSelected = currentTheme == theme
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setTheme(theme) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(theme.iconEmoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(theme.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(if (theme.isDark) "Dark mode colors" else "Light mode colors", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // VIP Contacts / Bypass list as requested by emergency bypass feature
        item {
            Text(
                text = "Emergency Override (VIP Whitelist)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("These critical contacts bypass Vibrate mode and ring through with full sound.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputVip,
                            onValueChange = { inputVip = it },
                            label = { Text("Contact Name / Number") },
                            placeholder = { Text("e.g. Mom, Boss") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (inputVip.isNotBlank()) {
                                    viewModel.addVipContact(inputVip)
                                    inputVip = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text("Add")
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    vipContacts.forEach { contact ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(contact, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            IconButton(onClick = { viewModel.removeVipContact(contact) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Snooze / Test switches helper
        item {
            Text(
                text = "Mock Simulator Controls",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Snooze Return Times Test", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { viewModel.snoozeSoundReturn(10) }, modifier = Modifier.weight(1f)) {
                            Text("+10 Min")
                        }
                        Button(onClick = { viewModel.snoozeSoundReturn(20) }, modifier = Modifier.weight(1f)) {
                            Text("+20 Min")
                        }
                        Button(onClick = { viewModel.snoozeSoundReturn(30) }, modifier = Modifier.weight(1f)) {
                            Text("+30 Min")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Test Trigger Notifications", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.activateMode(vibrate = true, scheduleName = "Office Hour", detail = "🔇 Mute mode activated. Silence is golden!") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Vibrate")
                        }
                        Button(
                            onClick = { viewModel.activateMode(vibrate = false, scheduleName = "Office Hour", detail = "🔔 Ringer activated! 0 missed alerts from whitelisted contacts.") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Sound")
                        }
                    }
                }
            }
        }
    }
}
