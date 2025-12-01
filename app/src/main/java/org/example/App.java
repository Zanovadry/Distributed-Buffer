package org.example;

import org.example.runner.QueueRunner;

public class App {

    public static void main(String[] args) {
        new QueueRunner(20, 10, 5, 20);
    }
}
