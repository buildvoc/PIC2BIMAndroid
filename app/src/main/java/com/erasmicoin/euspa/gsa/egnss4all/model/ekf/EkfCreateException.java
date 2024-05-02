package com.erasmicoin.euspa.gsa.egnss4all.model.ekf;

import eu.foxcom.gnss_compare_core.CalculationModule;

public class EkfCreateException extends EKFException {

    public EkfCreateException(String message) {
        super(message);
    }

    public EkfCreateException(CalculationModule.NameAlreadyRegisteredException e) {
        super(e);
    }

    public EkfCreateException(CalculationModule.NumberOfSeriesExceededLimitException e) {
        super(e);
    }
}


