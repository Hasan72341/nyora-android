package com.nyora.hasan72341.core

import android.util.Base64
import com.nyora.hasan72341.core.network.BaseHttpClient
import com.nyora.hasan72341.core.prefs.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import org.conscrypt.Conscrypt
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote master switch for sources. On launch the app fetches a small signed JSON
 * config from GitHub Pages, verifies its Ed25519 signature against a public key
 * baked into the app, and sets [AppSettings.isSourcesUnlocked] to the config's
 * `enabled` value. This lets a Store-review build ship with sources OFF and be
 * enabled remotely (no app update) once approved — just re-sign + publish the
 * config. A missing / invalid / unreachable config leaves the persisted flag
 * untouched (so a previously-unlocked device stays unlocked offline). Only a
 * successfully-verified config can flip the switch — a forged config is ignored.
 */
@Singleton
class RemoteSourceGate @Inject constructor(
	@BaseHttpClient private val okHttpClient: OkHttpClient,
	private val json: Json,
	private val settings: AppSettings,
) {

	suspend fun refresh() = withContext(Dispatchers.IO) {
		val enabled = runCatching { fetchVerifiedEnabled(CONFIG_URL) }.getOrNull() ?: return@withContext
		// A manual unlock is sticky — the remote switch can enable, but never
		// re-locks a device the user activated themselves.
		val effective = enabled || settings.isSourcesManuallyUnlocked
		if (settings.isSourcesUnlocked != effective) {
			settings.isSourcesUnlocked = effective
		}
	}

	/**
	 * Manual unlock: verify a user-pasted repository link and, if its signed payload
	 * says enabled, unlock sources. Returns true on success. Lets a user activate
	 * sources themselves without waiting on the built-in remote switch.
	 */
	suspend fun activateFromUrl(url: String): Boolean = withContext(Dispatchers.IO) {
		val enabled = runCatching { fetchVerifiedEnabled(url.trim()) }.getOrNull()
		if (enabled == true) {
			settings.isSourcesManuallyUnlocked = true
			settings.isSourcesUnlocked = true
			true
		} else {
			false
		}
	}

	/** @return the verified `enabled` value, or null if unreachable / invalid / unsigned. */
	private fun fetchVerifiedEnabled(url: String): Boolean? {
		val body = okHttpClient.newCall(
			Request.Builder().url(url).header("Accept", "application/json").build(),
		).execute().use { resp ->
			if (!resp.isSuccessful) return null
			resp.body?.string() ?: return null
		}
		val obj = json.parseToJsonElement(body).jsonObject
		val dataB64 = obj["data"]?.jsonPrimitive?.content ?: return null
		val sigB64 = obj["sig"]?.jsonPrimitive?.content ?: return null
		val payloadBytes = Base64.decode(dataB64, Base64.DEFAULT)
		val sigBytes = Base64.decode(sigB64, Base64.DEFAULT)
		if (!verify(payloadBytes, sigBytes)) return null
		val payload = json.parseToJsonElement(String(payloadBytes, Charsets.UTF_8)).jsonObject
		return payload["enabled"]?.jsonPrimitive?.booleanOrNull ?: false
	}

	private fun verify(data: ByteArray, sig: ByteArray): Boolean {
		val keySpec = X509EncodedKeySpec(Base64.decode(PUBLIC_KEY_B64, Base64.DEFAULT))
		// Ed25519 in the platform JCA is API 33+; the bundled Conscrypt provides it
		// on older devices. Try Conscrypt first, then fall back to the platform.
		runCatching { Conscrypt.newProvider() }.getOrNull()?.let { provider ->
			runCatching {
				val pub = KeyFactory.getInstance("Ed25519", provider).generatePublic(keySpec)
				val s = Signature.getInstance("Ed25519", provider)
				s.initVerify(pub)
				s.update(data)
				return s.verify(sig)
			}
		}
		return runCatching {
			val pub = KeyFactory.getInstance("Ed25519").generatePublic(keySpec)
			val s = Signature.getInstance("Ed25519")
			s.initVerify(pub)
			s.update(data)
			s.verify(sig)
		}.getOrDefault(false)
	}

	private companion object {
		const val CONFIG_URL = "https://hasan72341.github.io/nyora-android-switch/android-config.json"
		// Ed25519 public key (SPKI/DER, base64) — same keypair that signs the configs.
		const val PUBLIC_KEY_B64 = "MCowBQYDK2VwAyEA8pus7Do8tcNQXYqb+sZQZh2XJ70Iz3Zi/iE25USROT0="
	}
}
