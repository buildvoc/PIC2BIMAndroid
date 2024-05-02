package com.erasmicoin.euspa.gsa.egnss4all.model.extbluetooth;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.erasmicoin.euspa.gsa.egnss4all.R;


public class SelectDeviceDialog extends DialogFragment {

    private String[] deviceNames;

    public SelectDeviceDialog(String[] deviceNames) {
        this.deviceNames = deviceNames;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.bts_selectDevicePair)
                .setItems(deviceNames, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        //listener.onDialogSelection(deviceNames[which]);
                        Bundle x = new Bundle();
                        x.putString("DeviceName",deviceNames[which]);
                        getParentFragmentManager().setFragmentResult("DEVICE_SELECT", x);
                    }
                });
        return builder.create();
    }

    public interface DialogListener {
        public void onDialogSelection(String selectedDeviceName);
    }

    DialogListener listener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    /*@Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = (DialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString() + " must implement DialogListener");
        }
    }*/
}
