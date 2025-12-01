package org.example.utilities;

public class Stats {
    final private int[] producedStats;
    final private int[] consumedStats;
    final private int[] producerPassStats;
    final private int[] consumerPassStats;

    public Stats(int numProducers, int numConsumers) {
        producedStats = new int[numProducers];
        consumedStats = new int[numConsumers];
        producerPassStats = new int[numProducers];
        consumerPassStats = new int[numConsumers];
    }

    public void recordProduced(int producerId) {
        producedStats[producerId]++;
    }

    public void recordConsumed(int consumerId) {
        consumedStats[consumerId]++;
    }

    public void recordProducerPass(int producerId) {
        producerPassStats[producerId]++;
    }

    public void recordConsumerPass(int consumerId) {
        consumerPassStats[consumerId]++;
    }

    public int getProducedCount(int producerId) {
        return producedStats[producerId];
    }

    public int getConsumedCount(int consumerId) {
        return consumedStats[consumerId];
    }

    public int getProducerPassCount(int producerId) {
        return producerPassStats[producerId];
    }

    public int getConsumerPassCount(int consumerId) {
        return consumerPassStats[consumerId];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Produced Stats: ");
        for (int count : producedStats) {
            sb.append(count).append(" ");
        }
        sb.append("\nConsumed Stats: ");
        for (int count : consumedStats) {
            sb.append(count).append(" ");
        }
        sb.append("\nProducer Pass Stats: ");
        for (int count : producerPassStats) {
            sb.append(count).append(" ");
        }
        sb.append("\nConsumer Pass Stats: ");
        for (int count : consumerPassStats) {
            sb.append(count).append(" ");
        }
        return sb.toString();
    }
}
