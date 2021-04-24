/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.gouv.tchap.util

import android.app.Activity
import android.app.Dialog
import androidx.appcompat.app.AlertDialog
import android.widget.NumberPicker
import android.content.DialogInterface
import im.vector.R

class RoomRetentionPeriodPickerDialogFragment(private val activity: Activity) {

    fun create(value: Int, valueChangeListener: (Int) -> Unit): Dialog {

        val numberPicker = NumberPicker(activity)

        numberPicker.minValue = 1
        numberPicker.maxValue = 365
        numberPicker.value = value

        return AlertDialog.Builder(activity)
                .setTitle(R.string.tchap_room_settings_retention_title)
                .setPositiveButton(R.string.ok, DialogInterface.OnClickListener { _, _ ->
                    valueChangeListener.invoke(numberPicker.value)
                })
                .setNegativeButton(R.string.cancel, null)
                .setView(numberPicker)
                .create()
    }
}