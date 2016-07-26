/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.capside.realtimedemo.producer;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * @author ciberado
 */
@Data @AllArgsConstructor
public class ZombieLecture {
    
    @JsonProperty("drone-id")
    private long droneId;
    private String zombieId;
    private Date timestamp = new Date();
    private double latitude;
    private double longitude;
}

