package de.akesting.data;

import com.google.common.base.Preconditions;

import de.akesting.autogen.Format;

public class DataFormat {

    private final Format formatConfiguration;

    private double timeOffset = 0;
    private double posOffset = 0;

    public DataFormat(Format formatConfig) {
        this.formatConfiguration = Preconditions.checkNotNull(formatConfig);
        if (!formatConfig.isSetColPosition()) {
            System.out.println(" ... position not provided by data file ... read position from xml ...");
        }

        // offset in time (correction for summer time!)
        // if (XmlUtils.containsAttribute(elem, XmlElements.FormatTimeOffsetH)) {
        // timeOffset = 3600. * XmlUtils.getDoubleValue(elem, XmlElements.FormatTimeOffsetH);
        // System.out.println(" shift time  data: offset(h) = " + timeOffset / 3600.);
        // }

        // offset in time (for comparison to simulated data )
        // if (XmlUtils.containsAttribute(elem, XmlElements.FormatPositionOffsetKm)) {
        // posOffset = 1000. * XmlUtils.getDoubleValue(elem, XmlElements.FormatPositionOffsetKm);
        // System.out.println(" shift position data: offset(km) = " + posOffset / 1000.);
        // }
    }

    public Format getFormat() {
        return formatConfiguration;
    }

    public final double offsetTime() {
        return timeOffset;
    }

    public final double offsetPosition() {
        return posOffset;
    }
}
