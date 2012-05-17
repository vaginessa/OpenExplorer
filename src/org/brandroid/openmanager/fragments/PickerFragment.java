package org.brandroid.openmanager.fragments;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.adapters.ContentAdapter;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.utils.MenuUtils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

public class PickerFragment extends OpenFragment
	implements OnItemClickListener, OpenPathFragmentInterface
{
	private Context mContext;
	private OnOpenPathPickedListener mPickListener;
	private View view;
	private GridView mGrid;
	private ContentAdapter mAdapter;
	private TextView mSelection;
	private EditText mPickName;
	private OpenPath mPath;
	private boolean pickDirOnly = true;
	private String mDefaultName;
	
	public PickerFragment(Context context, OpenPath start)
	{
		mContext = context;
		mPath = start;
		Bundle args = getArguments();
		if(args == null)
			args = new Bundle();
		args.putParcelable("start", start);
		//onCreateView((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE), null, null);		
	}
	
	public void setDefaultName(String name)
	{
		mDefaultName = name;
		if(mPickName != null)
			mPickName.setText(name);
		pickDirOnly = false;
	}
	
	public void setPickDirOnly(boolean pickDirOnly)
	{
		this.pickDirOnly = pickDirOnly;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.picker, container, false);
		MenuUtils.setViewsVisible(view, false, android.R.id.button1, android.R.id.button2, android.R.id.title);
		mGrid = (GridView)view.findViewById(android.R.id.list);
		mGrid.setNumColumns(mContext.getResources().getInteger(R.integer.max_grid_columns));
		mSelection = (TextView)view.findViewById(R.id.pick_path);
		mPickName = (EditText)view.findViewById(R.id.pick_filename);
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mGrid.setOnItemClickListener(this);
		if(savedInstanceState != null && savedInstanceState.containsKey("start"))
			mPath = (OpenPath)savedInstanceState.getParcelable("start");
		setPath(mPath);
		if(pickDirOnly)
			mPickName.setVisibility(View.GONE);
		else if(savedInstanceState != null && savedInstanceState.containsKey("name"))
			mPickName.setText(savedInstanceState.getString("name"));
		else mPickName.setText(mDefaultName);
	}
	
	public void setPath(OpenPath path)
	{
		mPath = path;
		mAdapter = new ContentAdapter(mContext, OpenExplorer.VIEW_LIST, mPath);
		mAdapter.setShowPlusParent(path.getParent() != null);
		mAdapter.setShowFiles(false);
		mAdapter.setShowDetails(false);
		mAdapter.updateData();
		mGrid.setAdapter(mAdapter);
		mSelection.setText(mPath.getPath());
	}
	
	public interface OnOpenPathPickedListener
	{
		public void onOpenPathPicked(OpenPath path);
	}
	
	public void setOnOpenPathPickedListener(OnOpenPathPickedListener listener)
	{
		mPickListener = listener;
	}
	
	@Override
	public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
		setPath(mAdapter.getItem(pos));
	}
	
	@Override
	public View getView() {
		return view;
	}
	
	@Override
	public Drawable getIcon() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CharSequence getTitle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OpenPath getPath() {
		OpenPath ret = mPath;
		if(mPickName != null && mPickName.getVisibility() == View.VISIBLE && mPickName.getText() != null)
			ret = ret.getChild(mPickName.getText().toString());
		return ret;
	}

}
