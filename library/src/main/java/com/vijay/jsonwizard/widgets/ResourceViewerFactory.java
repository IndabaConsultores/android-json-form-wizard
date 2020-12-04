package com.vijay.jsonwizard.widgets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.demo.resources.ResourceResolver;
import com.vijay.jsonwizard.expressions.JsonExpressionResolver;
import com.vijay.jsonwizard.i18n.JsonFormBundle;
import com.vijay.jsonwizard.interfaces.CommonListener;
import com.vijay.jsonwizard.interfaces.FormWidgetFactory;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.utils.JsonFormUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ResourceViewerFactory implements FormWidgetFactory {

    private static final String TAG = "ResourceViewerWidget";

    @Override
    public List<View> getViewsFromJson(String stepName, final Context context, JSONObject jsonObject, final CommonListener listener,
                                       JsonFormBundle bundle, JsonExpressionResolver resolver,
                                       ResourceResolver resourceResolver, int visualizationMode) throws Exception {
        List<View> views = new ArrayList<>(1);
        View parentView = LayoutInflater.from(context).inflate(R.layout.item_resource_label, null);

        RelativeLayout wrapper = parentView.findViewById(R.id.wrapper);
        wrapper.setTag(R.id.type, JsonFormConstants.RESOURCE_VIEWER);

        TextView label = parentView.findViewById(R.id.label);
        ImageView icon = parentView.findViewById(R.id.icon);

        // Load json data
        String resource = jsonObject.getString("resource");
        if (resolver.isValidExpression(resource)) {
            JSONObject currentValues = getCurrentValues(context);
            resource = resolver.resolveAsString(resource, currentValues);
        }

        String resourcePath = resourceResolver.resolvePath(context, resource);
        if (resourcePath != null && new File(resourcePath).exists()) {
            wrapper.setTag(R.id.value, resourcePath);
        } else {
            wrapper.setTag(R.id.value, resource);
        }
        String labelText = bundle.resolveKey(jsonObject.getString("label"));
        String iconPath = bundle.resolveKey(jsonObject.getString("icon"));

        if (resolver.isValidExpression(labelText)) {
            JSONObject currentValues = getCurrentValues(context);
            labelText = resolver.resolveAsString(labelText, currentValues);
        } else {
            labelText = bundle.resolveKey(labelText);
        }

        wrapper.setTag(R.id.label, labelText);
        wrapper.setOnClickListener(listener);
        if (TextUtils.isEmpty(labelText) && TextUtils.isEmpty(iconPath)) {
            Log.w(TAG, "A resource_viewer widget should have at leas one of 'icon' or 'label' properties");
        } else {
            label.setText(labelText);
            if (!TextUtils.isEmpty(iconPath)) {
                String path = resourceResolver.resolvePath(context, iconPath);
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                icon.setImageBitmap(bitmap);
            }
            if (jsonObject.has("config")) {
                String expression = jsonObject.optString("config");
                JSONObject config;
                if (resolver.isValidExpression(expression)) {
                    JSONObject currentValues = getCurrentValues(context);
                    config = resolver.resolveAsObject(expression, currentValues);
                } else {
                    config = jsonObject.getJSONObject("config");
                }
                configure(context, wrapper, label, icon, config);
            }
        }

        views.add(parentView);
        return views;
    }

    private void configure(Context context, RelativeLayout wrapper, TextView label, ImageView icon, JSONObject config) {
        // Label color
        String color = config.optString("color");
        int colorValue = Color.BLUE; // Default color
        if (!TextUtils.isEmpty(color)) {
            colorValue = Color.parseColor(color);
        }
        label.setTextColor(colorValue);

        // Icon color
        String iconColor = config.optString("icon_color");
        if (!TextUtils.isEmpty(iconColor)) {
            icon.setColorFilter(Color.parseColor(iconColor));
        }

        // Label font size
        Double size = config.optDouble("size");
        if (size != null) {
            label.setTextSize(size.floatValue());
        }

        // Icon size
        Integer iconWidth = config.optInt("icon_width");
        Integer iconHeight = config.optInt("icon_height");
        int width = RelativeLayout.LayoutParams.WRAP_CONTENT;
        if (iconWidth != null) {
            width = (int) dpsToPx(context, iconWidth);
        }
        int height = RelativeLayout.LayoutParams.WRAP_CONTENT;
        if (iconHeight != null) {
            height = (int) dpsToPx(context, iconHeight);
        }
        RelativeLayout.LayoutParams iconParams = new RelativeLayout.LayoutParams(width, height);
        RelativeLayout.LayoutParams labelParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        // Icon alignment
        String iconAlignment = config.optString("icon_position");
        Integer marginVal = config.optInt("icon_margin", 8);
        int margin = (int) dpsToPx(context, marginVal);
        switch (iconAlignment) {
            case "start":
                iconParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                iconParams.setMarginEnd(margin);
                labelParams.addRule(RelativeLayout.END_OF, R.id.icon);
                labelParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                break;
            case "bottom":
                iconParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                iconParams.addRule(RelativeLayout.BELOW, R.id.label);
                iconParams.topMargin = margin;
                labelParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                break;
            case "end":
                iconParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                iconParams.addRule(RelativeLayout.END_OF, R.id.label);
                iconParams.setMarginStart(margin);
                labelParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                break;
            case "top":
            default:
                iconParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                iconParams.bottomMargin = margin;
                labelParams.addRule(RelativeLayout.BELOW, R.id.icon);
                labelParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                break;
        }
        icon.setLayoutParams(iconParams);
        label.setLayoutParams(labelParams);

        // Widget alignment
        String alignment = config.optString("align");
        if (!TextUtils.isEmpty(alignment)) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            switch (alignment) {
                case "end":
                    params.gravity = Gravity.END;
                    break;
                case "start":
                    params.gravity = Gravity.START;
                    break;
                default:
                    params.gravity = Gravity.CENTER;
                    break;
            }
            wrapper.setLayoutParams(params);
        }
    }

    @Nullable
    private JSONObject getCurrentValues(Context context) throws JSONException {
        JSONObject currentValues = null;
        if (context instanceof JsonApi) {
            String currentJsonState = ((JsonApi) context).currentJsonState();
            JSONObject currentJsonObject = new JSONObject(currentJsonState);
            currentValues = JsonFormUtils.extractDataFromForm(currentJsonObject, false);
        }
        return currentValues;
    }

    private float dpsToPx(Context context, int value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }

}
