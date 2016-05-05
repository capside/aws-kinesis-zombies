package com.capside.realtimedemo.geo;

/**
 *
 * @author ciberado
 * @see http://nacc.upc.es/tierra/node10.html
 */
public class Datum {

    public static final Datum WGS84 = new Datum("WGS84", 6378137.000, 6356752.314);
    public static final Datum ED50  = new Datum("ED50", 6378388.000, 6356911.946);

    public static final Datum[] enumeration = {WGS84, ED50};

    private final String name;
    private final double equatorialRadius;
    private final double polarRadius;

    private  double flattening;
    private  double inverseFlattening;
    private  double meanRadius;
    private  double scaleFactor;
    private  double eccentricity;
    private  double e2;
    private  double n;
    private  double a0;
    private  double b0;
    private  double c0;
    private  double d0;
    private  double e0;
    private  double sin1;
    private  double ei;
    private  double c1;
    private  double c2;
    private  double c3;
    private  double c4;


    public Datum(String name, double equatorialRadius, double polarRadius) {
        this.name = name;
        this.equatorialRadius = equatorialRadius;
        this.polarRadius = polarRadius;
        flattening = (equatorialRadius - polarRadius) / equatorialRadius;
        inverseFlattening = 1 / flattening;
        meanRadius = Math.pow(equatorialRadius * polarRadius, 0.5);
        scaleFactor = 0.9996;
        eccentricity = Math.sqrt(1 - Math.pow(polarRadius / equatorialRadius, 2));
        e2 = eccentricity * eccentricity / (1 - eccentricity * eccentricity);
        n = (equatorialRadius - polarRadius) / (equatorialRadius + polarRadius);
        a0 = equatorialRadius * (1 - n + (5 * n * n / 4) * (1 - n) + (81 * pow(n, 4) / 64) * (1 - n));
        b0 = (3 * equatorialRadius * n / 2) * (1 - n - (7 * n * n / 8) * (1 - n) + 55 * pow(n, 4) / 64);
        c0 = (15 * equatorialRadius * n * n / 16) * (1 - n + (3 * n * n / 4) * (1 - n));
        d0 = (35 * equatorialRadius * pow(n, 3) / 48) * (1 - n + 11 * n * n / 16);
        e0 = (315 * equatorialRadius * pow(n, 4) / 51) * (1 - n);
        sin1 = Math.PI / (180 * 3600);
        ei = (1 - pow(1 - eccentricity * eccentricity, 1.0 / 2.0)) /
                (1 + pow(1 - eccentricity * eccentricity, 1.0 / 2.0));
        c1 = 3 * ei / 2.0 - 27 * pow(ei, 3.0) / 32.0;
        c2 = 21 * ei * ei / 16 - 55 * pow(ei, 4) / 32;
        c3 = 151 * pow(ei, 3) / 96;
        c4 = 1097 * pow(ei, 4) / 512;
    }


    public double getEquatorialRadius() {
        return equatorialRadius;
    }

    public String getName() {
        return name;
    }

    public double getPolarRadius() {
        return polarRadius;
    }

    public CoordinateUTM latLonToUTM(CoordinateLatLon latLon, int forcedZone) {
        return latLonToUTM(latLon.getLat(), latLon.getLon(), forcedZone);
    }
    
    public CoordinateUTM latLonToUTM(double lat, double lon, int forcedZone) {
        int zone = (forcedZone >= 0) ? forcedZone : (int) Math.floor(lon / 6) + 31;

        double lonZone = 6 * zone - 183;

        double deltaLonSec = (lon - lonZone) * 3600.0 / 10000.0;

        double latRat = lat * Math.PI / 180;

        double lonRat = lon * Math.PI / 180;

        // =a*(1-e*e)/((1-(e*SIN(Q6))^2)^(3/2))
        double rho = equatorialRadius * (1 - eccentricity * eccentricity) /
                (pow(1 - (pow(eccentricity * sin(latRat), 2)), (3.0 / 2.0)));

        // =a/((1-(e*SIN(Q9))^2)^(1/2))
        double nu = equatorialRadius / (pow(1 - pow(eccentricity * sin(latRat), 2), (1.0 / 2.0)));

        // =A0*Q9-B0*SIN(2*Q9)+C0*SIN(4*Q9)-D0*SIN(6*Q9)+E0*SIN(8*Q9)
        double meridionalArc = a0 * latRat - b0 * sin(2 * latRat) + c0 * sin(4 * latRat) - d0 * sin(6 * latRat) + e0 * sin(8 * latRat);

        double ki = meridionalArc * scaleFactor;

        // =T9*SIN(Q9)*COS(Q9)*Sin1^2*k0*(100000000)/2
        double kii = nu * sin(latRat) * cos(latRat) * pow(Math.PI / (180 * 3600), 2) * scaleFactor * 100000000.0 / 2.0;

        // =((Sin1^4*T9*SIN(Q9)*COS(Q9)^3)/24)*(5-TAN(Q9)^2+9*e1sq*COS(Q9)^2+4*e1sq^2*COS(Q9)^4)*k0*(1E+016)
        double kiii = ((pow(sin1, 4) * nu * sin(latRat) * pow(cos(latRat), 3)) / 24) * (5 - pow(tan(latRat), 2) + 9 * e2 * pow(cos(lat), 2) + 4 * pow(e2, 2) * pow(cos(latRat), 4)) * scaleFactor * (1E+016);

        // =T9*COS(Q9)*Sin1*k0*10000
        double kiv = nu * cos(latRat) * sin1 * scaleFactor * 10000;

        // =(Sin1*COS(Q9))^3*(T9/6)*(1-TAN(Q9)^2+e1sq*COS(Q9)^2)*k0*(1000000000000)

        double kv = pow(sin1 * cos(latRat), 3) * (nu / 6) * (1 - pow(tan(latRat), 2) + e2 * pow(cos(latRat), 2)) * scaleFactor * (1000000000000.0);

        // =((P9*Sin1)^6*T9*SIN(Q9)*COS(Q9)^5/720)*(61-58*TAN(Q9)^2+TAN(Q9)^4+270*e1sq*COS(Q9)^2-330*e1sq*SIN(Q9)^2)*k0*(1E+024)
        double a6 = (pow((deltaLonSec * sin1), 6) * nu * sin(latRat) * pow(cos(latRat), 5) / 720) * (61 - 58 * pow(tan(latRat), 2) + pow(tan(latRat), 4) + 270 * e2 * pow(cos(latRat), 2) - 330 * e2 * pow(sin(latRat), 2)) * scaleFactor * (1E+024);

        // =(X9+Y9*P9*P9+Z9*P9^4)
        double rawNorthing = (ki + kii * deltaLonSec * deltaLonSec + kiii * pow(deltaLonSec, 4));

        double northing = (rawNorthing < 0) ? rawNorthing + 10000000 : rawNorthing;

        // =500000+(AA9*P9+AB9*P9^3)
        // 500000+(AA17*P17+AB17*P17^3)
        double easting = 500000 + kiv * deltaLonSec + kv * pow(deltaLonSec, 3);

        CoordinateUTM coords = new CoordinateUTM(zone, easting, northing, 1.0, this);

        return coords;
    }

    public CoordinateLatLon utmToLatLon(CoordinateUTM utm) {
        int hemis = utm.getHemis();
        int zone = utm.getZone();

        double easting = utm.getX();
        double northing = utm.getY();

        double correctedNorthing = (hemis == CoordinateUTM.NORTH ? northing : 10000000 - northing);
        double eastingPrime = 500000 - easting;

        double arcLength = northing / scaleFactor;

        double mu = arcLength / (equatorialRadius * (1 - pow(eccentricity, 2) / 4.0 - 3 * pow(eccentricity, 4) / 64.0 - 5 * pow(eccentricity, 6) / 256.0));

        double footPrintLat = mu + c1 * sin(2 * mu) + c2 * sin(4 * mu) + c3 * sin(6 * mu) + c4 * sin(8 * mu);

        double cc1 = e2 * pow(cos(footPrintLat), 2);

        double t1 = pow(tan(footPrintLat), 2);

        double n1 = equatorialRadius / pow(1 - pow(eccentricity * sin(footPrintLat), 2), (1.0 / 2.0));

        double r1 = equatorialRadius * (1 - eccentricity * eccentricity) / pow(1 - pow(eccentricity * sin(footPrintLat), 2), (3. / 2.0));

        double d = eastingPrime / (n1 * scaleFactor);

        double fact1 = n1 * tan(footPrintLat) / r1;

        double fact2 = d * d / 2.0;

        double fact3 = (5 + 3 * t1 + 10 * cc1 - 4 * cc1 * cc1 - 9 * e2) * pow(d, 4) / 24;

        double fact4 = (61 + 90 * t1 + 298 * cc1 + 45 * t1 * t1 - 252 * e2 - 3 * c1 * c1) * pow(d, 6) / 720;

        double loFact1 = d;

        double loFact2 = (1 + 2 * t1 + cc1) * pow(d, 3) / 6;

        double loFact3 = (5 - 2 * cc1 + 28 * t1 - 3 * pow(cc1, 2) + 8 * e2 + 24 * pow(t1, 2)) * pow(d, 5) / 120.0;

        double deltaLong = (loFact1 - loFact2 + loFact3) / cos(footPrintLat);

        double zoneCM = 6 * zone - 183;

        double rawLat = 180 * (footPrintLat - fact1 * (fact2 + fact3 + fact4)) / Math.PI;

        double lat = (hemis == CoordinateUTM.NORTH) ? rawLat : rawLat * -1;

        double lon = zoneCM - deltaLong * 180 / Math.PI;

        return new CoordinateLatLon(lat, lon);
    }

    private static double pow(double n, double e) {
        return Math.pow(n, e);
    }

    private static double sin(double n) {
        return Math.sin(n);
    }

    private static double cos(double n) {
        return Math.cos(n);
    }

    private static double tan(double n) {
        return Math.tan(n);
    }

}
