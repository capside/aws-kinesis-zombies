package com.capside.realtimedemo.consumer;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *
 * @author ciberado
 */
@Component
@Slf4j
public class ZombieRecordProcessorOnMemoryFactory implements IRecordProcessorFactory {
    
    private final Set<ZombieLecture> lectures = new HashSet<>();
    
    @Override
    public IRecordProcessor createProcessor() {
        return new ZombieRecordProcessorOnMemory(lectures);
    }

    public Set<ZombieLecture> getLectures() {
        return lectures;
    }
    
    
}
