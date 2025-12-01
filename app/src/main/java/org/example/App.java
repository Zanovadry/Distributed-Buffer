package org.example;

import org.example.runner.TimeoutRunner;

public class App {

    public static void main(String[] args) {
        new TimeoutRunner(10, 10, 5, 20);
    }
}
