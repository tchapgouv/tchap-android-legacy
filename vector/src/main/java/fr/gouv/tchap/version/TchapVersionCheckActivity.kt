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

package fr.gouv.tchap.version

import android.content.Intent
import androidx.core.view.isVisible
import butterknife.OnClick
import fr.gouv.tchap.activity.TchapLoginActivity
import fr.gouv.tchap.config.TchapConfiguration
import im.vector.R
import im.vector.activity.VectorAppCompatActivity
import im.vector.ui.themes.ActivityOtherThemes
import im.vector.util.openPlayStore
import kotlinx.android.synthetic.main.activity_check_version.*
import org.matrix.androidsdk.core.callback.SuccessCallback

class TchapVersionCheckActivity : VectorAppCompatActivity(), SuccessCallback<VersionCheckResult> {
    override fun getLayoutRes() = R.layout.activity_check_version

    override fun onResume() {
        super.onResume()
        VersionChecker.checkVersion(this, this)
    }

    override fun onSuccess(versionCheckResult: VersionCheckResult) {
        when (versionCheckResult) {
            is VersionCheckResult.Ok                -> finish(true)
            is VersionCheckResult.ShowUpgradeScreen -> updateUi(versionCheckResult)
            is VersionCheckResult.Unknown           ->
                // Ok for this time
                finish(true)
        }
    }

    private fun updateUi(versionCheckResult: VersionCheckResult.ShowUpgradeScreen) {
        VersionChecker.onUpgradeScreenDisplayed(this, versionCheckResult.forVersionCode)

        checkVersionProgress.isVisible = false
        checkVersionAllViews.isVisible = true
        checkVersionText.text = versionCheckResult.message
        checkVersionOpen.isVisible = versionCheckResult.canOpenApp
        checkVersionOpen.setText(if (versionCheckResult.displayOnlyOnce) R.string.an_update_is_available_ignore else R.string.an_update_is_available_later)
        checkVersionUpgrade.isVisible = packageName in TchapConfiguration.packageWhiteList
    }

    @OnClick(R.id.checkVersionUpgrade)
    fun openStore() {
        openPlayStore(this, packageName)
    }

    @OnClick(R.id.checkVersionOpen)
    fun openApp() {
        VersionChecker.showLater(this)
        finish(true)
    }

    private fun finish(startNext: Boolean) {
        finish()
        if (startNext) {
            val start = Intent(this, TchapLoginActivity::class.java)
            start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(start)
        }
    }
}
