package com.example.subskill.screen

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.concurrent.TimeUnit

@Composable
fun MainScreen(usageViewModel: UsageViewModel = viewModel()) {
    val tabs = listOf("7日", "30日", "90日", "解約候補")
    var selectedTab by remember { mutableIntStateOf(1) }
    val hasPermission by usageViewModel.hasPermission.collectAsState()
    val context = LocalContext.current
    var selectedApp by remember { mutableStateOf<AppUsageData?>(null) }

    LaunchedEffect(Unit) {
        usageViewModel.checkPermission()
    }

    if (selectedApp != null) {
        DetailScreen(
            app = selectedApp!!,
            onSave = { serviceName, monthlyFee ->
                usageViewModel.updateAppSettings(selectedApp!!.packageName, serviceName, monthlyFee)
                selectedApp = null
            },
            onCancel = { selectedApp = null },
            onToggleCandidate = {
                usageViewModel.toggleCandidate(selectedApp!!.packageName)
                selectedApp = null
            }
        )
        return
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        // ヘッダー
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "アプリ使用状況",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // タブ
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        if (!hasPermission) {
            PermissionScreen {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                usageViewModel.checkPermission()
            }
        } else {
            when (selectedTab) {
                0 -> UsageListScreen(days = 7, viewModel = usageViewModel, onAppClick = { selectedApp = it })
                1 -> UsageListScreen(days = 30, viewModel = usageViewModel, onAppClick = { selectedApp = it })
                2 -> UsageListScreen(days = 90, viewModel = usageViewModel, onAppClick = { selectedApp = it })
                3 -> CandidateScreen(viewModel = usageViewModel, onAppClick = { selectedApp = it })
            }
        }
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "利用状況へのアクセス権限が必要です",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRequestPermission) {
                Text("権限を設定する")
            }
        }
    }
}

@Composable
fun UsageListScreen(days: Int, viewModel: UsageViewModel, onAppClick: (AppUsageData) -> Unit) {
    val usageData by viewModel.usageData.collectAsState()

    LaunchedEffect(days) {
        viewModel.loadUsageData(days)
    }

    if (usageData.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(usageData) { app ->
                AppCard(app = app, onClick = { onAppClick(app) })
            }
        }
    }
}

@Composable
fun AppCard(app: AppUsageData, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon: Drawable? = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    val lastUsedText = remember(app.lastUsed) {
        if (app.lastUsed <= 0L) "不明" else {
            val diffDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - app.lastUsed)
            when {
                diffDays == 0L -> "今日"
                diffDays == 1L -> "昨日"
                else -> "${diffDays}日前"
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isCandidate)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // アイコン
            if (icon != null) {
                Image(
                    bitmap = icon.toBitmap(width = 48, height = 48).asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.take(1),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // アプリ情報
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatChip(label = "使用", value = "${app.totalTimeMinutes}分")
                    StatChip(label = "起動", value = "${app.launchCount}回")
                    StatChip(label = "最終", value = lastUsedText)
                }
                app.monthlyFee?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "¥$it / 月",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CandidateScreen(viewModel: UsageViewModel, onAppClick: (AppUsageData) -> Unit) {
    val usageData by viewModel.usageData.collectAsState()
    val candidates = usageData.filter { it.isCandidate }

    if (candidates.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🎉", fontSize = 40.sp)
                Text(
                    "解約候補はありません",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(candidates) { app ->
                AppCard(app = app, onClick = { onAppClick(app) })
            }
        }
    }
}