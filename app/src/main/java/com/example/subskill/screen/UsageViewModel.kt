package com.example.subskill.screen

import android.app.AppOpsManager
import android.app.Application
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.subskill.data.AppDatabase
import com.example.subskill.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class AppUsageData(
    val packageName: String,
    val appName: String,
    val totalTimeMinutes: Long,
    val launchCount: Int,
    val lastUsed: Long,
    val monthlyFee: Double? = null,
    val isCandidate: Boolean = false,
    val isSubscription: Boolean = false,
    val isManualSubscription: Boolean = false
)

class UsageViewModel(application: Application) : AndroidViewModel(application) {

    private val _usageData = MutableStateFlow<List<AppUsageData>>(emptyList())
    val usageData: StateFlow<List<AppUsageData>> = _usageData

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.appSettingsDao()

    private val subscriptionDictionary = setOf(
        "com.netflix.mediaclient",
        "com.google.android.youtube",
        "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus",
        "com.apple.atve.androidtv.appletv",
        "tv.twitch.android.app",
        "com.hulu.plus",
        "tv.abema",
        "jp.unext.mediaplayer",
        "com.nttdocomo.android.danimeapp",
        "jp.nicovideo.nicovideo",
        "com.dmm.dmmtv",
        "com.dazn.app",
        "com.discovery.discoveryplus",
        "com.spotify.music",
        "com.google.android.apps.youtube.music",
        "com.apple.android.music",
        "com.amazon.mp3",
        "com.audible.application",
        "deezer.android.app",
        "jp.radiko.Player",
        "com.amazon.mShop.android.shopping",
        "com.google.android.apps.subscriptions.red",
        "com.dropbox.android",
        "com.microsoft.skydrive",
        "com.openai.chatgpt",
        "ai.perplexity.app.android",
        "com.google.android.apps.bard",
        "notion.id",
        "com.evernote",
        "com.canva.editor",
        "com.todoist.android.Todoist",
        "com.ticktick.task",
        "com.adobe.lrmobile",
        "com.adobe.reader",
        "com.duolingo",
        "com.babbel.mobile.android.en",
        "com.busuu.android.enc",
        "com.quizlet.quizletandroid",
        "com.myfitnesspal.android",
        "com.fitbit.FitbitMobile",
        "com.nike.plusgps",
        "com.zwift.android.prod",
        "com.strava",
        "com.tinder",
        "com.bumble.app",
        "jp.eureka.pairs",
        "jp.co.tapple.app",
        "jp.with.android",
        "com.amazon.kindle",
        "com.shueisha.jumpplus",
        "jp.naver.linewebtoon",
        "jp.co.nttdocomo.dmagazine",
        "jp.co.rakuten.rakutenmagazine",
        "jp.cmoa.app.smartphone",
        "jp.bookwalker.kreader.android.epub",
        "jp.kakao.piccoma",
        "com.cookpad.android.activities",
        "jp.co.navitime.app",
        "com.snow.android",
        "com.ubercab",
        "com.gamepass"
    )

    private fun hasUsagePermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun checkPermission() {
        _hasPermission.value = hasUsagePermission(getApplication())
    }

    fun loadUsageData(days: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            if (!hasUsagePermission(context)) {
                _hasPermission.value = false
                return@launch
            }
            _hasPermission.value = true

            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -days)
            val startTime = calendar.timeInMillis

            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            val launchCounts = mutableMapOf<String, Int>()
            val lastUsedMap = mutableMapOf<String, Long>()
            val lastForegroundTimeMap = mutableMapOf<String, Long>()
            val SESSION_GAP_MS = 30_000L

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        val lastTime = lastForegroundTimeMap[event.packageName] ?: 0L
                        if (event.timeStamp - lastTime > SESSION_GAP_MS) {
                            launchCounts[event.packageName] = (launchCounts[event.packageName] ?: 0) + 1
                        }
                        lastForegroundTimeMap[event.packageName] = event.timeStamp
                        if ((lastUsedMap[event.packageName] ?: 0L) < event.timeStamp) {
                            lastUsedMap[event.packageName] = event.timeStamp
                        }
                    }
                }
            }

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startTime,
                endTime
            )

            val totalTimeMap = stats
                .filter { it.totalTimeInForeground > 0 }
                .groupBy { it.packageName }
                .mapValues { (_, list) -> list.sumOf { it.totalTimeInForeground } }

            val savedSettings = dao.getAll().associateBy { it.packageName }
            val pm = context.packageManager

            val result = totalTimeMap.map { (packageName, totalTime) ->
                val saved = savedSettings[packageName]
                val defaultName = try {
                    pm.getApplicationLabel(
                        pm.getApplicationInfo(packageName, 0)
                    ).toString()
                } catch (e: Exception) {
                    packageName
                }
                AppUsageData(
                    packageName = packageName,
                    appName = saved?.serviceName ?: defaultName,
                    totalTimeMinutes = totalTime / 1000 / 60,
                    launchCount = launchCounts[packageName] ?: 0,
                    lastUsed = lastUsedMap[packageName] ?: 0L,
                    monthlyFee = saved?.monthlyFee,
                    isCandidate = saved?.isCandidate ?: false,
                    isSubscription = subscriptionDictionary.contains(packageName)
                            || (saved?.isManualSubscription ?: false),
                    isManualSubscription = saved?.isManualSubscription ?: false
                )
            }
                .sortedByDescending { it.totalTimeMinutes }

            _usageData.value = result
        }
    }

    fun updateAppSettings(packageName: String, serviceName: String, monthlyFee: Double?) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getAll().find { it.packageName == packageName }
            if (existing != null) {
                dao.update(existing.copy(serviceName = serviceName, monthlyFee = monthlyFee))
            } else {
                dao.insert(AppSettings(packageName = packageName, serviceName = serviceName, monthlyFee = monthlyFee))
            }
            _usageData.value = _usageData.value.map { app ->
                if (app.packageName == packageName) {
                    app.copy(appName = serviceName, monthlyFee = monthlyFee)
                } else app
            }
        }
    }

    fun toggleCandidate(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _usageData.value.find { it.packageName == packageName } ?: return@launch
            val newIsCandidate = !current.isCandidate
            val existing = dao.getAll().find { it.packageName == packageName }
            if (existing != null) {
                dao.update(existing.copy(isCandidate = newIsCandidate))
            } else {
                dao.insert(AppSettings(packageName = packageName, isCandidate = newIsCandidate))
            }
            _usageData.value = _usageData.value.map { app ->
                if (app.packageName == packageName) {
                    app.copy(isCandidate = newIsCandidate)
                } else app
            }
        }
    }

    fun toggleManualSubscription(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _usageData.value.find { it.packageName == packageName } ?: return@launch
            val newValue = !current.isManualSubscription
            val existing = dao.getAll().find { it.packageName == packageName }
            if (existing != null) {
                dao.update(existing.copy(isManualSubscription = newValue))
            } else {
                dao.insert(AppSettings(packageName = packageName, isManualSubscription = newValue))
            }
            _usageData.value = _usageData.value.map { app ->
                if (app.packageName == packageName) {
                    app.copy(
                        isManualSubscription = newValue,
                        isSubscription = subscriptionDictionary.contains(packageName) || newValue
                    )
                } else app
            }
        }
    }
}