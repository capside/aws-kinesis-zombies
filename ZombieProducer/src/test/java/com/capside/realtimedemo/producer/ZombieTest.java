/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.capside.realtimedemo.producer;

import com.capside.realtimedemo.geo.CoordinateUTM;
import com.capside.realtimedemo.geo.Datum;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author ciberado
 */
public class ZombieTest {
    
    public ZombieTest() {
    }

    /* Coordinates: http://w20.bcn.cat/cartobcn/default.aspx?lang=es */
    @Test
    public void testMovement() {
        CoordinateUTM origin = new CoordinateUTM(31, 500_000, 4_580_000, 1, Datum.WGS84);
        Zombie zombie = new Zombie(0, origin, 10);
        zombie.move();
        
        Assert.assertEquals("Our zombie ran the right number of meters", 
                10, zombie.getCurrentPosition().distanceTo(origin), 0.001);
    }
    
}
