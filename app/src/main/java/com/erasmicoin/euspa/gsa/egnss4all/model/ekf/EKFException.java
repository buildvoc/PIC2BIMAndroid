package com.erasmicoin.euspa.gsa.egnss4all.model.ekf;

import eu.foxcom.gnss_compare_core.CalculationModule;

class EKFException extends Exception {
    CalculationModule.NameAlreadyRegisteredException nameAlreadyRegisteredException;
    CalculationModule.NumberOfSeriesExceededLimitException numberOfSeriesExceededLimitException;

    public EKFException(String message) {
        super(message);
    }

    public EKFException(CalculationModule.NameAlreadyRegisteredException e) {
        super(e);
        this.nameAlreadyRegisteredException = e;
    }

    public EKFException(CalculationModule.NumberOfSeriesExceededLimitException e) {
        super(e);
        this.numberOfSeriesExceededLimitException = e;
    }
}


