package com.cease.io

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.PI
import kotlin.math.sin

val SetupBlue = Color(0xFF0277BD)
private val SetupYellow = Color(0xFFFFD600)
private val SetupDialogGrey = Color(0xFF424242)

private enum class SetupState { OK, WARN, ERROR }

private class SetupItem(
    val title: String,
    val hint: String,
    val state: SetupState,
    val onClick: () -> Unit,
)

@Composable
fun SetupScreen(vm: MainViewModel) {
    BackHandler(enabled = vm.setupPage == 1 || vm.setupDone) {
        if (vm.setupDone) vm.closeSetup() else vm.setupBack()
    }
    Box(Modifier.fillMaxSize().background(SetupBlue)) {
        Snowfall()
        if (vm.setupPage == 0 && !vm.setupDone) WelcomePage(vm) else ChecklistPage(vm)
    }
}

private class Flake(
    val x: Float,
    val sizeSp: Int,
    val durationMs: Int,
    val phase: Float,
    val swayDp: Int,
    val alpha: Float,
    val spin: Float,
)

private val flakes = listOf(
    Flake(0.04f, 64, 16000, 0.05f, 22, 0.20f, 0.5f),
    Flake(0.18f, 34, 11000, 0.55f, 14, 0.28f, -1f),
    Flake(0.30f, 90, 20000, 0.30f, 26, 0.14f, 0.4f),
    Flake(0.42f, 28, 9500, 0.80f, 10, 0.30f, 1f),
    Flake(0.52f, 52, 14500, 0.15f, 20, 0.22f, -0.6f),
    Flake(0.64f, 38, 12000, 0.65f, 16, 0.26f, 1f),
    Flake(0.74f, 110, 22000, 0.45f, 30, 0.12f, -0.3f),
    Flake(0.86f, 30, 10000, 0.90f, 12, 0.30f, -1f),
    Flake(0.92f, 56, 15500, 0.25f, 18, 0.20f, 0.7f),
    Flake(0.10f, 24, 8500, 0.40f, 8, 0.32f, 1f),
    Flake(0.58f, 22, 8000, 0.10f, 8, 0.30f, -1f),
    Flake(0.36f, 44, 13000, 0.95f, 18, 0.22f, 0.8f),
)

@Composable
private fun Snowfall() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        flakes.forEach { FlakeView(it, wPx, hPx) }
    }
}

@Composable
private fun FlakeView(f: Flake, wPx: Float, hPx: Float) {
    val transition = rememberInfiniteTransition(label = "snow")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(f.durationMs, easing = LinearEasing),
            initialStartOffset = StartOffset((f.durationMs * f.phase).toInt(), StartOffsetType.FastForward),
        ),
        label = "fall",
    )
    val sizePx = with(LocalDensity.current) { f.sizeSp.sp.toPx() * 1.3f }
    Text(
        "\u2744\uFE0E",
        color = Color.White.copy(alpha = f.alpha),
        fontSize = f.sizeSp.sp,
        modifier = Modifier.graphicsLayer {
            translationX = f.x * wPx + sin(progress * 2f * PI.toFloat() * 2f) * f.swayDp.dp.toPx()
            translationY = -sizePx + progress * (hPx + sizePx)
            rotationZ = progress * 360f * f.spin
        },
    )
}

@Composable
private fun BoxScope.WelcomePage(vm: MainViewModel) {
    Column(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Welcome to Cease", color = Color.White, fontSize = FZType.display, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(
            "A box to freeze and store the apps you rarely use.",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = FZType.body,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { vm.setupNext() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SetupYellow, contentColor = Color.Black),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Get started", fontSize = FZType.body, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ChecklistPage(vm: MainViewModel) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var askedNotif by remember { mutableStateOf(false) }
    var rootBusy by remember { mutableStateOf(false) }
    var rootFailed by remember { mutableStateOf(false) }
    val resume = vm.resumeTick

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    val rootGranted = vm.rootOk == true
    val notifGranted = remember(tick, resume) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val batteryOk = remember(tick, resume) {
        (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)
    }

    val items = listOf(
        SetupItem(
            title = "Grant root access",
            hint = if (rootBusy) "Waiting for your root manager…" else "Required. Tap to allow Cease in Magisk / KernelSU",
            state = if (rootGranted) SetupState.OK else SetupState.ERROR,
            onClick = {
                if (!rootBusy) {
                    rootBusy = true
                    vm.checkRoot { ok -> rootBusy = false; if (!ok) rootFailed = true }
                }
            },
        ),
        SetupItem(
            title = "Allow background running",
            hint = "On some phones, Cease needs \"unrestricted background running\" so Auto Freeze keeps working",
            state = if (batteryOk) SetupState.OK else SetupState.WARN,
            onClick = {
                val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:${ctx.packageName}"))
                if (runCatching { ctx.startActivity(request) }.isFailure) {
                    runCatching { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                }
            },
        ),
        SetupItem(
            title = "Enable notification permission",
            hint = "Lets the Auto Freeze service keep running in the background",
            state = if (notifGranted) SetupState.OK else SetupState.WARN,
            onClick = {
                if (!askedNotif) {
                    askedNotif = true
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    runCatching {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName),
                        )
                    }
                }
            },
        ),
    )

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)) {
            Text("Set up Cease", color = Color.White, fontSize = FZType.display, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text(
                "To freeze apps, Cease needs root access.",
                color = Color.White,
                fontSize = FZType.body,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Cease only freezes the apps you choose, and only when you ask it to or when Auto Freeze is on.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = FZType.secondary,
            )
            Spacer(Modifier.height(20.dp))
            ModeChoice(vm)
            items.forEach { SetupItemRow(it) }
        }

        Button(
            onClick = { vm.finishSetup() },
            enabled = rootGranted,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp).height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SetupYellow,
                contentColor = Color.Black,
                disabledContainerColor = Color.White.copy(alpha = 0.15f),
                disabledContentColor = Color.White.copy(alpha = 0.45f),
            ),
        ) {
            Text(if (vm.setupDone) "Done" else "Finish setup", fontSize = FZType.body, fontWeight = FontWeight.Medium)
        }
    }

    if (rootFailed) {
        CeaseDialog(
            onDismiss = { rootFailed = false },
            title = "Couldn't get root permission",
            containerColor = SetupDialogGrey,
            inkColor = Color.White,
            confirmButton = { DialogButton("OK", { rootFailed = false }) },
        ) {
            Text(
                "Please ensure your device has been rooted, and allow Cease in your root manager (Magisk / KernelSU).",
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ModeChoice(vm: MainViewModel) {
    Text("What should Cease do with your apps?", color = Color.White, fontSize = FZType.title, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(4.dp))
    Mode.entries.forEach { m ->
        Row(
            Modifier.fillMaxWidth().clickable { vm.updateDefaultMode(m) }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RadioButton(
                selected = vm.defaultMode == m,
                onClick = { vm.updateDefaultMode(m) },
                colors = RadioButtonDefaults.colors(selectedColor = SetupYellow, unselectedColor = Color.White),
            )
            Column(Modifier.padding(start = 8.dp, top = 2.dp)) {
                Text(m.label, color = Color.White, fontSize = FZType.body, fontWeight = FontWeight.Medium)
                Text(
                    if (m == Mode.FREEZE) "Disables the app until you open it from Cease."
                    else "Stops the app and its background work. It stays enabled.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = FZType.secondary,
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun SetupItemRow(item: SetupItem) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = item.state != SetupState.OK, onClick = item.onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        StatusIcon(item.state)
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = Color.White, fontSize = FZType.title, fontWeight = FontWeight.Medium)
            if (item.state != SetupState.OK) {
                Text(
                    item.hint,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = FZType.secondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusIcon(state: SetupState) {
    val color = when (state) {
        SetupState.OK -> StatusGreen
        SetupState.WARN -> StatusAmber
        SetupState.ERROR -> StatusRed
    }
    Box(
        Modifier.padding(top = 2.dp).size(28.dp).border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            SetupState.OK -> Icon(Icons.Filled.Check, contentDescription = "Done", tint = color, modifier = Modifier.size(18.dp))
            SetupState.WARN -> Text("!", color = color, fontSize = FZType.body, fontWeight = FontWeight.Bold)
            SetupState.ERROR -> Icon(Icons.Filled.Close, contentDescription = "Missing", tint = color, modifier = Modifier.size(18.dp))
        }
    }
}
