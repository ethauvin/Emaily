/*
 * EmailyPrefs.java
 *
 * Copyright (c) 2011-2020, Erik C. Thauvin (erik@thauvin.net)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *   Neither the name of this project nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.thauvin.erik.android.emaily;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import java.util.Locale;

/**
 * The <code>EmailyPrefs</code> class implements a preferences screen.
 *
 * @author <a href="mailto:erik@thauvin.net">Erik C. Thauvin</a>
 * @since 1.0
 */
@SuppressLint("ExportedPreferenceActivity")
@SuppressWarnings("deprecation")
public class EmailyPrefs extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    private BitlyCredsDialog bitlyCreds;
    private SwitchPreference isgdBox;
    private SharedPreferences sharedPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.prefs);

        sharedPrefs = getPreferenceScreen().getSharedPreferences();

        isgdBox = (SwitchPreference) findPreference(getString(R.string.prefs_key_isgd_chkbox));
        bitlyCreds = (BitlyCredsDialog) findPreference(getString(R.string.prefs_key_bitly_creds));

        setBitlyCredsSummary();

        if (isgdBox.isChecked()) {
            bitlyCreds.setEnabled(false);
        }

        final Preference version = findPreference(getString(R.string.prefs_key_version));
        final PreferenceScreen feedback =
                (PreferenceScreen) findPreference(getString(R.string.prefs_key_feedback));
        try {
            final String vNumber =
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionName;

            version.setTitle(getString(R.string.prefs_version_title) + ' ' + vNumber);

            feedback.getIntent().setData(
                    Uri.parse(getString(R.string.prefs_feedback_url)
                              + "?subject="
                              + getString(R.string.prefs_feedback_subject,
                                          getString(R.string.app_name),
                                          vNumber,
                                          getString(R.string.prefs_feedback_title)
                                                  .toLowerCase(Locale.getDefault()),
                                          Build.MANUFACTURER,
                                          Build.PRODUCT,
                                          Build.VERSION.RELEASE)));

        } catch (NameNotFoundException ignore) {
            // Do nothing.
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.prefs_key_bitly_apikey))) {
            setBitlyCredsSummary();
        } else if (key.equals(getString(R.string.prefs_key_isgd_chkbox))) {
            final boolean checked = isgdBox.isChecked();

            bitlyCreds.setEnabled(!checked);

            final Editor editor = sharedPrefs.edit();
            editor.putBoolean(getString(R.string.prefs_key_isgd_enabled), checked);
            editor.apply();
        }
    }

    /**
     * Sets the bit.ly credentials summary.
     */
    private void setBitlyCredsSummary() {
        if (Emaily.isValid(sharedPrefs.getString(getString(R.string.prefs_key_bitly_apikey), ""))) {
            bitlyCreds.setSummary(getString(R.string.prefs_bitly_creds_summary_edit));
        } else {
            bitlyCreds.setSummary(getString(R.string.prefs_bitly_creds_summary_default));
        }
    }
}
