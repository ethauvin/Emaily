/*
 * @(#)Emaily.java
 *
 * Copyright (c) 2012 Erik C. Thauvin (http://erik.thauvin.net/)
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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * The <code>BitlyCredsDialog</code> class implements a bit.ly credential dialog.
 * 
 * @author <a href="mailto:erik@thauvin.net">Erik C. Thauvin</a>
 * @version $Revision$
 * @created March 28, 2012
 * @since 1.0
 */
public class BitlyCredsDialog extends DialogPreference
{
	private final Context mContext;
	private EditText username;
	private EditText apikey;

	public BitlyCredsDialog(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mContext = context;
		setPersistent(false);
	}

	@Override
	protected void onBindDialogView(View view)
	{
		super.onBindDialogView(view);

		final SharedPreferences sharedPrefs = getSharedPreferences();
		username = (EditText) view.findViewById(R.id.bitly_username_edit);
		apikey = (EditText) view.findViewById(R.id.bitly_apikey_edit);
		final TextView textFld = (TextView) view.findViewById(R.id.bitly_text_fld);

		username.setText(sharedPrefs.getString(mContext.getString(R.string.prefs_key_bitly_username), ""));
		apikey.setText(sharedPrefs.getString(mContext.getString(R.string.prefs_key_bitly_apikey), ""));

		textFld.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mContext.getString(R.string.prefs_bitly_creds_url)));
				mContext.startActivity(intent);
			}
		});
	}

	@Override
	protected void onDialogClosed(boolean positiveResult)
	{
		super.onDialogClosed(positiveResult);

		if (positiveResult)
		{
			final SharedPreferences sharedPrefs = getSharedPreferences();
			final Editor editor = sharedPrefs.edit();
			editor.putString(mContext.getString(R.string.prefs_key_bitly_username), username.getText().toString());
			editor.putString(mContext.getString(R.string.prefs_key_bitly_apikey), apikey.getText().toString());
			editor.commit();
		}

	}
}
