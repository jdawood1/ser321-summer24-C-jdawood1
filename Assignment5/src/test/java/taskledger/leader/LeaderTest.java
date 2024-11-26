package taskledger.leader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeaderTest {

    @BeforeEach
    void setUp() {
        System.out.println("Setting up the test...");
    }

    @AfterEach
    void tearDown() {
        System.out.println("Test completed.");
    }

    @Test
    void testPerformanceComparison() {
        int[][] scenarios = {
                {10, 50},    // {List size, Delay in ms}
                {100, 50},
                {1000, 100}
        };

        for (int[] scenario : scenarios) {
            int listSize = scenario[0];
            int delay = scenario[1];

            // Generate the list of numbers
            int[] numberList = generateNumbers(listSize);

            // Measure Single-Sum Calculation Time
            long singleStartTime = System.currentTimeMillis();
            int singleSum = performSingleSum(numberList, delay);
            long singleTimeTaken = System.currentTimeMillis() - singleStartTime;

            // Measure Distributed-Sum Calculation Time
            int numNodes = 5; // Assume 5 nodes for distribution
            long distributedStartTime = System.currentTimeMillis();
            int distributedSum = performDistributedSum(numberList, delay, numNodes);
            long distributedTimeTaken = System.currentTimeMillis() - distributedStartTime;

            // Assert that the results are equal
            assertEquals(singleSum, distributedSum, "Single-sum and distributed-sum results should match.");

            // Calculate speedup percentage
            double speedup = ((double) singleTimeTaken - distributedTimeTaken) / singleTimeTaken * 100;

            // Log the results
            System.out.printf("| %-9d | %-10d | %-21d | %-22d | %.2f%%         |%n",
                    listSize, delay, singleTimeTaken, distributedTimeTaken, speedup);

            // Verify that speedup is reasonable (not strictly required, but good for validation)
            assertTrue(speedup > 0, "Distributed computation should be faster than single-sum.");
        }
    }

    private int[] generateNumbers(int size) {
        int[] numbers = new int[size];
        for (int i = 0; i < size; i++) {
            numbers[i] = i + 1; // Generate numbers from 1 to `size`
        }
        return numbers;
    }

    private int performSingleSum(int[] numbers, int delay) {
        int sum = 0;
        try {
            for (int num : numbers) {
                sum += num;
                Thread.sleep(delay); // Simulate delay
            }
        } catch (InterruptedException e) {
            System.out.println("Error during single-sum calculation: " + e.getMessage());
        }
        return sum;
    }

    private int performDistributedSum(int[] numbers, int delay, int numNodes) {
        int splitSize = numbers.length / numNodes;
        List<Thread> threads = new ArrayList<>();
        List<Integer> partialSums = new ArrayList<>();

        for (int i = 0; i < numNodes; i++) {
            int start = i * splitSize;
            int end = (i == numNodes - 1) ? numbers.length : start + splitSize;
            int[] chunk = new int[end - start];
            System.arraycopy(numbers, start, chunk, 0, end - start);

            Thread thread = new Thread(() -> {
                int sum = 0;
                try {
                    for (int num : chunk) {
                        sum += num;
                        Thread.sleep(delay); // Simulate delay
                    }
                } catch (InterruptedException e) {
                    System.out.println("Error during distributed-sum calculation: " + e.getMessage());
                }
                synchronized (partialSums) {
                    partialSums.add(sum);
                }
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.out.println("Error waiting for thread: " + e.getMessage());
            }
        }

        // Combine partial sums
        return partialSums.stream().mapToInt(Integer::intValue).sum();
    }
}
