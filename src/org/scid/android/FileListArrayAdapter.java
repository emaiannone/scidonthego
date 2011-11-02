package org.scid.android;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FileListArrayAdapter extends ArrayAdapter<String> {

	public FileListArrayAdapter(Context context, int layoutResourceId,
			int textViewResourceId, List<String> fileNames) {
		super(context, layoutResourceId, textViewResourceId, fileNames);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);
		ImageView icon = (ImageView) view.findViewById(R.id.select_file_icon);
		String item = this.getItem(position);
		if (item != null) {
			File itemFile = new File(item);
			if (itemFile.isDirectory()) {
				icon.setImageResource(R.drawable.folder);
			} else {
				icon.setImageResource(R.drawable.scid_file);
			}
			TextView label = (TextView) view
					.findViewById(R.id.select_file_label);
			label.setText(new File(item).getName());
		}
		return view;
	}
}