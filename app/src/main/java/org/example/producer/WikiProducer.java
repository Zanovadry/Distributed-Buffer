package org.example.producer;

import org.example.utilities.DualChannel;
import org.example.utilities.Message;
import org.example.utilities.MessageType;
import org.example.utilities.Status;
import org.jcsp.lang.CSProcess;

public class WikiProducer implements CSProcess {
    private final DualChannel bufferGate;
    private final int maxToProduce;
    private final int producerIndex;
    private int successCount = 0;
    private int failureCount = 0;

    public WikiProducer(DualChannel bufferGate, int maxToProduce, int index) {
        this.bufferGate = bufferGate;
        this.maxToProduce = maxToProduce;
        this.producerIndex = index;
    }

    public void run() {
        while(true) {
            int payload = (int) (Math.random() * maxToProduce) + 1;
            Message request = new Message(MessageType.ORDER_POST, payload, bufferGate.getResponseChannel());
            System.out.println("Producer " + producerIndex + ": " + request);
            bufferGate.writeToRequest(request);
            Message response = bufferGate.readFromResponse();
            System.out.println("Producer " + producerIndex + ": " + response);
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
        return "Producer " + producerIndex + ": Balance [S:F]: " + this.successCount + ":" + this.failureCount + ")";
    }
}