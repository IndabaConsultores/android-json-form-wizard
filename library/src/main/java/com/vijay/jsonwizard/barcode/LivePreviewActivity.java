// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.vijay.jsonwizard.barcode;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.common.annotation.KeepName;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.barcode.barcodescanning.BarcodeGraphic;
import com.vijay.jsonwizard.barcode.barcodescanning.BarcodeScanningProcessor;
import com.vijay.jsonwizard.barcode.common.CameraImageGraphic;
import com.vijay.jsonwizard.barcode.common.CameraSource;
import com.vijay.jsonwizard.barcode.common.CameraSourcePreview;
import com.vijay.jsonwizard.barcode.common.GraphicOverlay;
import com.vijay.jsonwizard.barcode.common.GraphicOverlay.Graphic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo app showing the various features of ML Kit for Firebase. This class is used to
 * set up continuous frame processing on frames from a camera source.
 */
@KeepName
public final class LivePreviewActivity extends AppCompatActivity
    implements OnRequestPermissionsResultCallback, CompoundButton.OnCheckedChangeListener, Button.OnClickListener {
    private static final String TAG = "LivePreviewActivity";
    private static final int PERMISSION_REQUESTS = 1;
    private static final String PARAM_BARCODE = "barcode";
    private static final String PARAM_ERROR = "error";

    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private BarcodeScanningProcessor barcodeScanningProcessor;
    private TextView editText;

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_live_preview);

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

        preview = findViewById(R.id.firePreview);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = findViewById(R.id.fireFaceOverlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }

        ToggleButton facingSwitch = findViewById(R.id.facingSwitch);
        facingSwitch.setOnCheckedChangeListener(this);

        final Button submit = findViewById(R.id.submit);
        submit.setOnClickListener(this);

        editText = findViewById(R.id.selectedCode);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                submit.setEnabled(true);
            }
        });

        // Hide the toggle button if there is only 1 camera
        if (Camera.getNumberOfCameras() == 1) {
            facingSwitch.setVisibility(View.GONE);
        }

        if (allPermissionsGranted()) {
            isVisionModuleInstalled();
            createCameraSource();

        } else {
            getRuntimePermissions();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(TAG, "Set facing");
        if (cameraSource != null) {
            if (isChecked) {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
            } else {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
            }
        }
        preview.stop();
        startCameraSource();
    }

    private void createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }

        try {

            Log.i(TAG, "Using Barcode Detector Processor");
            barcodeScanningProcessor = new BarcodeScanningProcessor();
            cameraSource.setMachineLearningFrameProcessor(barcodeScanningProcessor);

        } catch (Exception e) {
            Log.e(TAG, "Can not create image processor: ", e);
            Toast.makeText(getApplicationContext(), "Can not create image processor: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        preview.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    private String[] getRequiredPermissions() {
        String[] permissions = new String[]{Manifest.permission.CAMERA};
        return permissions;
    }

    private boolean allPermissionsGranted() {
        for (String permission: getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission: getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (allPermissionsGranted()) {
            createCameraSource();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private BarcodeGraphic getGraphAtPos(GraphicOverlay overlay, float x, float y) {
        BarcodeGraphic selected = null;
        for (Graphic graph: overlay.getGraphics()) {
            if (graph instanceof BarcodeGraphic) {
                BarcodeGraphic barcodeGraphic = (BarcodeGraphic) graph;

                if (barcodeGraphic.contains(x, y)) {
                    Log.d("GraphicOverlay", "getGraphAtPos: winner!");
                    selected = barcodeGraphic;
                }
            }
        }
        return selected;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Log.d(TAG, "dispatchTouchEvent: event");
            float x = event.getRawX();
            float y = event.getRawY();

            int[] coordinates = new int[2];
            graphicOverlay.getLocationOnScreen(coordinates);

            x -= coordinates[0];
            y -= coordinates[1];

            BarcodeGraphic graph = getGraphAtPos(graphicOverlay, x, y);
            if (graph != null) {
                CameraImageGraphic lastImage = barcodeScanningProcessor.getLastImage();
                barcodeScanningProcessor.pause();
                cameraSource.stop();
                String barcodeValue = graph.getBarcode().getDisplayValue();
                Log.d(TAG, "dispatchTouchEvent: Selected = " + barcodeValue);
                graphicOverlay.highlight(graph);
                refreshOverlay(graphicOverlay, lastImage);
                editText.setText(barcodeValue);
            }

        }
        return super.dispatchTouchEvent(event);
    }

    private void refreshOverlay(GraphicOverlay overlay, CameraImageGraphic cameraImage) {
        List<Graphic> graphics = new ArrayList<>(overlay.getGraphics());
        overlay.clear();

        for (Graphic graphic: graphics) {
            overlay.add(graphic);
        }
        overlay.postInvalidate();
    }

    @Override
    public void onClick(View v) {
        if (v != null && v.getId() == R.id.submit) {
            Log.d(TAG, "onClick: Submit!!!");
            Intent result = new Intent();
            result.putExtra(PARAM_BARCODE, editText.getText().toString());
            setResult(Activity.RESULT_OK, result);
            finish();
        }
    }

    private void isVisionModuleInstalled() {
        final Boolean isAvailable = false;
        FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance().getVisionBarcodeDetector();
        int w = 32;
        int h = 32;
        Bitmap.Config conf = Bitmap.Config.ARGB_4444; // see other conf types
        Bitmap bmp = Bitmap.createBitmap(w, h, conf);
        Task<List<FirebaseVisionBarcode>> task = detector.detectInImage(FirebaseVisionImage.fromBitmap(bmp));
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onCreate: wait for install");
                Intent result = new Intent();
                result.putExtra(PARAM_ERROR, "");
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });

    }
}
