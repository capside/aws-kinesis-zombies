package com.capside.realtimedemo.consumer;

import static java.lang.String.format;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 *
 * @author ciberado
 */
public class ZombieRecordProcessorWebsockets extends ZombieRecordProcessor {

    private final SimpMessagingTemplate smt;

    public ZombieRecordProcessorWebsockets(SimpMessagingTemplate smt) {
        this.smt = smt;
    }
    
    
    
    @Override
    void processZombieLecture(ZombieLecture lecture) {
        String msg = format("%s;%s;%s", lecture.getZombieId(), lecture.getLatitude(), lecture.getLongitude());
        smt.convertAndSend("/topic/zombies", msg);
    }
    
}
