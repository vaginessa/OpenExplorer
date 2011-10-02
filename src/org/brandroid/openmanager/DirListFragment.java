package org.brandroid.openmanager;

import java.io.File;

import org.brandroid.openmanager.adapters.RecursiveDirectoryAdapter;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class DirListFragment extends Fragment
{
	ListView mList;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View ret = inflater.inflate(R.layout.directory_tree_list, null);
		mList = (ListView)ret.findViewById(R.id.list);
		return ret;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		if(mList == null)
			mList = (ListView)view.findViewById(R.id.list);
		RecursiveDirectoryAdapter adapter = new RecursiveDirectoryAdapter(new File("/"), getActivity());
		mList.setAdapter(adapter);
		mList.setOnItemClickListener(adapter);
		super.onViewCreated(view, savedInstanceState);
	}
}
