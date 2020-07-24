package me.steenman.injectview;

import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.StringBuilder;
import java.util.ArrayList;

public class InjectView extends CordovaPlugin {
	private static final String TAG = "cordova-plugin-injectview";

	private CordovaActivity activity;

	@Override
	public void pluginInitialize() {
		Log.v(TAG, "Plugin cordova-plugin-injectview loaded.");
		this.activity = (CordovaActivity)this.cordova.getActivity();
	}

	@Override
	public Object onMessage(String id, Object data) {
		if ("onPageFinished".equals(id)) {
			injectJavascriptFiles(getCordovaFiles());
		}

		return null;
	}

	private ArrayList<String> getCordovaFiles() {
		ArrayList<String> scripts = new ArrayList();

		try {
			String filenamesJSON = getFileContents("www/cordova-plugin-injectview.json");
			JSONArray filenames = new JSONArray(filenamesJSON);

			for (int i = 0; i < filenames.length(); i++) {
				String filename = filenames.getString(i);
				scripts.add(filename);
			}
		} catch (JSONException e) {
			Log.e(TAG, "Failed to load Cordova script manifest.");
			e.printStackTrace();
		}

		return scripts;
	}

	private void injectJavascriptFiles(ArrayList<String> filenames) {
		this.cordova.getThreadPool().execute(() -> {
			// Concatenate all scripts into one large script.
			StringBuilder script = new StringBuilder();
			for (String filename : filenames) {
				String content = getFileContents(filename);
				if (content == null || content.isEmpty()) {
					continue;
				}

				script.append(content);
				script.append("\n;\n");
			}

			// Inject scripts by direct evaluation in the WebView.
			String expression = script.toString();
			this.activity.runOnUiThread(() -> {
				try {
					this.webView.getEngine().evaluateJavascript(expression, null);
				} catch (Exception e) {
					Log.e(TAG, "Failed to inject scripts.");
					e.printStackTrace();
				}
			});
		});
	}

	private String getFileContents(String filename) {
		try (InputStream stream = this.activity.getResources().getAssets().open(filename)) {
			int size = stream.available();
			byte[] bytes = new byte[size];
			stream.read(bytes);
			return new String(bytes, "UTF-8");
		} catch (IOException e) {
			Log.e(TAG, String.format("Failed to read file: %s.", filename));
		}

		return null;
	}
}