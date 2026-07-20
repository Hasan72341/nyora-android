package com.nyora.hasan72341.scrobbling.common.ui.config

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.nyora.hasan72341.R
import com.nyora.hasan72341.core.nav.AppRouter
import com.nyora.hasan72341.core.ui.BaseViewModel
import com.nyora.hasan72341.core.util.ext.MutableEventFlow
import com.nyora.hasan72341.core.util.ext.call
import com.nyora.hasan72341.core.util.ext.onFirst
import com.nyora.hasan72341.core.util.ext.require
import com.nyora.hasan72341.list.ui.model.EmptyState
import com.nyora.hasan72341.list.ui.model.ListModel
import com.nyora.hasan72341.scrobbling.common.domain.Scrobbler
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerService
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblerUser
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblingInfo
import com.nyora.hasan72341.scrobbling.common.domain.model.ScrobblingStatus
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class ScrobblerConfigViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	scrobblersProvider: Provider<Set<@JvmSuppressWildcards Scrobbler>>,
) : BaseViewModel() {

	private val scrobblerService = getScrobblerService(savedStateHandle)
	private val scrobbler = scrobblersProvider.get().first { it.scrobblerService == scrobblerService }

	val titleResId = scrobbler.scrobblerService.titleResId

	val user = MutableStateFlow<ScrobblerUser?>(null)
	val onLoggedOut = MutableEventFlow<Unit>()

	val content = flowOf(emptyList<ScrobblingInfo>())
		.onStart { loadingCounter.increment() }
		.onFirst { loadingCounter.decrement() }
		.withErrorHandling()
		.map { buildContentList(it) }
		.stateIn(viewModelScope + Dispatchers.IO, SharingStarted.Eagerly, emptyList())

	init {
		scrobbler.user
			.onEach { user.value = it }
			.launchIn(viewModelScope + Dispatchers.IO)
	}

	fun onAuthCodeReceived(authCode: String) {
		launchLoadingJob(Dispatchers.IO) {
			val newUser = scrobbler.authorize(authCode)
			user.value = newUser
		}
	}

	fun logout() {
		launchLoadingJob(Dispatchers.IO) {
			scrobbler.logout()
			user.value = null
			onLoggedOut.call(Unit)
		}
	}

	private fun buildContentList(list: List<ScrobblingInfo>): List<ListModel> {
		if (list.isEmpty()) {
			return listOf(
				EmptyState(
					icon = R.drawable.ic_empty_history,
					textPrimary = R.string.nothing_here,
					textSecondary = R.string.scrobbling_empty_hint,
					actionStringRes = 0,
				),
			)
		}
		val grouped = list.groupBy { it.status }
		val statuses = ScrobblingStatus.entries
		val result = ArrayList<ListModel>(list.size + statuses.size)
		for (st in statuses) {
			val subList = grouped[st]
			if (subList.isNullOrEmpty()) {
				continue
			}
			result.add(st)
			result.addAll(subList)
		}
		return result
	}

	private fun getScrobblerService(
		savedStateHandle: SavedStateHandle,
	): ScrobblerService {
		val serviceId = savedStateHandle.get<Int>(AppRouter.KEY_ID) ?: 0
		if (serviceId != 0) {
			return ScrobblerService.entries.first { it.id == serviceId }
		}
		val uri = savedStateHandle.require<Uri>(AppRouter.KEY_DATA)
		return when (uri.host) {
			ScrobblerConfigActivity.HOST_SHIKIMORI_AUTH -> ScrobblerService.SHIKIMORI
			ScrobblerConfigActivity.HOST_ANILIST_AUTH -> ScrobblerService.ANILIST
			ScrobblerConfigActivity.HOST_MAL_AUTH -> ScrobblerService.MAL
			ScrobblerConfigActivity.HOST_KITSU_AUTH -> ScrobblerService.KITSU
			ScrobblerConfigActivity.HOST_BANGUMI_AUTH -> ScrobblerService.BANGUMI
			ScrobblerConfigActivity.HOST_MANGABAKA_AUTH -> ScrobblerService.MANGABAKA
			else -> error("Wrong scrobbler uri: $uri")
		}
	}
}
