package me.steenman.injectview;

import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
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

	private void injectJavascriptFiles(ArrayList<String> files) {
		InjectView injectView = this;

		this.cordova.getThreadPool().execute(new Runnable() {
			@Override
			public void run() {
				for (String file : files) {
					Log.v(TAG, String.format("Injecting script '%s'", file));

					String content = getFileContents(file);
					if (content == null) {
						Log.v(TAG, String.format("Could not load script '%s'", file));
						continue;
					}
					if (content.isEmpty()) {
						Log.v(TAG, String.format("No contents found in script '%s'", file));
						return;
					}
					final String script = "//# sourceURL=" + file + "\r\n" + content + "\r\nconsole.log('Loaded script "
							+ file + "');";
					injectView.activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							View webView = injectView.webView.getEngine().getView();
							ValueCallback<String> resultCallback = null;
							try {
								Method evaluateJavascriptMethod = webView.getClass().getMethod("evaluateJavascript",
										new Class[] { String.class,
												(Class<ValueCallback<String>>) (Class<?>) ValueCallback.class });
								evaluateJavascriptMethod.invoke(webView, script, resultCallback);
							} catch (Exception e) {
								Log.v(TAG, String.format(
										"WARNING: Webview does not support 'evaluateJavascript' method. Webview type: '%s'",
										webView.getClass().getName()));
								injectView.webView.getEngine().loadUrl("javascript:" + script, false);

								if (resultCallback != null) {
									resultCallback.onReceiveValue(null);
								}
							}
						}
					});
				}
			}
		});
	}

	private String getFileContents(String filename) {
		try (InputStream stream = activity.getResources().getAssets().open(filename)) {
			int size = stream.available();
			byte[] bytes = new byte[size];
			stream.read(bytes);
			return new String(bytes, "UTF-8");
		} catch (IOException e) {
			Log.e(TAG, String.format("Failed to read file: %s.", filename));
		}

		return "";
	}
}