package com.reelflow.platform.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelflow.platform.data.model.*
import com.reelflow.platform.data.repository.PlatformRepository
import com.reelflow.platform.data.repository.PublishingMode
import com.reelflow.platform.data.service.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Wires the UI to the repository + on-device services.
 *
 * - Veo video generation: the page's saved master prompt is sent 100%
 *   verbatim; the chosen Veo model (Standard / Fast, 3.0 / 3.1) is picked
 *   in the Create screen. The Veo API key lives in the encrypted store.
 * - Facebook connect: SDK Login button OR pasted Explorer User Token.
 *   No email+password field exists anywhere (Facebook forbids it).
 * - Thumbnails: frames extracted on-device from the video file.
 */
class PlatformViewModel : ViewModel() {

    val repository = PlatformRepository()

    val pages: StateFlow<List<FacebookPage>> = repository.pages
    val jobs: StateFlow<List<VideoReelItem>> = repository.jobs
    val logs: StateFlow<List<SystemLogEntry>> = repository.logs
    val publishingMode: StateFlow<PublishingMode> = repository.publishingMode

    private val _accounts = MutableStateFlow<List<ConnectedAccount>>(emptyList())
    val accounts: StateFlow<List<ConnectedAccount>> = _accounts.asStateFlow()

    private val _veoKeySet = MutableStateFlow(false)
    val veoKeySet: StateFlow<Boolean> = _veoKeySet.asStateFlow()

    private val _selectedVeoModel = MutableStateFlow(VeoModel.STANDARD_30)
    val selectedVeoModel: StateFlow<VeoModel> = _selectedVeoModel.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    /** Settings -> MainActivity: run the Facebook SDK login flow. */
    private val _fbLoginRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val fbLoginRequest: SharedFlow<Unit> = _fbLoginRequest.asSharedFlow()

    /** Settings -> MainActivity: connect with a pasted Explorer User Token. */
    private val _tokenLoginRequest = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val tokenLoginRequest: SharedFlow<String> = _tokenLoginRequest.asSharedFlow()

    private var appContext: Context? = null
    private var tokenStore: SecureTokenStore? = null
    private var accountStore: AccountStore? = null
    private var promptStore: PagePromptStore? = null
    private var thumbnailService: ThumbnailService? = null

    init {
        repository.bindScope(viewModelScope)
    }

    fun attachAppContext(context: Context) {
        val app = context.applicationContext
        appContext = app
        tokenStore = SecureTokenStore(app)
        accountStore = AccountStore(app)
        promptStore = PagePromptStore(app)
        thumbnailService = ThumbnailService(app)
        repository.attachPersistence(app)
        refreshAccounts()
        _veoKeySet.value = tokenStore?.getApiKey(SecureTokenStore.VEO_API_KEY) != null
    }

    fun attachRealPublishing(service: com.reelflow.platform.data.interfaces.MetaGraphPublishingService) {
        repository.attachRealPublisher(service)
    }

    fun showMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun clearMessage() {
        _statusMessage.value = null
    }

    // ------------------------------------------------------------------
    // Publishing mode + settings
    // ------------------------------------------------------------------

    fun setPublishingMode(mode: PublishingMode) = repository.setPublishingMode(mode)

    fun saveVeoKey(key: String) {
        tokenStore?.saveApiKey(SecureTokenStore.VEO_API_KEY, key)
        _veoKeySet.value = key.isNotBlank()
        showMessage(if (key.isBlank()) "Veo key removed" else "Veo API key saved (encrypted)")
    }

    fun selectVeoModel(model: VeoModel) {
        _selectedVeoModel.value = model
    }

    // ------------------------------------------------------------------
    // Facebook accounts / pages
    // ------------------------------------------------------------------

    fun requestFacebookLogin() {
        _fbLoginRequest.tryEmit(Unit)
    }

    fun requestTokenLogin(userToken: String) {
        if (userToken.isBlank()) {
            showMessage("Token khaali hai. Explorer se token copy kar ke paste karo.")
            return
        }
        _tokenLoginRequest.tryEmit(userToken)
    }

    /** Called from MainActivity with the login result (SDK or token flow). */
    fun onFacebookLoginResult(result: FbLoginResult) {
        when (result) {
            is FbLoginResult.Success -> {
                accountStore?.addAccount(
                    ConnectedAccount(
                        id = result.accountId.ifBlank { "acc_${System.currentTimeMillis()}" },
                        name = result.accountName.ifBlank { "Facebook Account" },
                        pageIds = result.pageIds
                    )
                )
                refreshAccounts()
                // Real pages replace demo pages: fetch /me/accounts names via
                // the stored page tokens is out of scope here; the helper
                // already saved each page token, so list pages by token.
                val connected = result.pageIds.mapIndexed { i, graphId ->
                    FacebookPage(
                        id = "fb_$graphId",
                        name = result.pageNames.getOrNull(i) ?: "Page $graphId",
                        pageId = graphId
                    )
                }
                if (connected.isNotEmpty()) repository.upsertPagesFromLogin(connected)
                showMessage(
                    if (result.pageCount > 0) "${result.pageCount} pages connected"
                    else "Login OK, but no pages found on this account"
                )
            }
            is FbLoginResult.Cancelled -> showMessage("Facebook login cancelled")
            is FbLoginResult.Error -> showMessage("Login failed: ${result.message}")
        }
    }

    fun refreshAccounts() {
        _accounts.value = accountStore?.getAccounts().orEmpty()
    }

    fun removeAccount(accountId: String) {
        val pageIds = accountStore?.removeAccount(accountId).orEmpty()
        pageIds.forEach { tokenStore?.removePageToken(it) }
        refreshAccounts()
        showMessage("Account removed")
    }

    fun verifyPageToken(page: FacebookPage) {
        repository.verifyPageToken(page) { result ->
            result
                .onSuccess { showMessage("Token OK: ${page.name}") }
                .onFailure { showMessage("Token check failed: ${it.message}") }
        }
    }

    fun hasPageToken(graphPageId: String): Boolean =
        tokenStore?.hasToken(graphPageId) ?: false

    // ------------------------------------------------------------------
    // Per-page master prompts (sent 100% verbatim to Veo)
    // ------------------------------------------------------------------

    fun getMasterPrompt(pageId: String): String? = promptStore?.getMasterPrompt(pageId)

    fun saveMasterPrompt(pageId: String, prompt: String) {
        promptStore?.saveMasterPrompt(pageId, prompt)
        showMessage("Master prompt saved for this page")
    }

    // ------------------------------------------------------------------
    // Video creation + Veo generation
    // ------------------------------------------------------------------

    /** Create a video job from the page's saved master prompt (verbatim). */
    fun createVideoFromPagePrompt(page: FacebookPage, model: VeoModel = _selectedVeoModel.value) {
        val prompt = promptStore?.getMasterPrompt(page.id)
        if (prompt.isNullOrBlank()) {
            showMessage("Is page ka master prompt save nahi hai — pehle Page Manager me prompt likho")
            return
        }
        val job = VideoReelItem(
            targetPageId = page.id,
            targetPageName = page.name,
            targetGraphPageId = page.pageId,
            prompt = prompt,
            veoModel = model,
            status = JobStatus.DRAFT
        )
        repository.addJob(job)
        generateVideo(job)
    }

    /** Create a video job from a typed prompt (also sent verbatim). */
    fun createVideoWithPrompt(page: FacebookPage, prompt: String, model: VeoModel = _selectedVeoModel.value) {
        if (prompt.isBlank()) {
            showMessage("Prompt khaali hai")
            return
        }
        val job = VideoReelItem(
            targetPageId = page.id,
            targetPageName = page.name,
            targetGraphPageId = page.pageId,
            prompt = prompt,
            veoModel = model,
            status = JobStatus.DRAFT
        )
        repository.addJob(job)
        generateVideo(job)
    }

    fun generateVideo(job: VideoReelItem) {
        val ctx = appContext ?: return
        val apiKey = tokenStore?.getApiKey(SecureTokenStore.VEO_API_KEY)
        if (apiKey.isNullOrBlank()) {
            showMessage("Veo API key nahi hai — Settings me key add karo")
            repository.updateJob(job.copy(status = JobStatus.FAILED))
            return
        }
        repository.updateJob(job.copy(status = JobStatus.AI_GENERATING, progressPercent = 5))
        viewModelScope.launch(Dispatchers.IO) {
            val service = Veo3GenerationService(ctx, apiKey, job.veoModel.modelId)
            val result = service.generateVideo(
                prompt = job.prompt, // 100% verbatim — never rewritten
                negativePrompt = job.negativePrompt,
                durationSeconds = job.durationSeconds,
                aspectRatio = job.aspectRatio,
                resolution = job.resolution
            )
            withContext(Dispatchers.Main) {
                result
                    .onSuccess { localPath ->
                        var updated = job.copy(
                            localUri = localPath,
                            status = JobStatus.READY_FOR_REVIEW,
                            progressPercent = 100
                        )
                        repository.updateJob(updated)
                        extractThumbnail(updated)
                        showMessage("Video ready — review karo")
                    }
                    .onFailure { e ->
                        repository.updateJob(job.copy(status = JobStatus.FAILED))
                        showMessage("Veo failed: ${e.message}")
                    }
            }
        }
    }

    // ------------------------------------------------------------------
    // Gallery import + thumbnails
    // ------------------------------------------------------------------

    /** Import a gallery video: copy into app storage + extract thumbnail frames. */
    fun importGalleryVideo(context: Context, uri: Uri, page: FacebookPage, title: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val copied = copyToFilesDir(context, uri)
            withContext(Dispatchers.Main) {
                if (copied == null) {
                    showMessage("Video import nahi ho saki")
                    return@withContext
                }
                val job = VideoReelItem(
                    targetPageId = page.id,
                    targetPageName = page.name,
                    targetGraphPageId = page.pageId,
                    prompt = "Gallery upload: ${File(copied).name}",
                    localUri = copied,
                    title = title,
                    status = JobStatus.READY_FOR_REVIEW
                )
                repository.addJob(job)
                extractThumbnail(job)
                showMessage("Video library me add ho gayi")
            }
        }
    }

    private fun copyToFilesDir(context: Context, uri: Uri): String? {
        return try {
            val outFile = File(context.filesDir, "gallery_${System.currentTimeMillis()}.mp4")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            } ?: return null
            outFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Extract cover frames for a video; middle frame becomes the thumbnail. */
    fun extractThumbnail(job: VideoReelItem) {
        val path = job.localUri ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val thumb = thumbnailService?.extractDefaultThumbnail(path)
            if (thumb != null) {
                withContext(Dispatchers.Main) {
                    repository.updateJob(job.copy(localThumbnailPath = thumb))
                }
            }
        }
    }

    /** All candidate frames for the Thumbnail Editor (25% / 50% / 75%). */
    suspend fun extractThumbnailFrames(job: VideoReelItem): List<String> {
        val path = job.localUri ?: return emptyList()
        return thumbnailService?.extractFrames(path).orEmpty()
    }

    fun setThumbnail(job: VideoReelItem, thumbnailPath: String, frameUs: Long? = null) {
        repository.updateJob(job.copy(localThumbnailPath = thumbnailPath, thumbnailFrameUs = frameUs))
    }

    // ------------------------------------------------------------------
    // Review / assignment / scheduling / publishing
    // ------------------------------------------------------------------

    fun approveJob(jobId: String, comment: String = "") = repository.approveJob(jobId, comment)
    fun rejectJob(jobId: String, comment: String = "") = repository.rejectJob(jobId, comment)
    fun deleteJob(jobId: String) = repository.deleteJob(jobId)
    fun publishVideo(jobId: String) = repository.publishVideo(jobId)
    fun reassignJob(jobId: String, page: FacebookPage) = repository.reassignJob(jobId, page)
    fun bulkReassignJobs(jobIds: Set<String>, page: FacebookPage) = repository.bulkReassignJobs(jobIds, page)

    fun scheduleJob(jobId: String, dateTimeIso: String, timezone: String): Boolean {
        val ok = repository.scheduleJob(jobId, dateTimeIso, timezone)
        showMessage(if (ok) "Scheduled" else "Pehle approve karo, phir schedule hoga")
        return ok
    }

    fun cancelSchedule(jobId: String) {
        repository.cancelSchedule(jobId)
        showMessage("Schedule cancel — wapas Approved")
    }

    fun updateJob(job: VideoReelItem) = repository.updateJob(job)

    fun togglePageEnabled(page: FacebookPage) =
        repository.updatePageStatus(page.id, !page.isEnabled)

    fun analytics(): AnalyticsSummary = repository.analytics()
}
