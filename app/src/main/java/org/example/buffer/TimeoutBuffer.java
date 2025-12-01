package org.example.buffer;
import org.jcsp.lang.*;

import org.example.utilities.*;

public class TimeoutBuffer implements CSProcess {
    final private Stats stats;
    final private One2OneChannelInt[] producers;
    final private One2OneChannelInt[] consumers;
    final private One2OneChannel<PassPayload> nextBuffer;
    final private One2OneChannel<PassPayload> prevBuffer;
    final private int bufferSize;
    final private int Id;
    private int current = 0; 

    public TimeoutBuffer (One2OneChannelInt[] producers, One2OneChannelInt[] consumers, One2OneChannel<PassPayload> nextBuffer, One2OneChannel<PassPayload> prevBuffer, int bufferSize, Stats stats, int Id) {
        this.stats = stats;
        this.producers = producers;
        this.consumers = consumers;
        this.nextBuffer = nextBuffer;
        this.prevBuffer = prevBuffer;
        this.bufferSize = bufferSize;
        this.Id = Id;
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
                System.out.println("Buffer " + Id + " selected producer " + index);
                One2OneChannelInt producer = producers[index];
                if (producer.in().read() == Payload.WHERE.ordinal()) {
                    if (current < bufferSize) {
                        producer.out().write(Payload.HERE.ordinal()); 
                        if (producer.in().read() == Payload.PACKAGE.ordinal()) {
                            current++;
                            stats.recordProduced(index);
                            System.out.println("Buffer " + Id + " received PACKAGE from producer " + index);
                        }                      
                    } else {
                        nextBuffer.out().write(new PassPayload(Id, index));
                        stats.recordProducerPass(index); 
                        System.out.println("Buffer " + Id + " passed producer " + index + " to next buffer");
                    }
                }

            } else if (index < producers.length + consumers.length) {
                System.out.println("Buffer " + Id + " selected consumer " + (index - producers.length));
                One2OneChannelInt consumer = consumers[index - producers.length];
                if (consumer.in().read() == Payload.WHERE.ordinal()) {
                    if (current > 0) {
                        consumer.out().write(Payload.HERE.ordinal()); 
                        consumer.out().write(Payload.PACKAGE.ordinal());
                        current--;
                        stats.recordConsumed(index - producers.length);  
                        System.out.println("Buffer " + Id + " sent PACKAGE to consumer " + (index - producers.length));                
                    } else {
                        prevBuffer.out().write(new PassPayload(Id, index - producers.length) );
                        stats.recordConsumerPass(index - producers.length); 
                        System.out.println("Buffer " + Id + " passed consumer " + (index - producers.length) + " to previous buffer");
                    }
                }

            } else if (index == guards.length - 1) {
                System.out.println("Buffer " + Id + " got producer from previous buffer");
                PassPayload passPayload = prevBuffer.in().read();
                int producerIndex = passPayload.pcIndex();
                int ogBufferIndex = passPayload.ogBufferIndex();
                if (current < bufferSize) {
                    System.out.println("Buffer " + Id + " accepting passed producer " + producerIndex);
                    producers[producerIndex].out().write(Payload.HERE.ordinal());
                    if (producers[producerIndex].in().read() == Payload.PACKAGE.ordinal()) {
                        current++;
                        stats.recordProduced(producerIndex);
                    }
                } else if (ogBufferIndex != Id) {
                    System.out.println("Buffer " + Id + " passing producer " + producerIndex + " to next buffer");
                    nextBuffer.out().write(passPayload);
                    stats.recordProducerPass(producerIndex);
                } else {
                    System.out.println("Buffer " + Id + " telling producer " + producerIndex + " to WAIT");
                    producers[producerIndex].out().write(Payload.WAIT.ordinal());
                }

            } else if (index == guards.length - 2) {
                System.out.println("Buffer " + Id + " got consumer from next buffer");
                PassPayload passPayload = nextBuffer.in().read();
                int consumerIndex = passPayload.pcIndex();
                int ogBufferIndex = passPayload.ogBufferIndex();
                if (current > 0) {
                    System.out.println("Buffer " + Id + " accepting passed consumer " + consumerIndex);
                    consumers[consumerIndex].out().write(Payload.HERE.ordinal());
                    consumers[consumerIndex].out().write(Payload.PACKAGE.ordinal());
                    current--;
                    stats.recordConsumed(consumerIndex);
                } else  if (ogBufferIndex != Id) {
                    System.out.println("Buffer " + Id + " passing consumer " + consumerIndex + " to previous buffer");
                    prevBuffer.out().write(passPayload);
                    stats.recordConsumerPass(consumerIndex);
                } else {
                    System.out.println("Buffer " + Id + " telling consumer " + consumerIndex + " to WAIT");
                    consumers[consumerIndex].out().write(Payload.WAIT.ordinal());
                }
            } else {
                System.out.println("Buffer " + Id + " error: invalid index " + index + "selected");
            }
        }
    }

}
