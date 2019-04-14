package de.akesting.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public enum OutputDataType {

    V_FREE,
    V_CONG,
    WEIGHT,
    V_OUT,

    FLOW_FREE,
    FLOW_CONG,
    FLOW_OUT,

    RHO_FREE,
    RHO_CONG,
    RHO_OUT,

    OCC_FREE,
    OCC_CONG,
    OCC_OUT,

    NORM_FREE,
    NORM_CONG,
    NORM_OUT;

    public static final Collection<OutputDataType> VALUES = Arrays.asList(OutputDataType.values());

}
