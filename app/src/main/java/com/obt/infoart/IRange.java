package com.obt.infoart;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Region;

import java.util.Collection;

/**
 * Created by Administrador on 04/03/2017.
 */

public interface IRange {
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region);
}
