package ch.uzh.ifi.attempto.mobileape;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import ch.uzh.ifi.attempto.mobileape.MobileApeHelper.ApiException;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


/**
 * TODO: SharedPreferences settings = getPreferences(0);
 * 
 * @author Kaarel Kaljurand
 */
public class MobileApe extends Activity {

	private EditText edittext = null;
	private TextView textview = null;

	private String soloType = "drspp";
	private boolean guess = false;
	private boolean noclex = false;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		textview = (TextView) findViewById(R.id.textview);

		edittext = (EditText) findViewById(R.id.edittext);

		// Restore preferences
		//SharedPreferences settings = getPreferences(0);
		//guess = settings.getBoolean("guess", false);


		edittext.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// If the event is a key-down event on the "enter" button
				if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
						(keyCode == KeyEvent.KEYCODE_ENTER)) {
					// Perform action on key press
					showMessage(edittext.getText().toString());
					parseAndShow(edittext.getText().toString(), soloType);
					return true;
				}
				return false;
			}
		});
	}


	@Override
	protected void onStop(){
		super.onStop();
		/*
		// We need an Editor object to make preference changes.
		// All objects are from android.context.Context
		SharedPreferences settings = getPreferences(0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("guess", guess);
		editor.commit();
		 */
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.parse:
			// BUG: We assume the edittext can never be null
			parseAndShow(edittext.getText().toString(), soloType);
			return true;
		case R.id.example:
			// BUG: We assume the edittext can never be null
			edittext.setText(getString(R.string.acetext_example_2));
			return true;
		case R.id.about:
			showMessage(getAboutString());
			return true;
		case R.id.save_text:
			if (isStorageWritable()) {
				saveText(edittext.getText().toString());
			}
			return true;
		case R.id.show_texts:
			if (isStorageReadable()) {
				String texts = showTexts();
				if (texts != null) {
					textview.setText(texts);
				}
			}
			return true;
		case R.id.drspp:
			setSoloType(item);
			return true;
		case R.id.paraphrase:
			setSoloType(item);
			return true;
		case R.id.owlfsspp:
			setSoloType(item);
			return true;
		case R.id.syntaxpp:
			setSoloType(item);
			return true;
		case R.id.guess:
			guess = flip(item);
			return true;
		case R.id.noclex:
			noclex = flip(item);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	private boolean flip(MenuItem item) {
		boolean b = ! item.isChecked();
		item.setChecked(b);
		return b;
	}


	private String getApeResult(String text, String soloType) throws ApiException {
		String urlBase = getString(R.string.apews_url_base);
		MobileApeHelper.prepareUserAgent(this);
		String encodedText = Uri.encode(text);
		String guessAsString = "off";
		String noclexAsString = "off";
		if (guess) {
			guessAsString = "on";
		}
		if (noclex) {
			noclexAsString = "on";
		}
		String expandClause = "&solo=" + soloType + "&guess=" + guessAsString + "&noclex=" + noclexAsString;
		//String finalUrl = String.format(urlBase + "?text=", encodedText, expandClause);
		String finalUrl = urlBase + "?text=" + encodedText + expandClause;
		Log.i("Simple APE client", finalUrl);
		return MobileApeHelper.getUrlContent(finalUrl);
	}


	/*
	private String getSoloType() {
		String soloType = "drspp";

		RadioGroup radiogroup_solo = (RadioGroup) findViewById(R.id.radiogroup_solo);
		if (radiogroup_solo != null) {
			int radiobuttonId = radiogroup_solo.getCheckedRadioButtonId();
			if (radiobuttonId != -1) {
				RadioButton radioSelected = (RadioButton) findViewById(radiobuttonId);
				soloType = radioSelected.getText().toString();
			}
		}
		return soloType;
	}
	 */


	private void parseAndShow(String text, String soloType) {
		String apeOutput = "";

		try {
			apeOutput = getApeResult(text, soloType);
		} catch (ApiException e) {
			apeOutput = "ERROR: " + e.getMessage();
		}

		textview.setText(apeOutput + "\n====\n" + textview.getText().toString());
	}


	private String getAboutString() {
		return getString(R.string.app_name) + " v" + getVersionName() + " by " + getString(R.string.app_author);
	}


	private void setSoloType(MenuItem item) {
		if (item.isChecked()) {
			item.setChecked(false);
		}
		else {
			item.setChecked(true);
			soloType = item.getTitle().toString();
		}
	}


	private boolean isStorageWritable() {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but all we need
			//  to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		//Toast.makeText(TransAnd.this, "SD: available: " + mExternalStorageAvailable + "; writable: " + mExternalStorageWriteable, Toast.LENGTH_LONG).show();

		return (mExternalStorageAvailable && mExternalStorageWriteable);
	}


	private boolean isStorageReadable() {
		boolean mExternalStorageAvailable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			mExternalStorageAvailable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			mExternalStorageAvailable = true;
		}
		return mExternalStorageAvailable;
	}


	private void saveText(String text) {
		File root = Environment.getExternalStorageDirectory();

		String appDirName = root + "/Android/data/" + getPackageName() + "/files";

		File appDir = new File(appDirName);
		if (! appDir.exists() && ! appDir.mkdirs()) {
			showMessage("Failed to create dir: " + appDir.getAbsolutePath());
		}

		String fileName = appDirName + "/" + getString(R.string.sentences_file_name);
		File sentencesFile = new File(fileName);
		if (! sentencesFile.exists()) {
			showMessage("File does not exist: " + sentencesFile.getAbsolutePath());
		}


		try {
			FileOutputStream fos = new FileOutputStream(sentencesFile, true);
			fos.write(text.getBytes());
			fos.write("\n----\n".getBytes());
			fos.close();
			showMessage("Appended to: " + sentencesFile.getAbsolutePath());
		} catch (IOException e) {
			showException(e);
		}
	}


	/**
	 * 
	 * @return
	 */
	private String showTexts() {
		File root = Environment.getExternalStorageDirectory();

		String appDirName = root + "/Android/data/" + getPackageName() + "/files";

		/*
		File appDir = new File(appDirName);
		if (! appDir.exists()) {
			showMessage("appDir does not exist: " + appDir.getAbsolutePath());
			return null;
		}
		 */

		String fileName = appDirName + "/" + getString(R.string.sentences_file_name);
		File sentencesFile = new File(fileName);
		if (! sentencesFile.exists()) {
			showMessage("File does not exist: " + sentencesFile.getAbsolutePath());
			return null;
		}

		String str = null;
		try {
			str = readFileAsString(fileName);
		} catch (IOException e) {
			showException(e);
		}

		return str;
	}


	private void showMessage(String msg) {
		Toast.makeText(MobileApe.this, msg, Toast.LENGTH_LONG).show();
	}


	private void showException(Exception e) {
		Toast.makeText(MobileApe.this, e.getMessage(), Toast.LENGTH_LONG).show();
	}


	/**
	 * 
	 * @param filePath
	 * @return
	 * @throws java.io.IOException
	 */
	private static String readFileAsString(String filePath) throws java.io.IOException {
		byte[] buffer = new byte[(int) new File(filePath).length()];
		BufferedInputStream f = null;
		try {
			f = new BufferedInputStream(new FileInputStream(filePath));
			f.read(buffer);
		} finally {
			if (f != null) try { f.close(); } catch (IOException ignored) { }
		}
		return new String(buffer);
	}


	private String getVersionName() {
		PackageInfo info = getPackageInfo();
		if (info == null) {
			return "";
		}
		return info.versionName;
	}


	private PackageInfo getPackageInfo() {
		PackageManager manager = getPackageManager();
		try {
			return manager.getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) {
			Log.e("MobileApe", "Couldn't find package information in PackageManager", e);
		}
		return null;
	}
}