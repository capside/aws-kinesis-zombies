/*
    Derived from: https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer-sample/src/com/amazonaws/services/kinesis/producer/sample/SampleProducer.java
 */
package com.capside.realtimedemo.producer;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.producer.Attempt;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.UserRecordFailedException;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.capside.realtimedemo.geo.CoordinateLatLon;
import com.capside.realtimedemo.geo.CoordinateUTM;
import com.capside.realtimedemo.geo.Datum;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.String.format;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import static java.lang.String.format;
import static java.lang.String.format;
import static java.lang.String.format;

/**
 * Aditional documentation: https://blogs.aws.amazon.com/bigdata/post/Tx3ET30EGDKUUI2/Implementing-Efficient-and-Reliable-Producers-with-the-Amazon-Kinesis-Producer-L
 * 
 * @author ciberado
 */
@Component
@Slf4j
public class Drone implements CommandLineRunner {

    private static final int SECONDS_TO_RUN = 60 * 60;

    public static final int NUMBER_OF_ZOMBIES = 1000;
    public static final double ZOMBIE_SPEED = 1;
    public static final double RADIOUS = 10 * 1000;

    private final ObjectMapper mapper;

    private final String streamName;

    private final  String region;

    private final  int id;
    private final double latitude;
    private final double longitude;
    private final List<Zombie> zombies = new ArrayList<>();
    
    private final RecordSentCallback recordSentCallback;

    /**
     * The sequence number of the next record.
     */
    private final AtomicLong currentRecordNumber;
    /**
     * How many records have been confirmed to be successfully sent.
     */
    private final AtomicLong recordsCompleted;

    private KinesisProducer producer;

    @Autowired
    public Drone(@Value("${drone}") int droneId,
            @Value("${stream}") String streamName,
            @Value("${region}") String region,
            @Value("${latitude}") double latitude,
            @Value("${longitude}") double longitude) {
        // --drone=7777 --stream=zombies --region=us-west-2 --latitude=41.3902 --longitude=2.15400
        // --drone=5555 --stream=zombies --region=us-west-2 --latitude=40.415363 --longitude=-3.707398
        this.mapper = new ObjectMapper();
        this.id = droneId;
        this.streamName = streamName;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
        this.currentRecordNumber = new AtomicLong(0);
        this.recordsCompleted = new AtomicLong(0);
        this.recordSentCallback  = new RecordSentCallback();
    }

    @SneakyThrows
    @Override
    public void run(String... args) {
        initZombies(latitude, longitude, RADIOUS);
        createKinesisProducer();
        while (true) {
            long t0 = System.currentTimeMillis();
            for (Zombie zombie : zombies) {
                zombie.move();
            }
            while (producer.getOutstandingRecordsCount() > NUMBER_OF_ZOMBIES * 3) {
                log.warn(format("Kinesis KPL is under pressure (count=%s). Waiting 1 second.", 
                        producer.getOutstandingRecordsCount()));
                Thread.sleep(1000);
            }
            sendZombiesToKinesis();
            long tf = System.currentTimeMillis();
            log.info("Main loop time: " + (tf-t0));
            if (tf - t0 < 1000) {
                Thread.sleep(1000 - (tf -t0));
            }
        }
    }

    /**
     *
     * @return KinesisProducer instance used to put records.
     */
    public KinesisProducer createKinesisProducer() {
        KinesisProducerConfiguration config = new KinesisProducerConfiguration();

        config.setRegion(region);
        config.setCredentialsProvider(new DefaultAWSCredentialsProviderChain());
        config.setMaxConnections(4);
        config.setRequestTimeout(60000);
        config.setRecordMaxBufferedTime(15000);
        producer = new KinesisProducer(config);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                log.info("Flushing remaining records.");
                producer.flushSync();
                log.info("All records flushed.");
                producer.destroy();
                log.info("Producer finished.");
            }
        }) {
        });

        return producer;
    }


    private void initZombies(double latitude, double longitude, double radious) {
        for (int i=0; i < NUMBER_OF_ZOMBIES; i++) {
            CoordinateUTM utm = Datum.WGS84.latLonToUTM(latitude, longitude, -1);
            double angle = 2 * PI * Math.random();
            double dx = radious * cos(angle);
            double dy = radious * sin(angle);
            utm.translate(dx, dy);
            Zombie zombie = new Zombie(id + "-" + i, utm, ZOMBIE_SPEED * 0.8 + ZOMBIE_SPEED * 0.2);
            zombies.add(zombie);
        }
    }
    
    public void sendZombiesToKinesis() {
        for (Zombie zombie : zombies) {
            putNewRecord(zombie);
        }
    }

    // The callback that will confirm or not that a new record has been sent
    class RecordSentCallback implements FutureCallback<UserRecordResult> {

        @Override
        public void onFailure(Throwable t) {
            if (t instanceof UserRecordFailedException ){
                Attempt last = Iterables.getLast(
                        ((UserRecordFailedException) t).getResult().getAttempts());
                log.error(format(
                        "Record failed to put - %s : %s",
                        last.getErrorCode(), last.getErrorMessage()));
            }
            log.error("Exception during put", t);
        }

        @Override
        public void onSuccess(UserRecordResult result) {
            recordsCompleted.getAndIncrement();
            if (recordsCompleted.get() % NUMBER_OF_ZOMBIES == 0) {
                log.info(format("Records completed: %s; Shard: %s; SequenceNumber: %s.",
                             recordsCompleted.get(), result.getShardId(), result.getSequenceNumber()));
                
            }
        }
    };

    @SneakyThrows
    public void putNewRecord(Zombie zombie) {        
        CoordinateUTM utm = zombie.getCurrentPosition();
        CoordinateLatLon latLon = Datum.WGS84.utmToLatLon(utm);
        ZombieLecture lect = new ZombieLecture(id, zombie.getId(), new Date(), latLon.getLat(), latLon.getLon());
        utm.setAccuracy(RADIOUS);
        String partitionKey = utm.getShortForm();
        String json = mapper.writeValueAsString(lect);
        ByteBuffer data = ByteBuffer.wrap(json.getBytes("UTF-8"));
        ListenableFuture<UserRecordResult> f
                = producer.addUserRecord(streamName, partitionKey, data);
        Futures.addCallback(f, this.recordSentCallback);
    }



}
