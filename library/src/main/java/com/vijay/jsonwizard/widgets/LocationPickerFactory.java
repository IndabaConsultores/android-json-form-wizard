package com.vijay.jsonwizard.widgets;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.customviews.MaterialTextInputLayout;
import com.vijay.jsonwizard.demo.resources.ResourceResolver;
import com.vijay.jsonwizard.expressions.JsonExpressionResolver;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.i18n.JsonFormBundle;
import com.vijay.jsonwizard.interfaces.ClickableFormWidget;
import com.vijay.jsonwizard.interfaces.CommonListener;
import com.vijay.jsonwizard.interfaces.FormWidgetFactory;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.maps.LocationPart;
import com.vijay.jsonwizard.maps.LocationTextWatcher;
import com.vijay.jsonwizard.maps.LocationValueReporter;
import com.vijay.jsonwizard.maps.MapsActivity;
import com.vijay.jsonwizard.maps.MapsUtils;
import com.vijay.jsonwizard.utils.ExpressionResolverContextUtils;
import com.vijay.jsonwizard.utils.JsonFormUtils;
import com.vijay.jsonwizard.utils.ValidationStatus;
import com.vijay.jsonwizard.validators.textinputlayout.EqualsValidator;
import com.vijay.jsonwizard.validators.textinputlayout.RequiredValidator;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.vijay.jsonwizard.maps.MapsActivity.EXTRA_CONFIG_DEFAULT_ZOOM;
import static com.vijay.jsonwizard.maps.MapsActivity.EXTRA_CONFIG_MAX_ZOOM;
import static com.vijay.jsonwizard.maps.MapsActivity.EXTRA_CONFIG_MIN_ZOOM;
import static com.vijay.jsonwizard.maps.MapsActivity.EXTRA_CUSTOM_MARKER_ICON;
import static com.vijay.jsonwizard.maps.MapsActivity.EXTRA_INITIAL_LOCATION;
import static com.vijay.jsonwizard.maps.MapsActivity.EXTRA_USE_ACCURACY;

public class LocationPickerFactory implements FormWidgetFactory, ClickableFormWidget {

    public static final String KEY_SUFFIX_LATITUDE = "_latitude";
    public static final String KEY_SUFFIX_LONGITUDE = "_longitude";
    public static final String KEY_SUFFIX_ACCURACY = "_accuracy";
    private static final int RESULT_LOAD_LOCATION = 3;

    private static final String TAG = "JsonFormActivity";

    private static final int INPUT_TYPE_DECIMAL_NUMBER = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
            | InputType.TYPE_NUMBER_FLAG_SIGNED;

    public static ValidationStatus validate(MaterialTextInputLayout editText) {
        boolean validate = editText.validate();
        if (!validate) {
            return new ValidationStatus(false, editText.getError().toString());
        }
        return new ValidationStatus(true, null);
    }

    @Override
    public List<View> getViewsFromJson(String stepName, Context context, JSONObject jsonObject,
        CommonListener listener, JsonFormBundle bundle, JsonExpressionResolver resolver,
        ResourceResolver resourceResolver, int visualizationMode) throws Exception {
        List<View> views;
        switch (visualizationMode) {
            case JsonFormConstants.VISUALIZATION_MODE_READ_ONLY:
                views = getReadOnlyViewsFromJson(stepName, context, jsonObject, bundle, resolver, resourceResolver);
                break;
            default:
                views = getEditableViewsFromJson(stepName, context, jsonObject, listener, bundle,
                    resolver, resourceResolver);
        }
        return views;
    }

    private List<View> getEditableViewsFromJson(String stepName, Context context,
        JSONObject jsonObject, final CommonListener listener, JsonFormBundle bundle,
        JsonExpressionResolver resolver, ResourceResolver resourceResolver) throws JSONException {

        String readonlyValue = jsonObject.optString("readonly");
        boolean readonly = false;

        if (resolver.isValidExpression(readonlyValue)) {
            JSONObject currentValues = getCurrentValues(context, stepName);
            readonly = resolver.existsExpression(readonlyValue, currentValues);
        } else {
            readonly = Boolean.TRUE.toString().equalsIgnoreCase(readonlyValue);
        }

        if (readonly) {
            return getReadOnlyViewsFromJson(stepName, context, jsonObject, bundle, resolver, resourceResolver);
        }

        String jsonKey = jsonObject.getString("key");
        String jsonInputType = jsonObject.getString("type");

        List<View> views = new ArrayList<>(1);
        final View parentView = LayoutInflater.from(context).inflate(R.layout.item_location_text,
            null);
        parentView.setTag(R.id.key, jsonKey);
        parentView.setTag(R.id.type, jsonInputType);

        loadMapConfig(stepName, context, jsonObject, resolver, parentView);

        boolean accuracyEnabled = jsonObject.has("accuracy") && jsonObject.getBoolean("accuracy");
        parentView.setTag(R.id.accuracy, accuracyEnabled);
        if (jsonObject.has("icon")) {
            String customIcon = jsonObject.getString("icon");
            customIcon = resourceResolver.resolvePath(context, customIcon);
            parentView.setTag(R.id.custom_icon, customIcon);
        }

        View mapContainer = parentView.findViewById(R.id.map_container);
        mapContainer.setId(View.generateViewId());
        mapContainer.setTag(R.id.map_container);

        final String hint = bundle.resolveKey(jsonObject.getString("hint"));
        final TextView label = parentView.findViewById(R.id.location_label);
        label.setText(hint);

        View inputContainer = parentView.findViewById(R.id.value_container);
        inputContainer.setTag(R.id.key, jsonKey);
        inputContainer.setTag(R.id.type, jsonInputType);

        boolean editable = jsonObject.optBoolean("editable");
        MaterialTextInputLayout etLatitude = parentView.findViewById(R.id.location_latitude);
        EditText etLatitudeEditText = etLatitude.getEditText();
        etLatitude.setId(View.generateViewId());
        etLatitude.setEnabled(editable);
        etLatitudeEditText.setInputType(INPUT_TYPE_DECIMAL_NUMBER);
        etLatitude.setTag(R.id.key, jsonKey + KEY_SUFFIX_LATITUDE);
        etLatitude.setTag(R.id.type, jsonInputType);
        etLatitudeEditText.setTag(R.id.key, jsonKey + KEY_SUFFIX_LATITUDE);
        etLatitudeEditText.setTag(R.id.type, jsonInputType);

        MaterialTextInputLayout etLongitude = parentView.findViewById(R.id.location_longitude);
        EditText etLongitudeEditText = etLongitude.getEditText();
        etLongitude.setId(View.generateViewId());
        etLongitude.setEnabled(editable);
        etLongitudeEditText.setInputType(INPUT_TYPE_DECIMAL_NUMBER);
        etLongitude.setTag(R.id.key, jsonKey + KEY_SUFFIX_LONGITUDE);
        etLongitude.setTag(R.id.type, jsonInputType);
        etLongitudeEditText.setTag(R.id.key, jsonKey + KEY_SUFFIX_LONGITUDE);
        etLongitudeEditText.setTag(R.id.type, jsonInputType);

        MaterialTextInputLayout etAccuracy = parentView.findViewById(R.id.location_accuracy);
        EditText etAccuracyEditText = etAccuracy.getEditText();
        etAccuracy.setId(View.generateViewId());
        etAccuracyEditText.setInputType(INPUT_TYPE_DECIMAL_NUMBER);
        etAccuracy.setTag(R.id.key, jsonKey + KEY_SUFFIX_ACCURACY);
        etAccuracy.setTag(R.id.type, jsonInputType);
        etAccuracyEditText.setTag(R.id.key, jsonKey + KEY_SUFFIX_ACCURACY);
        etAccuracyEditText.setTag(R.id.type, jsonInputType);

        if (accuracyEnabled) {
            etAccuracy.setEnabled(editable);
            etAccuracy.setVisibility(View.VISIBLE);
        } else {
            etAccuracy.setVisibility(View.GONE);
        }

        final ImageView imageView = parentView.findViewById(R.id.icon);
        String key = jsonKey;
        imageView.setTag(R.id.key, jsonKey);
        imageView.setTag(R.id.type, jsonInputType);

        String customIconInput = jsonObject.optString("icon_input");
        if (!TextUtils.isEmpty(customIconInput)) {
            String customIconPath = resourceResolver.resolvePath(context, customIconInput);
            Bitmap bitmap = BitmapFactory.decodeFile(customIconPath);
            imageView.setImageBitmap(bitmap);
        }
        final View.OnClickListener onClickListenerWithValue = getOnClickListenerWithValue(
            parentView, etLatitudeEditText, etLongitudeEditText, etAccuracyEditText, listener, accuracyEnabled);
        imageView.setOnClickListener(onClickListenerWithValue);
        mapContainer.setOnClickListener(onClickListenerWithValue);

        final String value = jsonObject.optString("value");
        if (!TextUtils.isEmpty(value)) {
            String resolvedValue;
            if (resolver.isValidExpression(value)) {
                resolvedValue = resolver.resolveAsString(value, getCurrentValues(context, stepName));
                if (resolvedValue == null) {
                    resolvedValue = "";
                }
            } else {
                resolvedValue = value;
            }
            fillDefaultValue(parentView, etLatitudeEditText, etLongitudeEditText, etAccuracyEditText, resolvedValue);
            mapContainer.findViewById(R.id.map_placeholder).setVisibility(View.GONE);
        } else {
            mapContainer.findViewById(R.id.map_placeholder).setVisibility(View.VISIBLE);
        }

        //add validators
        JSONObject requiredObject = jsonObject.optJSONObject("v_required");
        if (requiredObject != null) {
            String requiredValue = requiredObject.getString("value");
            if (!TextUtils.isEmpty(requiredValue)) {
                boolean required = false;
                if (resolver.isValidExpression(requiredValue)) {
                    JSONObject currentValues = getCurrentValues(context, stepName);
                    required = resolver.existsExpression(requiredValue, currentValues);
                } else {
                    required = Boolean.TRUE.toString().equalsIgnoreCase(requiredValue);
                }

                if (required) {
                    etLatitude.addValidator(
                        new RequiredValidator(bundle.resolveKey(requiredObject.getString("err"))));
                    etLongitude.addValidator(
                            new RequiredValidator(bundle.resolveKey(requiredObject.getString("err"))));
                    if (accuracyEnabled) {
                        etAccuracy.addValidator(
                                new RequiredValidator(bundle.resolveKey(requiredObject.getString("err"))));
                    }
                }
            }
        }

        JSONObject accuracyValidator = jsonObject.optJSONObject("v_exact_position");
        if (accuracyEnabled && accuracyValidator != null) {
            etAccuracy.addValidator(
                    new EqualsValidator("-1", bundle.resolveKey(accuracyValidator.getString("err")), true));
        }

        LocationValueReporter valueReporter;
        if (TextUtils.isEmpty(value)) {
            valueReporter = new LocationValueReporter(stepName, parentView, accuracyEnabled);
        } else {
            valueReporter = new LocationValueReporter(stepName, parentView, value, accuracyEnabled);
        }
        etLatitudeEditText.addTextChangedListener(new LocationTextWatcher(LocationPart.LATITUDE, valueReporter));
        etLongitudeEditText.addTextChangedListener(new LocationTextWatcher(LocationPart.LONGITUDE, valueReporter));
        if (accuracyEnabled) {
            etAccuracyEditText.addTextChangedListener(new LocationTextWatcher(LocationPart.ACCURACY, valueReporter));
        }
        views.add(parentView);
        return views;
    }

    private void loadMapConfig(String stepName, Context context, JSONObject jsonObject, JsonExpressionResolver resolver, View parentView) throws JSONException {
        if (jsonObject.has("map_config")) {
            String expression = jsonObject.optString("map_config");
            JSONObject mapConfig;
            if (resolver.isValidExpression(expression)) {
                JSONObject currentValues = getCurrentValues(context, stepName);
                mapConfig = resolver.resolveAsObject(expression, currentValues);
            } else {
                mapConfig = jsonObject.getJSONObject("map_config");
            }
            parentView.setTag(R.id.map_min_zoom, mapConfig.optDouble("min_zoom", MapsUtils.MIN_ZOOM_LEVEL));
            parentView.setTag(R.id.map_max_zoom, mapConfig.optDouble("max_zoom", MapsUtils.MAX_ZOOM_LEVEL));
            parentView.setTag(R.id.map_default_zoom, mapConfig.optDouble("default_zoom"));
        }
    }

    private List<View> getReadOnlyViewsFromJson(String stepName, Context context, JSONObject jsonObject,
                                                JsonFormBundle bundle, JsonExpressionResolver resolver, ResourceResolver resourceResolver) throws JSONException {
        List<View> views = new ArrayList<>(1);
        View parentView = LayoutInflater.from(context).inflate(R.layout.item_location_text, null);

        String jsonKey = jsonObject.getString("key");
        String jsonInputType = jsonObject.getString("type");
        parentView.setTag(R.id.key, jsonKey);
        parentView.setTag(R.id.type, jsonInputType);
        loadMapConfig(stepName, context, jsonObject, resolver, parentView);

        View mapContainer = parentView.findViewById(R.id.map_container);
        mapContainer.setId(View.generateViewId());
        mapContainer.setTag(R.id.map_container);

        final String hint = bundle.resolveKey(jsonObject.getString("hint"));
        final TextView label = parentView.findViewById(R.id.location_label);
        label.setText(hint);


        MaterialTextInputLayout etLatitude = parentView.findViewById(R.id.location_latitude);
        EditText etLatitudeEditText = etLatitude.getEditText();
        etLatitude.setId(View.generateViewId());
        etLatitude.setEnabled(false);
        etLatitudeEditText.setInputType(INPUT_TYPE_DECIMAL_NUMBER);
        etLatitude.setTag(R.id.key, jsonKey + KEY_SUFFIX_LATITUDE);
        etLatitude.setTag(R.id.type, jsonInputType);
        etLatitudeEditText.setTag(R.id.key, jsonKey + KEY_SUFFIX_LATITUDE);
        etLatitudeEditText.setTag(R.id.type, jsonInputType);

        MaterialTextInputLayout etLongitude = parentView.findViewById(R.id.location_longitude);
        EditText etLongitudeEditText = etLongitude.getEditText();
        etLongitude.setId(View.generateViewId());
        etLongitude.setEnabled(false);
        etLongitudeEditText.setInputType(INPUT_TYPE_DECIMAL_NUMBER);
        etLongitude.setTag(R.id.key, jsonKey + KEY_SUFFIX_LONGITUDE);
        etLongitude.setTag(R.id.type, jsonInputType);
        etLongitudeEditText.setTag(R.id.key, jsonKey + KEY_SUFFIX_LONGITUDE);
        etLongitudeEditText.setTag(R.id.type, jsonInputType);

        MaterialTextInputLayout etAccuracy = parentView.findViewById(R.id.location_accuracy);
        EditText etAccuracyEditText = etAccuracy.getEditText();
        etAccuracy.setId(View.generateViewId());
        etAccuracy.setEnabled(false);
        etAccuracyEditText.setInputType(INPUT_TYPE_DECIMAL_NUMBER);
        etAccuracy.setTag(R.id.key, jsonKey + KEY_SUFFIX_ACCURACY);
        etAccuracy.setTag(R.id.type, jsonInputType);
        etAccuracyEditText.setTag(R.id.key, jsonKey + KEY_SUFFIX_ACCURACY);
        etAccuracyEditText.setTag(R.id.type, jsonInputType);

        boolean accuracyEnabled = jsonObject.has("accuracy") && jsonObject.getBoolean("accuracy");
        if (accuracyEnabled) {

            etAccuracy.setVisibility(View.VISIBLE);
        }

        String value = jsonObject.optString("value");

        if (!TextUtils.isEmpty(value)) {
            String resolvedValue;
            if (resolver.isValidExpression(value)) {
                resolvedValue = resolver.resolveAsString(value, getCurrentValues(context, stepName));
                if (resolvedValue == null) {
                    resolvedValue = "";
                }
            } else {
                resolvedValue = value;
            }
            fillDefaultValue(parentView, etLatitudeEditText, etLongitudeEditText, etAccuracyEditText, resolvedValue);
        } else {
            mapContainer.setVisibility(View.GONE);
            FrameLayout containerParent = (FrameLayout) mapContainer.getParent();
            containerParent.setVisibility(View.GONE);
        }

        final ImageView imageView = parentView.findViewById(R.id.icon);
        imageView.setClickable(false);
        imageView.setEnabled(false);
        imageView.setVisibility(View.GONE);

        views.add(parentView);
        return views;
    }


    private void fillDefaultValue(View parentView, EditText etLatitude, EditText etLongitude,
                                  EditText etAccuracy, String value) {
        parentView.setTag(R.id.value, value);
        String[] parts = value.split(MapsUtils.COORD_SEPARATOR);
        if (parts.length > 0) {
            etLatitude.setText(parts[0].trim());
        }
        if (parts.length > 1) {
            etLongitude.setText(parts[1].trim());
        }
        if (parts.length > 2) {
            etAccuracy.setText(parts[2].trim());
        }
    }

    private View.OnClickListener getOnClickListenerWithValue(final View parentView, final EditText etLatitude,
                                                             final EditText etLongitude, final EditText etAccuracy,
                                                             final CommonListener listener, final boolean accuracy) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value;
                if (accuracy) {
                    value = MapsUtils.toString(etLatitude.getText().toString(),
                            etLongitude.getText().toString(),
                            etAccuracy.getText().toString());
                } else {
                    value = MapsUtils.toString(etLatitude.getText().toString(),
                            etLongitude.getText().toString());
                }
                parentView.setTag(R.id.value, value);
                listener.onClick(parentView);
            }
        };
    }

    @Nullable
    private JSONObject getCurrentValues(Context context, String stepName) throws JSONException {
        return ExpressionResolverContextUtils.getCurrentValues(context, stepName);
    }

    @Override
    public void onClick(JsonFormFragment jsonFormFragment, View v) {

        Log.d(TAG, "onClick: location");
        jsonFormFragment.hideKeyBoard();
        Intent intent = new Intent(v.getContext(), MapsActivity.class);
        String value = (String) v.getTag(R.id.value);
        boolean useAccuracy = (boolean) v.getTag(R.id.accuracy);
        if (value != null && MapsUtils.isValidPositionString(value)) {
            intent.putExtra(EXTRA_INITIAL_LOCATION, value);
        }
        String customIcon = (String) v.getTag(R.id.custom_icon);
        if (customIcon != null) {
            intent.putExtra(EXTRA_CUSTOM_MARKER_ICON, customIcon);
        }
        intent.putExtra(EXTRA_USE_ACCURACY, useAccuracy);
        intent.putExtra(EXTRA_CONFIG_MIN_ZOOM, (Double) v.getTag(R.id.map_min_zoom));
        intent.putExtra(EXTRA_CONFIG_MAX_ZOOM, (Double) v.getTag(R.id.map_max_zoom));
        Double defaultZoom = (Double) v.getTag(R.id.map_default_zoom);
        if (defaultZoom != null && !defaultZoom.isNaN()) {
            intent.putExtra(EXTRA_CONFIG_DEFAULT_ZOOM, defaultZoom);
        }

        jsonFormFragment.startActivityForResult(intent, RESULT_LOAD_LOCATION);
    }

    @Override
    public void onFocusChange(JsonFormFragment jsonFormFragment, boolean focus, View v) {

    }
}
