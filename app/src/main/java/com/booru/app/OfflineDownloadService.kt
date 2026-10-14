package com.booru.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.booru.app.data.BooruCacheManager
import com.booru.app.data.OfflineDownloader
import com.booru.app.data.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OfflineDownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watching = false
    private var foreground = false
    private var latestStartId = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        promote()
    }

    // Without the foreground state the download can't run in the background, so give up instead of looking stuck.
    private fun promote(): Boolean {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        foreground = runCatching { ServiceCompat.startForeground(this, PROGRESS_ID, buildProgress(), type) }.isSuccess
        if (!foreground) {
            OfflineDownloader.cancel()
            stopSelf()
        }
        return foreground
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = startId
        if (intent?.action == ACTION_STOP) {
            OfflineDownloader.cancel()
        }
        if (!watching) {
            // finish() drops the foreground state, so a start that lands after it has to promote again.
            if (!foreground && intent?.action != ACTION_STOP && !promote()) return START_NOT_STICKY
            watching = true
            scope.launch {
                delay(300)
                // A new download can start right after a cancel; keep watching it instead of stopping under it.
                do {
                    while (isActive && OfflineDownloader.downloading) {
                        runCatching { NotificationManagerCompat.from(this@OfflineDownloadService).notify(PROGRESS_ID, buildProgress()) }
                        delay(700)
                    }
                    delay(300)
                } while (isActive && OfflineDownloader.downloading)
                watching = false
                finish()
            }
        }
        return START_NOT_STICKY
    }

    // Android 15+ limits dataSync foreground time; stop cleanly rather than let the system crash the app.
    override fun onTimeout(startId: Int, fgsType: Int) {
        OfflineDownloader.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        foreground = false
        stopSelf()
    }

    private fun finish() {
        val added = OfflineDownloader.lastAdded
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        foreground = false
        if (added > 0 && NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            val lang = OfflineDownloader.language
            val done = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(Strings.tr(lang, "Offline posts ready", "Офлайн-посты готовы", "オフライン投稿の準備完了", "离线帖子已就绪", "오프라인 게시물 준비 완료", "المنشورات بلا إنترنت جاهزة"))
                .setContentText(Strings.tr(lang, "$added new posts saved", "Сохранено новых постов: $added", "新しい投稿 $added 件を保存", "已保存 $added 个新帖子", "새 게시물 ${added}개 저장됨", "تم حفظ $added منشور جديد"))
                .setContentIntent(openAppIntent())
                .setAutoCancel(true)
                .build()
            runCatching { NotificationManagerCompat.from(this).notify(DONE_ID, done) }
        }
        // Only stops if no newer start arrived meanwhile; that one then starts its own watch.
        stopSelfResult(latestStartId)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildProgress(): android.app.Notification {
        val lang = OfflineDownloader.language
        val collecting = OfflineDownloader.phase == 0
        val title = if (collecting) {
            Strings.tr(lang, "Finding offline posts…", "Подбираем офлайн-посты…", "オフライン投稿を探しています…", "正在查找离线帖子…", "오프라인 게시물 찾는 중…", "جارٍ البحث عن منشورات بلا إنترنت…")
        } else {
            Strings.tr(lang, "Downloading offline posts", "Скачиваем офлайн-посты", "オフライン投稿をダウンロード中", "正在下载离线帖子", "오프라인 게시물 다운로드 중", "جارٍ تنزيل المنشورات بلا إنترنت")
        }
        val text = if (collecting) {
            "${OfflineDownloader.done}"
        } else {
            "${com.booru.app.data.OfflineStore.posts.size} / ${OfflineDownloader.goal} · ${BooruCacheManager.formatBytes(com.booru.app.data.OfflineStore.sizeBytes)}"
        }
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, OfflineDownloadService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(OfflineDownloader.goal.coerceAtLeast(1), OfflineDownloader.base + OfflineDownloader.done, collecting)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent())
            .addAction(0, Strings.tr(lang, "Stop", "Остановить", "停止", "停止", "중지", "إيقاف"), stopIntent)
            .build()
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val name = Strings.tr(OfflineDownloader.language, "Offline downloads", "Офлайн-загрузки", "オフラインダウンロード", "离线下载", "오프라인 다운로드", "التنزيلات بلا إنترنت")
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW))
    }

    companion object {
        private const val CHANNEL_ID = "offline_downloads"
        private const val PROGRESS_ID = 4101
        private const val DONE_ID = 4102
        private const val ACTION_STOP = "com.booru.app.action.STOP_OFFLINE"

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, OfflineDownloadService::class.java)) }
        }
    }
}
