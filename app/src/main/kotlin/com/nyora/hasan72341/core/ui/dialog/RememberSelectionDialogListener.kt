package com.nyora.hasan72341.core.ui.dialog

import android.content.DialogInterface

class RememberSelectionDialogListener(initialValue: Int) : DialogInterface.OnClickListener {

	var selection: Int = initialValue
		private set

	override fun onClick(dialog: DialogInterface?, which: Int) {
		selection = which
	}
}
