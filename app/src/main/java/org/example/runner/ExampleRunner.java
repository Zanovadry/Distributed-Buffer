package org.example.runner;

import org.example.buffer.ExampleBuffer;
import org.example.consumer.ExampleConsumer;
import org.example.producer.ExampleProducer;
import org.jcsp.lang.*;
/** Main program class for Producer/Consumer example.
 * Sets up channels, creates processes then
 * executes them in parallel, using JCSP.
 */
public final class ExampleRunner
{
    public ExampleRunner () {
    }

    public void run ()
    {
//        Tworzymy kanały między procesami.
//        prodChan → producenci wysyłają dane do bufora.
//        consReq → konsumenci wysyłają żądania do bufora (prośba o dane).
//        consChan → bufor wysyła dane do konsumentów.

        final One2OneChannelInt[] prodChan = { Channel.one2oneInt(), Channel.one2oneInt()}; // Producers
        final One2OneChannelInt[] consReq = { Channel.one2oneInt(), Channel.one2oneInt() }; // Consumer requests
        final One2OneChannelInt[] consChan = { Channel.one2oneInt(), Channel.one2oneInt() }; // Consumer data

        CSProcess[] procList = {
                new ExampleProducer(prodChan[0], 0),
                new ExampleProducer(prodChan[1], 100),
                new ExampleBuffer(prodChan, consReq, consChan),
                new ExampleConsumer(consReq[0], consChan[0]),
                new ExampleConsumer(consReq[1], consChan[1]) };

        Parallel par = new Parallel(procList);
        par.run();
//        Tworzymy listę procesów: dwóch producentów, bufor i dwóch konsumentów.
//        Parallel uruchamia wszystkie procesy jednocześnie (wątkowo).
    }
}
