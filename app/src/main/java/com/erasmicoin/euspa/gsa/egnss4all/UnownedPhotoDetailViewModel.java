package com.erasmicoin.euspa.gsa.egnss4all;

import androidx.lifecycle.ViewModel;

public class UnownedPhotoDetailViewModel extends ViewModel {

    private boolean isLastNoteDialogShown = false;
    private String dialogNote;

    // region get, set

    public String getDialogNote() {
        return dialogNote;
    }

    public void setDialogNote(String dialogNote) {
        this.dialogNote = dialogNote;
    }

    public boolean isLastNoteDialogShown() {
        return isLastNoteDialogShown;
    }

    public void setLastNoteDialogShown(boolean lastNoteDialogShown) {
        isLastNoteDialogShown = lastNoteDialogShown;
    }

    // endregion

}


