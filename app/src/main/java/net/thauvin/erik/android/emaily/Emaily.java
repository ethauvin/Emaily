/*
 * Emaily.java
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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.Log;
import android.widget.Toast;

import net.thauvin.erik.bitly.Bitly;
import net.thauvin.erik.isgd.Isgd;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;


/**
 * The <code>Emaily</code> class implements a URL shortener intent.
 *
 * @author <a href="mailto:erik@thauvin.net">Erik C. Thauvin</a>
 * @since 1.0
 */
public class Emaily extends Activity {
    private String appName;
    private SharedPreferences sharedPrefs;

    /**
     * Validates a string.
     *
     * @param s The string to validate.
     * @return returns <code>true</code> if the string is not empty or null, <code>false</code>
     *         otherwise.
     */
    public static boolean isValid(String s) {
        return (s != null) && (!s.trim().isEmpty());
    }

    /**
     * Returns the value of the specified shared reference based on the specified string id.
     *
     * @param id           The string id.
     * @param defaultValue The default value, used if the preference is empty.
     * @return The preference value.
     */
    @SuppressWarnings("SameParameterValue")
    private boolean getBoolPref(int id, boolean defaultValue) {
        return sharedPrefs.getBoolean(getString(id), defaultValue);
    }

    /**
     * Returns the value of the specified shared reference based on the specified string id. The
     * default value is an empty string.
     *
     * @param id The string id.
     * @return The preference value.
     */
    @SuppressWarnings("SameParameterValue")
    private String getPref(int id) {
        return getPref(id, "");
    }

    /**
     * Returns the value of the specified shared reference based on the specified string id.
     *
     * @param id           The string id.
     * @param defaultValue The default value, used if the preference is empty.
     * @return The preference value.
     */
    @SuppressWarnings("SameParameterValue")
    private String getPref(int id, String defaultValue) {
        return sharedPrefs.getString(getString(id), defaultValue);
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        appName = getString(R.string.app_name);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            final boolean isGd = getBoolPref(R.string.prefs_key_isgd_enabled, true);
            startEmailyTask(intent, isGd);
        } else {
            Emaily.this.finish();
        }

    }

    /**
     * Starts the task.
     *
     * @param intent The original intent.
     * @param isGd   The is.gd flag.
     */
    private void startEmailyTask(final Intent intent, final boolean isGd) {
        final EmailyTask task;

        if (isGd) {
            //noinspection ConstantConditions
            task = new EmailyTask("", isGd);
        } else {
            //noinspection ConstantConditions
            task = new EmailyTask(getPref(R.string.prefs_key_bitly_apikey), isGd);
        }

        task.execute(intent);
    }


    /**
     * The <code>EmailyResult</code> class.
     */
    private static class EmailyResult {
        private final Intent intent;
        private int code = 0;
        private String message;

        public EmailyResult(Intent intent) {
            this.intent = intent;
        }

        public int getCode() {
            return code;
        }

        @SuppressWarnings("unused")
        public Intent getIntent() {
            return intent;
        }

        public String getMessage() {
            if (isValid(message)) {
                return message;
            } else {
                return "";
            }
        }

        public boolean hasError() {
            return code != 0;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * The <code>EmailyTask</code> class.
     */
    @SuppressLint("StaticFieldLeak")
    private class EmailyTask extends AsyncTask<Intent, Void, EmailyResult> {
        private final ProgressDialog dialog =
                new ProgressDialog(Emaily.this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        private final boolean isGd;
        private final String keytoken;

        public EmailyTask(String keytoken, boolean isGd) {
            this.keytoken = keytoken;
            this.isGd = isGd;
        }

        @Override
        protected EmailyResult doInBackground(Intent... intent) {
            final EmailyResult result = new EmailyResult(intent[0]);

            final Intent emailIntent = new Intent(Intent.ACTION_SEND);
            final Bundle extras = intent[0].getExtras();
            final String pageUrl;
            final String pageTitle;

            emailIntent.setType("text/plain");

            if (extras != null) {
                pageUrl = extras.getString(Intent.EXTRA_TEXT);
                pageTitle = extras.getString(Intent.EXTRA_SUBJECT);
            } else {
                pageTitle = null;
                pageUrl = null;
            }

            final StringBuilder textBefore = new StringBuilder();

            if (isValid(pageTitle)) {
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, pageTitle);
            }

            final StringBuilder shortUrl = new StringBuilder();

            if (isValid(pageUrl)) {
                final String[] splits = pageUrl.split("\\s");

                for (String item : splits) {
                    try {
                        new URL(item.trim());

                        try {
                            if (isGd) {
                                Log.d(appName, "is.gd -> " + item);
                                shortUrl.append(Isgd.shorten(item));
                            } else {
                                final Bitly bitly = new Bitly(keytoken);
                                shortUrl.append(bitly.bitlinks().shorten(item));
                                if (shortUrl.toString().equals(item)) {
                                    result.setCode(R.string.alert_error);
                                    //@TODO fixme
                                    result.setMessage("TBD");
                                }
                            }
                        } catch (Exception e) {
                            final Throwable cause = e.getCause();

                            if (cause instanceof UnknownHostException) {
                                result.setCode(R.string.alert_nohost);
                                result.setMessage(cause.getMessage());
                            } else {
                                result.setCode(R.string.alert_error);
                                result.setMessage(e.getMessage());
                            }
                        }

                        break;
                    } catch (MalformedURLException mue) {
                        Log.d(appName, "Attempted to process an invalid URL: " + item, mue);

                        if (textBefore.length() > 0) {
                            textBefore.append(" ");
                        }

                        textBefore.append(item);
                    }
                }
            } else {
                result.setCode(R.string.alert_nocreds);
            }

            if (shortUrl.length() > 0) {
                    emailIntent.putExtra(Intent.EXTRA_TEXT, shortUrl.toString());
                    Log.d(appName, "URL: " + emailIntent.getStringExtra(Intent.EXTRA_TEXT));

                if (!isValid(pageTitle) && textBefore.length() > 0) {
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, textBefore.toString());
                }
            } else {
                if (extras != null) {
                    final CharSequence chars = extras.getCharSequence(Intent.EXTRA_TEXT);

                    if (chars != null && chars.length() > 0) {
                        emailIntent.putExtra(Intent.EXTRA_TEXT, chars);
                    } else if (isValid(pageUrl)) {
                        emailIntent.putExtra(Intent.EXTRA_TEXT, pageUrl);
                    }
                }
            }

            try {
                startActivity(emailIntent);
            } catch (android.content.ActivityNotFoundException ignore) {
                if (!result.hasError() && shortUrl.length() > 0) {
                    @SuppressWarnings("deprecation")
                    final ClipboardManager clip =
                            (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clip != null) {
                        //noinspection deprecation
                        clip.setText(shortUrl);
                    }

                    result.setCode(R.string.alert_notfound_clip);
                } else {
                    result.setCode(R.string.alert_notfound);
                }
            }

            return result;
        }

        @Override
        protected void onPreExecute() {
            this.dialog.setMessage(getString(R.string.progress_msg));
            this.dialog.show();
        }

        @Override
        protected void onPostExecute(EmailyResult result) {
            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }

            if (result.hasError()) {
                Toast.makeText(
                        getApplicationContext(),
                        getString(result.getCode(),
                                  result.getMessage(),
                                  isGd ? getString(R.string.prefs_isgd_title)
                                       : getString(R.string.prefs_bitly_title)), Toast.LENGTH_LONG)
                     .show();

            }

            Emaily.this.finish();
        }
    }
}
