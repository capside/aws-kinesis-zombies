package com.capside.realtimedemo.consumer;


import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import java.io.UnsupportedEncodingException;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Consumer  {
   
    private final String streamName;
    
    private final String region;

    private final SimpMessagingTemplate wsTemplate;    
    
    private int processedRecords;
    
    @Autowired
    public Consumer(@Value("${stream}") String streamName, 
                    @Value("${region}") String region,
                    SimpMessagingTemplate wsTemplate) {
        this.streamName = streamName;
        this.region = region;
        this.wsTemplate = wsTemplate;
    }
    
    @PostConstruct
    private void runKinesisAsync() {
        new Thread() {
            @Override
            public void run() {
                initKinesis();
            }
        }.start();
    }

    private void initKinesis() {
        KinesisClientLibConfiguration config
                = new KinesisClientLibConfiguration(
                        "Zombies",
                        streamName,
                        new DefaultAWSCredentialsProviderChain(),
                        "ZombieConsumer")
                .withRegionName(region)
                .withFailoverTimeMillis(1000*30)                        
                .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON);

        new Worker.Builder()
                .recordProcessorFactory(new RecordProcessorFactory())
                .config(config)
                .build()
                .run();
    }

    private class RecordProcessorImpl implements IRecordProcessor {

        @Override
        public void initialize(InitializationInput initializationInput) {
            log.info("Procesando desde el shard {} empezando por la subsecuencia {}.", 
                     initializationInput.getShardId(), initializationInput.getExtendedSequenceNumber().getSubSequenceNumber());
        }

        @SneakyThrows
        @Override        
        public void processRecords(ProcessRecordsInput processRecordsInput) {
            List<Record> records = processRecordsInput.getRecords();
            // Utilizado para actualizar el último registro procesado
            IRecordProcessorCheckpointer checkpointer = processRecordsInput.getCheckpointer();
            log.info("Iniciando envío de registros.");
            for (Record r : records) {
                try {
                    int len = r.getData().remaining();
                    byte[] buffer = new byte[len];
                    r.getData().get(buffer);
                    String json = new String(buffer, "UTF-8");
                    log.debug(processedRecords++ + ": " + json);
                    wsTemplate.convertAndSend("/topic/zombies", json);
                    if (processedRecords % 1000 == 999) {
                        checkpointer.checkpoint();
                    }
                } catch (UnsupportedEncodingException | MessagingException ex) {
                    log.warn(ex.getMessage());
                }
            }
        }

        @Override
        @SneakyThrows
        public void shutdown(ShutdownInput shutdownInput) {
            IRecordProcessorCheckpointer checkpointer = shutdownInput.getCheckpointer();
            ShutdownReason reason = shutdownInput.getShutdownReason();
            log.info("Finalizado trabajo: {}.", reason);
            checkpointer.checkpoint();
        }
    }

    private class RecordProcessorFactory implements IRecordProcessorFactory {
        @Override
        public IRecordProcessor createProcessor() {
            return new RecordProcessorImpl();
        }
    }

}
