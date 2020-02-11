package com.vijay.jsonwizard.widgets;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.rey.material.util.ViewUtil;
import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.demo.resources.ResourceResolver;
import com.vijay.jsonwizard.expressions.JsonExpressionResolver;
import com.vijay.jsonwizard.i18n.JsonFormBundle;
import com.vijay.jsonwizard.interfaces.CommonListener;
import com.vijay.jsonwizard.interfaces.FormWidgetFactory;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.utils.JsonFormUtils;
import com.vijay.jsonwizard.utils.ValidationStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.ganfra.materialspinner.MaterialSpinner;

/**
 * Created by nipun on 30/05/15.
 */
public class SpinnerFactory implements FormWidgetFactory {

    public static ValidationStatus validate(MaterialSpinner spinner) {
        if (!(spinner.getTag(R.id.v_required) instanceof String) || !(spinner.getTag(R.id.error) instanceof String)) {
            return new ValidationStatus(true, null);
        }
        Boolean isRequired = Boolean.valueOf((String) spinner.getTag(R.id.v_required));
        if (!isRequired) {
            return new ValidationStatus(true, null);
        }
        int selectedItemPosition = spinner.getSelectedItemPosition();
        if (selectedItemPosition > 0) {
            return new ValidationStatus(true, null);
        }
        return new ValidationStatus(false, (String) spinner.getTag(R.id.error));
    }

    @Override
    public List<View> getViewsFromJson(String stepName, Context context, JSONObject jsonObject, CommonListener listener,
        JsonFormBundle bundle, JsonExpressionResolver resolver, ResourceResolver resourceResolver,
        int visualizationMode) throws JSONException {
        List<View> views = null;
        switch (visualizationMode) {
            case JsonFormConstants.VISUALIZATION_MODE_READ_ONLY:
                views = getReadOnlyViewsFromJson(context, jsonObject, bundle, resolver);
                break;
            default:
                views = getEditableViewsFromJson(context, jsonObject, listener, bundle, resolver);
        }
        return views;
    }

    private List<View> getEditableViewsFromJson(Context context, JSONObject jsonObject, CommonListener listener,
        JsonFormBundle bundle, JsonExpressionResolver resolver) throws JSONException {
        List<View> views = new ArrayList<>(1);
        MaterialSpinner spinner = (MaterialSpinner) LayoutInflater.from(context).inflate(R.layout.item_spinner, null);

        final String hint = bundle.resolveKey(jsonObject.optString("hint"));
        if (!TextUtils.isEmpty(hint)) {
            spinner.setHint(hint);
            spinner.setFloatingLabelText(hint);
        }

        spinner.setId(ViewUtil.generateViewId());

        spinner.setTag(R.id.key, jsonObject.getString("key"));
        spinner.setTag(R.id.type, jsonObject.getString("type"));

        JSONObject requiredObject = jsonObject.optJSONObject("v_required");
        if (requiredObject != null) {
            String requiredValue = requiredObject.getString("value");
            if (!TextUtils.isEmpty(requiredValue)) {
                spinner.setTag(R.id.v_required, requiredValue);
                spinner.setTag(R.id.error, bundle.resolveKey(requiredObject.optString("err")));
            }
        }

        String valueToSelect = "";
        int indexToSelect = -1;
        final String value = jsonObject.optString("value");
        if (!TextUtils.isEmpty(value)) {
            valueToSelect = value;
        }

        JSONArray valuesJson = resolveOptJSONArray("values", context, jsonObject, resolver);

        JSONArray labelsJson = resolveOptJSONArray("labels", context, jsonObject, resolver);

        ValueLabelPair[] values = getValues(valuesJson, labelsJson, bundle);
        String otherOption = bundle.resolveKey(jsonObject.optString("other"));
        if (!TextUtils.isEmpty(otherOption)) {
            List<ValueLabelPair> valuesWithOther = new ArrayList<>(Arrays.asList(values));
            valuesWithOther.add(new ValueLabelPair(otherOption, otherOption));
            values = valuesWithOther.toArray(values);
        }

        indexToSelect = getSelectedIdx(values, valueToSelect);

        if (values != null) {
            spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, values));
            spinner.setSelection(indexToSelect + 1, true);
            spinner.setOnItemSelectedListener(listener);
        }
        views.add(spinner);
        return views;
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

    private ValueLabelPair[] getValues(JSONArray valuesJson, JSONArray labelsJson, JsonFormBundle bundle) {
        ValueLabelPair[] values = null;
        if (valuesJson != null && valuesJson.length() > 0) {
            final int valuesJsonLength = valuesJson.length();
            values = new ValueLabelPair[valuesJsonLength];
            for (int i = 0; i < valuesJsonLength; i++) {
                String value = valuesJson.optString(i);
                if (labelsJson != null) {
                    String label = labelsJson.optString(i);
                    values[i] = new ValueLabelPair(value, bundle.resolveKey(label));
                } else {
                    values[i] = new ValueLabelPair(value, value);
                }
            }
        }
        return values;
    }

    private int getSelectedIdx(ValueLabelPair[] values, String valueToSelect) {
        int indexToSelect = -1;
        if (values != null && values.length > 0) {
            for (int i = 0; i < values.length; i++) {
                if (valueToSelect.equals(values[i].value)) {
                    indexToSelect = i;
                    break;
                }
            }
        }
        return indexToSelect;
    }

    private String getValuesAsJsonExpression(JSONObject jsonObject, JsonExpressionResolver resolver) {
        String valuesExpression = jsonObject.optString("values");
        if (resolver.isValidExpression(valuesExpression)) {
            return valuesExpression;
        }
        return null;
    }

    private List<View> getReadOnlyViewsFromJson(Context context, JSONObject jsonObject, JsonFormBundle bundle, JsonExpressionResolver resolver)
        throws JSONException {
        List<View> views = new ArrayList<>(1);
        MaterialEditText editText = (MaterialEditText) LayoutInflater.from(context).inflate(R.layout.item_edit_text,
            null);
        editText.setId(ViewUtil.generateViewId());
        final String hint = jsonObject.getString("hint");
        editText.setHint(hint);
        editText.setFloatingLabelText(hint);
        editText.setTag(R.id.key, jsonObject.getString("key"));
        editText.setTag(R.id.type, jsonObject.getString("type"));

        String value = jsonObject.optString("value");

        JSONArray labelsJson = resolveOptJSONArray("labels", context, jsonObject, resolver);
        if (labelsJson != null) {
            JSONArray valuesJson = resolveOptJSONArray("values", context, jsonObject, resolver);

            ValueLabelPair[] values = getValues(valuesJson, labelsJson, bundle);

            editText.setText(bundle.resolveKey(labelsJson.optString(getSelectedIdx(values, value))));
        } else {
            editText.setText(value);
        }

        editText.setEnabled(false);
        views.add(editText);
        return views;
    }

    private JSONArray resolveOptJSONArray(String key, Context context, JSONObject jsonObject,
        JsonExpressionResolver resolver) throws JSONException {

        String jsonExpression = jsonObject.optString(key);
        JSONArray array;
        if (resolver.isValidExpression(jsonExpression)) {
            JSONObject currentValues = getCurrentValues(context);
            array = resolver.resolveAsArray(jsonExpression, currentValues);
        } else {
            array = jsonObject.optJSONArray(key);
        }
        return array;
    }

    public static class ValueLabelPair {
        private String value;
        private String label;

        public ValueLabelPair(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        @NonNull
        @Override
        public String toString() {
            return this.label != null ? this.label : this.value;
        }
    }
}
