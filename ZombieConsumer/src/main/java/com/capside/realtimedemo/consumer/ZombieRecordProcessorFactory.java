package com.capside.realtimedemo.consumer;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 *
 * @author ciberado
 */
@Component
@Slf4j
public class ZombieRecordProcessorFactory implements IRecordProcessorFactory {
    
    private final SimpMessagingTemplate smt;

    @Autowired
    public ZombieRecordProcessorFactory(SimpMessagingTemplate smt) {
        this.smt = smt;
    }
            
    @Override
    public IRecordProcessor createProcessor() {
        return new ZombieRecordProcessorWebsockets(smt);
    }
    
}
