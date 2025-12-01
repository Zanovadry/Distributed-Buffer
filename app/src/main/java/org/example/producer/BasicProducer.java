package org.example.producer;
import org.jcsp.lang.*;
import java.util.Random;

import org.example.utilities.Payload;

public class BasicProducer implements CSProcess {
    final private One2OneChannelInt[] buffers;

    public BasicProducer (final One2OneChannelInt[] buffers) {
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
            System.out.println("Producer querying buffer...");
            One2OneChannelInt queryBuffer = buffers[rand.nextInt(buffers.length)];
            queryBuffer.out().write(Payload.WHERE.ordinal());
            System.out.println("Producer sent WHERE to buffer.");

            One2OneChannelInt sendBufferIndex = buffers[alt.select()];

            if (sendBufferIndex.in().read() == Payload.HERE.ordinal()) {
                System.out.println("Producer got HERE .");
                sendBufferIndex.out().write(Payload.PACKAGE.ordinal());
                System.out.println("Producer sent PACKAGE .");
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
