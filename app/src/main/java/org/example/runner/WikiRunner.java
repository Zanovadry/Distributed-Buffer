package org.example.runner;

import org.example.consumer.WikiConsumer;
import org.example.producer.WikiProducer;
import org.example.buffer.WikiBuffer;
import org.example.utilities.DualChannel;
import org.jcsp.lang.*;

import java.util.ArrayList;
import java.util.List;

public final class WikiRunner {
    public WikiRunner() {
        int NUMBER_OF_PRODUCERS = 30;
        int NUMBER_OF_CONSUMERS = 30;
        int NUMBER_OF_BUFFERS = 6;
        int BUFFER_SIZE = 6;
        int MAX_CLIENT_ACTION = BUFFER_SIZE / 2;

        List<CSProcess> processList = new ArrayList<>();

        for(int i = 0 ; i < NUMBER_OF_PRODUCERS ; i++) {
            One2OneChannel channel1 = Channel.one2one();
            One2OneChannel channel2 = Channel.one2one();
            DualChannel dualChannel = new DualChannel(channel1, channel2);
            processList.add(new WikiProducer(dualChannel, MAX_CLIENT_ACTION, i));
        }

        for(int i = 0 ; i < NUMBER_OF_CONSUMERS ; i++) {
            One2OneChannel channel1 = Channel.one2one();
            One2OneChannel channel2 = Channel.one2one();
            DualChannel dualChannel = new DualChannel(channel1, channel2);
            processList.add(new WikiConsumer(dualChannel, MAX_CLIENT_ACTION, i));
        }

        List<Integer> clientBuffer = new ArrayList<>();
        int bufferIndex = 0;
        for(int i = 0 ; i < processList.size() ; i++){  // dla każdego klienta
            System.out.println(bufferIndex);
            clientBuffer.add(bufferIndex);
            bufferIndex = (bufferIndex + 1) % NUMBER_OF_BUFFERS;
        }

        int firstBufferIndex = NUMBER_OF_PRODUCERS + NUMBER_OF_CONSUMERS;

        // TWORZENIE TOPOLOGII
        for(int i = 0 ; i < NUMBER_OF_BUFFERS ; i++) {
            List<DualChannel> clientChannels = new ArrayList<>();
            for(int j = 0 ; j < clientBuffer.size() ; j++) {
                if(clientBuffer.get(j) == i) {
                    var client = processList.get(j);
                    if(client instanceof WikiProducer) {
                        clientChannels.add(((WikiProducer) processList.get(j)).getBufferGate());
                    } else if(client instanceof WikiConsumer) {
                        clientChannels.add(((WikiConsumer) processList.get(j)).getBufferGate());
                    }
                }
            }
            One2OneChannel forwardChannel = null;
            One2OneChannel backwardChannel = null;

            if(i == 0) {
                forwardChannel = Channel.one2one();
                backwardChannel = Channel.one2one();
            } else if(i > 0 && i < NUMBER_OF_BUFFERS - 1) {
                forwardChannel = Channel.one2one();
                backwardChannel = ((WikiBuffer) processList.get(firstBufferIndex + i - 1)).getForwardBuffer();
            } else if(i == NUMBER_OF_BUFFERS - 1) {
                forwardChannel = ((WikiBuffer) processList.get(firstBufferIndex)).getBackwardBuffer();
                backwardChannel = ((WikiBuffer) processList.get(firstBufferIndex + i - 1)).getForwardBuffer();
            }
            processList.add(new WikiBuffer(forwardChannel, backwardChannel, clientChannels, BUFFER_SIZE, i));
        }

        CSProcess[] convertedProcessList = new CSProcess[processList.size()];
        convertedProcessList = processList.toArray(convertedProcessList);

        Parallel par = new Parallel(convertedProcessList);
        par.run();
    }

//    private static One2OneChannel[] getArrayChannels(List<One2OneChannel> list) {
//        One2OneChannel[] array = new One2OneChannel[list.size()];
//        return list.toArray(array);
//    }
}
