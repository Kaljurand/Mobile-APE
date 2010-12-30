package ch.uzh.ifi.attempto.mobileape;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ch.uzh.ifi.attempto.ape.ACEParser;
import ch.uzh.ifi.attempto.ape.ACEParserException;
import ch.uzh.ifi.attempto.ape.APEWebservice;
import ch.uzh.ifi.attempto.ape.Message;
import ch.uzh.ifi.attempto.ape.OutputType;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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
 * TODO: it seems that ACEParserException is never thrown (on the emulator)
 * TODO: there seems to be some problem with jdom (maybe this is the cause of the previous problem)
 * 
 * @author Kaarel Kaljurand
 */
public class MobileApe extends Activity {

	private static final int SNIPPET_SELECTED = 1;

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
					//showMessage(edittext.getText().toString());
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
			Intent intent = new Intent(this, ShowAbout.class);
			startActivity(intent);
			//showMessage(getAboutString());
			return true;
		case R.id.save_text:
			if (Utils.isStorageWritable()) {
				saveText(edittext.getText().toString());
			}
			return true;
		case R.id.show_texts:
			if (Utils.isStorageReadable()) {
				startActivityForResult(new Intent(this, SnippetListView.class), SNIPPET_SELECTED);
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
		case R.id.tptp:
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


	@Override 
	public void onActivityResult(int requestCode, int resultCode, Intent data) {     
		super.onActivityResult(requestCode, resultCode, data); 
		switch(requestCode) { 
		case (SNIPPET_SELECTED) : { 
			if (resultCode == Activity.RESULT_OK) {
				String selectedText = data.getStringExtra(SnippetListView.SELECTED_SNIPPET_EXTRA);
				parseAndShow(selectedText, soloType);
			} 
			break;
		} 
		} 
	}


	private boolean flip(MenuItem item) {
		boolean b = ! item.isChecked();
		item.setChecked(b);
		return b;
	}


	private void parseAndShow(String text, String soloType) {
		String apeOutput = "";
		OutputType outputType = OutputType.valueOf(soloType.toUpperCase());

		try {
			apeOutput = getApeResult(text, outputType);
		} catch (ACEParserException e) {
			for (Message m : e.getMessageContainer().getMessages()) {
				apeOutput += formatMessage(m) + "\n";
			}
		} catch (RuntimeException rte) {
			apeOutput = "API ERROR: " + rte.getMessage();
		}

		textview.setText(apeOutput + "\n====\n" + textview.getText().toString());
	}


	private String formatMessage(Message m) {
		return m.getType() + ": sentence " + m.getSentenceId() + " token " + m.getTokenId() + ": " + m.getValue() + ": " + m.getRepair();
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


	private void saveText(String text) {
		File root = Environment.getExternalStorageDirectory();

		String appDirName = root + "/Android/data/" + getPackageName() + "/files";

		File appDir = new File(appDirName);
		if (! appDir.exists() && ! appDir.mkdirs()) {
			showMessage("Failed to create dir: " + appDir.getAbsolutePath());
		}

		String fileName = appDirName + "/" + getString(R.string.sentences_file_name);
		File sentencesFile = new File(fileName);

		try {
			FileOutputStream fos = new FileOutputStream(sentencesFile, true);
			fos.write(text.getBytes());
			fos.write("\n".getBytes());
			fos.close();
			showMessage("Appended to: " + sentencesFile.getAbsolutePath());
		} catch (IOException e) {
			showException(e);
		}
	}


	private void showMessage(String msg) {
		Toast.makeText(MobileApe.this, msg, Toast.LENGTH_LONG).show();
	}


	private void showException(Exception e) {
		Toast.makeText(MobileApe.this, e.getMessage(), Toast.LENGTH_LONG).show();
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


	private String getApeResult(String aceText, OutputType outputType) throws ACEParserException {
		ACEParser ap = new APEWebservice(getString(R.string.apews_url_base));
		ap.setClexEnabled(! noclex);
		ap.setGuessingEnabled(guess);
		ap.setURI(getString(R.string.ape_param_uri));
		return ap.getSoloOutput(aceText, outputType);
	}


	private String getAboutString() {
		return getString(R.string.app_name) + " v" + getVersionName() + " by " + getString(R.string.app_author);
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
}