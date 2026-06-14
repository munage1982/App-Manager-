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
    val tabs = listOf("30日", "90日", "全アプリ", "解約候補")
    var selectedTab by remember { mutableIntStateOf(0) }
    val hasPermission by usageViewModel.hasPermission.collectAsState()
    val context = LocalContext.current
    var selectedApp by remember { mutableStateOf<AppUsageData?>(null) }
    var currencySymbol by remember { mutableStateOf("¥") }
    var showCurrencyMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        usageViewModel.checkPermission()
    }

    if (selectedApp != null) {
        DetailScreen(
            app = selectedApp!!,
            currencySymbol = currencySymbol,
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
                text = "サブスキラー",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                OutlinedButton(
                    onClick = { showCurrencyMenu = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(currencySymbol, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(
                    expanded = showCurrencyMenu,
                    onDismissRequest = { showCurrencyMenu = false }
                ) {
                    listOf("¥", "$", "€").forEach { symbol ->
                        DropdownMenuItem(
                            text = { Text(symbol, fontSize = 16.sp) },
                            onClick = {
                                currencySymbol = symbol
                                showCurrencyMenu = false
                            }
                        )
                    }
                }
            }
        }

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
                0 -> SubscriptionListScreen(days = 30, viewModel = usageViewModel, onAppClick = { selectedApp = it }, currencySymbol = currencySymbol, showLaunchCount = true)
                1 -> SubscriptionListScreen(days = 90, viewModel = usageViewModel, onAppClick = { selectedApp = it }, currencySymbol = currencySymbol, showLaunchCount = false)
                2 -> AllAppsScreen(days = 30, viewModel = usageViewModel, onAppClick = { selectedApp = it }, currencySymbol = currencySymbol)
                3 -> CandidateScreen(viewModel = usageViewModel, onAppClick = { selectedApp = it }, currencySymbol = currencySymbol)
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

fun formatFee(amount: Double): String {
    return if (amount == kotlin.math.floor(amount)) {
        "%,.0f".format(amount)
    } else {
        "%,.2f".format(amount)
    }
}

@Composable
fun SubscriptionListScreen(
    days: Int,
    viewModel: UsageViewModel,
    onAppClick: (AppUsageData) -> Unit,
    currencySymbol: String,
    showLaunchCount: Boolean
) {
    val usageData by viewModel.usageData.collectAsState()

    LaunchedEffect(days) {
        viewModel.loadUsageData(days)
    }

    val subscriptions = usageData.filter { it.isSubscription }
    val totalMonthly = subscriptions.mapNotNull { it.monthlyFee }.sum()
    val totalYearly = totalMonthly * 12

    Column(modifier = Modifier.fillMaxSize()) {
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
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$currencySymbol${formatFee(totalMonthly)}",
                        fontSize = 24.sp,
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
                        text = "$currencySymbol${formatFee(totalYearly)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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
                    AppCard(
                        app = app,
                        onClick = { onAppClick(app) },
                        showLaunchCount = showLaunchCount,
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
    }
}

@Composable
fun AllAppsScreen(
    days: Int,
    viewModel: UsageViewModel,
    onAppClick: (AppUsageData) -> Unit,
    currencySymbol: String
) {
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
                AppCard(
                    app = app,
                    onClick = { onAppClick(app) },
                    showLaunchCount = false,
                    currencySymbol = currencySymbol
                )
            }
        }
    }
}

@Composable
fun AppCard(
    app: AppUsageData,
    onClick: () -> Unit,
    showLaunchCount: Boolean = true,
    currencySymbol: String = "¥"
) {
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

    val usageText = if (app.totalTimeMinutes >= 60) {
        "${app.totalTimeMinutes / 60}時間${app.totalTimeMinutes % 60}分"
    } else {
        "${app.totalTimeMinutes}分"
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon.toBitmap(width = 56, height = 56).asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.take(1),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = usageText,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                app.monthlyFee?.let {
                    Text(
                        text = "$currencySymbol${formatFee(it)}/月",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = lastUsedText,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showLaunchCount) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${app.launchCount}回起動",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CandidateScreen(
    viewModel: UsageViewModel,
    onAppClick: (AppUsageData) -> Unit,
    currencySymbol: String
) {
    val usageData by viewModel.usageData.collectAsState()
    val candidates = usageData.filter { it.isCandidate }
    val totalMonthly = candidates.mapNotNull { it.monthlyFee }.sum()
    val totalYearly = totalMonthly * 12

    Column(modifier = Modifier.fillMaxSize()) {
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
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "$currencySymbol${formatFee(totalMonthly)}",
                        fontSize = 24.sp,
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
                        text = "$currencySymbol${formatFee(totalYearly)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
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
                    AppCard(
                        app = app,
                        onClick = { onAppClick(app) },
                        showLaunchCount = true,
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
    }
}