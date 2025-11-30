package org.example.buffer;
import org.jcsp.lang.*;

/** Buffer class: Manages communication between Producer2
 * and Consumer2 classes.
 */
public class ExampleBuffer implements CSProcess
{ private One2OneChannelInt[] in; // Input from Producer
    private One2OneChannelInt[] req; // Request for data from Consumer
    private One2OneChannelInt[] out; // Output to Consumer
    // The buffer itself
    private int[] buffer = new int[10];
    // Subscripts for buffer
    int hd = -1;
    int tl = -1;
    public ExampleBuffer (final One2OneChannelInt[] in, final One2OneChannelInt[] req, final One2OneChannelInt[] out) {
        this.in = in;
        this.req = req;
        this.out = out;
    }
// alteratywa dotyczy kanałów
    //na których kanałach działa alternatywa, co robi
    //panią interesuje równowaźenie obciążenia w buforze
    //bufory nie muszą być ze sobą połączone
    //ale można wprowadzić managera, który rozporządza przydzielanymi buforami - nie przepuszczamy przez niego danych (manager musi znać stan wszystkich buforów - używamy algorytmu randrobin)
    public void run () {
        final Guard[] guards = { in[0].in(), in[1].in(), req[0].in(), req[1].in() };
        final Alternative alt = new Alternative(guards);
//        Alternative pozwala buforowi czekać równocześnie na wielu kanałach.
//        in[i].in() → kanał wejściowy od producentów.
//        req[i].in() → kanał wejściowy od żądań konsumentów.
//        alt.select() wybiera pierwszy gotowy kanał.

        int countdown = 4; // Number of processes running
        while (countdown > 0) {
            int index = alt.select();
            switch (index) {
                case 0:
                case 1: // A Producer is ready to send
                    if (hd < tl + 11) {
                        int item = in[index].in().read();
                        if (item < 0) {
                            countdown--;
                        }
                        else {
                            { hd++;
                                buffer[hd%buffer.length] = item;
                            }
                        }
                    }
                    break;
                case 2:
                case 3: // A Consumer is ready to read
                    if (tl < hd) {
                        req[index-2].in().read(); // Read and discard request
                        tl++;
                        int item = buffer[tl%buffer.length];
                        out[index-2].out().write(item);
//                      tl = tail, indeks ostatnio wysłanego elementu.
//                      Jeśli brak danych, bufor może wysłać -1 jako sygnał końca.
                    }
                    else if (countdown <= 2)  {
                        req[index-2].in().read();
                        out[index-2].out().write(-1); // Signal end
                        countdown--;
                    }
                break;
            }
        }
        System.out.println("Buffer ended.");
    }
}
