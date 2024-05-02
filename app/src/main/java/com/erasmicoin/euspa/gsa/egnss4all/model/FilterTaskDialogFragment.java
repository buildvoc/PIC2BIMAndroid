package com.erasmicoin.euspa.gsa.egnss4all.model;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.erasmicoin.euspa.gsa.egnss4all.R;
import com.erasmicoin.euspa.gsa.egnss4all.TaskOverviewActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FilterTaskDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = FilterTaskDialogFragment.class.getName();

    private TaskOverviewActivity taskOverviewActivity;

    public FilterTaskDialogFragment(TaskOverviewActivity taskOverviewActivity) {
        this.taskOverviewActivity = taskOverviewActivity;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new BottomSheetDialog(getContext(), R.style.sheetDialog);
        if (taskOverviewActivity.getFilterView().getParent() != null) {
            ((ViewGroup) taskOverviewActivity.getFilterView().getParent()).removeView(taskOverviewActivity.getFilterView());
        }
        dialog.setContentView(taskOverviewActivity.getFilterView());
        taskOverviewActivity.initFilter();
        return dialog;
    }

    // region get, set


    // endregion
}


