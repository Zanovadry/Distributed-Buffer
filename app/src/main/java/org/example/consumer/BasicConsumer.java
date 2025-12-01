package org.example.consumer;
import org.jcsp.lang.*;
import java.util.Random;

import org.example.utilities.Payload;

public class BasicConsumer implements CSProcess {
    final private One2OneChannelInt[] buffers;

    public BasicConsumer (final One2OneChannelInt[] buffers) {
        this.buffers = buffers;
    }

    public void run () {
        Guard[] guards = new Guard[buffers.length];
        for (int i = 0; i < buffers.length; i++) {
            guards[i] = buffers[i].in();
        }
        
        Alternative alt = new Alternative(guards);
        Random rand = new Random();

        while (true) {
            System.out.println("Consumer querying buffer...");
            One2OneChannelInt queryBuffer = buffers[rand.nextInt(buffers.length)];
            queryBuffer.out().write(Payload.WHERE.ordinal());
            System.out.println("Consumer sent WHERE to buffer.");

            One2OneChannelInt readBufferIndex = buffers[alt.select()];

            if (readBufferIndex.in().read() == Payload.HERE.ordinal()) {
                System.out.println("Consumer got HERE .");
                if (readBufferIndex.in().read() == Payload.PACKAGE.ordinal()) {
                    System.out.println("Consumer received PACKAGE .");
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
