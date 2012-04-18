package org.brandroid.openmanager.fragments;

import java.util.ArrayList;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.utils.MenuUtils;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.ClipboardManager;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

public class LogViewerFragment extends DialogFragment implements OnClickListener
{
	private final SpannableStringBuilder builder;
	private TextView mTextLog;
	
	public LogViewerFragment() {
		builder = new SpannableStringBuilder();
	}
	
	public void print(final String txt, final int color)
	{
		builder.insert(0, colorify(txt, color));
		if(isVisible())
			getActivity().runOnUiThread(new Runnable() {public void run() {
				if(mTextLog != null)
					mTextLog.setText(builder);
			}});
	}
	private CharSequence colorify(String txt, int color)
	{
		if(color != 0)
		{
			SpannableString line = new SpannableString(txt + "\n");
			line.setSpan(new ForegroundColorSpan(color), 0, line.length(), Spanned.SPAN_COMPOSING);
			return line;
		} else return txt + "\n";
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.log_viewer, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mTextLog = (TextView)view.findViewById(R.id.log_text);
		MenuUtils.setOnClicks(view, this, R.id.log_clear, R.id.log_clear, R.id.log_copy, R.id.log_close);
		if(getShowsDialog())
		{
			getDialog().setTitle(R.string.s_pref_logview);
			MenuUtils.setViewsVisible(view, false, android.R.id.title, R.id.log_close);
		}
	}
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		if(getExplorer() != null && getExplorer().getPreferences() != null)
			getExplorer().getPreferences().setSetting(null, "pref_logview", false);
		super.onDismiss(dialog);
	}
	
	public OpenExplorer getExplorer() { return (OpenExplorer)getActivity(); }

	@SuppressWarnings("deprecation")
	@Override
	public void onClick(View v) {
		switch(v.getId())
		{
		case R.id.log_clear:
			builder.clear();
			mTextLog.setText("");
			break;
		case R.id.log_copy:
			((ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE))
				.setText(mTextLog.getText().toString());
			break;
		default:
			getExplorer().onClick(v);
		}
	}
}
