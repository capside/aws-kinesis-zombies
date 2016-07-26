package com.capside.realtimedemo.consumer;


import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import java.lang.management.ManagementFactory;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/* More information: https://blogs.aws.amazon.com/bigdata/post/TxFCI3UJJJYEXJ/Process-Large-DynamoDB-Streams-Using-Multiple-Amazon-Kinesis-Client-Library-KCL
*/
@Component
@Slf4j
public class Consumer  {
   
    private final String streamName;
    
    private final String region;

    private final IRecordProcessorFactory zombieRecordFactory;
    
    @Autowired
    public Consumer(@Value("${stream}") String streamName, 
                    @Value("${region}") String region,
                    IRecordProcessorFactory zombieRecordFactory) {
        this.streamName = streamName;
        this.region = region;
        this.zombieRecordFactory = zombieRecordFactory;
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
        String pid = ManagementFactory.getRuntimeMXBean().getName();
        pid = pid.indexOf('@') == -1 ? pid : pid.substring(0, pid.indexOf('@'));
        log.info("Creating kinesis consumer with pid {}.", pid);
        KinesisClientLibConfiguration config
                = new KinesisClientLibConfiguration(
                        "Zombies" /* aplication name */,  
                        streamName,
                        new DefaultAWSCredentialsProviderChain(),
                        "ZombieConsumer_"+pid /* worker id*/ )
                .withRegionName(region)
                .withFailoverTimeMillis(1000*30) // after 30 seconds this worker is considered ko                        
                .withMaxLeasesForWorker(1) // forced to read only 1 shard for demo reasons.
                .withMaxRecords(10000) // max recrods per GetRecords
                .withCallProcessRecordsEvenForEmptyRecordList(false) // no records -> no precessing
                .withInitialLeaseTableWriteCapacity(10) // Dynamodb lease table capacity
                .withInitialLeaseTableReadCapacity(10)
                .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON);

        new Worker.Builder()
                .recordProcessorFactory(zombieRecordFactory)
                .config(config)
                .build()
                .run();
    }



}
