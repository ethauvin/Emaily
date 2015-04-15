/*
 * @(#)EmailyPrefs.java
 *
 * Copyright (c) 2011-2015 Erik C. Thauvin (http://erik.thauvin.net/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the authors nor the names of its contributors may be
 * used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.thauvin.erik.android.emaily;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

/**
 * The <code>EmailyPrefs</code> class implements a preferences screen.
 * 
 * @author <a href="mailto:erik@thauvin.net">Erik C. Thauvin</a>
 * @created Oct 11, 2011
 * @since 1.0
 */
@SuppressWarnings("deprecation")
public class EmailyPrefs extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	private SharedPreferences sharedPrefs;

	private CheckBoxPreference googlBox;
	private BitlyCredsDialog bitlyCreds;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.prefs);

		sharedPrefs = getPreferenceScreen().getSharedPreferences();

		googlBox = (CheckBoxPreference) findPreference(getString(R.string.prefs_key_googl_chkbox));
		bitlyCreds = (BitlyCredsDialog) findPreference(getString(R.string.prefs_key_bitly_creds));

		setSummary(bitlyCreds, getString(R.string.prefs_key_bitly_username), getString(R.string.prefs_bitly_creds_summary));
		setSummary(googlBox, getString(R.string.prefs_key_googl_account), "");

		if (googlBox.isChecked())
		{
			bitlyCreds.setEnabled(false);
		}

		final Preference version = findPreference(getString(R.string.prefs_key_version));
		final PreferenceScreen feedback = (PreferenceScreen) findPreference(getString(R.string.prefs_key_feedback));
		try
		{
			final String vNumber = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;

			version.setTitle(getString(R.string.prefs_version_title) + ' ' + vNumber);

			feedback.getIntent().setData(
					Uri.parse(getString(R.string.prefs_feedback_url)
							+ "?subject="
							+ getString(R.string.prefs_feedback_subject, getString(R.string.app_name), vNumber,
									getString(R.string.prefs_feedback_title).toLowerCase(Locale.getDefault()), Build.MANUFACTURER,
									Build.PRODUCT, Build.VERSION.RELEASE)));

		}
		catch (NameNotFoundException ignore)
		{
			// Do nothing.
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		sharedPrefs.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}

	@SuppressLint("CommitPrefEdits")
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if (key.equals(getString(R.string.prefs_key_bitly_username)))
		{
			setSummary(bitlyCreds, key, getString(R.string.prefs_bitly_creds_summary));
		}
		else if (key.equals(getString(R.string.prefs_key_googl_chkbox)))
		{
			final boolean checked = googlBox.isChecked();

			bitlyCreds.setEnabled(!checked);

			final Editor editor = sharedPrefs.edit();
			editor.putBoolean(getString(R.string.prefs_key_googl_enabled), checked);

			if (!checked)
			{
				editor.putString(getString(R.string.prefs_key_googl_account), "");
				editor.putLong(getString(R.string.prefs_key_googl_token_expiry), 0L);
				googlBox.setSummary("");
			}

			editor.commit();
		}
	}

	/**
	 * Sets a preference's summary.
	 * 
	 * @param editPref The preference.
	 * @param key The preference key.
	 * @param defValue The default value.
	 */
	private void setSummary(Preference editPref, String key, String defValue)
	{
		final String value = sharedPrefs.getString(key, defValue);

		if (Emaily.isValid(value))
		{
			editPref.setSummary(value);
		}
		else
		{
			editPref.setSummary(defValue);
		}
	}
}
