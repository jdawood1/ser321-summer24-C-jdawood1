# Distributed System for Summing Numbers
This project implements a distributed system to calculate the sum of a list of numbers by distributing tasks across multiple nodes. It compares the performance between a single leader-based calculation and a distributed approach, including a consensus verification step to ensure result consistency.

##### The system components are:
1. **Client:**
    - Prompts the user for a list of numbers and a delay value.
    - Sends the input to the Leader for processing.
2. **Leader:**
    - Performs a single-threaded sum for comparison.
    - Waits for at least three Nodes to connect.
    - Divides the list of numbers into portions and assigns tasks to connected Nodes.
    - Collects results, performs consensus checks, and sends the final sum to the Client.
3. **Nodes:**
    - Compute the partial sum for their assigned portion of numbers.
    - Simulate computation delay between additions.
    - Return their partial sum to the Leader.
    - Participate in a consensus verification step.

##### Purpose and Functionality
This project demonstrates distributed computation by dividing a task (summing numbers) across multiple nodes. It highlights the benefits and challenges of distributed systems, including fault tolerance and performance gains. A consensus mechanism ensures that the results from all Nodes are consistent before sending them to the Client.

##### Workflow
1. **Client:**
    - Prompts the user for input:
      - Enter initial greeting to the leader
      - A list of numbers separated by commas (e.g., 1,2,3,4,5).
      - A delay value in milliseconds (e.g., 50).
    - Sends the input to the Leader.
2. **Leader:**
    - Performs a single-threaded sum with the provided delay for comparison.
    - Waits for at least three Nodes to connect. If fewer than three Nodes are available, an error is sent to the Client.
    - Divides the list of numbers into equal portions and sends tasks to the connected Nodes.
3. **Nodes:**
    - Each Node computes the sum of its assigned portion.
    - Simulates computation delay (e.g., 50ms between each addition).
    - Returns the result to the Leader.
    - Participates in a consensus step to verify the correctness of another Node's partial sum.
4. **Leader:**
    - Collects the results from all Nodes.
    - Performs a consensus verification:
      - Sends each Node another Node’s result for validation.
      - Requires a majority agreement to finalize the result.
    - If consensus is achieved, computes the total sum and sends it to the Client.
    - If consensus fails, sends an error message to the Client.
5. **Client:**
    - Receives and displays the final sum and computation times, or an error message if the computation fails.
    - Program end.

##### Error Handling
1. **Client Input Validation**:
   - Ensures valid input for:
     - Numbers: Comma-separated integers.
     - Delay: Positive integer values only.
   - Rejects invalid formats or empty inputs.
2. **Node Connection**:
   - The Leader requires at least three nodes to proceed. If fewer than three nodes are connected, an error is sent to the Client, and the computation is aborted.
3. **Node Response Timeout**:
   - If a node fails to respond within a 5-second timeout during task distribution or consensus verification, it is marked as unresponsive.
4. **Consensus Failure**:
   - If fewer than 50% of nodes agree on the results, consensus is deemed to have failed. The Leader sends an error message to the Client.
5. **Faulty Nodes**:
   - The Leader detects inconsistent results during consensus and excludes faulty nodes from the final computation.

##### Running the Program
The project uses Gradle to build and run the components.

##### Prerequisites
- Java Development Kit (JDK) 17 or higher.
- Gradle build tool.

##### Commands to Run Components
###### Run the Leader:
``gradle runLeader -q --console=plain``

###### Run the Client:
``gradle runClient -q --console=plain``

###### The Client will prompt you to input:
- A list of numbers separated by commas.
- A delay value in milliseconds.

###### Run Nodes:
``gradle runNode -q --console=plain``

###### To run a faulty node:
``gradle runNode -Pfault -q --console=plain``

###### To run a time test: *(Note test takes roughly 2min to complete.)*
``gradle test -q --console=plain``

##### Chosen Protocol
The leader-worker model is used, where the leader distributes work to nodes and performs a cross-verification of the results. A majority consensus is needed to proceed with delivering the final sum.

##### Requirements Fulfilled
- Client-Server Structure: Implemented client, leader, and nodes.
- Gradle Tasks: `runClient`, `runLeader`, `runNode` for starting components.
- Node Verification: Implemented consensus mechanism.
- Performance Comparison: Single-thread versus distributed calculation.
- Faulty Node Simulation: Ability to simulate faulty nodes.

##### Commands and Execution Order
- Start Leader: `gradle runLeader`
- Start Client: `gradle runClient` to initiate calculation.
- Start Nodes: Run at least three nodes (`gradle runNode`).
- Optionally, run a faulty node (`gradle runNode -Pfault`).


### Performance Analysis
#### Test Results

| List Size | Delay (ms) | Single Sum Time (ms) | Distributed Time (ms) | Speedup (%)  |
|-----------|------------|----------------------|-----------------------|--------------|
| 10        | 50         | 539                  | 120                   | 77.74%       |
| 100       | 50         | 5327                 | 1064                  | 80.03%       |
| 1000      | 100        | 103274               | 20665                 | 79.99%       |

#### Observations
1. **Small Lists**:
   - Even with a small list of 10 numbers, distributed computation demonstrates significant improvement, achieving a 77.74% speedup.
   - The benefits stem from parallel execution across multiple nodes, which mitigates the effect of computation delays.
2. **Large Lists**:
   - For lists of size 100 and 1000, distributed computation shows even greater performance improvements due to increased workload distribution.
   - Communication overhead becomes negligible compared to the gains from parallelism.
3. **Higher Delays**:
   - Longer delays (e.g., 100 ms) further emphasize the advantages of distributed systems, as each node's parallel processing significantly reduces overall computation time.
4. **Without Consensus:**
   - If the consensus mechanism were not implemented, the distributed computation would return the calculated sum and times to the client without verifying results. This could lead to errors in environments with faulty nodes.
5. **Without Threading:**
   - Without threading for node communication, the distributed computation would be sequential, negating the advantages of parallelism. Computation times would be similar to or worse than single-sum calculations.

### Conclusion
Distributed computation is highly effective even for smaller lists, as demonstrated by the 77.74% speedup for a list size of 10. Larger lists and higher delays amplify these benefits, making distributed systems ideal for handling computationally intensive or time-sensitive tasks.


