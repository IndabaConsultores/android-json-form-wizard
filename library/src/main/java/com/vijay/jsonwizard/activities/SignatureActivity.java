package com.vijay.jsonwizard.activities;

import static com.vijay.jsonwizard.state.StateContract.COL_JSON;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;


import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.utils.ImageUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class SignatureActivity extends AppCompatActivity{

    private Signature mSignature;
    private Bitmap bitmap;
    private ConstraintLayout mContent;
    private ConstraintLayout signatureContainer;
    private View view;
    private View deleteButton;
    private View saveButton;
    private Boolean isTimestampVisible;

    public static int RESULT_OK = 200;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getExtras();
        setupView();
        setupButtons();

    }

    private void getExtras() {
        Bundle extras = getIntent().getExtras();
        isTimestampVisible = extras.getBoolean("timestamp");
    }


    private void setupView() {
        setContentView(R.layout.activity_signature);
        signatureContainer = findViewById(R.id.signatureContainer);
        mContent = findViewById(R.id.signature);
        mSignature = new Signature(this, null);
        mContent.addView(mSignature);
        setTimestamp();
        view = signatureContainer;
    }

    private void setTimestamp() {
        TextView timestamp = findViewById(R.id.timestampTv);
        if(isTimestampVisible){
            Date date = new Date(System.currentTimeMillis());
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
            timestamp.setText(formatter.format(date));
            timestamp.setVisibility(View.VISIBLE);
        }else{
            timestamp.setVisibility(View.GONE);
        }
    }

    private void setupButtons(){
        deleteButton = findViewById(R.id.cleanBt);
        deleteButton.setOnClickListener(v -> mSignature.clear());
        saveButton = findViewById(R.id.saveBt);
        saveButton.setOnClickListener(v ->{
            if(hasSigned()){
                saveImage();
            }else{
                Toast.makeText(this, getString(R.string.signature_empty), Toast.LENGTH_LONG).show();
            }
        });

    }

    private void saveImage() {
        String fileName = String.valueOf(System.currentTimeMillis());
        File file = new File(getExternalFilesDir(null), fileName + ".jpg");
        bitmap = Bitmap.createBitmap(mContent.getWidth(), mContent.getHeight(), Bitmap.Config.RGB_565);
        final Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        bitmap = ImageUtils.scaleToFit(bitmap, 1600);
        //ImageUtils.compressAndSave(bitmap, 80, file.getAbsolutePath());
        ImageUtils.saveToFile(bitmap, file);
        Intent intent = new Intent();
        intent.putExtra("signatureFileName",fileName);
        setResult(RESULT_OK,intent);
        finish();
    }

    private boolean hasSigned() {
        return mSignature.lastTouchX != 0.0f && mSignature.lastTouchY != 0.0f;
    }



    public class Signature extends View {
        private static final float STROKE_WIDTH = 10f;
        private static final float HALF_STROKE_WIDTH = STROKE_WIDTH / 2;
        private static final String TAG = "Signature";
        private final RectF dirtyRect = new RectF();
        private final Paint paint = new Paint();
        private final Path path = new Path();
        private float lastTouchX;
        private float lastTouchY;

        public Signature(Context context, AttributeSet attrs) {
            super(context, attrs);
            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(STROKE_WIDTH);
        }


        public void clear() {
            path.reset();
            lastTouchX = 0.0f;
            lastTouchY = 0.0f;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawPath(path, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            final float eventX = event.getX();
            final float eventY = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(eventX, eventY);
                    lastTouchX = eventX;
                    lastTouchY = eventY;
                    return true;

                case MotionEvent.ACTION_MOVE:

                case MotionEvent.ACTION_UP:
                    resetDirtyRect(eventX, eventY);
                    final int historySize = event.getHistorySize();
                    for (int i = 0; i < historySize; i++) {
                        final float historicalX = event.getHistoricalX(i);
                        final float historicalY = event.getHistoricalY(i);
                        expandDirtyRect(historicalX, historicalY);
                        path.lineTo(historicalX, historicalY);
                    }
                    path.lineTo(eventX, eventY);
                    break;
                default:
                    Log.d(TAG, "Ignored touch event: " + event.toString());
                    return false;
            }

            invalidate((int) (dirtyRect.left - HALF_STROKE_WIDTH), (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                (int) (dirtyRect.right + HALF_STROKE_WIDTH), (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));

            lastTouchX = eventX;
            lastTouchY = eventY;

            return true;
        }

        private void expandDirtyRect(float historicalX, float historicalY) {
            if (historicalX < dirtyRect.left) {
                dirtyRect.left = historicalX;
            } else if (historicalX > dirtyRect.right) {
                dirtyRect.right = historicalX;
            }

            if (historicalY < dirtyRect.top) {
                dirtyRect.top = historicalY;
            } else if (historicalY > dirtyRect.bottom) {
                dirtyRect.bottom = historicalY;
            }
        }

        private void resetDirtyRect(float eventX, float eventY) {
            dirtyRect.left = Math.min(lastTouchX, eventX);
            dirtyRect.right = Math.max(lastTouchX, eventX);
            dirtyRect.top = Math.min(lastTouchY, eventY);
            dirtyRect.bottom = Math.max(lastTouchY, eventY);
        }}
    }


