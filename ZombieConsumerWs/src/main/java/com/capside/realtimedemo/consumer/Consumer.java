package com.capside.realtimedemo.consumer;


import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/* More information: https://blogs.aws.amazon.com/bigdata/post/TxFCI3UJJJYEXJ/Process-Large-DynamoDB-Streams-Using-Multiple-Amazon-Kinesis-Client-Library-KCL
*/
@Component
@Slf4j
public class Consumer  {
   
    private final String streamName;
    
    private final String region;

    private final SimpMessagingTemplate wsTemplate;    
            
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

    // Must read: https://github.com/awslabs/amazon-kinesis-client/blob/master/src/main/java/com/amazonaws/services/kinesis/clientlibrary/lib/worker/KinesisClientLibConfiguration.java
    private void initKinesis() {
        KinesisClientLibConfiguration config
                = new KinesisClientLibConfiguration(
                        "Zombies",
                        streamName,
                        new DefaultAWSCredentialsProviderChain(),
                        "ZombieConsumer")
                .withRegionName(region)
                .withFailoverTimeMillis(1000*30) // after 30 seconds this worker is considered ko                        
                .withMaxLeasesForWorker(1) // forced to read only 1 shard for demo reasons.
                .withMaxRecords(10000) // max recrods per GetRecords
                .withCallProcessRecordsEvenForEmptyRecordList(false) // no records -> no precessing
                .withInitialLeaseTableWriteCapacity(10) // Dynamodb lease table capacity
                .withInitialLeaseTableReadCapacity(10)
                .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON);

        new Worker.Builder()
                .recordProcessorFactory(new RecordProcessorFactory())
                .config(config)
                .build()
                .run();
    }


    private class RecordProcessorFactory implements IRecordProcessorFactory {
        @Override
        public IRecordProcessor createProcessor() {
            return new ZombieRecordProcessor();
        }
    }

}
