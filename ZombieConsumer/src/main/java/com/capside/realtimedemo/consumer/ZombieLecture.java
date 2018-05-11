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
    
    @JsonProperty("drone_id")
    private long droneId;
    @JsonProperty("zombie_id")
    private String zombieId;
    private Date timestamp = new Date();
    private double latitude;
    private double longitude;
    private String utm;
}

