package de.akesting.california;

import java.util.Arrays;

import com.google.common.base.Preconditions;

// hashCode and equals used in Set to check for redundent stations in input
public class FreewayStation {

    // returns null if line cannot be parsed and
    public static FreewayStation parse(String[] line) {
        FreewayStation station = null;
        try{
            station = new FreewayStation(line);
            return station;
        }
        catch(IllegalArgumentException e){
            System.err.println("parsing error in line="+Arrays.toString(line));
            System.err.println(e);
            return null;
        }
    }

    static final String[] STATION_COLUMNS = "Freeway,Station,Lanetype,Absolute_Postmile,Lanes,Direction,District,Name,California_Postmile,Latitude,Longitude".split(",");
    private final double FACTOR_MILES_TO_METERS = 1609.34;
    
    private final String freeway;
    private final String station;
    private final String lanetype;
    private final double absolutePostmileMeters;
    private final int lanes;
    private final boolean reverseDirection;
    private final String name;
    private final String district;
    private final String californiaPostmile;
    private final double latitude;
    private final double longitude;
    
    private FreewayStation(String[] line) throws NumberFormatException, IllegalArgumentException{
        Preconditions.checkArgument(line.length == STATION_COLUMNS.length);
        trim(line);
        // simply read in all columns in order of format
        int column = 0;
        freeway = line[column++];
        station = line[column++];
        lanetype    = line[column++];
        absolutePostmileMeters = FACTOR_MILES_TO_METERS* Double.parseDouble(line[column++]);
        lanes = Integer.parseInt(line[column++]);
        reverseDirection = line[column++].equals("d"); // decreasing=reverse direction 
        district = line[column++];
        name = line[column++];
        californiaPostmile = line[column++];
        latitude = Double.parseDouble(line[column++]);
        longitude = Double.parseDouble(line[column++]);
    }
    
    private static void trim(String[] data) {
        for (int i = 0, N = data.length; i < N; i++) {
            data[i] = data[i].trim();
        }
    }

    String freeway() {
        return freeway;
    }

    String station() {
        return station;
    }
    
    String lanetype() {
    	return lanetype;
    }

    double absolutePostmileMeters() {
        return absolutePostmileMeters;
    }

    int lanes() {
        return lanes;
    }

    boolean reverseDirection() {
        return reverseDirection;
    }

    String name() {
        return name;
    }

    String district() {
        return district;
    }

    String californiaPostmile() {
        return californiaPostmile;
    }

    double latitude() {
        return latitude;
    }

    double longitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return "FreewayStation [district="+district+", freeway=" + freeway + ", station=" + station + ", lanetype=" + lanetype + ", absolutePostmileMeters="
                + absolutePostmileMeters + ", lanes=" + lanes + ", reverseDirection=" + reverseDirection + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(FACTOR_MILES_TO_METERS);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(absolutePostmileMeters);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((californiaPostmile == null) ? 0 : californiaPostmile.hashCode());
        result = prime * result + ((district == null) ? 0 : district.hashCode());
        result = prime * result + ((freeway == null) ? 0 : freeway.hashCode());
        result = prime * result + lanes;
        temp = Double.doubleToLongBits(latitude);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (reverseDirection ? 1231 : 1237);
        result = prime * result + ((station == null) ? 0 : station.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FreewayStation other = (FreewayStation) obj;
        if (Double.doubleToLongBits(FACTOR_MILES_TO_METERS) != Double.doubleToLongBits(other.FACTOR_MILES_TO_METERS))
            return false;
        if (Double.doubleToLongBits(absolutePostmileMeters) != Double.doubleToLongBits(other.absolutePostmileMeters))
            return false;
        if (californiaPostmile == null) {
            if (other.californiaPostmile != null)
                return false;
        } else if (!californiaPostmile.equals(other.californiaPostmile))
            return false;
        if (district == null) {
            if (other.district != null)
                return false;
        } else if (!district.equals(other.district))
            return false;
        if (freeway == null) {
            if (other.freeway != null)
                return false;
        } else if (!freeway.equals(other.freeway))
            return false;
        if (lanes != other.lanes)
            return false;
        if (Double.doubleToLongBits(latitude) != Double.doubleToLongBits(other.latitude))
            return false;
        if (Double.doubleToLongBits(longitude) != Double.doubleToLongBits(other.longitude))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (reverseDirection != other.reverseDirection)
            return false;
        if (station == null) {
            if (other.station != null)
                return false;
        } else if (!station.equals(other.station))
            return false;
        return true;
    }

   

}
