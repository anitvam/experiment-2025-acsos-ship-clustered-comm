# Explanation of data values:
MMSI | [Maritime Mobile Service Identity](https://en.wikipedia.org/wiki/MMSI)
TIME | data timestamp AIS format – unix timestamp Human readable format – UTC
LONGITUDE | geographical longitude AIS format – in 1/10000 minute i.e. degrees multiplied by 600000 Human readable format – degrees
LATITUDE | geographical latitude AIS format – in 1/10000 minute i.e. degrees multiplied by 600000 Human readable format – degrees
COG | Course Over Ground AIS format – in 1/10 degrees i.e. degrees multiplied by 10. COG=3600 means “not available” Human readable format – degrees. COG=360.0 means “not available”
SOG | Speed Over Ground AIS format – in 1/10 knots i.e. knots multiplied by 10. SOG=1024 means “not available” Human readable format – knots. SOG=102.4 means “not available”
HEADING | current heading of the AIS vessel at the time of the last message value in degrees, HEADING=511 means “not available”
PAC | (AIS format only) – Position Accuracy 0 – low accuracy 1 – high accuracy
ROT | (AIS format only) - [Rate of Turn](https://www.navcen.uscg.gov/?pageName=AISMessagesA)
NAVSTAT | [Navigational Status](https://www.navcen.uscg.gov/?pageName=AISMessagesA)
IMO | [IMO ship identification number](https://en.wikipedia.org/wiki/IMO_ship_identification_number)
NAME | vessel’s name (max.20 chars)
CALLSIGN | vessel’s callsign
TYPE | vessel’s type ([more details here](https://www.navcen.uscg.gov/?pageName=AISMessagesA))
DEVICE | positioning device type ([more details here](https://www.navcen.uscg.gov/?pageName=AISMessagesA))
A | Dimension to Bow (meters)
B | Dimension to Stern (meters)
C | Dimension to Port (meters)
D | Dimension to Starboard (meters)
DRAUGHT | AIS format – in 1/10 meters i.e. draught multiplied by 10. Human readable format – meters
DEST | vessel’s destination
ETA | Estimated Time of Arrival
AIS | format ([see here](https://www.navcen.uscg.gov/?pageName=AISMessagesA)).
Human readable format – UTC date/time

### Source of data 
https://www.aishub.net/
