package com.nyora.hasan72341.core.os

import kotlinx.coroutines.Dispatchers
import org.jetbrains.annotations.Blocking
import com.nyora.hasan72341.mihon.parsers.util.suspendlazy.suspendLazy
import java.io.InputStreamReader

object RomCompat {

	val isMiui = suspendLazy(Dispatchers.IO) {
		getProp("ro.miui.ui.version.name").isNotEmpty()
	}

	@Blocking
	private fun getProp(propName: String): String {
		require(propName.matches(Regex("[A-Za-z0-9._-]+"))) { "Invalid property name: $propName" }
		val process = Runtime.getRuntime().exec(arrayOf("getprop", propName))
		return try {
			process.inputStream.use {
				it.reader().use(InputStreamReader::readText).trim()
			}
		} finally {
			process.destroy()
		}
	}
}
