package com.erasmicoin.euspa.gsa.egnss4all.model.ekf;

import eu.foxcom.gnss_compare_core.CalculationModule;

public class EKFStartExeception extends EKFException {
    public EKFStartExeception(String message) {
        super(message);
    }

    public EKFStartExeception(CalculationModule.NameAlreadyRegisteredException e) {
        super(e);
    }

    public EKFStartExeception(CalculationModule.NumberOfSeriesExceededLimitException e) {
        super(e);
    }
}


