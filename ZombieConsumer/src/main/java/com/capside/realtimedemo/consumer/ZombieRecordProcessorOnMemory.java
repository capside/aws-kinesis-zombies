package com.capside.realtimedemo.consumer;

import static java.lang.String.format;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author ciberado
 */
@Slf4j
public class ZombieRecordProcessorOnMemory extends ZombieRecordProcessor {

    private final Set<ZombieLecture> lectures;   

    public ZombieRecordProcessorOnMemory(Set<ZombieLecture> lectures) {
        this.lectures = lectures;
    }

    @Override
    void processZombieLecture(ZombieLecture lecture) {
        String msg = format("%s;%s;%s;%s\r\n", lecture.getTimestamp(), lecture.getZombieId(), lecture.getLatitude(), lecture.getLongitude());            
        log.debug("Storing new lecture {}.", msg);
        if (lectures.contains(lecture)) lectures.remove(lecture);
        lectures.add(lecture);
    }


    
    
}
