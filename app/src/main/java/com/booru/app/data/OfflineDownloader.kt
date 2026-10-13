package com.booru.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.booru.app.OfflineDownloadService
import com.booru.app.RemoteMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object OfflineDownloader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    var downloading by mutableStateOf(false)
        private set
    var done by mutableIntStateOf(0)
        private set
    var target by mutableIntStateOf(0)
        private set
    var phase by mutableIntStateOf(0)
        private set
    var bytes by mutableLongStateOf(0L)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var language by mutableStateOf(AppLanguage.ENGLISH)
        private set
    var lastAdded by mutableIntStateOf(-1)
        private set
    var goal by mutableIntStateOf(0)
        private set
    var base by mutableIntStateOf(0)
        private set

    fun start(
        context: Context,
        goal: Int,
        count: Int,
        preferOriginal: Boolean,
        lang: AppLanguage,
        collect: suspend (onFound: (Int) -> Unit) -> List<RemoteMedia>
    ) {
        if (downloading) return
        error = null
        done = 0
        bytes = 0L
        target = count
        phase = 0
        lastAdded = -1
        language = lang
        this.goal = goal
        base = OfflineStore.posts.size
        downloading = true
        OfflineDownloadService.start(context.applicationContext)
        val previous = job
        job = scope.launch {
            val own = coroutineContext[Job]
            try {
                previous?.cancelAndJoin()
                val chosen = collect { found -> if (job === own) done = found }
                if (chosen.isEmpty()) {
                    error = Strings.tr(lang, "No new posts for your filters", "Новых постов под ваши фильтры нет", "フィルターに合う新しい投稿がありません", "没有符合筛选的新帖子", "필터에 맞는 새 게시물이 없습니다", "لا منشورات جديدة تطابق عوامل التصفية")
                    return@launch
                }
                target = chosen.size
                done = 0
                phase = 1
                val before = OfflineStore.posts.size
                OfflineStore.download(chosen, preferOriginal) { d, b ->
                    if (job === own) {
                        done = d
                        bytes = b
                    }
                }
                if (job === own) lastAdded = (OfflineStore.posts.size - before).coerceAtLeast(0)
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                if (job === own) error = e.message ?: "Download failed"
            } finally {
                withContext(NonCancellable) {
                    if (job === own) {
                        downloading = false
                        phase = 0
                    }
                }
            }
        }
    }

    fun clearLastAdded() {
        lastAdded = -1
    }

    fun cancel() {
        OfflineStore.cancelActive()
        job?.cancel()
        downloading = false
        phase = 0
    }

    suspend fun stopAndWait() {
        val current = job
        cancel()
        current?.join()
    }
}
