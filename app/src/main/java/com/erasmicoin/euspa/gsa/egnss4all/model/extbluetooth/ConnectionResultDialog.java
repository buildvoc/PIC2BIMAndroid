package com.erasmicoin.euspa.gsa.egnss4all.model.extbluetooth;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.erasmicoin.euspa.gsa.egnss4all.R;

public class ConnectionResultDialog extends DialogFragment {
    private boolean status;

    public ConnectionResultDialog(boolean status) {
        this.status = status;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(status ? R.string.bts_resultDialogTitleOk : R.string.bts_resultDialogTitleKo)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }
}
