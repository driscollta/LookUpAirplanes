package com.cyclebikeapp.lookup.airplanes;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import static com.cyclebikeapp.lookup.airplanes.Constants.ADF_KEY_ICAO24_NUMBER;
import static com.cyclebikeapp.lookup.airplanes.Constants.ADF_KEY_LINK;
import static com.cyclebikeapp.lookup.airplanes.Constants.ADF_KEY_NUM_SEARCH_RESULTS;

public class AirplaneDialogFragment extends DialogFragment {
    private static final String DDF_KEY_CALLINGTAG = "DDF_key_callingtag";
    static final String SEARCH = "Search";
    private AlertDialog.Builder mDialog;

    @Override
    public Dialog onCreateDialog(final Bundle bundle) {
        String title = getArguments().getString(Constants.ADF_KEY_TITLE);
        final String link = getArguments().getString(ADF_KEY_LINK);
        final String icao24Number = getArguments().getString(ADF_KEY_ICAO24_NUMBER);
        final int numSearchResults = getArguments().getInt(ADF_KEY_NUM_SEARCH_RESULTS);
        String negativeButtonText = getString(R.string.more_info);
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

        mDialog = new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setIcon(R.drawable.ic_airplane_title)
                .setCancelable(true);
        Log.w(this.getClass().getName(), String.format("number results icao24: %s results: %s ", icao24Number, numSearchResults));

        if (numSearchResults >= 1 && numSearchResults < 101) {
            Log.w(this.getClass().getName(), String.format("inflating icao24: %s results: %s ", icao24Number, numSearchResults));

            View content = inflater.inflate(R.layout.airplane_info_alert, null);
            mDialog.setView(content);

            TextView regTV = content.findViewById(R.id.reg_value);
            regTV.setText(getArguments().getString(Constants.ADF_KEY_REGISTRATION));

            TextView icao24TV = content.findViewById(R.id.icao24_value);
            icao24TV.setText(getArguments().getString(Constants.ADF_KEY_ICAO24_NUMBER));

            TextView manTV = content.findViewById(R.id.man_value);
            manTV.setText(getArguments().getString(Constants.ADF_KEY_MANUFACTURER));

            TextView modelTV = content.findViewById(R.id.mod_value);
            modelTV.setText(getArguments().getString(Constants.ADF_KEY_MODEL));

            TextView ownerTV = content.findViewById(R.id.owner_value);
            ownerTV.setText(getArguments().getString(Constants.ADF_KEY_OWNER));
            if (link != null) {
                mDialog.setNegativeButton(negativeButtonText,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Bundle bundle = new Bundle();
                                bundle.putCharSequence(ADF_KEY_LINK, link);
                                bundle.putCharSequence(ADF_KEY_ICAO24_NUMBER, icao24Number);
                                if (getTag().contains(SEARCH)) {
                                    ((SearchableActivity) getActivity()).doAirplaneDialogNegativeClick(bundle);
                                }
                            }
                        }
                );
            }
        }
        mDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (getTag().contains(SEARCH)) {
                            Bundle bundle = new Bundle();
                            bundle.putInt(ADF_KEY_NUM_SEARCH_RESULTS, numSearchResults);
                            ((SearchableActivity) getActivity()).doAirplaneDialogPostiveClick(bundle);
                        }
                    }
                });

        return mDialog.create();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SatelliteDialogFragment.
     */
    public static AirplaneDialogFragment newInstance(Bundle b) {
        AirplaneDialogFragment frag = new AirplaneDialogFragment();
        String callingTag = frag.getTag();
        b.putCharSequence(DDF_KEY_CALLINGTAG, callingTag);
        frag.setArguments(b);
        return frag;
    }
    public AirplaneDialogFragment() {
        // Required empty public constructor
    }

}
