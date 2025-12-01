package org.example.utilities;

import org.jcsp.lang.AltingChannelInput;
import org.jcsp.lang.One2OneChannel;

import org.example.utilities.Message;

public class DualChannel {
    private final One2OneChannel requestChannel;
    private final One2OneChannel responseChannel;

    public DualChannel(One2OneChannel requestChannel, One2OneChannel responseChannel) {
        this.requestChannel = requestChannel;
        this.responseChannel = responseChannel;
    }

    public void writeToRequest(Message payload) {
        requestChannel.out().write(payload);
    }

    public void writeToResponse(Message payload) {
        responseChannel.out().write(payload);
    }

    public Message readFromRequest() {
        return (Message) requestChannel.in().read();
    }

    public Message readFromResponse() {
        return (Message) responseChannel.in().read();
    }

    public AltingChannelInput getRequestChannelIn() {
        return requestChannel.in();
    }

    public One2OneChannel getResponseChannel() {
        return responseChannel;
    }
}
