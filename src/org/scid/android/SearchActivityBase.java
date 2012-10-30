package org.scid.android;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

class SearchActivityBase extends Activity {
	protected boolean onCreateHelper(int id) {
		setContentView(id);
	    Tools.setKeepScreenOn(this, true);
	    return true;
	}

	protected int filterOperation = 0;
	protected void addSpinner() {
		Spinner spinner = (Spinner) findViewById(R.id.search_filter_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.search_filter_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			public void onItemSelected(AdapterView<?> parent, View view, int pos,	long id) {
				filterOperation = pos;
			}
			public void onNothingSelected(AdapterView<?> parent) {
				// do nothing
			}
		});
	}
	
	public void onCancelClick(View view) {
		setResult(RESULT_CANCELED);
		finish();
	}
}