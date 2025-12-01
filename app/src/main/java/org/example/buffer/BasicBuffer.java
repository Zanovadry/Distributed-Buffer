package org.example.buffer;
import org.jcsp.lang.*;

import org.example.utilities.Payload;
import org.example.utilities.Stats;

public class BasicBuffer implements CSProcess {
    final private Stats stats;
    final private One2OneChannelInt[] producers;
    final private One2OneChannelInt[] consumers;
    final private One2OneChannelInt nextBuffer;
    final private One2OneChannelInt prevBuffer;
    final private int bufferSize;
    private int current = 0; 

    public BasicBuffer (One2OneChannelInt[] producers, One2OneChannelInt[] consumers, One2OneChannelInt nextBuffer, One2OneChannelInt prevBuffer, int bufferSize, Stats stats) {
        this.stats = stats;
        this.producers = producers;
        this.consumers = consumers;
        this.nextBuffer = nextBuffer;
        this.prevBuffer = prevBuffer;
        this.bufferSize = bufferSize;
    }

    public void run () {
        Guard[] guards = new Guard[producers.length + consumers.length + 2];
        for (int i = 0; i < producers.length; i++) {
            guards[i] = producers[i].in();
        }
        for (int i = 0; i < consumers.length; i++) {
            guards[producers.length + i] = consumers[i].in();
        }
        guards[guards.length - 2] = nextBuffer.in();
        guards[guards.length - 1] = prevBuffer.in();

        Alternative alt = new Alternative(guards);

        while (true) {
            int index = alt.select();
            if (index < producers.length) {
                One2OneChannelInt producer = producers[index];
                if (producer.in().read() == Payload.WHERE.ordinal()) {
                    if (current < bufferSize) {
                        producer.out().write(Payload.HERE.ordinal()); 
                        if (producer.in().read() == Payload.PACKAGE.ordinal()) {
                            current++;
                            stats.recordProduced(index);
                        }                      
                    } else {
                        nextBuffer.out().write(index);
                        stats.recordProducerPass(index); 
                    }
                }
            } else if (index < producers.length + consumers.length) {
                One2OneChannelInt consumer = consumers[index - producers.length];
                if (consumer.in().read() == Payload.WHERE.ordinal()) {
                    if (current > 0) {
                        consumer.out().write(Payload.HERE.ordinal()); 
                        consumer.out().write(Payload.PACKAGE.ordinal());
                        current--;
                        stats.recordConsumed(index - producers.length);                  
                    } else {
                        prevBuffer.out().write(index - producers.length);
                        stats.recordConsumerPass(index - producers.length); 
                    }
                }

            } else if (index == guards.length - 2) {
                int producerIndex = nextBuffer.in().read();
                if (current < bufferSize) {
                    producers[producerIndex].out().write(Payload.HERE.ordinal());
                    if (producers[producerIndex].in().read() == Payload.PACKAGE.ordinal()) {
                        current++;
                        stats.recordProduced(producerIndex);
                    }
                } else {
                    nextBuffer.out().write(producerIndex);
                    stats.recordProducerPass(producerIndex);
                }

            } else {
                int consumerIndex = prevBuffer.in().read();
                if (current > 0) {
                    consumers[consumerIndex].out().write(Payload.HERE.ordinal());
                    consumers[consumerIndex].out().write(Payload.PACKAGE.ordinal());
                    current--;
                    stats.recordConsumed(consumerIndex);
                } else {
                    prevBuffer.out().write(consumerIndex);
                    stats.recordConsumerPass(consumerIndex);
                }
            }
        }
    }

}
