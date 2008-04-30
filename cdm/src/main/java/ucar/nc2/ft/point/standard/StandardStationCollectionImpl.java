/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.ft.point.standard;

import ucar.nc2.ft.point.StationCollectionImpl;
import ucar.nc2.ft.point.StationFeatureImpl;
import ucar.nc2.ft.point.standard.NestedTable;
import ucar.nc2.ft.*;
import ucar.nc2.units.DateUnit;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureData;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Object Heirarchy for StationFeatureCollection:
 *   StationFeatureCollection (StandardStationCollectionImpl
 *   PointFeatureCollectionIterator (anon)
 *     StationFeature (StandardStationFeatureImpl)
 *     PointFeatureIterator (StandardStationPointIterator)
 *       PointFeatureImpl
 *
 * @author caron
 * @since Mar 28, 2008
 */
public class StandardStationCollectionImpl extends StationCollectionImpl {
  private DateUnit timeUnit;
  private NestedTable ft;

  StandardStationCollectionImpl(DateUnit timeUnit, NestedTable ft) throws IOException {
    super(ft.getName());
    this.timeUnit = timeUnit;
    this.ft = ft;

    // LOOK can we defer StationHelper ?
    StructureDataIterator siter = ft.getStationDataIterator(-1);
    while (siter.hasNext()) {
      StructureData stationData = siter.next();
      stationHelper.addStation( makeStation(stationData));
    }
  }

  private Station makeStation(StructureData stationData) {
    Station s = ft.makeStation(stationData);
    return new StandardStationFeatureImpl(s, timeUnit, stationData);
  }

  public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
    return new PointFeatureCollectionIterator() {
      Iterator<Station> stationIter= stationHelper.getStations().iterator();

      public boolean hasNext() throws IOException {
        return stationIter.hasNext();
      }

      public PointFeatureCollection next() throws IOException {
        return (StationFeatureImpl) stationIter.next();
      }

      public void setBufferSize(int bytes) { }
    };
  }

  private class StandardStationFeatureImpl extends StationFeatureImpl {
    StructureData stationData;
    List<StructureData> sdataList;

    StandardStationFeatureImpl(Station s, DateUnit dateUnit, StructureData stationData) {
      super(s, dateUnit, -1);
      this.stationData = stationData;
      sdataList = new ArrayList<StructureData>(2);
      sdataList.add(null);
      sdataList.add(stationData);
    }

    // an iterator over Features of type PointFeature
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      StructureDataIterator obsIter = ft.getStationObsDataIterator(stationData, bufferSize);
      return new StandardStationPointIterator((size() < 0) ? this : null, obsIter, sdataList);
    }

  }

  // the iterator over the observations
  private class StandardStationPointIterator extends StandardPointFeatureIterator {
    StationFeatureImpl station;

    StandardStationPointIterator(StationFeatureImpl station, StructureDataIterator structIter, List <StructureData> sdataList) throws IOException {
      super(ft, timeUnit, structIter, sdataList, false);
      this.station = station;
    }

    // decorate to capture npts
    @Override
    public boolean hasNext() throws IOException {
      boolean result = super.hasNext();
      if (!result && (station != null))
        station.setNumberPoints(getCount());
      return result;
    }
  }

}