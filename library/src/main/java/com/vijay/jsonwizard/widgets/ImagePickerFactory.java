package com.vijay.jsonwizard.widgets;

import static com.vijay.jsonwizard.utils.FormUtils.dpToPixels;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.demo.resources.ResourceResolver;
import com.vijay.jsonwizard.expressions.JsonExpressionResolver;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.i18n.JsonFormBundle;
import com.vijay.jsonwizard.interfaces.ClickableFormWidget;
import com.vijay.jsonwizard.interfaces.CommonListener;
import com.vijay.jsonwizard.interfaces.FormWidgetFactory;
import com.vijay.jsonwizard.utils.ImagePicker;
import com.vijay.jsonwizard.utils.ImageUtils;
import com.vijay.jsonwizard.utils.ValidationStatus;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by vijay on 24-05-2015.
 */
public class ImagePickerFactory implements FormWidgetFactory {
    private static final int RESULT_LOAD_IMG = 1;

    public static ValidationStatus validate(ImageView imageView) {
        if (!(imageView.getTag(R.id.v_required) instanceof String) || !(imageView.getTag(
            R.id.error) instanceof String)) {
            return new ValidationStatus(true, null);
        }
        Boolean isRequired = Boolean.valueOf((String) imageView.getTag(R.id.v_required));
        if (!isRequired) {
            return new ValidationStatus(true, null);
        }
        Object path = imageView.getTag(R.id.imagePath);
        if (path instanceof String && !TextUtils.isEmpty((String) path)) {
            return new ValidationStatus(true, null);
        }
        return new ValidationStatus(false, (String) imageView.getTag(R.id.error));
    }

    @Override
    public List<View> getViewsFromJson(String stepName, final Context context, JSONObject jsonObject, CommonListener listener,
                                       JsonFormBundle bundle, JsonExpressionResolver resolver, ResourceResolver resourceResolver,
                                       int visualizationMode) throws JSONException {
        if (visualizationMode == JsonFormConstants.VISUALIZATION_MODE_READ_ONLY) {
            return getReadOnlyViews(context, jsonObject, bundle);
        } else {
            return getEditableViews(context, jsonObject, listener, bundle);
        }
    }

    private List<View> getReadOnlyViews(Context context, JSONObject jsonObject, JsonFormBundle bundle) throws JSONException {
        String imagePath = jsonObject.optString("value");
        if (!TextUtils.isEmpty(imagePath)) {
            List<View> views = new ArrayList<>(1);
            View rootView = LayoutInflater.from(context).inflate(R.layout.item_image_picker, null);
            final ImageView imageView = rootView.findViewById(R.id.image_preview);
            Bitmap bitmap = ImageUtils.loadBitmapFromFile(imagePath, ImageUtils.getDeviceWidth(context), dpToPixels(context, 200));
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.mipmap.grey_bg);
            }
            Button btn = rootView.findViewById(R.id.btn_upload);
            btn.setEnabled(false);
            btn.setText(bundle.resolveKey(jsonObject.getString("uploadButtonText")));
            views.add(rootView);
            return views;
        } else {
            return Collections.emptyList();
        }
    }

    private List<View> getEditableViews(final Context context, JSONObject jsonObject, CommonListener listener,
                                        JsonFormBundle bundle) throws JSONException {
        List<View> views = new ArrayList<>(1);
        View rootView = LayoutInflater.from(context).inflate(R.layout.item_image_picker, null);
        rootView.setTag(R.id.type, jsonObject.getString("type"));

        final ImageView imageView = rootView.findViewById(R.id.image_preview);
        imageView.setTag(R.id.key, jsonObject.getString("key"));
        imageView.setTag(R.id.type, jsonObject.getString("type"));

        JSONObject requiredObject = jsonObject.optJSONObject("v_required");
        if (requiredObject != null) {
            String requiredValue = requiredObject.getString("value");
            if (!TextUtils.isEmpty(requiredValue)) {
                imageView.setTag(R.id.v_required, requiredValue);
                imageView.setTag(R.id.error, bundle.resolveKey(requiredObject.optString("err")));
            }
        }
        final Button clearBtn = rootView.findViewById(R.id.btn_clear);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setImageResource(R.mipmap.grey_bg);
                imageView.setTag(R.id.imagePath, null);
                clearBtn.setVisibility(View.GONE);
            }
        });

        String imagePath = jsonObject.optString("value");
        if (!TextUtils.isEmpty(imagePath)) {
            imageView.setTag(R.id.imagePath, imagePath);
            imageView.setImageBitmap(
                    ImageUtils.loadBitmapFromFile(imagePath, ImageUtils.getDeviceWidth(context), dpToPixels(context, 200)));

            clearBtn.setVisibility(View.VISIBLE);
        }
        Button uploadButton = rootView.findViewById(R.id.btn_upload);
        uploadButton.setText(bundle.resolveKey(jsonObject.getString("uploadButtonText")));
        uploadButton.setOnClickListener(listener);
        uploadButton.setTag(R.id.key, jsonObject.getString("key"));
        uploadButton.setTag(R.id.type, jsonObject.getString("type"));

        views.add(rootView);
        return views;
    }
}
