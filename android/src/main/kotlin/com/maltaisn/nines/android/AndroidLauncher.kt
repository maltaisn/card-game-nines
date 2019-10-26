/*
 * Copyright 2019 Nicolas Maltais
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

package com.maltaisn.nines.android

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.maltaisn.nines.core.GameApp
import com.maltaisn.nines.core.GameListener


class AndroidLauncher : AndroidApplication(), GameListener {

    private lateinit var inputDialog: AlertDialog
    private lateinit var inputField: EditText

    override val isTextInputDelegated = true

    override var isFullscreen = false
        set(value) {
            field = value

            // Enable or disable fullscreen
            runOnUiThread {
                var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                if (Build.VERSION.SDK_INT >= 19) {
                    // Fullscreen doesn't work really well before API 19 without immersive...
                    flags = flags or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                }
                window.decorView.systemUiVisibility = if (value) {
                    window.decorView.systemUiVisibility or flags
                } else {
                    window.decorView.systemUiVisibility and flags.inv()
                }
            }
        }


    override val isRateAppSupported = true


    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        // Input dialog
        val view = layoutInflater.inflate(R.layout.dialog_input, null, false)
        inputField = view.findViewById(R.id.input_field)
        inputField.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener if (actionId == EditorInfo.IME_ACTION_DONE) {
                inputDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                true
            } else {
                false
            }
        }
        inputDialog = AlertDialog.Builder(this).apply {
            setView(view)
            setPositiveButton(R.string.action_ok, null)
            setNegativeButton(R.string.action_cancel, null)
        }.create()

        // Start game
        val config = AndroidApplicationConfiguration()
        config.useAccelerometer = false
        config.useCompass = false
        initialize(GameApp(this), config)
    }


    override fun onRateAppClicked() {
        // Open play store app page
        try {
            startActivity(rateIntentForUrl("market://details"))
        } catch (e: ActivityNotFoundException) {
            startActivity(rateIntentForUrl("https://play.google.com/store/apps/details"))
        }
    }

    override fun onReportBugClicked() {
        // Open email app with device info in body
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "support@maltaisn.com", null))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Send feedback - Nines")
        startActivity(Intent.createChooser(emailIntent, "Send feedback"))
    }

    private fun rateIntentForUrl(url: String): Intent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(String.format("%s?id=%s", url, packageName)))
        val flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                if (Build.VERSION.SDK_INT >= 21) {
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                } else {
                    Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
                }
        intent.addFlags(flags)
        return intent
    }

    override fun onTextInput(text: CharSequence?, title: CharSequence?,
                             onTextEntered: (String) -> Unit) {
        runOnUiThread {
            inputField.apply {
                setText(text)
                requestFocus()
                setSelection(text?.length ?: 0)
            }
            inputDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

            inputDialog.apply {
                setTitle(title)
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        inputDialog.dismiss()
                        onTextEntered(inputField.text.toString())

                    }
                    getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { cancel() }
                }
                setOnCancelListener {
                    onTextEntered(text.toString())
                }
                setOnDismissListener {
                    // Update fullscreen which will have been disabled.
                    isFullscreen = isFullscreen
                }
                show()
            }
        }
    }

}
