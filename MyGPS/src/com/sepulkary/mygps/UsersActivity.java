package com.sepulkary.mygps;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

public class UsersActivity extends Activity {
	private EditText sTarget1Number;
	private EditText sTarget1Pass;
	private EditText sTarget2Number;
	private EditText sTarget2Pass;
	private Button enterUsersButton;
    private Button cancelUsersButton;

	String t1NumberInput = "";
	String t1PassInput = "";
	String t2NumberInput = "";
	String t2PassInput = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_users);

        cancelUsersButton = (Button) findViewById(R.id.cancelUsersButton);
		enterUsersButton = (Button) findViewById(R.id.enterUsersButton);
		sTarget1Number = (EditText) findViewById(R.id.sTarget1NumberInput);
		sTarget1Pass = (EditText) findViewById(R.id.sTarget1PassInput);
		sTarget2Number = (EditText) findViewById(R.id.sTarget2NumberInput);
		sTarget2Pass = (EditText) findViewById(R.id.sTarget2PassInput);

		Intent intent = getIntent();
		t1NumberInput = (intent.getStringExtra("sTarget1Number"));
		t1PassInput = (intent.getStringExtra("sTarget1Pass"));
		t2NumberInput = (intent.getStringExtra("sTarget2Number"));
		t2PassInput = (intent.getStringExtra("sTarget2Pass"));

		sTarget1Number.setText(t1NumberInput);
		sTarget1Pass.setText(t1PassInput);
		sTarget2Number.setText(t2NumberInput);
		sTarget2Pass.setText(t2PassInput);

		enterUsersButton.setOnClickListener(new Button.OnClickListener() {
					@Override
					public void onClick(View arg0) {
						Intent intent = new Intent(UsersActivity.this, MainActivity.class);
						intent.putExtra("sTarget1Number", sTarget1Number.getText().toString());
						intent.putExtra("sTarget1Pass", sTarget1Pass.getText().toString());
						intent.putExtra("sTarget2Number", sTarget2Number.getText().toString());
						intent.putExtra("sTarget2Pass", sTarget2Pass.getText().toString());

						setResult(RESULT_OK, intent);
						finish();
					}
				});

        cancelUsersButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(UsersActivity.this, MainActivity.class);
				intent.putExtra("sTarget1Number", t1NumberInput);
				intent.putExtra("sTarget1Pass", t1PassInput);
				intent.putExtra("sTarget2Number", t2NumberInput);
				intent.putExtra("sTarget2Pass", t2PassInput);

                setResult(RESULT_OK, intent);
                finish();
            }
        });
	}
}
