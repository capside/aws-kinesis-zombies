package com.capside.realtimedemo.consumer;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import java.lang.management.ManagementFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/* More information: https://blogs.aws.amazon.com/bigdata/post/TxFCI3UJJJYEXJ/Process-Large-DynamoDB-Streams-Using-Multiple-Amazon-Kinesis-Client-Library-KCL
 */
@Component
@Slf4j
public class Consumer {

    private final String streamName;

    private final String region;

    private final AWSCredentialsProvider awsCreds;
    
    private final IRecordProcessorFactory zombieRecordFactory;

    @Autowired
    public Consumer(@Value("${stream}") String streamName,
            @Value("${region}") String region,
            IRecordProcessorFactory zombieRecordFactory, 
            @Value("${accesskey}") String accessKey,
            @Value("${secretKey}") String secretKey) {
        this.streamName = streamName;
        this.region = region;
        this.zombieRecordFactory = zombieRecordFactory;
        this.awsCreds = accessKey != null ? 
                        new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)) :
                        new DefaultAWSCredentialsProviderChain();
        this.initKinesis();
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
                        awsCreds,
                        "ZombieConsumer_" + pid /* worker id*/)
                .withRegionName(region)
                .withFailoverTimeMillis(1000 * 30) // after 30 seconds this worker is considered ko                        
                .withMaxLeasesForWorker(2) // forced to read only 1 shard for demo reasons.
                .withMaxRecords(500) // max records per GetRecords
                .withCallProcessRecordsEvenForEmptyRecordList(false) // no records -> no processing
                .withInitialLeaseTableWriteCapacity(10) // Dynamodb lease table capacity
                .withInitialLeaseTableReadCapacity(10)
                .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON);

        final Worker worker
                = new Worker.Builder()
                .recordProcessorFactory(zombieRecordFactory)
                .config(config)
                .build();

        new Thread() {
            @Override
            public void run() {
                worker.run();
            }
        }.start();
    }

}
