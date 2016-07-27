/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.capside.realtimedemo.producer;

import com.capside.realtimedemo.geo.CoordinateLatLon;
import com.capside.realtimedemo.geo.CoordinateUTM;
import com.capside.realtimedemo.geo.Datum;
import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author ciberado
 */
@Getter @Setter
public class Zombie {
    private static final int SECONDS_WALKING_IN_THE_SAME_DIRECTION = 60*3;
    
    private String id;
    private CoordinateUTM currentPosition;
    private CoordinateUTM destination;
    /** speed in meters per second */
    private double speed;
    private double angle;
    
    public Zombie(String id, CoordinateUTM originalPosition, double speed) {
        this.id = id;
        this.currentPosition = new CoordinateUTM(originalPosition);
        this.destination = new CoordinateUTM(originalPosition);
        this.speed = speed;
        this.angle = 0;
    }
    
    /** The zombie will move for a second in its current direction. It will choose
        a new destination if it arrives to the its previous one. */
    public void move() {
        if (currentPosition.distanceTo(destination) < 10) {
            createNewDestination();
        }
        translateCoordinate(currentPosition, 1);
    }
    
    void createNewDestination() {
        angle = Math.random() * 2 * PI;
        translateCoordinate(destination, SECONDS_WALKING_IN_THE_SAME_DIRECTION);
    }

    /** Translate the provided coordinate using the current zombie's direction.
     * @param utm The coordinate to be translated
     * @param seconds The number of seconds the zombie will walk
     */
    void translateCoordinate(CoordinateUTM utm, int seconds) {
        double sx = speed * cos(angle) * seconds;
        double sy = speed * sin(angle) * seconds;
        utm.translate(sx, sy);
    }
    

    /* Quick test. *
    public static void main(String[] args) {
        CoordinateUTM origin = new CoordinateUTM(31, 500_000, 4_580_000, 1, Datum.WGS84);
        Zombie zombie = new Zombie("", origin, 10);
        zombie.move();
        for (int i=0; i<100; i++) {
            CoordinateLatLon latLon= Datum.WGS84.utmToLatLon(zombie.getCurrentPosition());
            System.out.println(i + "," + latLon.getLat() + "," + latLon.getLon());
            zombie.move();
        }
            
        
    }    
    */
    
}
