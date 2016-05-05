/*
    Derived from: https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer-sample/src/com/amazonaws/services/kinesis/producer/sample/SampleProducer.java
 */
package com.capside.realtimedemo.producer;

import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.producer.Attempt;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.UserRecordFailedException;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.capside.realtimedemo.geo.CoordinateUTM;
import com.capside.realtimedemo.geo.Datum;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Date;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Producer implements CommandLineRunner {

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);

    public ObjectMapper mapper;

    private static final int SECONDS_TO_RUN = 60 * 60;

    public static final int MAX_ZOMBIES = 3;

    private static final int RECORDS_PER_SECOND = 300;

    public String streamName;

    public String region;

    public int droneId;

    private double lastLat;
    private double lastLon;
    private double radious;

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
    public Producer(@Value("${drone}") int droneId,
            @Value("${stream}") String streamName,
            @Value("${region}") String region,
            @Value("${latitude}") double latitude,
            @Value("${longitude}") double longitude) {
        // --drone=7777 --stream=zombies --region=us-west-2 --latitude=41.3902 --longitude=2.15400
        // --drone=5555 --stream=zombies --region=us-west-2 --latitude=40.415363 --longitude=-3.707398
        this.mapper = new ObjectMapper();
        this.droneId = droneId;
        this.streamName = streamName;
        this.region = region;
        this.lastLat = latitude;
        this.lastLon = longitude;
        this.radious = 0.025;

        this.currentRecordNumber = new AtomicLong(0);
        this.recordsCompleted = new AtomicLong(0);
    }

    @SneakyThrows
    @Override
    public void run(String... args) {
        createKinesisProducer();

        // Each second we will show the progress of the job
        EXECUTOR.scheduleAtFixedRate(new ProgressTask(), 1, 1, TimeUnit.SECONDS);

        // Kick off the puts
        log.info(String.format(
                "Starting puts... will run for %d seconds at %d records per second",
                SECONDS_TO_RUN, RECORDS_PER_SECOND));
        executeAtTargetRate(EXECUTOR, new PutNewRecordTask(), currentRecordNumber, SECONDS_TO_RUN, RECORDS_PER_SECOND);

        EXECUTOR.awaitTermination(SECONDS_TO_RUN + 1, TimeUnit.SECONDS);
        producer.flushSync();
        log.info("All records complete.");

        // This kills the child process and shuts down the threads managing it.
        producer.destroy();
        log.info("Finished.");
    }

    /**
     *
     * @return KinesisProducer instance used to put records.
     */
    public KinesisProducer createKinesisProducer() {
        KinesisProducerConfiguration config = new KinesisProducerConfiguration();

        config.setRegion(region);
        config.setCredentialsProvider(new DefaultAWSCredentialsProviderChain());
        config.setMaxConnections(1);
        config.setRequestTimeout(60000);
        config.setRecordMaxBufferedTime(15000);
        producer = new KinesisProducer(config);

        return producer;
    }

    class ProgressTask implements Runnable {
        @Override
        public void run() {
            long recordNumber = currentRecordNumber.get();
            long total = RECORDS_PER_SECOND * SECONDS_TO_RUN;
            double putPercent = 100.0 * recordNumber / total;
            long done = recordsCompleted.get();
            double donePercent = 100.0 * done / total;
            log.info(String.format(
                    "Put %d of %d so far (%.2f %%), %d have completed (%.2f %%)",
                    recordNumber, total, putPercent, done, donePercent));
        }
    }

    // The callback that will confirm or not that a new record has been sent
    class RecordSentCallback implements FutureCallback<UserRecordResult> {

        @Override
        public void onFailure(Throwable t) {
            if (t instanceof UserRecordFailedException) {
                Attempt last = Iterables.getLast(
                        ((UserRecordFailedException) t).getResult().getAttempts());
                log.error(String.format(
                        "Record failed to put - %s : %s",
                        last.getErrorCode(), last.getErrorMessage()));
            }
            log.error("Exception during put", t);
        }

        @Override
        public void onSuccess(UserRecordResult result) {
            recordsCompleted.getAndIncrement();
        }
    };

    /**
     * The task that will send the record, in a thread parallel to the rest of
     * the app.
     */
    private class PutNewRecordTask implements Runnable {

        @Override
        @SneakyThrows
        public void run() {
            ZombieLecture lect = generateNextZombieLecture();
            CoordinateUTM utm
                    = Datum.WGS84.latLonToUTM(lect.getLatitude(), lect.getLongitude(), 0);
            utm.setAccuracy(1000);
            String partitionKey = utm.getShortForm();
            String json = mapper.writeValueAsString(lect);
            ByteBuffer data = ByteBuffer.wrap(json.getBytes("UTF-8"));
            ListenableFuture<UserRecordResult> f
                    = producer.addUserRecord(streamName, partitionKey, data);
            Futures.addCallback(f, new RecordSentCallback());
        }
    };

    private ZombieLecture generateNextZombieLecture() {
        ZombieLecture lect = new ZombieLecture();
        lect.setDroneId(droneId);
        lect.setTimestamp(new Date());
        lect.setZombies((int) (Math.random() * MAX_ZOMBIES) + 1);

        lastLat = lastLat + (-radious / 2 + Math.random() * radious);
        lect.setLatitude(lastLat);
        lastLon = lastLon + (-radious / 2 + Math.random() * radious);
        lect.setLongitude(lastLon);

        return lect;
    }

    /**
     * Executes a function N times per second for M seconds with a
     * ScheduledExecutorService. The executor is shutdown at the end. This is
     * more precise than simply using scheduleAtFixedRate.
     *
     * @param exec Executor
     * @param task Task to perform
     * @param counter Counter used to track how many times the task has been
     * executed
     * @param durationSeconds How many seconds to run for
     * @param ratePerSecond How many times to execute task per second
     */
    private void executeAtTargetRate(
            final ScheduledExecutorService exec, final PutNewRecordTask task,
            final AtomicLong counter, final int durationSeconds, final int ratePerSecond) {
        exec.scheduleWithFixedDelay(new Runnable() {
            final long startTime = System.nanoTime();

            @Override
            public void run() {
                double secondsRun = (System.nanoTime() - startTime) / 1e9;
                double targetCount = Math.min(durationSeconds, secondsRun) * ratePerSecond;

                while (counter.get() < targetCount) {
                    counter.getAndIncrement();
                    try {
                        task.run();
                    } catch (Exception e) {
                        log.error("Error running task", e);
                    }
                }

                if (secondsRun >= durationSeconds) {
                    exec.shutdown();
                }
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
    }

}
