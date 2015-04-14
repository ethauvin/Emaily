/*
 * @(#)Emaily.java
 *
 * Copyright (c) 2011-2012 Erik C. Thauvin (http://erik.thauvin.net/)
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
 *
 * $Id$
 *
 */
package net.thauvin.erik.android.emaily;

import static com.rosaloves.bitlyj.Bitly.as;
import static com.rosaloves.bitlyj.Bitly.shorten;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpRequest;
import com.google.api.client.http.json.JsonHttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.urlshortener.Urlshortener;
import com.google.api.services.urlshortener.UrlshortenerRequest;
import com.google.api.services.urlshortener.model.Url;

/**
 * The <code>Emaily</code> class implements a URL shortener intent.
 *
 * @author <a href="mailto:erik@thauvin.net">Erik C. Thauvin</a>
 * @version $Revision$
 * @created Oct 11, 2011
 * @since 1.0
 */
@SuppressWarnings("deprecation")
public class Emaily extends Activity
{
    private static final String ACCOUNT_TYPE = "com.google";
    private static final String OAUTH_URL = "oauth2:https://www.googleapis.com/auth/urlshortener";

    private String appName;
    private SharedPreferences sharedPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        appName = getString(R.string.app_name);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (Intent.ACTION_SEND.equals(intent.getAction()))
        {
            final boolean isGoogl = getBoolPref(R.string.prefs_key_googl_enabled, true);

            if (isGoogl)
            {
                final String account = getPref(R.string.prefs_key_googl_account);

                if (isValid(account))
                {
                    startEmailyTask(intent, new Account(account, ACCOUNT_TYPE), false);
                }
                else
                {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                    builder.setTitle(R.string.dialog_accounts_title);

                    final Account[] accounts = AccountManager.get(this).getAccountsByType(ACCOUNT_TYPE);
                    final int size = accounts.length;
                    if (size > 0)
                    {
                        if (size == 1)
                        {
                            startEmailyTask(intent, accounts[0], false);
                        }
                        else
                        {
                            final CharSequence[] names = new CharSequence[size];
                            for (int i = 0; i < size; i++)
                            {
                                names[i] = accounts[i].name;
                            }

                            builder.setSingleChoiceItems(names, 0, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();

                                    final Editor editor = sharedPrefs.edit();
                                    editor.putString(getString(R.string.prefs_key_googl_account), names[which].toString());
                                    editor.putLong(getString(R.string.prefs_key_googl_token_expiry), 0L);
                                    editor.commit();

                                    startEmailyTask(intent, accounts[which], false);
                                }

                            });

                            builder.create().show();
                        }
                    }
                    else
                    {
                        startEmailyTask(intent, isGoogl, false);
                    }
                }
            }
            else
            {
                startEmailyTask(intent, isGoogl, false);
            }
        }
        else
        {
            Emaily.this.finish();
        }

    }

    /**
     * Starts the task.
     *
     * @param intent  The original intent.
     * @param isGoogl The goo.gl flag.
     * @param isRetry The retry flag.
     */
    private void startEmailyTask(final Intent intent, final boolean isGoogl, final boolean isRetry)
    {
        final EmailyTask task;

        if (isGoogl)
        {
            task = new EmailyTask(getPref(R.string.prefs_key_googl_account), getPref(R.string.prefs_key_googl_token), isGoogl,
                    getBoolPref(R.string.prefs_key_html_chkbox), isRetry);
        }
        else
        {
            task = new EmailyTask(getPref(R.string.prefs_key_bitly_username), getPref(R.string.prefs_key_bitly_apikey), isGoogl,
                    getBoolPref(R.string.prefs_key_html_chkbox), isRetry);
        }

        task.execute(intent);
    }

    /**
     * Starts the task.
     *
     * @param intent  The original intent.
     * @param account The account.
     * @param isRetry The retry flag.
     */
    private void startEmailyTask(final Intent intent, final Account account, final boolean isRetry)
    {
        final GoogleAccountManager googleAccountManager = new GoogleAccountManager(Emaily.this);

        final long expiry = sharedPrefs.getLong(getString(R.string.prefs_key_googl_token_expiry), 0L);
        final long now = System.currentTimeMillis();
        final long maxLife = (60L * 55L) * 1000L; // 55 minutes

        Log.d(appName, "Token Expires: " + new Date(expiry));

        if (expiry >= (now + maxLife) || expiry <= now)
        {
            final String token = getPref(R.string.prefs_key_googl_token);
            if (isValid(token))
            {
                googleAccountManager.manager.invalidateAuthToken(ACCOUNT_TYPE, token);

                Log.d(appName, "Token Invalidated: " + token);
            }
        }

        googleAccountManager.manager.getAuthToken(account, OAUTH_URL, null, Emaily.this, new AccountManagerCallback<Bundle>()
        {
            @Override
            public void run(AccountManagerFuture<Bundle> future)
            {
                try
                {
                    final String token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                    final Editor editor = sharedPrefs.edit();
                    final long now = System.currentTimeMillis();

                    final long expires;
                    if (expiry < now)
                    {
                        expires = now + maxLife;
                    }
                    else
                    {
                        expires = expiry;
                    }

                    editor.putLong(getString(R.string.prefs_key_googl_token_expiry), expires);
                    editor.putString(getString(R.string.prefs_key_googl_token), token);
                    editor.commit();

                    Log.d(appName, account.toString());
                    Log.d(appName, "Token: " + token);
                    Log.d(appName, "Expires: " + new Date(expires));

                    startEmailyTask(intent, true, isRetry);

                }
                catch (OperationCanceledException e)
                {
                    Log.e(appName, "Auth token request has been canceled.", e);
                }
                catch (Exception e)
                {
                    Log.e(appName, "Exception while requesting the auth token.", e);
                }
            }
        }, null);
    }

    /**
     * Retries the task.
     *
     * @param intent The original intent.
     */
    public void retry(final Intent intent)
    {
        sharedPrefs.edit().putLong(getString(R.string.prefs_key_googl_token_expiry), 0L).commit();

        startEmailyTask(intent, new Account(getPref(R.string.prefs_key_googl_account), ACCOUNT_TYPE), true);
    }

    /**
     * Validates a string.
     *
     * @param s The string to validate.
     * @return returns <code>true</code> if the string is not empty or null, <code>false</code> otherwise.
     */
    public static boolean isValid(String s)
    {
        return (s != null) && (!s.trim().isEmpty());
    }

    /**
     * Returns the value of the specified shared reference based on the specified string id. The default value is an empty string.
     *
     * @param id The string id.
     * @return The preference value.
     */
    public String getPref(int id)
    {
        return getPref(id, "");
    }

    /**
     * Returns the value of the specified shared reference based on the specified string id.
     *
     * @param id           The string id.
     * @param defaultValue The default value, used if the preference is empty.
     * @return The preference value.
     */
    public String getPref(int id, String defaultValue)
    {
        return sharedPrefs.getString(getString(id), defaultValue);
    }

    /**
     * Returns the value of the specified shared reference based on the specified string id. The default value is <code>false</code>.
     *
     * @param id The string id.
     * @return The preference value.
     */
    public boolean getBoolPref(int id)
    {
        return getBoolPref(id, false);
    }

    /**
     * Returns the value of the specified shared reference based on the specified string id.
     *
     * @param id           The string id.
     * @param defaultValue The default value, used if the preference is empty.
     * @return The preference value.
     */
    public boolean getBoolPref(int id, boolean defaultValue)
    {
        return sharedPrefs.getBoolean(getString(id), defaultValue);
    }

    /**
     * The <code>EmailyTask</code> class.
     */
    private class EmailyTask extends AsyncTask<Intent, Void, EmailyResult>
    {
        private final ProgressDialog dialog = new ProgressDialog(Emaily.this);
        private final String username;
        private final String keytoken;
        private final boolean isGoogl;
        private final boolean isHtml;
        private final boolean isRetry;

        public EmailyTask(String username, String keytoken, boolean isGoogl, boolean isHtml, boolean isRetry)
        {
            this.username = username;
            this.keytoken = keytoken;
            this.isGoogl = isGoogl;
            this.isHtml = isHtml;
            this.isRetry = isRetry;
        }

        @Override
        protected EmailyResult doInBackground(Intent... intent)
        {
            final EmailyResult result = new EmailyResult(intent[0]);

            final Intent emailIntent = new Intent(Intent.ACTION_SEND);

            if (isHtml)
            {
                emailIntent.setType("text/html");
            }
            else
            {
                emailIntent.setType("text/plain");
            }


            final Bundle extras = intent[0].getExtras();

            final String pageUrl = extras.getString(Intent.EXTRA_TEXT);
            final String pageTitle = extras.getString(Intent.EXTRA_SUBJECT);
            final StringBuilder textBefore = new StringBuilder();

            if (isValid(pageTitle))
            {
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, pageTitle);
            }

            final boolean hasCredentials = isValid(username) && isValid(keytoken);
            final StringBuilder shortUrl = new StringBuilder();

            if (isValid(pageUrl))
            {
                final HttpTransport transport = new NetHttpTransport();
                final JsonFactory jsonFactory = new JacksonFactory();

                String version = "";

                try
                {
                    version = '/' + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                }
                catch (NameNotFoundException ignore)
                {
                    // Do nothing;
                }

                final Url toInsert = new Url();

                final String[] splits = pageUrl.split("\\s");

                for (String item : splits)
                {
                    try
                    {
                        new URL(item.trim());

                        if (isGoogl || !hasCredentials)
                        {
                            Log.d(appName, "goo.gl -> " + item);

                            final Urlshortener shortener = Urlshortener.builder(transport, jsonFactory).setApplicationName(appName + version)
                                    .setJsonHttpRequestInitializer(new JsonHttpRequestInitializer()
                                    {
                                        @Override
                                        public void initialize(JsonHttpRequest request) throws IOException
                                        {
                                            UrlshortenerRequest shortnerRequest = (UrlshortenerRequest) request;

                                            shortnerRequest.setKey(getString(R.string.secret_apikey));

                                            if (isValid(keytoken))
                                            {
                                                shortnerRequest.setOauthToken(keytoken);

                                            }
                                            shortnerRequest.put("client_id", getString(R.string.secret_client_id));
                                            shortnerRequest.put("client_secret", getString(R.string.secret_client_secret));
                                        }
                                    }).build();

                            toInsert.setLongUrl(item.trim());

                            try
                            {
                                final Url shortened = shortener.url().insert(toInsert).execute();

                                shortUrl.append(shortened.getId());
                            }
                            catch (GoogleJsonResponseException e)
                            {
                                result.setCode(R.string.alert_error);

                                final GoogleJsonError err = e.getDetails();

                                result.setMessage(err.message);

                                if (err.code == 401)
                                {
                                    if (!isRetry)
                                    {
                                        result.setRetry(true);
                                    }
                                }

                                Log.e(appName, "Exception while shortening '" + item + "' via goo.gl.", e);
                            }
                            catch (UnknownHostException e)
                            {
                                result.setCode(R.string.alert_nohost);
                                result.setMessage(e.getMessage());

                                Log.e(appName, "UnknownHostException while shortening '" + item + "' via goo.gl.", e);
                            }
                            catch (IOException e)
                            {
                                result.setCode(R.string.alert_error);
                                result.setMessage(e.getMessage());

                                Log.e(appName, "IOException while shortening '" + item + "' via goo.gl.", e);
                            }
                        }
                        else
                        {
                            Log.d(appName, "bit.ly -> " + item);

                            try
                            {
                                shortUrl.append(as(username, keytoken).call(shorten(item.trim())).getShortUrl());
                            }
                            catch (Exception e)
                            {
                                final Throwable cause = e.getCause();

                                if (cause != null && cause instanceof UnknownHostException)
                                {
                                    result.setCode(R.string.alert_nohost);
                                    result.setMessage(cause.getMessage());
                                }
                                else
                                {
                                    result.setCode(R.string.alert_error);
                                    result.setMessage(e.getMessage());
                                }

                                Log.e(appName, "Exception while shortening '" + item + "' via bit.ly.", e);
                            }

                            break;
                        }

                        break;
                    }
                    catch (MalformedURLException mue)
                    {
                        Log.d(appName, "Attempted to process an invalid URL: " + item, mue);

                        if (textBefore.length() > 0)
                        {
                            textBefore.append(" ");
                        }

                        textBefore.append(item);
                    }
                }
            }
            else
            {
                result.setCode(R.string.alert_nocreds);
            }

            if (!result.isRetry())
            {
                if (shortUrl.length() > 0)
                {
                    if (isHtml)
                    {
                        emailIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml("<a href=\"" + shortUrl + "\">" + shortUrl + "</a>"));
                    }
                    else
                    {
                        emailIntent.putExtra(Intent.EXTRA_TEXT, shortUrl.toString());
                    }

                    if (!isValid(pageTitle) && textBefore.length() > 0)
                    {
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, textBefore.toString());
                    }
                }
                else
                {
                    final CharSequence chars = extras.getCharSequence(Intent.EXTRA_TEXT);

                    if (chars.length() > 0)
                    {
                        emailIntent.putExtra(Intent.EXTRA_TEXT, chars);
                    }
                    else if (isValid(pageUrl))
                    {
                        emailIntent.putExtra(Intent.EXTRA_TEXT, pageUrl);
                    }
                }

                try
                {
                    startActivity(emailIntent);
                }
                catch (android.content.ActivityNotFoundException ignore)
                {
                    if (!result.hasError() && shortUrl.length() > 0)
                    {
                        final ClipboardManager clip = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        clip.setText(shortUrl);

                        result.setCode(R.string.alert_notfound_clip);
                    }
                    else
                    {
                        result.setCode(R.string.alert_notfound);
                    }
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(EmailyResult result)
        {
            if (this.dialog.isShowing())
            {
                this.dialog.dismiss();
            }

            if (result.isRetry())
            {
                Emaily.this.retry(result.getIntent());
            }
            else
            {
                if (result.hasError())
                {
                    Toast.makeText(
                            getApplicationContext(),
                            getString(result.getCode(), result.getMessage(), isGoogl ? getString(R.string.prefs_googl_title)
                                    : getString(R.string.prefs_bitly_title)), Toast.LENGTH_LONG).show();

                }

                Emaily.this.finish();
            }
        }

        @Override
        protected void onPreExecute()
        {
            if (isRetry)
            {
                this.dialog.setMessage(getString(R.string.progress_msg_retry));
            }
            else
            {
                this.dialog.setMessage(getString(R.string.progress_msg));
            }

            this.dialog.show();
        }
    }

    /**
     * The <code>EmailyResult</code> class.
     */
    private class EmailyResult
    {
        private int code = 0;
        private String message;
        private boolean retry = false;
        private final Intent intent;

        public EmailyResult(Intent intent)
        {
            this.intent = intent;
        }

        public int getCode()
        {
            return code;
        }

        public String getMessage()
        {
            if (isValid(message))
            {
                return message;
            }
            else
            {
                return "";
            }
        }

        public Intent getIntent()
        {
            return intent;
        }

        public boolean hasError()
        {
            return code != 0;
        }

        public boolean isRetry()
        {
            return retry;
        }

        public void setCode(int code)
        {
            this.code = code;
        }

        public void setMessage(String message)
        {
            this.message = message;
        }

        public void setRetry(boolean retry)
        {
            this.retry = retry;
        }
    }
}