package com.example.subskill.screen

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

@Composable
fun DetailScreen(
    app: AppUsageData,
    onSave: (serviceName: String, monthlyFee: Int?) -> Unit,
    onCancel: () -> Unit,
    onToggleCandidate: () -> Unit,
    onToggleManualSubscription: () -> Unit
) {
    val context = LocalContext.current
    var serviceName by remember { mutableStateOf(app.appName) }
    var monthlyFeeText by remember { mutableStateOf(app.monthlyFee?.toString() ?: "") }

    val icon: Drawable? = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "戻る"
                )
            }
            Text(
                text = "詳細・編集",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon.toBitmap(width = 64, height = 64).asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app.appName.take(1),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column {
                    Text(
                        text = app.appName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            HorizontalDivider()

            OutlinedTextField(
                value = serviceName,
                onValueChange = { serviceName = it },
                label = { Text("サービス名") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = monthlyFeeText,
                onValueChange = { monthlyFeeText = it },
                label = { Text("月額（円）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                prefix = { Text("¥") }
            )

            Spacer(modifier = Modifier.weight(1f))

            // 辞書未登録のアプリのみ手動サブスク登録ボタンを表示
            if (!subscriptionDictionary(app.packageName)) {
                Button(
                    onClick = onToggleManualSubscription,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (app.isManualSubscription)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text(
                        text = if (app.isManualSubscription) "サブスク登録を解除" else "サブスクとして登録",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Button(
                onClick = onToggleCandidate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (app.isCandidate)
                        MaterialTheme.colorScheme.secondary
                    else
                        MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = if (app.isCandidate) "解約候補から外す" else "解約候補に追加",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("キャンセル")
                }
                Button(
                    onClick = {
                        val fee = monthlyFeeText.toIntOrNull()
                        onSave(serviceName, fee)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("保存", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 辞書チェック用のヘルパー関数
private fun subscriptionDictionary(packageName: String): Boolean {
    val dict = setOf(
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
    return dict.contains(packageName)
}