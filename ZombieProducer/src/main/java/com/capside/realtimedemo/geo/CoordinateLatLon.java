package com.capside.realtimedemo.geo;


/**
 *
 * @author javi
 */
 public class CoordinateLatLon implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    
    private double lat;
    
    private double lon;
    
    
    public CoordinateLatLon(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }


    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getAccuracy() {
        return 1;
    }

    

}
