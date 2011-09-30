package org.brandroid.openmanager;

import java.io.File;

import org.brandroid.openmanager.adapters.RecursiveDirectoryAdapter;

import android.app.ExpandableListActivity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

public class DirListFragment extends Fragment
{
	ExpandableListView mList;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.directory_tree_list, null);
		mList = (ExpandableListView)view.findViewById(R.id.list_tree);
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		RecursiveDirectoryAdapter adapter = new RecursiveDirectoryAdapter(new File("/"), getActivity());
		mList.setAdapter(adapter);
	}
}
