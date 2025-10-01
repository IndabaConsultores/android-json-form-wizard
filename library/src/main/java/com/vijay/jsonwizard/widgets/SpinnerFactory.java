package com.vijay.jsonwizard.widgets;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.demo.resources.ResourceResolver;
import com.vijay.jsonwizard.expressions.JsonExpressionResolver;
import com.vijay.jsonwizard.i18n.JsonFormBundle;
import com.vijay.jsonwizard.interfaces.CommonListener;
import com.vijay.jsonwizard.interfaces.FormWidgetFactory;
import com.vijay.jsonwizard.utils.ExpressionResolverContextUtils;
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

        if((spinner.getTag(R.id.error) instanceof String)){
            ((TextView)spinner.getSelectedView()).setError((String) spinner.getTag(R.id.error));
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
                views = getReadOnlyViewsFromJson(stepName, context, jsonObject, bundle, resolver);
                break;
            default:
                views = getEditableViewsFromJson(stepName, context, jsonObject, listener, bundle, resolver);
        }
        return views;
    }

    private List<View> getEditableViewsFromJson(String stepName, Context context, JSONObject jsonObject, CommonListener listener,
                                                JsonFormBundle bundle, JsonExpressionResolver resolver) throws JSONException {
        List<View> views = new ArrayList<>(1);
        MaterialSpinner spinner = (MaterialSpinner) LayoutInflater.from(context).inflate(R.layout.item_spinner, null);
        final String hint = bundle.resolveKey(jsonObject.optString("hint"));
        if (!TextUtils.isEmpty(hint)) {
            spinner.setHint(hint);
            spinner.setFloatingLabelText(hint);
        }

        spinner.setId(View.generateViewId());
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
            if (resolver.isValidExpression(value)) {
                valueToSelect = resolver.resolveAsString(value, getCurrentValues(context, stepName));
                if (valueToSelect == null) {
                    valueToSelect = "";
                }
            } else {
                valueToSelect = value;
            }
        }

        JSONArray valuesJson = resolveOptJSONArray("values", context, stepName, jsonObject, resolver);

        JSONArray labelsJson = resolveOptJSONArray("labels", context, stepName, jsonObject, resolver);

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
    private JSONObject getCurrentValues(Context context, String stepName) throws JSONException {
        return ExpressionResolverContextUtils.getCurrentValues(context, stepName);
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
        return indexToSelect    ;
    }

    private String getValuesAsJsonExpression(JSONObject jsonObject, JsonExpressionResolver resolver) {
        String valuesExpression = jsonObject.optString("values");
        if (resolver.isValidExpression(valuesExpression)) {
            return valuesExpression;
        }
        return null;
    }

    private List<View> getReadOnlyViewsFromJson(String stepName, Context context, JSONObject jsonObject, JsonFormBundle bundle, JsonExpressionResolver resolver)
            throws JSONException {
        List<View> views = new ArrayList<>(1);
        TextInputLayout textInputLayout = (TextInputLayout) LayoutInflater.from(context).inflate(R.layout.item_material_edit_text,
                null);
        EditText editText = textInputLayout.getEditText();
        editText.setId(View.generateViewId());
        final String hint = jsonObject.getString("hint");
        try {
            editText.setHint(hint);
        } catch (Exception e) {
            Log.d(SpinnerFactory.class.getSimpleName(), "material not available setting hint on textInputLayout");
            textInputLayout.setHint(hint);
        }

        textInputLayout.setTag(R.id.key, jsonObject.getString("key"));
        textInputLayout.setTag(R.id.type, jsonObject.getString("type"));
        editText.setTag(R.id.key, jsonObject.getString("key"));
        editText.setTag(R.id.type, jsonObject.getString("type"));

        String value = jsonObject.optString("value");

        JSONArray labelsJson = resolveOptJSONArray("labels", context, stepName, jsonObject, resolver);
        if (labelsJson != null) {
            JSONArray valuesJson = resolveOptJSONArray("values", context, stepName, jsonObject, resolver);

            ValueLabelPair[] values = getValues(valuesJson, labelsJson, bundle);

            editText.setText(bundle.resolveKey(labelsJson.optString(getSelectedIdx(values, value))));
        } else {
            editText.setText(value);
        }

        editText.setEnabled(false);
        views.add(textInputLayout);
        return views;
    }

    private JSONArray resolveOptJSONArray(String key, Context context, String stepName, JSONObject jsonObject,
                                          JsonExpressionResolver resolver) throws JSONException {

        String jsonExpression = jsonObject.optString(key);
        JSONArray array;
        if (resolver.isValidExpression(jsonExpression)) {
            JSONObject currentValues = getCurrentValues(context, stepName);
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
