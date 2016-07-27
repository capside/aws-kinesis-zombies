package com.capside.realtimedemo.consumer;

import java.util.Set;

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
        lectures.add(lecture);
    }


    
    
}
