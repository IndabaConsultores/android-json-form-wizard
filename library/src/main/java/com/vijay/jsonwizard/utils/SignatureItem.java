package com.vijay.jsonwizard.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.vijay.jsonwizard.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SignatureItem {

    private static final String TAG = "ImagePicker";
    private static final String TEMP_IMAGE_NAME = "tempImage";




    public static Bitmap getImageFromResult(Context context, int resultCode, Intent imageReturnedIntent) {
        Log.d(TAG, "getImageFromResult, resultCode: " + resultCode);
        Bitmap bm = null;

        File imageFile = getTempFile(context);
        if (resultCode == Activity.RESULT_OK) {


           /* Uri selectedImage;
            boolean isCamera =
                (imageReturnedIntent == null || imageReturnedIntent.getData() == null || imageReturnedIntent.getData()
                                                                                                            .toString()
                                                                                                            .contains(
                                                                                                                imageFile
                                                                                                                    .toString()));
            if (isCamera) {
                selectedImage = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName(),
                    getTempFile(context));
            } else {
                selectedImage = imageReturnedIntent.getData();
            }
            Log.d(TAG, "selectedImage: " + selectedImage);

            bm = getImageResized(context, selectedImage);
            int rotation = getRotation(context, selectedImage, isCamera);
            bm = rotate(bm, rotation);*/
        }
        return bm;
    }


    private static File getTempFile(Context context) {
        File imageFile = new File(context.getExternalFilesDir(null), TEMP_IMAGE_NAME);
        imageFile.getParentFile().mkdirs();
        return imageFile;
    }

    private static Bitmap decodeBitmap(Context context, Uri theUri, int sampleSize) {
        AssetFileDescriptor fileDescriptor = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            try {
                fileDescriptor = context.getContentResolver().openAssetFileDescriptor(theUri, "r");

                Bitmap actuallyUsableBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(),
                    null, options);
                Log.d(TAG, options.inSampleSize + " sample method bitmap ... " + actuallyUsableBitmap.getWidth() + " "
                    + actuallyUsableBitmap.getHeight());
                return actuallyUsableBitmap;

            } catch (FileNotFoundException e) {
                Log.e(TAG, "decodeBitmap: error", e);
            }
            return null;
        } finally {
            if (fileDescriptor != null) {
                try {
                    fileDescriptor.close();
                } catch (Exception e) {
                    //Ignore
                }
            }
        }

    }

    /**
     * Resize to avoid using too much memory loading big images (e.g.: 2560*1920)
     **/
    private static Bitmap getImageResized(Context context, Uri selectedImage) {
        Bitmap bm = decodeBitmap(context, selectedImage, 1);
        bm = ImageFileUtils.scaleToFit(bm, ImageFileUtils.IMAGE_MAX_SIZE);
        return bm;
    }


}
