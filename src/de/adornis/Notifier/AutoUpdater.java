package de.adornis.Notifier;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class AutoUpdater {

	private int currentVersion = 0;
	private int futureVersion = 0;
	private IOException e;
	private String outputFilePath = "";

	public AutoUpdater(int currentVersion) {
		this.currentVersion = currentVersion;
	}

	public void check() throws IOException {
		MainInterface.log("checking");
		VersionChecker vs = new VersionChecker(true);
		vs.execute();

		if(e != null) {
			throw e;
		}
	}

	public int getVersion() throws IOException {
		MainInterface.log("getting version");

		VersionChecker vs = new VersionChecker(false);
		vs.execute();

		if(e != null) {
			throw e;
		}

		try {
			MainInterface.log("waiting for result");
			return vs.get();
		} catch (InterruptedException e1) {
			return 0;
		} catch (ExecutionException e1) {
			return 0;
		}
	}

	public void update(final Context c) {
		MainInterface.log("updating");

		new AsyncTask<Void, IOException, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try {
					URL url = new URL("http://www.adornis.de/dl/Notifier.apk");
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.connect();
					InputStream is = connection.getInputStream();

					// maybe expect 200 error code here, but whatever. I know what's on the server

					outputFilePath = c.getExternalCacheDir().getPath() + "Notifier_update_" + futureVersion + ".apk";
					MainInterface.log(outputFilePath);
					OutputStream os = new FileOutputStream(outputFilePath);

					byte data[] = new byte[4096];
					int count;
					while((count = is.read(data)) != -1) {
						os.write(data, 0, count);
					}
					MainInterface.log("done downloading");
				} catch (MalformedURLException e1) {
					MainInterface.log("url malformed in auto updater, should never happen because it's hardcoded");
					// hardcoded, shouldn't happen
				} catch (IOException e1) {
					MainInterface.log("Exception in auto updater: " + e1.getMessage());
					publishProgress(e);
				}

				return null;
			}

			@Override
			protected void onProgressUpdate(IOException... values) {
				e = values[0];
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				MainInterface.log("starting the update intent");
				Intent intent = new Intent(Intent.ACTION_VIEW);

				Uri uri = Uri.fromFile(new File(outputFilePath));
				intent.setDataAndType(uri, "application/vnd.android.package-archive");
				c.startActivity(intent);
			}
		}.execute();
	}

	class VersionChecker extends AsyncTask<Void, IOException, Integer> {

		private boolean notifyAllAftwards = false;

		public VersionChecker(boolean notifyAllAftwards) {
			this.notifyAllAftwards = notifyAllAftwards;
		}

		@Override
		protected Integer doInBackground(Void... params) {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			int version = 0;
			try {
				HttpResponse response = httpClient.execute(new HttpGet("http://www.adornis.de/dl/Notifier.version"));
				BufferedReader input = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				version = Integer.parseInt(input.readLine());
			} catch (IOException e) {
				publishProgress(e);
			}
			MainInterface.log("online: " + version + ", offline: " + currentVersion);
			futureVersion = version;
			return version;
		}

		@Override
		protected void onProgressUpdate(IOException... values) {
			e = values[0];
		}

		@Override
		protected void onPostExecute(Integer version) {
			if (version != currentVersion && notifyAllAftwards) {
				PreferenceListener.notifyAll(PreferenceListener.UPDATE_AVAILABLE);
			}
		}
	}
}
