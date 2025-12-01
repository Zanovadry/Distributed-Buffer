package org.example.buffer;

import org.jcsp.lang.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.example.utilities.Message;
import org.example.utilities.MessageType;
import org.example.utilities.Status;
import org.example.utilities.DualChannel;

public class WikiBuffer implements CSProcess {
    private int BUFFER_SIZE;
    private int bufferValue;
    private final One2OneChannel forwardBuffer;
    private final One2OneChannel backwardBuffer;
    private final List<DualChannel> clients;
    private final int bufferIndex;
    private final Deque<Message> queue;
    private boolean hasToken;
    private int successCount = 0;
    private int failureCount = 0;

    public WikiBuffer(One2OneChannel forwardBuffer, One2OneChannel backwardBuffer, List<DualChannel> clients, int size, int index) {
        this.forwardBuffer = forwardBuffer;
        this.backwardBuffer = backwardBuffer;
        this.clients = clients;
        this.bufferValue = 0;
        this.BUFFER_SIZE = size;
        this.bufferIndex = index;
        this.queue = new ArrayDeque<>();
        this.hasToken = index == 0;
    }

    public void run() {
        Guard[] guards = new Guard[clients.size()];

        for(int i = 0 ; i < clients.size() ; i++) {
            guards[i] = clients.get(i).getRequestChannelIn();
        }

        Alternative alt = new Alternative(guards);

        while (true) {
            while(queue.size() == 0){   // jak możesz to wykonuj zlecenia
                System.out.println("Bufor " + bufferIndex + ": Czekam na klienta");
                int index = alt.select();
                Message message = clients.get(index).readFromRequest();
                System.out.println("Bufor " + bufferIndex + ": Dostaję od klienta (w pętli while) -> " + message);
                message.setOwner(this.bufferIndex);
                processOrder(message);
            }
            System.out.println("Bufor " + bufferIndex + ": Wyszedłem z while, queue.size = " + queue.size());
            // tu mamy queue size == 1 - od klienta
            // na początku jeden bufor ma token
            if(!hasToken) {
                waitForToken();
                Message message = waitForMessageFromBackBuffer();
                if(message.ownerBufferId == this.bufferIndex) { // jak wróciła wiadomość po kółku to odsyłam failure klientowi
                    System.out.println("Bufor " + bufferIndex + ": Wraca do mnie wiadomość po kółku - robię failure");
                    message.setStatus(Status.FAILURE);
                    message.directResponseChannel.out().write(message);
                } else {
                    processOrder(message);
                }
            }
            sendTokenToNextBuffer();
            sendFirstQueueItemToNextBuffer();
        }
    }

    private void processOrder(Message message) {
        if (message.type == MessageType.ORDER_POST) {
            if (canAddToBuffer(message.payload)) {
                processProduction(message);
            } else {
                queue.add(message);
                this.failureCount++;
                System.out.println("Bufor " + bufferIndex + "Wsadzam do queue. Queue size: " + queue.size() + " | Task to: " + message.payload);
                System.out.println("Bufor " + bufferIndex + ": I have in buffer " + bufferValue);
            }
        }
        else if (message.type == MessageType.ORDER_GET) {
            if (canTakeFromBuffer(message.payload)) {
                processConsumption(message);
            } else {
                queue.add(message);
                this.failureCount++;
                System.out.println("Wsadzam do queue. Queue size: " + queue.size() + " | Task to: " + message.payload * -1);
                System.out.println("Buffer value: " + bufferValue);
            }
        }
        else if (message.type == MessageType.TOKEN) {
            System.out.println("Bufor " + bufferIndex + ": Dostałem Token");
            this.hasToken = true;
        }
    }

    private void processProduction(Message message) {
        System.out.println("Bufor " + bufferIndex + ": Dodaję -> " + message.payload);
        this.bufferValue += message.payload;
        Message response = new Message(MessageType.RESPONSE_POST);
        response.setStatus(Status.SUCCESS);
        message.directResponseChannel.out().write(response);
        this.successCount++;
        System.out.println("Bufor " + bufferIndex + ": Koniec produckji");
        System.out.println(this.getBalance());
    }

    private void processConsumption(Message message) {
        System.out.println("Bufor " + bufferIndex + ": Odejmuję -> " + message.payload);
        this.bufferValue -= message.payload;
        Message response = new Message(MessageType.RESPONSE_GET, message.payload);
        response.setStatus(Status.SUCCESS);
        message.directResponseChannel.out().write(response);
        this.successCount++;
        System.out.println("Bufor " + bufferIndex + ": Koniec konsumpcji");
        System.out.println(this.getBalance());
    }

    private void waitForToken() {
        System.out.println("Bufor " + bufferIndex + ": Czekam na token...");
        Message message = (Message) backwardBuffer.in().read();
        processOrder(message);
    }

    private Message waitForMessageFromBackBuffer() {
        System.out.println("Bufor " + bufferIndex + ": Czekam na wiadomość od back buffera...");
        Message message = (Message) backwardBuffer.in().read(); // czeka na prawdziwą wiadomość
        return message;
    }

    private void sendTokenToNextBuffer() {
        System.out.println("Bufor " + bufferIndex + ": Wysyłam token dalej...");
        sendMessageToNextBuffer(new Message(MessageType.TOKEN));
        this.hasToken = false;
    }

    private void sendMessageToNextBuffer(Message message) {
        System.out.println("Bufor " + bufferIndex + ": Wysyłam wiadomość dalej... Message = " + message);
        forwardBuffer.out().write(message);
    }

    private void sendFirstQueueItemToNextBuffer() {
        Message messageToSendForward = queue.poll();
        sendMessageToNextBuffer(messageToSendForward);
    }

    private boolean canAddToBuffer(int value) {
        return this.bufferValue + value <= this.BUFFER_SIZE;
    }

    private boolean canTakeFromBuffer(int value) {
        return this.bufferValue - value >= 0;
    }

    public One2OneChannel getForwardBuffer() {
        return forwardBuffer;
    }

    public One2OneChannel getBackwardBuffer() {
        return backwardBuffer;
    }

    public String getBalance() {
        return "Bufor " + bufferIndex + ": Balance [S:F]: " + this.successCount + ":" + this.failureCount + ")";
    }
}