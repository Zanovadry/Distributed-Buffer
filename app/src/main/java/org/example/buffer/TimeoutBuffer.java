package org.example.buffer;
import org.jcsp.lang.*;

import org.example.utilities.*;

public class TimeoutBuffer implements CSProcess {
    final private Stats stats;
    final private One2OneChannelInt[] fromProducerChannels;
    final private One2OneChannelInt[] toProducerChannels;
    final private One2OneChannelInt[] fromConsumerChannels;
    final private One2OneChannelInt[] toConsumerChannels;
    final private One2OneChannel<PassPayload> nextBuffer;
    final private One2OneChannel<PassPayload> prevBuffer;
    final private int bufferSize;
    final private int Id;
    private int current = 0; 

    public TimeoutBuffer (One2OneChannelInt[] fromProducerChannels, One2OneChannelInt[] toProducerChannels,
                          One2OneChannelInt[] fromConsumerChannels, One2OneChannelInt[] toConsumerChannels, 
                          One2OneChannel<PassPayload> nextBuffer, One2OneChannel<PassPayload> prevBuffer, 
                          int bufferSize, Stats stats, int Id) {
        this.stats = stats;
        this.fromProducerChannels = fromProducerChannels;
        this.toProducerChannels = toProducerChannels;
        this.fromConsumerChannels = fromConsumerChannels;
        this.toConsumerChannels = toConsumerChannels;
        this.nextBuffer = nextBuffer;
        this.prevBuffer = prevBuffer;
        this.bufferSize = bufferSize;
        this.Id = Id;
    }

    public void run () {
        Guard[] guards = new Guard[fromProducerChannels.length + fromConsumerChannels.length + 2];
        for (int i = 0; i < fromProducerChannels.length; i++) {
            guards[i] = fromProducerChannels[i].in();
        }
        for (int i = 0; i < fromConsumerChannels.length; i++) {
            guards[fromProducerChannels.length + i] = fromConsumerChannels[i].in();
        }
        guards[guards.length - 2] = nextBuffer.in();
        guards[guards.length - 1] = prevBuffer.in();

        Alternative alt = new Alternative(guards);

        while (true) {
            int index = alt.select();
            if (index < fromProducerChannels.length) {
                int producerIndex = index;
                System.out.println("Buffer " + Id + " selected producer " + producerIndex);
                if (fromProducerChannels[producerIndex].in().read() == Payload.WHERE.ordinal()) {
                    if (current < bufferSize) {
                        toProducerChannels[producerIndex].out().write(Payload.HERE.ordinal()); 
                        if (fromProducerChannels[producerIndex].in().read() == Payload.PACKAGE.ordinal()) {
                            current++;
                            stats.recordProduced(producerIndex);
                            System.out.println("Buffer " + Id + " received PACKAGE from producer " + producerIndex);
                        }                      
                    } else {
                        nextBuffer.out().write(new PassPayload(Id, producerIndex));
                        stats.recordProducerPass(producerIndex); 
                        System.out.println("Buffer " + Id + " passed producer " + producerIndex + " to next buffer");
                    }
                }

            } else if (index < fromProducerChannels.length + fromConsumerChannels.length) {
                int consumerIndex = index - fromProducerChannels.length;
                System.out.println("Buffer " + Id + " selected consumer " + consumerIndex);
                if (fromConsumerChannels[consumerIndex].in().read() == Payload.WHERE.ordinal()) {
                    if (current > 0) {
                        toConsumerChannels[consumerIndex].out().write(Payload.HERE.ordinal()); 
                        toConsumerChannels[consumerIndex].out().write(Payload.PACKAGE.ordinal());
                        current--;
                        stats.recordConsumed(consumerIndex);  
                        System.out.println("Buffer " + Id + " sent PACKAGE to consumer " + consumerIndex);                
                    } else {
                        prevBuffer.out().write(new PassPayload(Id, consumerIndex));
                        stats.recordConsumerPass(consumerIndex); 
                        System.out.println("Buffer " + Id + " passed consumer " + consumerIndex + " to previous buffer");
                    }
                }

            } else if (index == guards.length - 1) {
                System.out.println("Buffer " + Id + " got producer from previous buffer");
                PassPayload passPayload = prevBuffer.in().read();
                int producerIndex = passPayload.pcIndex();
                int ogBufferIndex = passPayload.ogBufferIndex();
                if (current < bufferSize) {
                    System.out.println("Buffer " + Id + " accepting passed producer " + producerIndex);
                    toProducerChannels[producerIndex].out().write(Payload.HERE.ordinal());
                    if (fromProducerChannels[producerIndex].in().read() == Payload.PACKAGE.ordinal()) {
                        current++;
                        stats.recordProduced(producerIndex);
                    }
                } else if (ogBufferIndex != Id) {
                    System.out.println("Buffer " + Id + " passing producer " + producerIndex + " to next buffer");
                    nextBuffer.out().write(passPayload);
                    stats.recordProducerPass(producerIndex);
                } else {
                    System.out.println("Buffer " + Id + " telling producer " + producerIndex + " to WAIT");
                    toProducerChannels[producerIndex].out().write(Payload.WAIT.ordinal());
                }

            } else if (index == guards.length - 2) {
                System.out.println("Buffer " + Id + " got consumer from next buffer");
                PassPayload passPayload = nextBuffer.in().read();
                int consumerIndex = passPayload.pcIndex();
                int ogBufferIndex = passPayload.ogBufferIndex();
                if (current > 0) {
                    System.out.println("Buffer " + Id + " accepting passed consumer " + consumerIndex);
                    toConsumerChannels[consumerIndex].out().write(Payload.HERE.ordinal());
                    toConsumerChannels[consumerIndex].out().write(Payload.PACKAGE.ordinal());
                    current--;
                    stats.recordConsumed(consumerIndex);
                } else  if (ogBufferIndex != Id) {
                    System.out.println("Buffer " + Id + " passing consumer " + consumerIndex + " to previous buffer");
                    prevBuffer.out().write(passPayload);
                    stats.recordConsumerPass(consumerIndex);
                } else {
                    System.out.println("Buffer " + Id + " telling consumer " + consumerIndex + " to WAIT");
                    toConsumerChannels[consumerIndex].out().write(Payload.WAIT.ordinal());
                }
            } else {
                System.out.println("Buffer " + Id + " error: invalid index " + index + "selected");
            }
        }
    }

}
