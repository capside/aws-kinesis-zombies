package com.capside.realtimedemo.consumer;

import java.util.HashSet;
import java.util.Set;
import static java.lang.String.format;

/**
 *
 * @author ciberado
 */
public class ZombieRecordProcessorOnMemory extends ZombieRecordProcessor {

    private final Set<ZombieLecture> lectures;       

    public ZombieRecordProcessorOnMemory(Set<ZombieLecture> lectures) {
        this.lectures = lectures;
    }

    @Override
    void processZombieLecture(ZombieLecture lecture) {
        String msg = format("%s;%s;%s\r\n", lecture.getZombieId(), lecture.getLatitude(), lecture.getLongitude());
        lectures.add(lecture);
    }


    
    
}
