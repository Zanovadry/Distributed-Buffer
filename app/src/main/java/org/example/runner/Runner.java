package org.example.runner;
import org.jcsp.lang.*;

import org.example.producer.BasicProducer;
import org.example.consumer.BasicConsumer;
import org.example.buffer.BasicBuffer;
import org.example.utilities.Stats;

public class Runner {

    public Runner(int numProducers, int numConsumers, int numBuffers, int bufferSize) {
        One2OneChannelInt[] ringChannels = new One2OneChannelInt[numBuffers];
        for (int i = 0; i < numBuffers; i++) {
            ringChannels[i] = Channel.one2oneInt();
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
            processes[i] = new BasicProducer(channelsToBuffers);
        }

        for (int i = 0; i < numConsumers; i++) {
            One2OneChannelInt[] channelsToBuffers = new One2OneChannelInt[numBuffers];
            for (int b = 0; b < numBuffers; b++) channelsToBuffers[b] = consumerChannels[i][b];
            processes[numProducers + i] = new BasicConsumer(channelsToBuffers);
        }

        Stats[] stats = new Stats[numBuffers];
        for (int i = 0; i < numBuffers; i++) {
            One2OneChannelInt[] producersForBuffer = new One2OneChannelInt[numProducers];
            for (int p = 0; p < numProducers; p++) producersForBuffer[p] = producerChannels[p][i];

            One2OneChannelInt[] consumersForBuffer = new One2OneChannelInt[numConsumers];
            for (int c = 0; c < numConsumers; c++) consumersForBuffer[c] = consumerChannels[c][i];

            One2OneChannelInt nextBuffer = ringChannels[(i + 1) % numBuffers];
            One2OneChannelInt prevBuffer = ringChannels[(i - 1 + numBuffers) % numBuffers];
            stats[i] = new Stats(numProducers, numConsumers);
            processes[numProducers + numConsumers + i] = new BasicBuffer(producersForBuffer, consumersForBuffer, nextBuffer, prevBuffer, bufferSize, stats[i]);
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
