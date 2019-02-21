package com.cyclebikeapp.lookup.airplanes;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

import static com.cyclebikeapp.lookup.airplanes.Constants.DISTANCE_TYPE_METRIC;
import static com.cyclebikeapp.lookup.airplanes.Constants.PREFS_KEY_UNIT;
import static com.cyclebikeapp.lookup.airplanes.Constants.maxRangeValuesMetric;
import static com.cyclebikeapp.lookup.airplanes.Constants.maxRangeValuesMiles;

public class NumberPickerPreference extends DialogPreference {

    private static final int MIN_VALUE = 0;
    // enable or disable the 'circular behavior'
    private static final boolean WRAP_SELECTOR_WHEEL = false;
    private NumberPicker picker;
    private int value;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View onCreateDialogView() {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        picker = new NumberPicker(getContext());
        picker.setLayoutParams(layoutParams);
        EditText numberPickerChild = (EditText) picker.getChildAt(0);
        numberPickerChild.setFocusable(false);
        numberPickerChild.setInputType(InputType.TYPE_NULL);
        picker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
        FrameLayout dialogView = new FrameLayout(getContext());
        dialogView.addView(picker);
        return dialogView;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        picker.setWrapSelectorWheel(WRAP_SELECTOR_WHEEL);
        picker.setValue(getValue());
        //Initializing a new string array with elements
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String distanceUnit = sharedPref.getString(PREFS_KEY_UNIT, Constants.ZERO);
        final String[] values = distanceUnit.equals(String.valueOf(DISTANCE_TYPE_METRIC))?maxRangeValuesMetric:maxRangeValuesMiles;
        //Populate NumberPicker values from String array values
        //Set the minimum value of NumberPicker
        picker.setMinValue(0); //from array first value
        //Specify the maximum value/number of NumberPicker
        picker.setMaxValue(values.length-1); //to array last value
        //Specify the NumberPicker data source as array elements
        picker.setDisplayedValues(values);
        picker.setValue(getValue());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            picker.clearFocus();
            int newValue = picker.getValue();
            if (callChangeListener(newValue)) {
                setValue(newValue);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, MIN_VALUE);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setValue(restorePersistedValue ? getPersistedInt(MIN_VALUE) : (Integer) defaultValue);

    }

    public void setValue(int value) {
        this.value = value;
        persistInt(this.value);
    }

    public int getValue() {
        return this.value;
    }
}
