package org.example.buffer;
import org.jcsp.lang.*;

import java.util.Deque;
import java.util.ArrayDeque;

import org.example.utilities.*;

public class QueueBuffer implements CSProcess {
    final private Stats stats;
    final private One2OneChannelInt[] fromProducerChannels;
    final private One2OneChannelInt[] toProducerChannels;
    final private One2OneChannelInt[] fromConsumerChannels;
    final private One2OneChannelInt[] toConsumerChannels;
    final private One2OneChannel<PassPayload> passProducerChannel;
    final private One2OneChannel<PassPayload> receivePassedProducerChannel;
    final private One2OneChannel<PassPayload> passConsumerChannel;
    final private One2OneChannel<PassPayload> receivePassedConsumerChannel;
    final private int bufferSize;
    final private int Id;
    private int current = 0; 

    public QueueBuffer (One2OneChannelInt[] fromProducerChannels, One2OneChannelInt[] toProducerChannels,
                          One2OneChannelInt[] fromConsumerChannels, One2OneChannelInt[] toConsumerChannels, 
                          One2OneChannel<PassPayload> passProducerChannel, One2OneChannel<PassPayload> receivePassedProducerChannel,
                          One2OneChannel<PassPayload> passConsumerChannel, One2OneChannel<PassPayload> receivePassedConsumerChannel, 
                          int bufferSize, Stats stats, int Id) {
        this.stats = stats;
        this.fromProducerChannels = fromProducerChannels;
        this.toProducerChannels = toProducerChannels;
        this.fromConsumerChannels = fromConsumerChannels;
        this.toConsumerChannels = toConsumerChannels;
        this.passProducerChannel = passProducerChannel;
        this.receivePassedProducerChannel = receivePassedProducerChannel;
        this.passConsumerChannel = passConsumerChannel;
        this.receivePassedConsumerChannel = receivePassedConsumerChannel;
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
        guards[guards.length - 2] = receivePassedProducerChannel.in();
        guards[guards.length - 1] = receivePassedConsumerChannel.in();

        Alternative alt = new Alternative(guards);

        Deque<Integer> producerQueue = new ArrayDeque<>();
        Deque<Integer> consumerQueue = new ArrayDeque<>();

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
                            if (!consumerQueue.isEmpty()) {
                                int queuedConsumerIndex = consumerQueue.removeFirst();
                                System.out.println("Buffer " + Id + " servicing queued consumer " + queuedConsumerIndex);
                                toConsumerChannels[queuedConsumerIndex].out().write(Payload.HERE.ordinal());
                                toConsumerChannels[queuedConsumerIndex].out().write(Payload.PACKAGE.ordinal());
                                current--;
                                stats.recordConsumed(queuedConsumerIndex);
                                System.out.println("Buffer " + Id + " sent PACKAGE to consumer " + queuedConsumerIndex);
                            }
                        }                      
                    } else {
                        passProducerChannel.out().write(new PassPayload(Id, producerIndex));
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
                        if (!producerQueue.isEmpty()) {
                            int queuedProducerIndex = producerQueue.removeFirst();
                            System.out.println("Buffer " + Id + " servicing queued producer " + queuedProducerIndex);
                            toProducerChannels[queuedProducerIndex].out().write(Payload.HERE.ordinal());
                            if (fromProducerChannels[queuedProducerIndex].in().read() == Payload.PACKAGE.ordinal()) {
                                current++;
                                stats.recordProduced(queuedProducerIndex);
                                System.out.println("Buffer " + Id + " received PACKAGE from producer " + queuedProducerIndex);
                            }
                        }          
                    } else {
                        passConsumerChannel.out().write(new PassPayload(Id, consumerIndex));
                        stats.recordConsumerPass(consumerIndex); 
                        System.out.println("Buffer " + Id + " passed consumer " + consumerIndex + " to previous buffer");
                    }
                }

            } else if (index == guards.length - 2) {
                System.out.println("Buffer " + Id + " got producer from previous buffer");
                PassPayload passPayload = receivePassedProducerChannel.in().read();
                int producerIndex = passPayload.pcIndex();
                int ogBufferIndex = passPayload.ogBufferIndex();
                if (current < bufferSize) {
                    System.out.println("Buffer " + Id + " accepting passed producer " + producerIndex);
                    toProducerChannels[producerIndex].out().write(Payload.HERE.ordinal());
                    if (fromProducerChannels[producerIndex].in().read() == Payload.PACKAGE.ordinal()) {
                        current++;
                        stats.recordProduced(producerIndex);
                        if (!consumerQueue.isEmpty()) {
                            int queuedConsumerIndex = consumerQueue.removeFirst();
                            System.out.println("Buffer " + Id + " servicing queued consumer " + queuedConsumerIndex);
                            toConsumerChannels[queuedConsumerIndex].out().write(Payload.HERE.ordinal());
                            toConsumerChannels[queuedConsumerIndex].out().write(Payload.PACKAGE.ordinal());
                            current--;
                            stats.recordConsumed(queuedConsumerIndex);
                            System.out.println("Buffer " + Id + " sent PACKAGE to consumer " + queuedConsumerIndex);
                        }
                    }
                } else if (ogBufferIndex != Id) {
                    System.out.println("Buffer " + Id + " passing producer " + producerIndex + " to next buffer");
                    passProducerChannel.out().write(passPayload);
                    stats.recordProducerPass(producerIndex);
                } else {
                    System.out.println("Buffer " + Id + " adding producer " + producerIndex + " to queue");
                    producerQueue.addLast(producerIndex);
                }

            } else if (index == guards.length - 1) {
                System.out.println("Buffer " + Id + " got consumer from next buffer");
                PassPayload passPayload = receivePassedConsumerChannel.in().read();
                int consumerIndex = passPayload.pcIndex();
                int ogBufferIndex = passPayload.ogBufferIndex();
                if (current > 0) {
                    System.out.println("Buffer " + Id + " accepting passed consumer " + consumerIndex);
                    toConsumerChannels[consumerIndex].out().write(Payload.HERE.ordinal());
                    toConsumerChannels[consumerIndex].out().write(Payload.PACKAGE.ordinal());
                    current--;
                    stats.recordConsumed(consumerIndex);
                    if (!producerQueue.isEmpty()) {
                        int queuedProducerIndex = producerQueue.removeFirst();
                        System.out.println("Buffer " + Id + " servicing queued producer " + queuedProducerIndex);
                        toProducerChannels[queuedProducerIndex].out().write(Payload.HERE.ordinal());
                        if (fromProducerChannels[queuedProducerIndex].in().read() == Payload.PACKAGE.ordinal()) {
                            current++;
                            stats.recordProduced(queuedProducerIndex);
                            System.out.println("Buffer " + Id + " received PACKAGE from producer " + queuedProducerIndex);
                        }
                    }
                } else  if (ogBufferIndex != Id) {
                    System.out.println("Buffer " + Id + " passing consumer " + consumerIndex + " to previous buffer");
                    passConsumerChannel.out().write(passPayload);
                    stats.recordConsumerPass(consumerIndex);
                } else {
                    System.out.println("Buffer " + Id + " adding consumer " + consumerIndex + " to queue");
                    consumerQueue.addLast(consumerIndex);
                }
            } else {
                System.out.println("Buffer " + Id + " error: invalid index " + index + "selected");
            }
        }
    }

}
