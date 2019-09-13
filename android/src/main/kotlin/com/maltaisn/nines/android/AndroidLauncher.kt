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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.maltaisn.nines.core.GameApp
import com.maltaisn.nines.core.GameListener


class AndroidLauncher : AndroidApplication(), GameListener {

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        val config = AndroidApplicationConfiguration()
        config.useAccelerometer = false
        config.useCompass = false

        initialize(GameApp(this), config)
    }


    override val isRateAppSupported = true

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

}
