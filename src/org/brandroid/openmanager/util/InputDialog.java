package org.brandroid.openmanager.util;

import org.brandroid.openmanager.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class InputDialog extends Builder
{
	private View view;
	private EditText mEdit, mEdit2;
	
	public InputDialog(Context mContext) {
		super(mContext);
		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = inflater.inflate(R.layout.input_dialog_layout, null);
		mEdit = (EditText)view.findViewById(R.id.dialog_input);
		mEdit2 = (EditText)view.findViewById(R.id.dialog_input_top);
		setViewVisible(false, R.id.dialog_message, R.id.dialog_message_top);
		super.setView(view);
	}
	
	public void setViewVisible(boolean visible, int... ids)
	{
		for(int id : ids)
			if(view.findViewById(id) != null)
				view.findViewById(id).setVisibility(visible ? View.VISIBLE : View.GONE);
	}
	
	public InputDialog setTitle(int resId) { super.setTitle(resId); return this; }
	public InputDialog setIcon(int resId) { super.setIcon(resId); return this; }
	
	public String getInputTopText() { return mEdit2.getText().toString(); }
	public String getInputText() { return mEdit.getText().toString(); }
	
	public InputDialog setDefaultText(String s) 
	{
		mEdit.setText(s);
		return this;
	}

	public InputDialog setDefaultTop(CharSequence s) { mEdit2.setText(s); return this; }
	
	public InputDialog setPrompt(String s) {
		((TextView)view.findViewById(R.id.dialog_message_top)).setVisibility(View.VISIBLE);
		((EditText)view.findViewById(R.id.dialog_message_top)).setText(s);
		return this;
	}
	
	@Override
	public InputDialog setMessage(CharSequence message) {
		super.setMessage(message);
		return this;
	}
	@Override
	public InputDialog setMessage(int messageId) {
		super.setMessage(messageId);
		return this;
	}
	
	@Override
	public InputDialog setCancelable(boolean cancelable) {
		super.setCancelable(cancelable);
		return this;
	}
	
	@Override
	public InputDialog setTitle(CharSequence title) {
		super.setTitle(title);
		return this;
	}
	
	@Override
	public InputDialog setNegativeButton(int text, OnClickListener listener) {
		super.setNegativeButton(text, listener);
		return this;
	}
	
	@Override
	public InputDialog setPositiveButton(int text, OnClickListener listener) {
		super.setPositiveButton(text, listener);
		return this;
	}
	
	@Override
	public InputDialog setIcon(Drawable icon) {
		super.setIcon(icon);
		return this;
	}

}