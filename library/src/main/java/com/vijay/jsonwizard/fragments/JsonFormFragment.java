package com.vijay.jsonwizard.fragments;

import static android.view.View.VISIBLE;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.customviews.MaterialTextInputLayout;
import com.vijay.jsonwizard.demo.resources.ResourceResolver;
import com.vijay.jsonwizard.expressions.JsonExpressionResolver;
import com.vijay.jsonwizard.i18n.JsonFormBundle;
import com.vijay.jsonwizard.interfaces.CommonListener;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.maps.MapsUtils;
import com.vijay.jsonwizard.mvp.MvpFragment;
import com.vijay.jsonwizard.presenters.JsonFormFragmentPresenter;
import com.vijay.jsonwizard.utils.CarouselAdapter;
import com.vijay.jsonwizard.utils.PropertiesUtils;
import com.vijay.jsonwizard.views.JsonFormFragmentView;
import com.vijay.jsonwizard.viewstates.JsonFormFragmentViewState;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by vijay on 5/7/15.
 */
public class JsonFormFragment extends MvpFragment<JsonFormFragmentPresenter, JsonFormFragmentViewState>
    implements CommonListener, JsonFormFragmentView<JsonFormFragmentViewState> {
    private static final String TAG = "JsonFormFragment";
    private LinearLayout mMainView;
    private Menu mMenu;
    private JsonApi mJsonApi;

    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    public static JsonFormFragment getFormFragment(String stepName) {
        JsonFormFragment jsonFormFragment = new JsonFormFragment();
        Bundle bundle = new Bundle();
        bundle.putString("stepName", stepName);
        jsonFormFragment.setArguments(bundle);
        return jsonFormFragment;
    }

    public JsonApi getJsonApi() {
        return mJsonApi;
    }

    public void setJsonApi(JsonApi jsonApi) {
        this.mJsonApi = jsonApi;
    }

    public ActivityResultLauncher<String[]> getRequestPermissionsLauncher() { return requestPermissionsLauncher; }

    @Override
    public void onAttach(Context activity) {
        if (activity instanceof JsonApi) {
            mJsonApi = (JsonApi) activity;
        }
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);

        requestPermissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    boolean allGranted = true;

                    for(Map.Entry<String, Boolean> entry : result.entrySet()) {
                        if(Boolean.FALSE.equals(entry.getValue())) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (allGranted) {
                        presenter.launchPickerIntent();
                    } else {
                        AlertDialog alertDialog = new AlertDialog.Builder(requireContext()).create();
                        alertDialog.setTitle(getString(R.string.permission_required));
                        alertDialog.setMessage(getString(R.string.permission_required_message));
                        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.accept),
                                (dialog, which) -> startActivity(new Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:"+ requireActivity().getPackageName())
                                )));
                        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel),
                                (dialog, which) -> dialog.dismiss());
                        alertDialog.show();
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_json_wizard, null);
        mMainView = (LinearLayout) rootView.findViewById(R.id.main_layout);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter.addFormElements();
    }

    @Override
    public void onResume() {

        super.onResume();
        int childCount = mMainView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = mMainView.getChildAt(i);
            if (JsonFormConstants.LOCATION_PICKER.equals(view.getTag(R.id.type))) {
                // init map if value available
                String value = (String) view.getTag(R.id.value);
                if (!TextUtils.isEmpty(value)) {
                    String key = (String) view.getTag(R.id.key);
                    redrawMap(key, value);
                }
            }
        }
    }

    @Override
    protected JsonFormFragmentViewState createViewState() {
        return new JsonFormFragmentViewState();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mMenu = menu;
        menu.clear();
        inflater.inflate(R.menu.menu_toolbar, menu);
        presenter.setUpToolBar();
    }

    @Override
    public void setActionBarTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            presenter.onBackClick();
            return true;
        } else if (item.getItemId() == R.id.action_next) {
            presenter.onNextClick(mMainView);
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            presenter.onSaveClick(mMainView);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        presenter.onClick(v);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        presenter.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDetach() {
        mJsonApi = null;
        super.onDetach();
    }

    @Override
    public void updateRelevantImageView(Bitmap bitmap, String imagePath, String currentKey,
        String stepName) {
        int childCount = mMainView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = mMainView.getChildAt(i);
            if (JsonFormConstants.CHOOSE_IMAGE.equals(view.getTag(R.id.type))||JsonFormConstants.SIGNATURE.equals(view.getTag(R.id.type))) {
                ImageView imageView = view.findViewById(R.id.image_preview);
                String key = (String) imageView.getTag(R.id.key);
                if (key.equals(currentKey)) {
                    imageView.setImageBitmap(bitmap);
                    imageView.setVisibility(VISIBLE);
                    imageView.setTag(R.id.imagePath, imagePath);
                    //imageView.setAdjustViewBounds(true);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                    view.findViewById(R.id.btn_clear).setVisibility(VISIBLE);
                    writeValue(stepName,key,imagePath);
                }
            }
        }
    }

    private MaterialTextInputLayout findMaterialTextInputLayoutByTag(ViewGroup v, String searchKey) {

        MaterialTextInputLayout found = null;

        for (int i = 0; i < v.getChildCount(); i++) {
            Object child = v.getChildAt(i);
            if (child instanceof MaterialTextInputLayout) {

                MaterialTextInputLayout materialTextInputLayout = (MaterialTextInputLayout) child;
                String key = (String) materialTextInputLayout.getTag(R.id.key);
                if (key.equals(searchKey)) {
                    return materialTextInputLayout;
                }
            } else if (child instanceof ViewGroup) {
                found = findMaterialTextInputLayoutByTag((ViewGroup) child, searchKey);
                if (found != null) {
                    break;
                }
            }
        }
        return found;
    }

    @Override
    public void updateRelevantMap(String key, String value) {
        int childCount = mMainView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = mMainView.getChildAt(i);
            if (JsonFormConstants.LOCATION_PICKER.equals(view.getTag(R.id.type))) {
                String widgetKey = (String) view.getTag(R.id.key);
                if (widgetKey != null && widgetKey.equals(key)) {
                    view.setTag(R.id.value, value);
                    break;
                }
            }
        }
        // Map will be redrawn by onResume lifecycle method
    }

    @Override
    public void updateRelevantTextInputLayout(String currentKey, String value) {

        MaterialTextInputLayout textInputLayout =  (MaterialTextInputLayout) findMaterialTextInputLayoutByTag(mMainView, currentKey);

        if (textInputLayout != null) {
            TextInputEditText editText = (TextInputEditText) textInputLayout.getEditText();
            editText.setText(value);
        }
    }

    private void redrawMap(String key, String value) {
        View view = getView();
        View inputView = findViewWithTagKeyValue(view, key);
        String customIcon = (String) inputView.getTag(R.id.custom_icon);
        View mapContainer = inputView.findViewWithTag(R.id.map_container);
        Double zoomLevel = (Double) inputView.getTag(R.id.map_default_zoom);
        Float zoomLevelF = zoomLevel != null && !zoomLevel.isNaN() ? zoomLevel.floatValue() : null;
        MapsUtils.loadStaticMap(this, mapContainer.getId(), key, value, customIcon, zoomLevelF);
    }

    private View findViewWithTagKeyValue(View view, String key) {
        if (key.equals(view.getTag(R.id.key))) {
            return view;
        } else if (view instanceof ViewGroup) {
            ViewGroup v = (ViewGroup) view;
            for (int i = 0; i < v.getChildCount(); i++) {
                View child = v.getChildAt(i);
                View result = findViewWithTagKeyValue(child, key);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    @Override
    public void writeValue(String stepName, String key, String s) {
        try {
            mJsonApi.writeValue(stepName, key, s);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void writeValue(String stepName, String prentKey, String childObjectKey, String childKey, String value) {
        try {
            mJsonApi.writeValue(stepName, prentKey, childObjectKey, childKey, value);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public JSONObject getStep(String stepName) {
        return mJsonApi.getStep(stepName);
    }

    @Override
    public String getCurrentJsonState() {
        return mJsonApi.currentJsonState();
    }

    @Override
    protected JsonFormFragmentPresenter createPresenter() {
        JsonFormFragmentPresenter presenter = new JsonFormFragmentPresenter();
        presenter.setVisualizationMode(mJsonApi.getVisualizationMode());
        return presenter;
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public CommonListener getCommonListener() {
        return this;
    }

    @Override
    public void addFormElements(List<View> views) {
        for (View view: views) {
            mMainView.addView(view);
        }
    }

    @Override
    public ActionBar getSupportActionBar() {
        return ((JsonFormActivity) getActivity()).getSupportActionBar();
    }

    @Override
    public Toolbar getToolbar() {
        return ((JsonFormActivity) getActivity()).getToolbar();
    }

    @Override
    public void setToolbarTitleColor(int colorId) {
        getToolbar().setTitleTextColor(getContext().getResources().getColor(colorId));
    }

    @Override
    public void updateVisibilityOfNextAndSave(boolean next, boolean save) {
        mMenu.findItem(R.id.action_next).setVisible(next);
        mMenu.findItem(R.id.action_save).setVisible(save);
    }

    @Override
    public void hideKeyBoard() {
        super.hideSoftKeyboard();
    }

    @Override
    public void backClick() {
        if(!getActivity().getSupportFragmentManager().popBackStackImmediate( ) ){
            getActivity().finish();
        }
    }

    @Override
    public void unCheckAllExcept(String parentKey, String childKey) {
        int childCount = mMainView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = mMainView.getChildAt(i);
            if (view instanceof RadioGroup) {
                RadioGroup rg = (RadioGroup) view;
                for (int j = 0; j < rg.getChildCount(); j++) {
                    MaterialRadioButton child = (MaterialRadioButton) rg.getChildAt(j);
                    String parentKeyAtIndex = (String) child.getTag(R.id.key);
                    String childKeyAtIndex = (String) child.getTag(R.id.childKey);
                    if (parentKeyAtIndex.equals(parentKey) && !childKeyAtIndex.equals(childKey)) {
                        child.setChecked(false);
                    }
                }
            }
        }
    }

    @Override
    public String getCount() {
        return mJsonApi.getCount();
    }

    @Override
    public void finishWithResult(Intent returnIntent) {
        getActivity().setResult(Activity.RESULT_OK, returnIntent);
        getActivity().finish();
    }

    @Override
    public void setUpBackButton() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void transactThis(JsonFormFragment next) {
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.add(((ViewGroup) getView().getParent()).getId(), next);
        transaction.hide(this);
        transaction.addToBackStack(next.getClass().getSimpleName());
        transaction.commit();

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        presenter.onCheckedChanged(buttonView, isChecked);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        presenter.onItemSelected(parent, view, position, id);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Not implementation needed
    }

    public LinearLayout getMainView() {
        return mMainView;
    }

    @Override
    public void onInitialValueSet(String parentKey, String childKey, String value) {
        // no ops
    }

    @Override
    public void onValueChange(String parentKey, String childKey, String value) {
        // no ops
    }

    @Override
    public void onVisibilityChange(String key, String o, boolean b) {
        // no ops
    }

    @Override
    public JsonFormBundle getBundle(Locale locale) {
        return mJsonApi.getBundle(locale);
    }

    @Override
    public JsonExpressionResolver getExpressionResolver() {
        return mJsonApi.getExpressionResolver();
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return mJsonApi.getResourceResolver();
    }

    @Override
    public void historyPush(String stepName) {
        try {
            mJsonApi.historyPush(stepName);
        } catch (Exception e) {
            Log.e(TAG, "Error popping history", e);
        }
    }

    @Override
    public void historyPop() {
        try {
            mJsonApi.historyPop();
        } catch (Exception e) {
            Log.e(TAG, "Error popping history", e);
        }
    }

    @Override
    public void onCurrentItemChanged(@Nullable CarouselAdapter.ViewHolder viewHolder, int adapterPosition) {
        presenter.onCurrentItemChanged(viewHolder, adapterPosition);
    }

    @Override
    public void onScrollStart(@NonNull CarouselAdapter.ViewHolder currentItemHolder, int adapterPosition) {

    }

    @Override
    public void onScrollEnd(@NonNull CarouselAdapter.ViewHolder currentItemHolder, int adapterPosition) {

    }

    @Override
    public void onScroll(float scrollPosition, int currentPosition, int newPosition,
        @Nullable CarouselAdapter.ViewHolder currentHolder, @Nullable CarouselAdapter.ViewHolder newCurrent) {

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mJsonApi.getVisualizationMode() == JsonFormConstants.VISUALIZATION_MODE_EDIT) {
            presenter.writeValuesAndValidate(mMainView);
            PropertiesUtils.getInstance(getContext()).setPausedStep(presenter.getStepName());
        }
    }

    @Override
    public void onFocusChange(View view, boolean b) {
        presenter.onFocusChange(view,b);
    }
}
