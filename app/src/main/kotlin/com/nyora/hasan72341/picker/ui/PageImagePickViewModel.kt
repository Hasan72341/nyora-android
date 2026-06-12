package com.nyora.hasan72341.picker.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import com.nyora.hasan72341.core.ui.BaseViewModel
import com.nyora.hasan72341.core.util.ext.MutableEventFlow
import com.nyora.hasan72341.core.util.ext.call
import com.nyora.hasan72341.reader.ui.PageSaveHelper
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PageImagePickViewModel @Inject constructor() : BaseViewModel() {

	val onFileReady = MutableEventFlow<File>()

	fun savePageToTempFile(pageSaveHelper: PageSaveHelper, task: PageSaveHelper.Task) {
		launchLoadingJob(Dispatchers.IO) {
			val file = pageSaveHelper.saveToTempFile(task)
			onFileReady.call(file)
		}
	}
}
