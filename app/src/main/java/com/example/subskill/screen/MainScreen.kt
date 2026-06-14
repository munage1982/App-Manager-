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
    val tabs = listOf("サブスク90日", "サブスク30日", "全アプリ", "解約候補")
    var selectedTab by remember { mutableIntStateOf(0) }
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
            },
            onToggleManualSubscription = {
                usageViewModel.toggleManualSubscription(selectedApp!!.packageName)
                selectedApp = null
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "subs-killer",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 0.dp
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
                0 -> SubscriptionListScreen(days = 90, viewModel = usageViewModel, onAppClick = { selectedApp = it })
                1 -> SubscriptionListScreen(days = 30, viewModel = usageViewModel, onAppClick = { selectedApp = it })
                2 -> AllAppsScreen(days = 90, viewModel = usageViewModel, onAppClick = { selectedApp = it })
                3 -> CandidateScreen(viewModel = usageViewModel, onAppClick = { selectedApp = it })
            }
        }
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
fun SubscriptionListScreen(days: Int, viewModel: UsageViewModel, onAppClick: (AppUsageData) -> Unit) {
    val usageData by viewModel.usageData.collectAsState()

    LaunchedEffect(days) {
        viewModel.loadUsageData(days)
    }

    val subscriptions = usageData.filter { it.isSubscription }
    val totalMonthly = subscriptions.mapNotNull { it.monthlyFee }.sum()
    val totalYearly = totalMonthly * 12

    Column(modifier = Modifier.fillMaxSize()) {
        if (totalMonthly > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "月額合計",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "¥${"%,d".format(totalMonthly)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "年額合計",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "¥${"%,d".format(totalYearly)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        if (subscriptions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subscriptions) { app ->
                    AppCard(app = app, onClick = { onAppClick(app) }, showLaunchCount = days != 90)
                }
            }
        }
    }
}

@Composable
fun AllAppsScreen(days: Int, viewModel: UsageViewModel, onAppClick: (AppUsageData) -> Unit) {
    val usageData by viewModel.usageData.collectAsState()

    LaunchedEffect(days) {
        viewModel.loadUsageData(days)
    }

    val allApps = usageData.filter { !it.isSubscription }

    if (allApps.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allApps) { app ->
                AppCard(app = app, onClick = { onAppClick(app) }, showLaunchCount = false)
            }
        }
    }
}

@Composable
fun AppCard(app: AppUsageData, onClick: () -> Unit, showLaunchCount: Boolean = true) {
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
                    StatChip(
                        label = "使用",
                        value = if (app.totalTimeMinutes >= 60) {
                            "${app.totalTimeMinutes / 60}時間${app.totalTimeMinutes % 60}分"
                        } else {
                            "${app.totalTimeMinutes}分"
                        }
                    )
                    if (showLaunchCount) {
                        StatChip(label = "起動", value = "${app.launchCount}回")
                    }
                    StatChip(label = "最終", value = lastUsedText)
                }
                app.monthlyFee?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "¥${"%,d".format(it)} / 月",
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
    val totalMonthly = candidates.mapNotNull { it.monthlyFee }.sum()
    val totalYearly = totalMonthly * 12

    Column(modifier = Modifier.fillMaxSize()) {
        if (totalMonthly > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "解約で節約できる月額",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "¥${"%,d".format(totalMonthly)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "年額換算",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "¥${"%,d".format(totalYearly)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(candidates) { app ->
                    AppCard(app = app, onClick = { onAppClick(app) })
                }
            }
        }
    }
}