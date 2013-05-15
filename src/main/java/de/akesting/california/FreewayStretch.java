package de.akesting.california;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;

public class FreewayStretch implements Iterable<FreewayStation> {

    // mapping stationId --> Station
    private final Map<String, FreewayStation> stations = new HashMap<>();
    private final Set<String> districts = new HashSet<>();

    public void add(FreewayStation station) {
        Preconditions.checkNotNull(station);
        if (!stations.isEmpty()) {
            Preconditions.checkArgument(station.reverseDirection() == isReverseDirection(),
                    "inconsistent drivingDirection=" + isReverseDirection() + ", but station=" + station);
        }

        
        boolean alreadyAdded = stations.put(station.station(), station) !=null;
        if (!alreadyAdded) {
            if (!stations.isEmpty() && !districts.contains(station.district())) {
                // System.out.println("freeway="+station.freeway()+" in additional district=" + station.district());
                districts.add(station.district());
            }
        } else {
            System.err.println("station already added=" + station);
        }
    }
    
    @Override
    public Iterator<FreewayStation> iterator() {
        return stations.values().iterator();
    }

    public boolean isReverseDirection() {
        Preconditions.checkArgument(!stations.isEmpty());
        return Iterators.get(iterator(), 0).reverseDirection();
    }

    public Iterable<String> getDistricts() {
        Preconditions.checkArgument(!stations.isEmpty());
        return Collections.unmodifiableSet(districts);
    }

    public FreewayStation getStation(String stationId) {
        return stations.get(stationId);
    }

    public String getFreewayName() {
        Preconditions.checkArgument(!stations.isEmpty());
        return Iterators.get(iterator(), 0).freeway();
    }
    
    public Collection<FreewayStation> getStations(){
        return Collections.unmodifiableCollection(stations.values());
    }

}
