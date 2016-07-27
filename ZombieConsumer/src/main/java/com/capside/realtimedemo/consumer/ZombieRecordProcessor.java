package com.capside.realtimedemo.consumer;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;

/**
 *
 * @author ciberado
 */
@Slf4j
abstract class ZombieRecordProcessor implements IRecordProcessor {

    private final ObjectMapper mapper;
    private int processedRecords;

    public ZombieRecordProcessor() {
        this.processedRecords = 0;
        this.mapper = new ObjectMapper();
    }

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
        log.info("Recuperando registros desde kinesis.");
        for (Record r : records) {
            try {
                int len = r.getData().remaining();
                byte[] buffer = new byte[len];
                r.getData().get(buffer);
                String json = new String(buffer, "UTF-8");
                ZombieLecture lecture = mapper.readValue(json, ZombieLecture.class);
                this.processZombieLecture(lecture);
                log.debug(processedRecords++ + ": " + json);
                if (processedRecords % 1000 == 999) {
                    // Uncomment next line to keep track of the processed lectures. 
                    checkpointer.checkpoint();
                }
            } catch (UnsupportedEncodingException | MessagingException ex) {
                log.warn(ex.getMessage());
            }
        }
    }

    abstract void processZombieLecture(ZombieLecture lecture);

    @Override
    @SneakyThrows
    public void shutdown(ShutdownInput shutdownInput) {
        IRecordProcessorCheckpointer checkpointer = shutdownInput.getCheckpointer();
        ShutdownReason reason = shutdownInput.getShutdownReason();
        log.info("Finalizado trabajo: {}.", reason);
        checkpointer.checkpoint();
    }

}
