package com.nyora.hasan72341.settings.search

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.XmlRes
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.preference.get
import dagger.Reusable
import com.nyora.hasan72341.R
import com.nyora.hasan72341.backups.ui.periodical.PeriodicalBackupSettingsFragment
import com.nyora.hasan72341.core.LocalizedAppContext
import com.nyora.hasan72341.settings.AppearanceSettingsFragment
import com.nyora.hasan72341.settings.DownloadsSettingsFragment
import com.nyora.hasan72341.settings.ProxySettingsFragment
import com.nyora.hasan72341.settings.ReaderSettingsFragment
import com.nyora.hasan72341.settings.ServicesSettingsFragment
import com.nyora.hasan72341.settings.StorageAndNetworkSettingsFragment
import com.nyora.hasan72341.settings.SuggestionsSettingsFragment
import com.nyora.hasan72341.settings.about.AboutSettingsFragment
import com.nyora.hasan72341.settings.discord.DiscordSettingsFragment
import com.nyora.hasan72341.settings.sources.SourcesSettingsFragment
import com.nyora.hasan72341.settings.tracker.TrackerSettingsFragment
import com.nyora.hasan72341.settings.userdata.BackupsSettingsFragment
import com.nyora.hasan72341.settings.userdata.storage.DataCleanupSettingsFragment
import javax.inject.Inject

@Reusable
@SuppressLint("RestrictedApi")
class SettingsSearchHelper @Inject constructor(
    @LocalizedAppContext private val context: Context,
) {

    fun inflatePreferences(): List<SettingsItem> {
        val preferenceManager = PreferenceManager(context)
        val result = ArrayList<SettingsItem>()
        preferenceManager.inflateTo(result, R.xml.pref_appearance, emptyList(), AppearanceSettingsFragment::class.java)
        preferenceManager.inflateTo(result, R.xml.pref_sources, emptyList(), SourcesSettingsFragment::class.java)
        preferenceManager.inflateTo(result, R.xml.pref_reader, emptyList(), ReaderSettingsFragment::class.java)
        preferenceManager.inflateTo(
            result,
            R.xml.pref_network_storage,
            emptyList(),
            StorageAndNetworkSettingsFragment::class.java,
        )
        preferenceManager.inflateTo(result, R.xml.pref_backups, emptyList(), BackupsSettingsFragment::class.java)
        preferenceManager.inflateTo(
            result,
            R.xml.pref_data_cleanup,
            listOf(context.getString(R.string.storage_and_network)),
            DataCleanupSettingsFragment::class.java,
        )
        preferenceManager.inflateTo(result, R.xml.pref_downloads, emptyList(), DownloadsSettingsFragment::class.java)
        preferenceManager.inflateTo(result, R.xml.pref_tracker, emptyList(), TrackerSettingsFragment::class.java)
        preferenceManager.inflateTo(result, R.xml.pref_services, emptyList(), ServicesSettingsFragment::class.java)
        preferenceManager.inflateTo(result, R.xml.pref_about, emptyList(), AboutSettingsFragment::class.java)
        preferenceManager.inflateTo(
            result,
            R.xml.pref_backup_periodic,
            listOf(context.getString(R.string.backup_restore)),
            PeriodicalBackupSettingsFragment::class.java,
        )
        preferenceManager.inflateTo(
            result,
            R.xml.pref_proxy,
            listOf(context.getString(R.string.storage_and_network)),
            ProxySettingsFragment::class.java,
        )
        preferenceManager.inflateTo(
            result,
            R.xml.pref_suggestions,
            listOf(context.getString(R.string.services)),
            SuggestionsSettingsFragment::class.java,
        )
        preferenceManager.inflateTo(
            result,
            R.xml.pref_discord,
            listOf(context.getString(R.string.services)),
            DiscordSettingsFragment::class.java,
        )
        preferenceManager.inflateTo(
            result,
            R.xml.pref_sources,
            listOf(),
            SourcesSettingsFragment::class.java,
        )
        return result
    }

    private fun PreferenceManager.inflateTo(
        result: MutableList<SettingsItem>,
        @XmlRes resId: Int,
        breadcrumbs: List<String>,
        fragmentClass: Class<out PreferenceFragmentCompat>
    ) {
        val screen = inflateFromResource(context, resId, null)
        val screenTitle = screen.title?.toString()
        screen.inflateTo(
            result = result,
            breadcrumbs = if (screenTitle.isNullOrEmpty()) breadcrumbs else breadcrumbs + screenTitle,
            fragmentClass = fragmentClass,
        )
    }

    private fun PreferenceScreen.inflateTo(
        result: MutableList<SettingsItem>,
        breadcrumbs: List<String>,
        fragmentClass: Class<out PreferenceFragmentCompat>
    ): Unit = repeat(preferenceCount) { i ->
        val pref = this[i]
        if (pref is PreferenceScreen) {
            val screenTitle = pref.title?.toString()
            pref.inflateTo(
                result = result,
                breadcrumbs = if (screenTitle.isNullOrEmpty()) breadcrumbs else breadcrumbs + screenTitle,
                fragmentClass = fragmentClass,
            )
        } else {
            result.add(
                SettingsItem(
                    key = pref.key ?: return@repeat,
                    title = pref.title ?: return@repeat,
                    breadcrumbs = breadcrumbs,
                    fragmentClass = fragmentClass,
                ),
            )
        }
    }
}
