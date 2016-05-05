package com.capside.realtimedemo.geo;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.commons.lang.StringUtils;

 public class CoordinateUTM implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	public static final int NORTH = 1;
    public static final int SOUT = 2;
    public static final int DEFAULT_ZONE = 31;
    /** Hemisferio. */
    private int hemis = NORTH;
    /** Huso UTM. */
    private int zone;
    /** Coordeanada x respecto al uso actual. */
    private double x;
    /** Coordenada y respecto al uso actual. */
    private double y;
    /** Precision en metros, por defecto 10000.0 (cuadrados de 10x10 km) */
    private double accuracy;
    
    private Datum datum;


    public CoordinateUTM(int zone, double xUTM, double yUTM, double accuracy, Datum datum) {
        this.setZone(zone);
        this.setX(xUTM);
        this.setY(yUTM);
        this.setAccuracy(accuracy);
        this.datum = datum;
    }


    public CoordinateUTM(int zone, String utm) {
        this.setZone(zone);
        setFromString(zone, utm);
    }

    public CoordinateUTM(String utm) {
        this(calculateZone(utm), utm);
    }

    public CoordinateUTM(CoordinateUTM utm) {
        this(utm.zone, utm.x, utm.y, utm.accuracy, utm.getDatum());
    }
    
    public static int calculateZone(String utm) {
        int zone;
        if (utm.length() > "AA0000".length()) {
            zone = Integer.parseInt(utm.substring(0, 2));
        } else {
            String digraphX = utm.substring(0, 1).toUpperCase();

            if ("MNPQ".indexOf(digraphX) != -1) {
                zone = 29;
            } else if ("TUVWXY".indexOf(digraphX) != -1) {
                zone = 30;
            } else if ("BCDEF".indexOf(digraphX) == -1) {
                zone = 31;
            } else {
                zone = DEFAULT_ZONE;
            }
        }

        return zone;
    }
    
    private static final NumberFormat nfx = new DecimalFormat("000000");
    private static final NumberFormat nfy = new DecimalFormat("0000000");

    public String getSquare() {

        if ((x < 1E5) || (y < 1E6)) {
            return "";
        }

        // Algoritmo explicado en p�g 154 de Talleres Servicios Geogr�ficos Ej�rcito (1976)

        char digraphX = 0x0000;
        int centKmX = Integer.parseInt(nfx.format(getX()).substring(0, 1));
        if (zone % 3 == 1) {
            digraphX = "ABCDEFGH".charAt(centKmX - 1);
        } else if (zone % 3 == 2) {
            digraphX = "JKLMNPQR".charAt(centKmX - 1);
        } else if (zone % 3 == 0) {
            digraphX = "STUVWXYZ".charAt(centKmX - 1);
        }

        char digraphY = 0x0000;
        int millKm = Integer.parseInt(nfy.format(getY()).substring(0, 1));
        int centKm = Integer.parseInt(nfy.format(getY()).substring(1, 2));
        if (millKm % 2 == 0) {
            if (zone % 2 == 1) {
                digraphY = "ABCDEFGHJK".charAt(centKm);
            } else if (zone % 1 == 0) {
                digraphY = "FGHJKLMNPQ".charAt(centKm);
            }
        } else {
            if (zone % 2 == 1) {
                digraphY = "LMNPQRSTUV".charAt(centKm);
            } else if (zone % 1 == 0) {
                digraphY = "RSTUVABCDE".charAt(centKm);
            }
        }

        int len = 6 - String.valueOf((int) accuracy).length();

        String digX = String.valueOf(Math.round(getX()));
        digX = digX.substring(1, 1 + len);

        String digY = String.valueOf(Math.round(getY()));
        digY = digY.substring(2, 2 + len);

        return String.valueOf(digraphX) +  String.valueOf(digraphY)
                + digX + digY;
    }

    public String getYLetter() {
        double lat = datum.utmToLatLon(this).getLat();
        String letter;
        if ((84 >= lat) && (lat >= 72)) {
            letter = "X";
        } else if ((72 > lat) && (lat >= 64)) {
            letter = "W";
        } else if ((64 > lat) && (lat >= 56)) {
            letter = "V";
        } else if ((56 > lat) && (lat >= 48)) {
            letter = "U";
        } else if ((48 > lat) && (lat >= 40)) {
            letter = "T";
        } else if ((40 > lat) && (lat >= 32)) {
            letter = "S";
        } else if ((32 > lat) && (lat >= 24)) {
            letter = "R";
        } else if ((24 > lat) && (lat >= 16)) {
            letter = "Q";
        } else if ((16 > lat) && (lat >= 8)) {
            letter = "P";
        } else if ((8 > lat) && (lat >= 0)) {
            letter = "N";
        } else if ((0 > lat) && (lat >= -8)) {
            letter = "M";
        } else if ((-8 > lat) && (lat >= -16)) {
            letter = "L";
        } else if ((-16 > lat) && (lat >= -24)) {
            letter = "K";
        } else if ((-24 > lat) && (lat >= -32)) {
            letter = "J";
        } else if ((-32 > lat) && (lat >= -40)) {
            letter = "H";
        } else if ((-40 > lat) && (lat >= -48)) {
            letter = "G";
        } else if ((-48 > lat) && (lat >= -56)) {
            letter = "F";
        } else if ((-56 > lat) && (lat >= -64)) {
            letter = "E";
        } else if ((-64 > lat) && (lat >= -72)) {
            letter = "D";
        } else if ((-72 > lat) && (lat >= -80)) {
            letter = "C";
        } else {
            letter = "Z";
        }
        return letter;
    }

    private void setFromString(int zone, String mgrs) {
        mgrs = StringUtils.remove(mgrs, " ");
        mgrs = mgrs.toUpperCase();
        if (Character.isDigit(mgrs.charAt(0)) == false ) {
            mgrs = zone + "T" + mgrs;
        } else if (Character.isDigit(mgrs.charAt(1)) == false ) {
            mgrs = "0" + mgrs;
        }
        char yLetter = mgrs.charAt(2);
        char digraphX = mgrs.charAt(3);
        char digraphY = mgrs.charAt(4);

        int precission = (mgrs.length() - "31TXX".length()) / 2;
        this.setAccuracy(Math.pow(10, 5 - precission));
        double offsetX = Double.parseDouble(
                mgrs.substring("31TXX".length(), "31TXX".length() + precission)) * accuracy;
        double offsetY = Double.parseDouble(
                mgrs.substring("31TXX".length() + precission, mgrs.length())) * accuracy;


        int eastDigraphX = digraphX - 'A';
        if (eastDigraphX > 15) {
            eastDigraphX = eastDigraphX - 1;
        }
        if (eastDigraphX > 9) {
            eastDigraphX = eastDigraphX - 1;
        }

        double easting = (((eastDigraphX % 8 + 1) * 100000)) % 1000000 + offsetX;

        int northDigraphY = digraphY - 'A' + 1;

        int group = (zone - 1) % 6 + 1;
        if (northDigraphY > 15) {
            northDigraphY = northDigraphY - 1;
        }
        if (northDigraphY > 9) {
            northDigraphY = northDigraphY - 1;
        }
        if ((group % 2) == 0) {
            northDigraphY = northDigraphY - 5;
        }
        if (northDigraphY < 0) {
            northDigraphY = northDigraphY + 16;
        }

        double northing = 0;

        boolean isOffset = ((group % 2) == 0);

        if (yLetter == 'Q') {
            if ((!isOffset && digraphY < 'T')
                    || (isOffset && (digraphY < 'C' || digraphY > 'E'))) {
                northing = 2000000;
            }
        }
        if (yLetter == 'R') {
            northing = 2000000;
        }
        if (yLetter == 'S') {
            if ((!isOffset && digraphY < 'R') || (isOffset && (digraphY > 'E'))) {
                northing = 4000000;
            } else {
                northing = 2000000;
            }
        }
        if (yLetter == 'T') {
            northing = 4000000;
        }
        if (yLetter == 'U') {
            if ((!isOffset && digraphY < 'P') || (isOffset && (digraphY < 'U'))) {
                northing = 6000000;
            } else {
                northing = 4000000;
            }
        }
        if ((yLetter == 'V') || (yLetter == 'W')) {
            northing += 6000000;
        }
        if (yLetter == 'X') {
            northing = 8000000;
        }

        northing = northing + (100000 * (northDigraphY - 1)) + offsetY;

        this.setX(easting);
        this.setY(northing);
        this.setZone(zone);
    }

    private void setX(double x) {
        this.x = x;
    }

    public double getX() {
        return x;
    }

    private void setY(double y) {
        this.y = y;
    }

    public double getY() {
        return y;
    }

    private void setZone(int zone) {
        this.zone = zone;
    }

    public int getZone() {
        return zone;
    }

    public String getShortForm() {
        return getZone() + getYLetter() + getSquare();
    }

    public String toString() {
        return "(" + getZone() + ", " + getX() + ", " + getY() + ", " + getSquare() + ")";
    }

    public void setAccuracy(double precision) {
        this.accuracy = precision;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public double distanceTo(CoordinateUTM other) {
        if (this.zone != other.zone) {
            throw new IllegalArgumentException("Must be from the same zone.");
        }
        return Math.sqrt((this.x - other.x) * (this.x - other.x)
                + (this.y - other.y) * (this.y - other.y));
    }

    public void translate(double dx, double dy) {
        x = x + dx;
        y = y + dy;
    }

    public void translateWithAngle(double angle, double radius) {
        x = x + Math.cos(angle) * radius;
        y = y + Math.sin(angle) * radius;
    }

    public int getHemis() {
        return hemis;
    }

    public void setHemis(int hemis) {
        this.hemis = hemis;
    }
    
    public void setDatum(Datum datum) {
		this.datum = datum;
	}
    
    public Datum getDatum() {
		return datum;
	}
    
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CoordinateUTM other = (CoordinateUTM) obj;
        if (this.hemis != other.hemis) {
            return false;
        }
        if (this.zone != other.zone) {
            return false;
        }
        if (this.x != other.x) {
            return false;
        }
        if (this.y != other.y) {
            return false;
        }
        if (this.accuracy != other.accuracy) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + this.hemis;
        hash = 13 * hash + this.zone;
        hash = 13 * hash + (int) (Double.doubleToLongBits(this.x) ^ (Double.doubleToLongBits(this.x) >>> 32));
        hash = 13 * hash + (int) (Double.doubleToLongBits(this.y) ^ (Double.doubleToLongBits(this.y) >>> 32));
        hash = 13 * hash + (int) (Double.doubleToLongBits(this.accuracy) ^ (Double.doubleToLongBits(this.accuracy) >>> 32));
        return hash;
    }



}
