package org.example.runner;
import org.jcsp.lang.*;

import org.example.producer.TimeoutProducer;
import org.example.consumer.TimeoutConsumer;
import org.example.buffer.TimeoutBuffer;
import org.example.utilities.*;

public class TimeoutRunner {

    public TimeoutRunner(int numProducers, int numConsumers, int numBuffers, int bufferSize) {
        One2OneChannel<PassPayload>[] passBufferChannels = (One2OneChannel<PassPayload>[]) new One2OneChannel[numBuffers];
        for (int i = 0; i < numBuffers; i++) {
            passBufferChannels[i] = Channel.one2one();
        }

        One2OneChannelInt[][] producerChannels = new One2OneChannelInt[numProducers][numBuffers];
        for (int p = 0; p < numProducers; p++) {
            for (int b = 0; b < numBuffers; b++) {
                producerChannels[p][b] = Channel.one2oneInt();
            }
        }

        One2OneChannelInt[][] consumerChannels = new One2OneChannelInt[numConsumers][numBuffers];
        for (int c = 0; c < numConsumers; c++) {
            for (int b = 0; b < numBuffers; b++) {
                consumerChannels[c][b] = Channel.one2oneInt();
            }
        }

        CSProcess[] processes = new CSProcess[numProducers + numConsumers + numBuffers];

        for (int i = 0; i < numProducers; i++) {
            One2OneChannelInt[] channelsToBuffers = new One2OneChannelInt[numBuffers];
            for (int b = 0; b < numBuffers; b++) channelsToBuffers[b] = producerChannels[i][b];
            processes[i] = new TimeoutProducer(channelsToBuffers, i);
        }

        for (int i = 0; i < numConsumers; i++) {
            One2OneChannelInt[] channelsToBuffers = new One2OneChannelInt[numBuffers];
            for (int b = 0; b < numBuffers; b++) channelsToBuffers[b] = consumerChannels[i][b];
            processes[numProducers + i] = new TimeoutConsumer(channelsToBuffers, i);
        }

        Stats[] stats = new Stats[numBuffers];
        for (int i = 0; i < numBuffers; i++) {
            One2OneChannelInt[] producersForBuffer = new One2OneChannelInt[numProducers];
            for (int p = 0; p < numProducers; p++) producersForBuffer[p] = producerChannels[p][i];

            One2OneChannelInt[] consumersForBuffer = new One2OneChannelInt[numConsumers];
            for (int c = 0; c < numConsumers; c++) consumersForBuffer[c] = consumerChannels[c][i];

            One2OneChannel<PassPayload> nextBuffer = passBufferChannels[(i + 1) % numBuffers];
            One2OneChannel<PassPayload> prevBuffer = passBufferChannels[(i - 1 + numBuffers) % numBuffers];
            stats[i] = new Stats(numProducers, numConsumers);
            processes[numProducers + numConsumers + i] = new TimeoutBuffer(producersForBuffer, consumersForBuffer, nextBuffer, prevBuffer, bufferSize, stats[i], i);
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
