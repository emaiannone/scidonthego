package org.scid.android;

import org.scid.android.gamelogic.ChessController;

import android.os.AsyncTask;

public class StartEngineTask extends AsyncTask {
	private ScidAndroidActivity activity;
	private ChessController ctrl;

	@Override
	protected Object doInBackground(Object... params) {
		this.activity = (ScidAndroidActivity) params[0];
		this.ctrl = (ChessController) params[1];
		String engineFileName = (String) params[2];
		ctrl.startEngine(engineFileName);
		return null;
	}

	@Override
	protected void onPostExecute(Object result) {
		activity.onFinishStartAnalysis();
	}
}
