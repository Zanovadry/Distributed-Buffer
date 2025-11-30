package org.example.producer;

import org.jcsp.lang.*;
/** Producer class: produces 100 random integers and sends them on
 * output channel, then sends -1 and terminates.
 * The random integers are in a given range [start...start+100)
 */
public class ExampleProducer implements CSProcess {

    private One2OneChannelInt channel;
    private int start;

    public ExampleProducer (final One2OneChannelInt out, int start) {
        channel = out;
        this.start = start;
    }

    public void run () {
        int item;
        for (int k = 0; k < 100; k++) {
            item = (int)(Math.random()*100)+1+start;
            channel.out().write(item);
        }
        channel.out().write(-1);
        System.out.println("Producer" + start + " ended.");
//        Producent generuje 100 losowych liczb i wysyła je kanałem wyjściowym (channel.out().write).
//        Na końcu wysyła -1, aby oznaczyć koniec danych.
//        Każdy producent ma inny zakres liczb (start) – np. pierwszy 0–100, drugi 100–200.
    }
}