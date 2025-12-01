package org.example;

import org.example.runner.TimeoutRunner;

public class App {

    public static void main(String[] args) {
        new TimeoutRunner(1, 1, 3, 5);
    }
}
