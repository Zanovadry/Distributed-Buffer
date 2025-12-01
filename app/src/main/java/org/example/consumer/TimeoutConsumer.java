package org.example.consumer;
import org.jcsp.lang.*;
import java.util.Random;

import org.example.utilities.Payload;

public class TimeoutConsumer implements CSProcess {
    final private One2OneChannelInt[] buffers;
    final private int Id;

    public TimeoutConsumer (One2OneChannelInt[] buffers, int Id) {
        this.buffers = buffers;
        this.Id = Id;
    }

    public void run () {
        Guard[] guards = new Guard[buffers.length];
        for (int i = 0; i < buffers.length; i++) {
            guards[i] = buffers[i].in();
        }
        
        Alternative alt = new Alternative(guards);
        Random rand = new Random();

        while (true) {
            int queryBufferIndex = rand.nextInt(buffers.length);
            System.out.println("Consumer " + Id + " querying buffer " + queryBufferIndex + "...");
            One2OneChannelInt queryBuffer = buffers[queryBufferIndex];
            queryBuffer.out().write(Payload.WHERE.ordinal());
            System.out.println("Consumer " + Id + " sent WHERE to buffer " + queryBufferIndex + ".");

            int selectedBufferIndex = alt.select();

            One2OneChannelInt readBufferIndex = buffers[selectedBufferIndex];
            int response = readBufferIndex.in().read();

            System.out.println("Consumer " + Id + " received response from buffer " + selectedBufferIndex + ".");

            while (response == Payload.WAIT.ordinal()) {
                System.out.println("Consumer " + Id + " got WAIT.");
                try {
                    Thread.sleep(100 + rand.nextInt(400));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                queryBufferIndex = rand.nextInt(buffers.length);
                System.out.println("Consumer " + Id + " querying buffer " + queryBufferIndex + " after WAIT...");
                queryBuffer = buffers[queryBufferIndex];
                queryBuffer.out().write(Payload.WHERE.ordinal());
                System.out.println("Consumer " + Id + " sent WHERE to buffer " + queryBufferIndex + ".");
                
                readBufferIndex = buffers[alt.select()];
                response = readBufferIndex.in().read();
            }

            
            if (response == Payload.HERE.ordinal()) {
                System.out.println("Consumer " + Id + " got HERE.");
                if (readBufferIndex.in().read() == Payload.PACKAGE.ordinal()) {
                    System.out.println("Consumer " + Id + " received PACKAGE.");
                    try {
                        Thread.sleep(100 + rand.nextInt(400));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }        
    }
}
