package com.nyora.hasan72341.settings.sources.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.nyora.hasan72341.R
import com.nyora.hasan72341.browser.BaseBrowserActivity
import com.nyora.hasan72341.browser.BrowserCallback
import com.nyora.hasan72341.browser.BrowserClient
import com.nyora.hasan72341.core.model.getTitle
import com.nyora.hasan72341.core.nav.AppRouter
import com.nyora.hasan72341.core.parser.ParserMangaRepository
import com.nyora.hasan72341.core.util.ext.getDisplayMessage
import com.nyora.hasan72341.mihon.parsers.MangaParserAuthProvider
import com.nyora.hasan72341.mihon.parsers.model.MangaSource
import com.nyora.hasan72341.mihon.parsers.util.runCatchingCancellable

@AndroidEntryPoint
class SourceAuthActivity : BaseBrowserActivity(), BrowserCallback {

	private lateinit var authProvider: MangaParserAuthProvider

	private var authCheckJob: Job? = null

	override fun onCreate2(savedInstanceState: Bundle?, source: MangaSource, repository: ParserMangaRepository?) {
		if (repository == null) {
			finishAfterTransition()
			return
		}
		authProvider = repository.getAuthProvider() ?: run {
			Toast.makeText(
				this,
				getString(R.string.auth_not_supported_by, source.getTitle(this)),
				Toast.LENGTH_SHORT,
			).show()
			finishAfterTransition()
			return
		}
		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = true)
		viewBinding.webView.webViewClient = BrowserClient(this, adBlock)
		lifecycleScope.launch {
			try {
				proxyProvider.applyWebViewConfig()
			} catch (e: Exception) {
				Snackbar.make(viewBinding.webView, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG).show()
			}
			if (savedInstanceState == null) {
				val url = authProvider.authUrl
				onTitleChanged(
					source.getTitle(this@SourceAuthActivity),
					getString(R.string.loading_),
				)
				viewBinding.webView.loadUrl(url)
			}
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		android.R.id.home -> {
			viewBinding.webView.stopLoading()
			setResult(RESULT_CANCELED)
			finishAfterTransition()
			true
		}

		else -> super.onOptionsItemSelected(item)
	}

	override fun onLoadingStateChanged(isLoading: Boolean) {
		super.onLoadingStateChanged(isLoading)
		if (isLoading) {
			return
		}
		val prevJob = authCheckJob
		authCheckJob = lifecycleScope.launch {
			prevJob?.join()
			val isAuthorized = runCatchingCancellable {
				authProvider.isAuthorized()
			}.getOrDefault(false)
			if (isAuthorized) {
				Toast.makeText(this@SourceAuthActivity, R.string.auth_complete, Toast.LENGTH_SHORT).show()
				setResult(RESULT_OK)
				finishAfterTransition()
			}
		}
	}

	class Contract : ActivityResultContract<MangaSource, Boolean>() {

		override fun createIntent(context: Context, input: MangaSource) = AppRouter.sourceAuthIntent(context, input)

		override fun parseResult(resultCode: Int, intent: Intent?) = resultCode == RESULT_OK
	}

	companion object {
		const val TAG = "SourceAuthActivity"
	}
}
