package org.example.consumer;

import org.jcsp.lang.CSProcess;

import org.example.utilities.DualChannel;
import org.example.utilities.Message;
import org.example.utilities.MessageType;
import org.example.utilities.Status;

public class WikiConsumer implements CSProcess {
    private final DualChannel bufferGate;
    private final int maxToConsume;
    private final int consumerIndex;
    private int successCount = 0;
    private int failureCount = 0;

    public WikiConsumer(DualChannel bufferGate, int maxToConsume, int index) {
        this.bufferGate = bufferGate;
        this.maxToConsume = maxToConsume;
        this.consumerIndex = index;
    }

    public void run() {
        while(true) {
            int toConsume = (int) ((Math.random() * (maxToConsume - 1)) + 1);
            Message request = new Message(MessageType.ORDER_GET, toConsume, bufferGate.getResponseChannel());
            System.out.println("Consumer " + consumerIndex + ": " + request);
            bufferGate.writeToRequest(request);
            Message response = bufferGate.readFromResponse();

            System.out.println("Consumer " + consumerIndex + ": " + response);
            processResponse(response);
        }
    }

    private void processResponse(Message response) {
        if(response.status == Status.SUCCESS) {
            this.successCount++;
        } else {
            this.failureCount++;
        }
        System.out.println(getBalance());
    }

    public DualChannel getBufferGate() {
        return this.bufferGate;
    }

    public String getBalance() {
        return "Consumer " + consumerIndex + ": Balance [S:F]: " + this.successCount + ":" + this.failureCount + ")";
    }
}