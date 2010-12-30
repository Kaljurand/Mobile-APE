package ch.uzh.ifi.attempto.mobileape;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SnippetListView extends ListActivity {

	public static final String SELECTED_SNIPPET_EXTRA = "SELECTED_SNIPPET_EXTRA";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		List<String> snippets = getSnippets();

		if (snippets == null) {
			setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item, new String[] {}));
		}
		else {
			setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item, snippets));
		}

		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				TextView textView = (TextView) view;
				Toast.makeText(getApplicationContext(), textView.getText(), Toast.LENGTH_SHORT).show();
				Intent resultIntent = new Intent();
				resultIntent.putExtra(SELECTED_SNIPPET_EXTRA, textView.getText().toString());
				setResult(Activity.RESULT_OK, resultIntent);
				finish();
			}
		});
	}


	private List<String> getSnippets() {
		File root = Environment.getExternalStorageDirectory();
		String appDirName = root + "/Android/data/" + getPackageName() + "/files";
		String fileName = appDirName + "/" + getString(R.string.sentences_file_name);
		File sentencesFile = new File(fileName);
		if (! sentencesFile.exists()) {
			Utils.showMessage(this, "File does not exist: " + sentencesFile.getAbsolutePath());
			return null;
		}

		try {
			return Utils.readFileToLines(fileName);
		} catch (IOException e) {
			Utils.showException(this, e);
		}

		return null;
	}
}