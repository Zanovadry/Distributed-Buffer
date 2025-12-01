package org.example.producer;
import org.jcsp.lang.*;
import java.util.Random;

import org.example.utilities.Payload;

public class TimeoutProducer implements CSProcess {
    final private One2OneChannelInt[] toBufferChannels;
    final private One2OneChannelInt[] fromBufferChannels;
    final private int Id;

    public TimeoutProducer (One2OneChannelInt[] toBufferChannels, One2OneChannelInt[] fromBufferChannels, int Id) {
        this.toBufferChannels = toBufferChannels;
        this.fromBufferChannels = fromBufferChannels;
        this.Id = Id;
    }

    public void run () {
        Guard[] guards = new Guard[fromBufferChannels.length];
        for (int i = 0; i < fromBufferChannels.length; i++) {
            guards[i] = fromBufferChannels[i].in();
        }

        Alternative alt = new Alternative(guards);
        Random rand = new Random();

        while (true) {
            int queryBufferIndex = rand.nextInt(toBufferChannels.length);
            System.out.println("Producer " + Id + " querying buffer " + queryBufferIndex + "...");
            toBufferChannels[queryBufferIndex].out().write(Payload.WHERE.ordinal());
            System.out.println("Producer " + Id + " sent WHERE to buffer " + queryBufferIndex + ".");

            int selectedBufferIndex = alt.select();

            int response = fromBufferChannels[selectedBufferIndex].in().read();

            System.out.println("Producer " + Id + " received response from buffer " + selectedBufferIndex + ".");

            while (response == Payload.WAIT.ordinal()) {
                System.out.println("Producer " + Id + " got WAIT.");
                try {
                    Thread.sleep(100 + rand.nextInt(400));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                queryBufferIndex = rand.nextInt(toBufferChannels.length);
                System.out.println("Producer " + Id + " querying buffer " + queryBufferIndex + " after WAIT...");
                toBufferChannels[queryBufferIndex].out().write(Payload.WHERE.ordinal());
                System.out.println("Producer " + Id + " sent WHERE to buffer " + queryBufferIndex + ".");
                selectedBufferIndex = alt.select();
                response = fromBufferChannels[selectedBufferIndex].in().read();
            }

            if (response == Payload.HERE.ordinal()) {
                System.out.println("Producer " + Id + " got HERE .");
                toBufferChannels[selectedBufferIndex].out().write(Payload.PACKAGE.ordinal());
                System.out.println("Producer " + Id + " sent PACKAGE .");
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
