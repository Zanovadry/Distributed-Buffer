package org.example.producer;
import org.jcsp.lang.*;
import java.util.Random;

import org.example.utilities.Payload;

public class TimeoutProducer implements CSProcess {
    final private One2OneChannelInt[] buffers;
    final private int Id;

    public TimeoutProducer (One2OneChannelInt[] buffers, int Id) {
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
            System.out.println("Producer " + Id + " querying buffer " + queryBufferIndex + "...");
            One2OneChannelInt queryBuffer = buffers[queryBufferIndex];
            queryBuffer.out().write(Payload.WHERE.ordinal());
            System.out.println("Producer " + Id + " sent WHERE to buffer " + queryBufferIndex + ".");

            int selectedBufferIndex = alt.select();

            One2OneChannelInt sendBufferIndex = buffers[selectedBufferIndex];
            int response = sendBufferIndex.in().read();

            System.out.println("Producer " + Id + " received response from buffer " + selectedBufferIndex + ".");

            while (response == Payload.WAIT.ordinal()) {
                System.out.println("Producer " + Id + " got WAIT.");
                try {
                    Thread.sleep(100 + rand.nextInt(400));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                queryBufferIndex = rand.nextInt(buffers.length);
                System.out.println("Producer " + Id + " querying buffer " + queryBufferIndex + " after WAIT...");
                queryBuffer = buffers[queryBufferIndex];
                queryBuffer.out().write(Payload.WHERE.ordinal());
                System.out.println("Producer " + Id + " sent WHERE to buffer " + queryBufferIndex + ".");
                sendBufferIndex = buffers[alt.select()];
                response = sendBufferIndex.in().read();
            }

            if (response == Payload.HERE.ordinal()) {
                System.out.println("Producer " + Id + " got HERE .");
                sendBufferIndex.out().write(Payload.PACKAGE.ordinal());
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
