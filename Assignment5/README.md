# Description
This project implements a distributed system to calculate the sum of a list of numbers by distributing tasks across multiple nodes. It compares the performance between a single leader-based calculation and a distributed approach, including a consensus verification step to ensure result consistency.

##### The system components are:
- **Client:** Sends a list of numbers and delay to the leader for processing.
- **Leader:** Divides work among nodes, collects results, and performs consensus checks.
- **Nodes:** Compute a partial sum of the assigned numbers and participate in consensus.

##### Running the Program
The project uses Gradle to build and run the components.

##### Prerequisites
- Java Development Kit (JDK) 17 or higher.
- Gradle build tool.

##### Commands to Run Components
###### Run the Leader:
``gradle runLeader``

###### Run the Client:
``gradle runClient -q --console=plain``

###### Optionally, provide list of numbers and delay:
``gradle runClient -q --console=plain --args="1,2,3,4,5 50"``

###### Run Nodes:
``gradle runNode -q --console=plain``

###### To run a faulty node:
``gradle runNode -q --console=plain --args="-fault"``

##### Purpose and Functionality
The project distributes the sum calculation across nodes, demonstrating a basic consensus mechanism to verify correctness. The performance of single-threaded versus distributed calculations is compared.

###### Workflow
- Client sends numbers and delay to the leader.
- Leader performs a single-sum calculation for comparison.
- Nodes connect to the leader (minimum 3 nodes).
- Distributed Sum Calculation across nodes.
- Consensus Verification among nodes.
- Client receives result after verification.

##### Chosen Protocol
The leader-follower model is used, where the leader distributes work to nodes and performs a cross-verification of the results. A majority consensus is needed to proceed with delivering the final sum.

###### Intended Workflow
- The client sends a list of numbers and delay value.
- The leader calculates the single sum, waits for three nodes to connect, then distributes the list among nodes.
- The nodes compute partial sums and participate in a consensus check.
- The leader sends the final result to the client if consensus is achieved, or an error otherwise.

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
- Optionally, run a faulty node (`gradle runNode --args="-fault"`).

#### Analysis of Distributed Calculation
The distributed approach aims to speed up the calculation by dividing work across nodes. Depending on the delay and number of nodes, distributed computation may outperform the single-threaded approach. For smaller lists or lower delays, communication overhead may negate performance gains. For larger lists and higher delays, parallel computation often yields significant performance improvements. The implementation leverages threading for node handling, allowing concurrent processing and reduced overall computation time compared to a sequential approach.