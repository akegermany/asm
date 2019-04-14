package de.akesting.data;

import java.util.Arrays;
import java.util.Collection;

public enum InputDataType {

    X,
    T,
    V,
    Q,
    RHO,
    OCC,
    WEIGHT;

    public static final Collection<InputDataType> VALUES = Arrays.asList(InputDataType.values());

}
