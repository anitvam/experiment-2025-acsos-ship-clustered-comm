# Kiel Map Properties from S57

The goal here is to prepare two maps types that will be used through development.
These two maps mimic the properties of the orginal map that will be used in the creation of this project (of type S57).

The two maps are:

1. ``.geojson`` polygon map, where all the properties are defined as shapely polygons or lines.
2. ``.npy`` raster map, where all the properties are tansfered as layers in a 3D numpy arrays.

## Environment

Listed below are the layers of the exported maps and the properties used in each layer.
For the creation of each layer, the couterpart S57 property is listed in the brackets.

### General Properties

1. Coast Line: Union[``SLCONS``, ``COALNE``, ``LNDARE``]
2. Depth: Union [``DEPARE``, ``DEPCNT``] 
   a. Min: [``DRVAL1``] for ``DEPARE`` and [``VALDCO``] for ``DEPCNT``
   b. Max: [``DRVAL2``]       
3. Restricted Area: ``RESARE``
4. Nature Reserve: [``??``]
5. Archor Area: [``ACHARE``]
6. Harbour: [``HRBFAC``, `HRBARE`]
   
   > NOTE: This is only a point in the middle of the a harbour, not a polygon regresenting a harbour boarders
7. Buoys: [``BOYCAR``, ``BOYINB``, ``BOYISD``, ``BOYLAT``, ``BOYSAW``, ``BOYSPP``]
    a. Type: See the list above
    b. Color: ``COLOR``
8. Fairway: Closed polygon that connects the red-green buoys
9. Beacons: [``BCNCAR``,``BCNISD``,``BCNLAT``,``BCNSAW``,``BCNSPP``]
10. Fog Signal: [``FOGSIG``]
    a. Type: ``CATFOG``
11. Pile: [``PILPNT``]
    a. Type: ``CATPLE``
12. Mooring Facility: [``MORFAC``]
    a. Type: ``CATMOR``
13. Navigation Lines: [``NAVLNE``]
    a. Type: ``CATNAV``
    b. Angle: ``ORIENT``

### "Ploygon" Map

+ All **General Properties**. The result map has ``geojson`` type.
+ Regarding the General Property 7: Add a polygon with <u>hand-drawn</u> harbour area, and keep property **7** as a value since it can be used for coarse aim.
+ Connect <u>by hand</u> the red-boyes and the same for the green-boyes, to create a new property (Nr. 14) under the name **Water Way**

### Raster Map (10m pro pixel resolution)

+ **General Properties** except [1, 2, 12, 14]
  + Regarding property **1** and **2**, the raster map will fuse both coastline and depths in one layer (the first depth-layer in the ndarray), where ``Depths``: {>=0: ``Land``, <0: ``Water``}

## References

- https://www.teledynecaris.com/s-57/frames/S57catalog.htm