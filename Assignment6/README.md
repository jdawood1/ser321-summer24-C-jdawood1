# GRPC Services and Registry (Task 1 & 2)

The following folder contains a `Registry.jar` which includes a registering service where nodes can register to allow clients to find them and use their implemented GRPC services.

Please refer to the `build.gradle` file for configuration details and follow the instructions below to run and test the project.

---

## **Project Description**

This project implements GRPC-based services, including:
- **Echo Service**: Echoes back user input.
- **Joke Service**: Provides jokes and allows adding new ones.
- **Flowers Service**: Manages a garden where flowers can be planted, watered, cared for, and viewed.
- **Follow Service**: Allows users to follow others and view their connections.
- **Trivia Master Service**: A trivia game where questions can be added, answered, and retrieved randomly or in bulk.

### **Key Features**
1. **Multiple GRPC Services**: Each service supports unique functionality, such as trivia, jokes, and garden management.
2. **Registry Integration**: Nodes can register themselves to a central registry, enabling dynamic service discovery.
3. **Persistence**: Server-side data (e.g., trivia questions, user follow connections) remains persistent throughout the application's runtime.

---

## **How to Run the Project**

### **Running Without Registry**

**1. Start the Node Server**:
   ```
   gradle runNode
   ```
**2. Start the Client (in a separate terminal):**
   ```
   gradle runClient -q --console=plain
   ```
**3. Running With Registry**
* **Start Registry Server:**
    ```
    gradle runRegistryServer
    ```
* **Start Node Server (with registry enabled):**
    ```
    gradle runNode -PregOn=true
    ```
* **Start Client (with registry enabled):**
    ```
    gradle runClient -PregOn=true
    ```

---

## **How to Use the Program**
When running the client, you will be presented with a menu to select the desired service. Input numbers or strings as prompted based on the selected service. Below is a brief overview of each service and its options:

**1. Echo Service**
   - Input a message, and the service will echo it back.

**2. Joke Service**
   - Get Jokes: Request a specific number of jokes.
   - Add a Joke: Add your own joke to the server.

**3. Flowers Service**
   - Plant a Flower: Add a flower with its name, water times, and bloom duration.
   - View Flowers: See a list of all planted flowers and their statuses.
   - Water a Flower: Water a specific flower.
   - Care for a Flower: Extend a blooming flower's duration.

**4. Follow Service**
   - Add User: Register a new user.
   - Follow User: Make one user follow another.
   - View Following: See the list of users a specific user is following.

**5. Trivia Master Service**
   - Add a Trivia Question: Input a question and its answer.
   - Get Trivia Questions: Retrieve trivia questions randomly or in bulk.
   - Answer a Trivia Question: Attempt to answer a specific trivia question.

**6. Exit**
   - Exit the program gracefully.

---

## **Testing the Program**

**Running Tests**
You can test the implemented services using:
```
gradle test
```

**Features of Testing**

All test cases run independently without requiring the server to be manually started.
The tests cover all implemented services, including Echo, Joke, Flowers, Follow, and Trivia Master.
Requirements Fulfilled

* **Echo Service:** Implements a simple method to echo user input.
* **Joke Service:** Adds and retrieves jokes with persistence on the server.
* **Flowers Service:** Manages flowers with state transitions (PLANTED -> BLOOMING -> DEAD).
* **Follow Service:** Allows user interactions, including adding and following users.
* **Trivia Master Service:** Handles trivia questions with random and bulk retrieval options.
* **Integration With Registry:** Nodes register and discover services via the central registry.
* **Persistence:** Server-side data is maintained throughout the application's runtime.

---

## **gRPC Client2-Server** (Task 3)

This project implements a gRPC client-server application where the client (Client2) can dynamically connect to various services registered with a central registry. Each service is implemented in its own gRPC class, including Echo, Joke, Flowers, Follow, and TriviaMaster services. The registry helps the client to discover available services and invoke them dynamically.

The client can be run to list available services, allow a user to choose a service, and provide necessary input to communicate with that service. The client will continue to run until the user decides to exit.

## **How to run**

**Start the Registry Server**
```
gradle runRegistryServer -PgrpcPort=9002
```
**Run Service Node**
```
gradle runNode -PnodeName="testNode" -PgrpcPort=9002 -PregistryHost="localhost" -PserviceHost="localhost" -PservicePort=8000 -PdiscoveryPort=10000 -PregOn=true
```
**Run the Client2**
```
gradle runClient2 -PgrpcPort=9002 -PregistryHost="localhost" -PserviceHost="localhost" -PservicePort=8000 -Pmessage="Hello" -PregOn=true
```

## **How to Use the Client**

Fetch Available Services
When the client starts, it automatically contacts the Registry to get the list of all available services. It prints the available services for the user to select.

**Service Selection**
- Enter the exact name of the service you want to use, such as `services.Echo/parrot`.

## **Service Input Requirements:**
>Each service requires different inputs. Here are the prompts and the input requirements for each service:

**services.Echo/parrot:** Enter a message to be echoed back by the server. Example input: `Hello World`

**services.Joke/getJoke:** Enter the number of jokes you want to retrieve. Example input: `3`

**services.Joke/setJoke:** Enter a joke to add to the list. Example input: `Why don't skeletons fight each other? They don't have the guts.`

**services.Flowers/plantFlower:** Enter the flower details in the format: `<name> <waterTimes> <bloomTime>`. Example input: `Rose 3 5`

**services.Flowers/careForFlower:** Enter the name of the flower you want to care for. Example input: `Rose`

**services.Flowers/viewFlowers:** No additional input needed. Just press enter.

**services.Flowers/waterFlower:** Enter the name of the flower you want to water. Example input: `Rose`

**services.Follow/addUser:** Enter the name of the user you want to add. Example input: `Alice`

**services.Follow/follow:** Enter the follower and the followee in the format: `<follower> <followee>`. Example input: `Alice Bob`

**services.Follow/viewFollowing:** Enter the name of the user to view their following list. Example input: `Alice`

**services.TriviaMaster/addQuestion:** Enter the question and answer in the format: `<question>|<answer>`. Example input: What is the capital of `France?|Paris`

**services.TriviaMaster/getQuestion:** Enter the number of questions you want to retrieve. Example input: `2`

**services.TriviaMaster/answerQuestion:** Enter the question and your answer in the format: `<question>|<answer>`. Example input: What is the capital of `France?|Paris`

Loop Through Services
After invoking a service, the client will prompt you to either select another service or exit. Enter `exit` if you wish to stop using the client.








