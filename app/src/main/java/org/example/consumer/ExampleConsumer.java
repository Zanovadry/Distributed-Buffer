package org.example.consumer;

import org.jcsp.lang.*;
/** Consumer class: reads ints from input channel, displays them,
 then
 * terminates when a negative value is read.
 */
public class ExampleConsumer implements CSProcess
{ private One2OneChannelInt in;
    private One2OneChannelInt req;

    public ExampleConsumer (final One2OneChannelInt req, final One2OneChannelInt in) {
        this.req = req;
        this.in = in;
    }

    public void run () {
        int item;
        while (true) {
            req.out().write(0); // Request data - blocks until data is available
            item = in.in().read();
            if (item < 0) {
                break;
            }
            System.out.println(item);
        }
//        Konsument najpierw wysyła żądanie (req.out().write) do bufora, że chce dane.
//        Następnie odbiera dane (in.in().read) z bufora.
//        Jeśli dostanie -1, kończy działanie.

        System.out.println("Consumer ended.");
    }
}