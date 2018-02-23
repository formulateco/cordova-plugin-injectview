package me.steenman.injectview;

import android.util.Log;
import android.net.Uri;
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
import java.net.URL;
import java.net.HttpURLConnection;

public class InjectView extends CordovaPlugin {
	private static final String LOG_TAG = "CordovaPluginInjectView";

	private CordovaActivity activity;

	@Override
	public void pluginInitialize() {
		Log.v(LOG_TAG, "Initializing plugin");
		this.activity = (CordovaActivity) this.cordova.getActivity();
	}

	@Override
	public Object onMessage(String id, Object data) {
		if ("onPageFinished".equals(id)) {
			Log.v(LOG_TAG, "Page finished loading, inject!");

			ArrayList<String> files = new ArrayList();
			files.add("www/cordova.js");
			files.add("www/cordova_plugins.js");
			files.addAll(getCordovaPlugins());
			injectJavascriptFiles(files);
		}

		return null;
	}

	private ArrayList<String> getCordovaPlugins() {
		ArrayList<String> plugins = new ArrayList();
		String pluginsFile = getFileContents("www/cordova_plugins.js");

		String start = "module.exports = ";
		int offset = pluginsFile.indexOf(start) + start.length();

		String pluginsJson = "";
		int bracketCount = 0;

		for (offset = offset; offset < pluginsFile.length(); offset++) {
			char nextChar = pluginsFile.charAt(offset);
			if (nextChar == '[')
				bracketCount++;
			if (bracketCount > 0)
				pluginsJson += nextChar;
			if (nextChar == ']')
				bracketCount--;

			if (bracketCount == 0 && nextChar == ']')
				break;
		}

		Log.d(LOG_TAG, "Found JSON for cordova plugins: " + pluginsJson);
		try {
			JSONArray pluginsArray = new JSONArray(pluginsJson);

			Log.d(LOG_TAG, "cordova plugins count = " + pluginsArray.length());

			for (int i = 0; i < pluginsArray.length(); i++) {
				JSONObject pluginObject = pluginsArray.getJSONObject(i);
				String plugin = (String) pluginObject.get("file");
				// plugin = plugin.substring(plugin.indexOf("www"));
				Log.d(LOG_TAG, "Add plugin www/" + plugin);
				plugins.add("www/" + plugin);
			}
		} catch (JSONException e) {
			Log.e(LOG_TAG, "Could not parse plugin JSON: " + pluginsJson);
			e.printStackTrace();
			return plugins;
		}

		return plugins;
	}

	private String getFileContents(String file) {
		String contents = null;

		try {
			Uri uri = Uri.parse(file);
			if (uri.isRelative()) {
				try {
					InputStream inputStream = activity.getResources().getAssets().open(file);
					contents = InjectView.readStreamContent(inputStream);
				} catch (IOException e) {
					Log.e(LOG_TAG, String.format("ERROR: failed to load script file '%s'", file));
					e.printStackTrace();
				}
			} else {
				URL url = new URL(file);
				try {
					HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
					try {
						InputStream inputStream = urlConnection.getInputStream();
						contents = InjectView.readStreamContent(inputStream);
					} finally {
						urlConnection.disconnect();
					}
				} catch (IOException e) {
					Log.e(LOG_TAG, String.format("ERROR: failed to load script file from url '%s'", file));
				}
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, String.format("ERROR: Failed to parse filename '%s'", file));
		}

		return contents;
	}

	private void injectJavascriptFiles(ArrayList<String> files) {
		InjectView injectView = this;

		this.cordova.getThreadPool().execute(new Runnable() {
			@Override
			public void run() {
				for (String file : files) {
					Log.v(LOG_TAG, String.format("Injecting script '%s'", file));

					String content = getFileContents(file);
					if (content == null) {
						Log.v(LOG_TAG, String.format("Could not load script '%s'", file));
						continue;
					}
					if (content.isEmpty()) {
						Log.v(LOG_TAG, String.format("No contents found in script '%s'", file));
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
								Log.v(LOG_TAG, String.format(
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

	private static String readStreamContent(InputStream inputStream) throws IOException {
		int size = inputStream.available();
		byte[] bytes = new byte[size];
		inputStream.read(bytes);
		inputStream.close();
		String content = new String(bytes, "UTF-8");
		return content;
	}
}
