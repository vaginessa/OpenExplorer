package org.brandroid.openmanager.activities;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.util.OpenChromeClient;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WebViewFragment
		extends DialogFragment
{
	private Uri uri;
	private WebView web;
	
	public WebViewFragment() {
	}
	
	public WebViewFragment setUri(Uri uri) {
		this.uri = uri;
		if(web != null && isVisible())
			web.loadUrl(uri.toString());
		return this;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.webview, null);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if(view != null && view instanceof WebView)
			web = (WebView)view;
		else if(view.findViewById(R.id.webview) != null)
			web = (WebView)view.findViewById(R.id.webview);
		OpenChromeClient client = new OpenChromeClient();
		web.setWebViewClient(new MyWebViewClient());
		web.setWebChromeClient(client);
		WebSettings settings = web.getSettings();
		settings.setJavaScriptEnabled(true);
		if(savedInstanceState != null && savedInstanceState.containsKey("url"))
			uri = Uri.parse(savedInstanceState.getString("url"));
		else if(getArguments() != null && getArguments().containsKey("url"))
			uri = Uri.parse(savedInstanceState.getString("url"));
		if(uri != null)
			web.loadUrl(uri.toString());
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(savedInstanceState != null && savedInstanceState.containsKey("url"))
			setUri(Uri.parse(savedInstanceState.getString("url")));
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(web != null)
		{
			uri = Uri.parse(web.getUrl());
			if(outState != null)
				outState.putString("url", web.getUrl());
		}
	}
	
	public class MyWebViewClient extends WebViewClient
	{
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			return false;
		}
		
		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			super.onReceivedError(view, errorCode, description, failingUrl);
			Toast.makeText(getActivity(), description, Toast.LENGTH_LONG);
		}
		
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			if(getShowsDialog())
			{
				getDialog().setTitle(R.string.s_status_loading);
				if(view != null && view.getTitle() != null)
					getDialog().setTitle(view.getTitle());
			}
			view.setEnabled(false);
			//getDialog().setFeatureDrawable(Window.FEATURE_LEFT_ICON, new BitmapDrawable(getResources(), favicon));
		}
		
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			if(getShowsDialog())
				getDialog().setTitle(view.getTitle());
			view.setEnabled(true);
		}
	}
}
