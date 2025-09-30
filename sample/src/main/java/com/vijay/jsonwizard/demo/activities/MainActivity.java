package com.vijay.jsonwizard.demo.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.demo.R;
import com.vijay.jsonwizard.state.StateProvider;
import com.vijay.jsonwizard.demo.utils.CommonUtils;
import com.vijay.jsonwizard.utils.JsonFormUtils;
import com.vijay.jsonwizard.utils.PropertiesUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import static com.vijay.jsonwizard.state.StateContract.COL_JSON;

/**
 * Created by vijay on 5/16/15.
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_GET_JSON = 1;

    private static final String TAG = "MainActivity";
    private static final String DATA_JSON_PATH = "form.json";
    private static final String COMPLETE_JSON_PATH = "complete.json";
    private static final String MAPS_SAMPLE = "maps.json";

    private static JSONObject extractDataFromForm(String form) {
        try {
            return JsonFormUtils.extractDataFromForm(
                (JSONObject) new JSONTokener(form).nextValue(), false);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON document", e);
        }
        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.button_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, JsonFormActivity.class);
                String json = null;
                if("testFormId".equals(PropertiesUtils.getInstance(v.getContext()).getFormId())) {
                    json = PropertiesUtils.getInstance(v.getContext()).getFormJson();
                    Toast.makeText(v.getContext(), "Form restored form previous state", Toast.LENGTH_LONG).show();
                    intent.putExtra("pausedStep", PropertiesUtils.getInstance(v.getContext()).getPausedStep());
                } else {
                    json = CommonUtils.loadJSONFromAsset(getApplicationContext(), COMPLETE_JSON_PATH);
                }

                intent.putExtra("json", json);
                intent.putExtra(JsonFormConstants.EXTRA_TRACK_HISTORY, true);
                intent.putExtra("resolver",
                    "com.vijay.jsonwizard.demo.expressions.AssetsContentResolver");
                //intent.putExtra(JsonFormConstants.ORIENTATION_EXTRA, JsonFormConstants
                // .ORIENTATION_LANDSCAPE);
                //intent.putExtra(JsonFormConstants.INPUT_METHOD_EXTRA, JsonFormConstants
                // .INPUT_METHOD_HIDDEN);
                startActivityForResult(intent, REQUEST_CODE_GET_JSON);
            }
        });
        findViewById(R.id.button_start_ro).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, JsonFormActivity.class);
                String json = CommonUtils.loadJSONFromAsset(getApplicationContext(),
                    COMPLETE_JSON_PATH);
                intent.putExtra("json", json);
                intent.putExtra(JsonFormConstants.VISUALIZATION_MODE_EXTRA,
                    JsonFormConstants.VISUALIZATION_MODE_READ_ONLY);
                startActivityForResult(intent, REQUEST_CODE_GET_JSON);
            }
        });
        findViewById(R.id.button_launch_maps).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, JsonFormActivity.class);
                String json = CommonUtils.loadJSONFromAsset(getApplicationContext(), MAPS_SAMPLE);
                intent.putExtra("json", json);
                // intent.putExtra(JsonFormConstants.VISUALIZATION_MODE_EXTRA, JsonFormConstants.VISUALIZATION_MODE_READ_ONLY);
                // intent.putExtra("pausedStep", "step2");
                startActivityForResult(intent, REQUEST_CODE_GET_JSON);
            }
        });
        findViewById(R.id.button_launch_big_form).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, JsonFormActivity.class);
                String json = CommonUtils.loadJSONFromAsset(getApplicationContext(), COMPLETE_JSON_PATH);
                Uri uri = StateProvider.saveState(MainActivity.this, json);
                intent.putExtra("jsonUri", uri);
                startActivityForResult(intent, REQUEST_CODE_GET_JSON);
            }
        });
        /*findViewById(R.id.button_multi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, JsonFormActivity.class);
                String json = CommonUtils.loadJSONFromAsset(getApplicationContext(),
                        COMPLETE_JSON_PATH);
                try {
                    JSONObject multiForm = new JSONObject(json);
                    String preForm = multiForm.getString("pre");
                    CommonUtils.loadJSONFromAsset(getApplicationContext(),
                            COMPLETE_JSON_PATH);

                    intent.putExtra("json", json);
                    intent.putExtra(JsonFormConstants.VISUALIZATION_MODE_EXTRA,
                            JsonFormConstants.VISUALIZATION_MODE_READ_ONLY);
                    startActivityForResult(intent, REQUEST_CODE_GET_JSON);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });*/

        View rootView = findViewById(android.R.id.content);
        Window window = getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false);

            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

                v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));

                return insets;
            });

            WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(false);
        } else {
            WindowInsetsControllerCompat controller =
                    new WindowInsetsControllerCompat(window, rootView);
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );

            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_GET_JSON && resultCode == RESULT_OK) {
            String json = data.getStringExtra("json");
            if (json == null) {
                Uri jsonUri = data.getParcelableExtra("uri");
                try (Cursor c = getContentResolver().query(jsonUri, null, null, null, null)) {
                    if (c != null && c.moveToFirst()) {
                        json = c.getString(c.getColumnIndex(COL_JSON));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Could not resolve JsonForm URI: " + jsonUri, e);
                }
            }
            Log.d(TAG, json);
            try {
                JSONObject jsonObject = new JSONObject(json);
                JSONArray history = jsonObject.optJSONArray("_history");
                if (history != null) {
                    Log.i(TAG, String.format("History: %s", history.toString()));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JSONObject result = extractDataFromForm(json);
            Log.d(TAG, result.toString());
            /*if(data.getBooleanExtra("pause", false)) {
                PropertiesUtils.getInstance(this).setFormJson(json);
                PropertiesUtils.getInstance(this).setFormId("testFormId");
            }else{*/
                PropertiesUtils.getInstance(this).setFormJson(null);
                PropertiesUtils.getInstance(this).setFormId(null);
                PropertiesUtils.getInstance(this).setPausedStep(null);
            //}
        } else if (requestCode == REQUEST_CODE_GET_JSON
            && resultCode == JsonFormConstants.RESULT_JSON_PARSE_ERROR) {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Error");
            alertDialog.setMessage((CharSequence) data.getData().toString());
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
            alertDialog.show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
