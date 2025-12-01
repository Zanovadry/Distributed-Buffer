package org.example.utilities;

import org.jcsp.lang.One2OneChannel;

import org.example.utilities.MessageType;
import org.example.utilities.Status;

public class Message {
    public MessageType type;
    public Status status;
    public int payload;
    public One2OneChannel directResponseChannel;
    public int ownerBufferId = -1;

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, int payload) {
        this(type);
        this.payload = payload;
    }

    public Message(MessageType type, int payload, One2OneChannel channel) {
        this(type, payload);
        this.directResponseChannel = channel;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
    public void setOwner(int id) {
        this.ownerBufferId = id;
    }

    public String toString() {
        return "Message: " + type + ", " + status + ", " + payload;
    }
}
