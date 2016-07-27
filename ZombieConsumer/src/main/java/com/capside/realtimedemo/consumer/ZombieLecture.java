package com.capside.realtimedemo.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 *
 * @author ciberado
 */
@Data @NoArgsConstructor @AllArgsConstructor 
@EqualsAndHashCode(of = {"droneId", "zombieId"}) @ToString
public class ZombieLecture {
    
    @JsonProperty("drone-id")
    private long droneId;
    private String zombieId;
    private Date timestamp = new Date();
    private double latitude;
    private double longitude;
}

