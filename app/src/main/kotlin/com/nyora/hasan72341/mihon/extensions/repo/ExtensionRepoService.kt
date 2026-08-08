package com.nyora.hasan72341.mihon.extensions.repo

import android.util.Log
import androidx.annotation.Keep
import com.nyora.hasan72341.tachiyomi.network.GET
import com.nyora.hasan72341.tachiyomi.network.awaitSuccess
import com.nyora.hasan72341.core.network.MangaHttpClient
import com.nyora.hasan72341.core.prefs.AppSettings
import com.nyora.hasan72341.core.prefs.GitHubMirror
import com.nyora.hasan72341.mihon.MihonExtensionLoader
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionRepoService @Inject constructor(
	@MangaHttpClient private val httpClient: OkHttpClient,
	private val json: Json,
	private val settings: AppSettings,
) {

	private fun applyMirror(url: String): String {
		if (url.startsWith("https://raw.githubusercontent.com/")) {
			return when (settings.gitHubMirror) {
				GitHubMirror.KEIYOUSHI -> {
					if (url.contains("/keiyoushi/extensions/")) {
						url.replace("raw.githubusercontent.com", "raw.github.com")
					} else {
						"https://raw.github.com/keiyoushi/extensions/refs/heads/repo/${url.substringAfter("raw.githubusercontent.com/")}"
					}
				}
				GitHubMirror.KKGITHUB -> url.replace("raw.githubusercontent.com", "raw.kkgithub.com")
				GitHubMirror.GHPROXY -> "https://mirror.ghproxy.com/$url"
				GitHubMirror.GHPROXY_NET -> "https://ghproxy.net/$url"
				else -> url
			}
		}
		return url
	}

	private fun deriveRepoName(baseUrl: String, defaultName: String): String {
		val url = baseUrl.toHttpUrlOrNull() ?: return defaultName
		val segments = url.pathSegments.filter { it.isNotEmpty() }
		if (segments.size >= 2 && url.host.contains("githubusercontent.com")) {
			return "${segments[0]}/${segments[1]}"
		} else if (segments.size >= 2 && url.host == "github.com") {
			return "${segments[0]}/${segments[1]}"
		} else if (segments.isNotEmpty()) {
			return segments.last()
		}
		return url.host
	}

	suspend fun fetchRepoDetails(baseUrl: String, type: ExternalExtensionType): ExternalExtensionRepo {
		if (type == ExternalExtensionType.IREADER || type == ExternalExtensionType.JAR) {
			val now = System.currentTimeMillis()
			val derived = deriveRepoName(baseUrl, if (type == ExternalExtensionType.IREADER) "IReader" else "Nyora")
			val repoName = if (type == ExternalExtensionType.IREADER) "IReader: $derived" else "Nyora: $derived"
			val repoShort = derived
			var version: String? = null
			if (type == ExternalExtensionType.JAR) {
				val indexUrl = applyMirror("$baseUrl/index.min.json")
				runCatching {
					withTimeout(REPO_DETAILS_TIMEOUT_MS) {
						val body = httpClient.newCall(GET(indexUrl)).awaitSuccess().use { response ->
							response.body.string()
						}
						val dto = json.decodeFromString<List<ExtensionIndexDto>>(body)
						version = dto.firstOrNull()?.version
					}
				}
			}

			return ExternalExtensionRepo(
				type = type,
				baseUrl = baseUrl,
				name = repoName,
				shortName = repoShort,
				website = baseUrl,
				signingKeyFingerprint = baseUrl.hashCode().toString(16), // Use baseUrl hash as pseudo-fingerprint for JAR/IReader
				createdAt = now,
				updatedAt = now,
				lastSuccessAt = now,
				lastError = null,
				version = version,
			)
		}

		val repoJsonUrl = applyMirror("$baseUrl/repo.json")
		val startedAt = System.currentTimeMillis()
		Log.d(TAG, "fetchRepoDetails:start type=$type url=$repoJsonUrl")
		return withTimeout(REPO_DETAILS_TIMEOUT_MS) {
			val body = httpClient.newCall(GET(repoJsonUrl)).awaitSuccess().use { response ->
				response.body.string()
			}
			val dto = json.decodeFromString<RepoMetaWrapperDto>(body)
			val now = System.currentTimeMillis()
			ExternalExtensionRepo(
				type = type,
				baseUrl = baseUrl,
				name = dto.meta.name,
				shortName = dto.meta.shortName,
				website = dto.meta.website,
				signingKeyFingerprint = dto.meta.signingKeyFingerprint,
				createdAt = now,
				updatedAt = now,
				lastSuccessAt = now,
				lastError = null,
			)
		}.also { repo ->
			Log.d(
				TAG,
				"fetchRepoDetails:success type=$type baseUrl=${repo.baseUrl} name=${repo.displayName} elapsedMs=${System.currentTimeMillis() - startedAt}",
			)
		}
	}

	suspend fun fetchAvailableExtensions(repo: ExternalExtensionRepo): List<RepoAvailableExtension> {
		val indexUrl = "${repo.baseUrl}/index.min.json"
		val requestUrl = applyMirror(indexUrl)
		val startedAt = System.currentTimeMillis()
		Log.d(TAG, "fetchAvailableExtensions:start type=${repo.type} url=$requestUrl")
		return runCatching {
			withTimeout(CATALOG_TIMEOUT_MS) {
				val body = httpClient.newCall(GET(requestUrl)).awaitSuccess().use { response ->
					response.body.string()
				}
				if (repo.type == ExternalExtensionType.IREADER) {
					val dto = json.decodeFromString<List<IReaderExtensionIndexDto>>(body)
					dto.asSequence()
						.mapNotNull { item -> item.toAvailableExtension(repo) }
						.toList()
				} else {
					var dto = json.decodeFromString<List<ExtensionIndexDto>>(body)
					// Keiyoushi now answers index.min.json with a two-entry "update your app" stub
					// and publishes the real manifest at index.json in a newer shape. Without this
					// the catalogue shows two fake extensions instead of ~1400 real ones.
					if (dto.isStubIndex()) {
						Log.d(TAG, "fetchAvailableExtensions:stub_index falling_back_to_index_json repo=${repo.baseUrl}")
						// Only take the fallback when it is strictly better, so a genuinely tiny
						// repo that merely looks stub-shaped can never end up worse off than before.
						val modern = fetchModernIndex(repo)
						if (modern.size > dto.size) dto = modern
					}
					dto.asSequence()
						.mapNotNull { item -> item.toAvailableExtension(repo) }
						.toList()
				}
			}
		}.onSuccess { extensions ->
			Log.d(
				TAG,
				"fetchAvailableExtensions:success type=${repo.type} baseUrl=${repo.baseUrl} count=${extensions.size} elapsedMs=${System.currentTimeMillis() - startedAt}",
			)
		}.onFailure { error ->
			Log.e(
				TAG,
				"fetchAvailableExtensions:failed type=${repo.type} baseUrl=${repo.baseUrl} elapsedMs=${System.currentTimeMillis() - startedAt} message=${error.message}",
				error,
			)
		}.getOrDefault(emptyList())
	}

	fun normalizeIndexUrl(input: String): String? {
		val processUrl = input.trim()

		val url = processUrl.toHttpUrlOrNull() ?: return null
		if (url.scheme != "https") {
			return null
		}
		val normalizedSegments = url.pathSegments
			.filter { it.isNotEmpty() }
			.toMutableList()
		if (normalizedSegments.lastOrNull() != "index.min.json") {
			normalizedSegments += "index.min.json"
		}
		val normalizedPath = "/" + normalizedSegments.joinToString("/")
		return url.newBuilder()
			.encodedPath(normalizedPath)
			.fragment(null)
			.query(null)
			.build()
			.toString()
	}

	fun baseUrlFromIndexUrl(indexUrl: String): String {
		return indexUrl.removeSuffix("/index.min.json")
	}

	/**
	 * True when the index is the placeholder a repo serves to clients it considers outdated: a
	 * couple of entries whose "extension" is really a notice. Detected by shape rather than by
	 * exact wording, which the repos change freely — a real catalogue is never this small, and the
	 * stub's entries carry no sources.
	 */
	private fun List<ExtensionIndexDto>.isStubIndex(): Boolean =
		isNotEmpty() && size <= 2 && all { it.sources.orEmpty().size <= 1 && it.code <= 1L }

	/**
	 * The current manifest shape, published at `index.json`: extensions nested under
	 * `extensionList.extensions`, versions split into `versionName`/`versionCode`, and the apk
	 * given as a full CDN url rather than a bare filename. Mapped onto the legacy DTO so the rest
	 * of the pipeline is untouched.
	 */
	private suspend fun fetchModernIndex(repo: ExternalExtensionRepo): List<ExtensionIndexDto> = runCatching {
		val url = applyMirror("${repo.baseUrl}/index.json")
		val body = httpClient.newCall(GET(url)).awaitSuccess().use { it.body.string() }
		json.decodeFromString<ModernIndexDto>(body).extensionList.extensions.map { e ->
			ExtensionIndexDto(
				name = e.name,
				pkg = e.packageName,
				// The downloader appends this to "<repo>/apk/", so keep only the filename.
				apk = e.resources?.apkUrl?.substringAfterLast('/').orEmpty(),
				// No per-extension language field any more; the package path still carries it
				// ("…extension.all.ahottie" / "…extension.en.foo"), with the sources as backup.
				lang = e.packageName.split('.').getOrNull(4)
					?: e.sources.firstOrNull()?.language
					?: "all",
				code = e.versionCode.toLongOrNull() ?: 0L,
				version = e.versionName,
				nsfw = if (e.contentWarning == CONTENT_WARNING_NSFW) 1 else 0,
				sources = e.sources.map { s -> ExtensionSourceDto(name = s.name) },
			)
		}
	}.onFailure { error ->
		Log.e(TAG, "fetchModernIndex:failed repo=${repo.baseUrl} message=${error.message}", error)
	}.getOrDefault(emptyList())

	private fun ExtensionIndexDto.toAvailableExtension(repo: ExternalExtensionRepo): RepoAvailableExtension? {
		val libVersion = runCatching { version.substringBeforeLast('.').toDouble() }.getOrNull() ?: if (repo.type == ExternalExtensionType.IREADER) 0.0 else return null
		val supported = when (repo.type) {
			ExternalExtensionType.MIHON -> libVersion in MihonExtensionLoader.LIB_VERSION_MIN..MihonExtensionLoader.LIB_VERSION_MAX
			ExternalExtensionType.ANIYOMI -> libVersion in (1.2)..(1.9)
			ExternalExtensionType.IREADER -> true
			ExternalExtensionType.JAR -> true
		}
		val displayName = when (repo.type) {
			ExternalExtensionType.MIHON -> name.removePrefix("Tachiyomi: ")
			ExternalExtensionType.ANIYOMI -> name.removePrefix("Aniyomi: ")
			ExternalExtensionType.IREADER -> name.removePrefix("IReader: ")
			ExternalExtensionType.JAR -> name
		}

		return RepoAvailableExtension(
			type = repo.type,
			name = displayName,
			pkgName = pkg,
			versionName = version,
			versionCode = code,
			libVersion = libVersion,
			lang = lang,
			isNsfw = nsfw == 1,
			sourceNames = sources.orEmpty().map { it.name },
			apkName = apk,
			iconUrl = applyMirror(if (repo.type == ExternalExtensionType.IREADER) "${repo.baseUrl}/icon/${apk.replace(".apk", ".png")}" else "${repo.baseUrl}/icon/$pkg.png"),
			repoUrl = repo.baseUrl,
			repoName = repo.displayName,
			signatureHash = repo.signingKeyFingerprint,
			isCompatible = supported,
		)
	}

	private fun IReaderExtensionIndexDto.toAvailableExtension(repo: ExternalExtensionRepo): RepoAvailableExtension {
		val libVersion = runCatching { version.substringBeforeLast('.').toDouble() }.getOrNull() ?: 0.0
		val displayName = name.removePrefix("IReader: ")

		return RepoAvailableExtension(
			type = repo.type,
			name = displayName,
			pkgName = pkg,
			versionName = version,
			versionCode = code,
			libVersion = libVersion,
			lang = lang,
			isNsfw = nsfw,
			sourceNames = emptyList(), // IReader plugins don't declare subset sources natively
			apkName = apk,
			iconUrl = applyMirror("${repo.baseUrl}/icon/${apk.replace(".apk", ".png")}"),
			repoUrl = repo.baseUrl,
			repoName = repo.displayName,
			// IReader repos currently don't expose a verifiable APK signing fingerprint.
			// `repo.signingKeyFingerprint` is a synthetic repo identifier for repo management,
			// not the package certificate fingerprint, so using it for trust checks would
			// always misclassify installed IReader extensions as untrusted.
			signatureHash = "",
			isCompatible = true,
		)
	}



	@Keep
	@Serializable
	private data class RepoMetaWrapperDto(
		val meta: RepoMetaDto,
	)

	@Keep
	@Serializable
	private data class RepoMetaDto(
		val name: String,
		@SerialName("shortName")
		val shortName: String? = null,
		val website: String,
		@SerialName("signingKeyFingerprint")
		val signingKeyFingerprint: String,
	)

	@Keep
	@Serializable
	private data class ExtensionIndexDto(
		val name: String,
		val pkg: String,
		val apk: String,
		val lang: String = "all",
		val code: Long,
		val version: String,
		val nsfw: Int = 0,
		val sources: List<ExtensionSourceDto>? = null,
	)

	@Keep
	@Serializable
	private data class ExtensionSourceDto(
		val name: String,
	)

	// ---- current ("index.json") manifest shape ----

	@Keep
	@Serializable
	private data class ModernIndexDto(
		val extensionList: ModernExtensionListDto = ModernExtensionListDto(),
	)

	@Keep
	@Serializable
	private data class ModernExtensionListDto(
		val extensions: List<ModernExtensionDto> = emptyList(),
	)

	@Keep
	@Serializable
	private data class ModernExtensionDto(
		val name: String = "",
		val packageName: String = "",
		val resources: ModernResourcesDto? = null,
		val versionCode: String = "0",
		val versionName: String = "",
		val contentWarning: String? = null,
		val sources: List<ModernSourceDto> = emptyList(),
	)

	@Keep
	@Serializable
	private data class ModernResourcesDto(
		val apkUrl: String? = null,
		val iconUrl: String? = null,
	)

	@Keep
	@Serializable
	private data class ModernSourceDto(
		val id: String = "",
		val name: String = "",
		val language: String? = null,
		val homeUrl: String? = null,
	)

	@Keep
	@Serializable
	private data class IReaderExtensionIndexDto(
		val name: String = "",
		val pkg: String = "",
		val apk: String = "",
		val lang: String = "en",
		val code: Long = 1,
		val version: String = "1.0",
		val nsfw: Boolean = false,
	)

	private companion object {
		const val TAG = "ExtensionRepo"
		const val REPO_DETAILS_TIMEOUT_MS = 15_000L
		const val CATALOG_TIMEOUT_MS = 20_000L
		const val CONTENT_WARNING_NSFW = "CONTENT_WARNING_NSFW"
	}
}
