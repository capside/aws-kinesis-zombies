package com.capside.realtimedemo.consumer;

import static java.lang.String.format;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

/**
 *
 * @author ciberado
 */
@Controller
@Slf4j
public class ZombieCtrl { 

    private final ZombieRecordProcessorFactoryOnMemory lectureSource;

    private final SimpMessagingTemplate smt;

    @Autowired
    public ZombieCtrl(ZombieRecordProcessorFactoryOnMemory lectures,
            SimpMessagingTemplate smt) {
        this.lectureSource = lectures;
        this.smt = smt;
    }

    @Scheduled(fixedRate = 2 * 1000)
    public void sendZombies() {
        for (ZombieLecture lecture : lectureSource.getLectures()) {
            String msg = format("%s;%s;%s;%s\r\n", lecture.getTimestamp(), lecture.getZombieId(), lecture.getLatitude(), lecture.getLongitude());            
            log.debug("Sending message {}.", msg);
            smt.convertAndSend("/topic/zombies", msg);
        }
    }

}
