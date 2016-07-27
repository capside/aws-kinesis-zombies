package com.capside.realtimedemo.consumer;

import static java.lang.String.format;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

/**
 *
 * @author ciberado
 */
@Controller
public class ZombieCtrl {

    private final ZombieRecordProcessorFactoryOnMemory lectureSource;

    private final SimpMessagingTemplate smt;

    @Autowired
    public ZombieCtrl(ZombieRecordProcessorFactoryOnMemory lectures,
            SimpMessagingTemplate smt) {
        this.lectureSource = lectures;
        this.smt = smt;
    }

    @Scheduled(fixedRate = 5 * 1000)
    public void sendZombies() {
        for (ZombieLecture lecture : lectureSource.getLectures()) {
            String msg = format("%s;%s;%s\r\n", lecture.getZombieId(), lecture.getLatitude(), lecture.getLongitude());            
            smt.convertAndSend(msg);
        }
    }

}
