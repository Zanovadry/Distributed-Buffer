package org.example.runner;
import org.jcsp.lang.*;
import org.example.producer.QueueProducer;
import org.example.producer.TimeoutProducer;
import org.example.consumer.QueueConsumer;
import org.example.consumer.TimeoutConsumer;
import org.example.buffer.QueueBuffer;
import org.example.buffer.TimeoutBuffer;
import org.example.utilities.*;

public class QueueRunner {

    public QueueRunner(int numProducers, int numConsumers, int numBuffers, int bufferSize) {
        One2OneChannel<PassPayload>[] passProducerChannels = (One2OneChannel<PassPayload>[]) new One2OneChannel[numBuffers];
        One2OneChannel<PassPayload>[] passConsumerChannels = (One2OneChannel<PassPayload>[]) new One2OneChannel[numBuffers];
        for (int i = 0; i < numBuffers; i++) {
            passProducerChannels[i] = Channel.one2one();
            passConsumerChannels[i] = Channel.one2one();
        }

        One2OneChannelInt[][] producerToBufferChannels = new One2OneChannelInt[numProducers][numBuffers];
        One2OneChannelInt[][] producerFromBufferChannels = new One2OneChannelInt[numProducers][numBuffers];
        for (int p = 0; p < numProducers; p++) {
            for (int b = 0; b < numBuffers; b++) {
                producerToBufferChannels[p][b] = Channel.one2oneInt();
                producerFromBufferChannels[p][b] = Channel.one2oneInt();
            }
        }

        One2OneChannelInt[][] consumerToBufferChannels = new One2OneChannelInt[numConsumers][numBuffers];
        One2OneChannelInt[][] consumerFromBufferChannels = new One2OneChannelInt[numConsumers][numBuffers];
        for (int c = 0; c < numConsumers; c++) {
            for (int b = 0; b < numBuffers; b++) {
                consumerToBufferChannels[c][b] = Channel.one2oneInt();
                consumerFromBufferChannels[c][b] = Channel.one2oneInt();
            }
        }

        CSProcess[] processes = new CSProcess[numProducers + numConsumers + numBuffers];

        for (int i = 0; i < numProducers; i++) {
            One2OneChannelInt[] ptbc = new One2OneChannelInt[numBuffers];
            One2OneChannelInt[] pfbc = new One2OneChannelInt[numBuffers];
            for (int b = 0; b < numBuffers; b++) {
                ptbc[b] = producerToBufferChannels[i][b];
                pfbc[b] = producerFromBufferChannels[i][b];
            
            }
            processes[i] = new QueueProducer(ptbc, pfbc, i);
        }

        for (int i = 0; i < numConsumers; i++) {
            One2OneChannelInt[] ctbc = new One2OneChannelInt[numBuffers];
            One2OneChannelInt[] cfbc = new One2OneChannelInt[numBuffers];
            for (int b = 0; b < numBuffers; b++) {
                ctbc[b] = consumerToBufferChannels[i][b];
                cfbc[b] = consumerFromBufferChannels[i][b];
            
            }
            processes[i + numProducers] = new QueueConsumer(ctbc, cfbc, i);
        }
        Stats[] stats = new Stats[numBuffers];
        for (int i = 0; i < numBuffers; i++) {
            One2OneChannelInt[] fromProducerChannels = new One2OneChannelInt[numProducers];
            One2OneChannelInt[] toProducerChannels = new One2OneChannelInt[numProducers];
            
            for (int p = 0; p < numProducers; p++) {
                fromProducerChannels[p] = producerToBufferChannels[p][i];
                toProducerChannels[p] = producerFromBufferChannels[p][i];
            }

            One2OneChannelInt[] fromConsumerChannels = new One2OneChannelInt[numConsumers];
            One2OneChannelInt[] toConsumerChannels = new One2OneChannelInt[numConsumers];
            for (int c = 0; c < numConsumers; c++) {
                fromConsumerChannels[c] = consumerToBufferChannels[c][i];
                toConsumerChannels[c] = consumerFromBufferChannels[c][i];
            }
            
            One2OneChannel<PassPayload> passProducerChannel = passProducerChannels[i];
            One2OneChannel<PassPayload> receivePassedProducerChannel = passProducerChannels[(i - 1 + numBuffers) % numBuffers];
            One2OneChannel<PassPayload> passConsumerChannel = passConsumerChannels[i];
            One2OneChannel<PassPayload> receivePassedConsumerChannel = passConsumerChannels[(i + 1) % numBuffers];

            stats[i] = new Stats(numProducers, numConsumers);
            processes[numProducers + numConsumers + i] = new QueueBuffer(fromProducerChannels,  toProducerChannels, fromConsumerChannels, toConsumerChannels, passProducerChannel, receivePassedProducerChannel, passConsumerChannel, receivePassedConsumerChannel, bufferSize, stats[i], i);
        }

        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                for (int i = 0; i < numBuffers; i++) {
                    System.out.println("Buffer " + i + " Stats:");
                    System.out.println(stats[i]);
                }
            }
        });

        thread.start();

        Parallel par = new Parallel(processes);
        par.run();

    }

}
