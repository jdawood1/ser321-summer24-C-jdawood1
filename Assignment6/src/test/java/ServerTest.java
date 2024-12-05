import example.grpcclient.EchoImpl;
import example.grpcclient.JokeImpl;
import example.grpcclient.FlowersImpl;
import example.grpcclient.FollowImpl;
import example.grpcclient.TriviaImpl;

import static org.junit.Assert.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.junit.Test;
import service.*;
import java.util.concurrent.TimeUnit;


public class ServerTest {
    private Server server;
    private ManagedChannel channel; // gRPC channel;
    private EchoGrpc.EchoBlockingStub blockingStub;
    private JokeGrpc.JokeBlockingStub blockingStub2;
    private FlowersGrpc.FlowersBlockingStub flowersStub;
    private FollowGrpc.FollowBlockingStub followStub;
    private TriviaMasterGrpc.TriviaMasterBlockingStub triviaStub;
    private static final String SERVER_HOST = "localhost";

    @org.junit.Before
    public void setUp() throws Exception {
        System.out.println("Setting up test environment...");

        // Start a new server instance for each test
        Server server = ServerBuilder.forPort(0)
                .addService(new EchoImpl())  // Add service
                .addService(new JokeImpl())
                .addService(new FlowersImpl())
                .addService(new FollowImpl())
                .addService(new TriviaImpl())
                .build()
                .start();

        // Retrieve the dynamically assigned port
        int assignedPort = server.getPort();
        System.out.println("Server started on port: " + assignedPort);

        // Create a new channel and stubs for the test
        channel = ManagedChannelBuilder.forAddress(SERVER_HOST, assignedPort)
                .usePlaintext()
                .build();

        blockingStub = EchoGrpc.newBlockingStub(channel);
        blockingStub2 = JokeGrpc.newBlockingStub(channel);
        flowersStub = FlowersGrpc.newBlockingStub(channel);
        followStub = FollowGrpc.newBlockingStub(channel);
        triviaStub = TriviaMasterGrpc.newBlockingStub(channel);
    }

    @org.junit.After
    public void close() throws Exception {
        System.out.println("Shutting down test environment...");

        if (server != null) {
            server.shutdownNow();
            if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("Server did not terminate in the specified time.");
            }
            server = null;
        }

        if (channel != null) {
            channel.shutdownNow();
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("Channel did not terminate in the specified time.");
            }
            channel = null;
        }
    }

    @Test
    public void testParrot() {
        System.out.println("\n=== Testing Echo Service ===");

        // Success case: Valid message
        System.out.println("Sending valid message to Echo service...");
        ClientRequest request = ClientRequest.newBuilder().setMessage("test").build();
        ServerResponse response = blockingStub.parrot(request);
        System.out.println("Sent message: 'test'");
        System.out.println("Received response: Success=" + response.getIsSuccess() + ", Message='" + response.getMessage() + "'");
        assertTrue("Expected success for valid message", response.getIsSuccess());
        assertEquals("Echo: test", response.getMessage());

        // Error case: Missing message
        System.out.println("\nSending request with missing message (null)...");
        request = ClientRequest.newBuilder().build();
        response = blockingStub.parrot(request);
        System.out.println("Sent message: <missing>");
        System.out.println("Received response: Success=" + response.getIsSuccess() + ", Error='" + response.getError() + "'");
        assertFalse("Expected failure for missing message", response.getIsSuccess());
        assertEquals("No message provided", response.getError());

        // Error case: Empty message
        System.out.println("\nSending empty message to Echo service...");
        request = ClientRequest.newBuilder().setMessage("").build();
        response = blockingStub.parrot(request);
        System.out.println("Sent message: ''");
        System.out.println("Received response: Success=" + response.getIsSuccess() + ", Error='" + response.getError() + "'");
        assertFalse("Expected failure for empty message", response.getIsSuccess());
        assertEquals("No message provided", response.getError());
    }


    @Test
    public void joke() {
        System.out.println("\n=== Testing Joke Service ===");

        // Getting first joke
        System.out.println("Requesting 1 joke...");
        JokeReq request = JokeReq.newBuilder().setNumber(1).build();
        JokeRes response = blockingStub2.getJoke(request);
        System.out.println("Received jokes:");
        response.getJokeList().forEach(joke -> System.out.println("  -> " + joke));
        assertEquals("Expected 1 joke", 1, response.getJokeCount());
        assertEquals("Did you hear the rumor about butter? Well, I'm not going to spread it!", response.getJoke(0));

        // Getting next 3 jokes
        System.out.println("Requesting 3 jokes...");
        request = JokeReq.newBuilder().setNumber(3).build();
        response = blockingStub2.getJoke(request);
        System.out.println("Received jokes:");
        response.getJokeList().forEach(joke -> System.out.println("  -> " + joke));
        assertEquals("Expected 3 jokes", 3, response.getJokeCount());
        assertEquals("What do you call someone with no body and no nose? Nobody knows.", response.getJoke(1));
        assertEquals("I don't trust stairs. They're always up to something.", response.getJoke(2));

        // Getting 5 jokes, exhausting the server stack
        System.out.println("Requesting 5 jokes (exceeding available jokes)...");
        request = JokeReq.newBuilder().setNumber(5).build();
        response = blockingStub2.getJoke(request);
        System.out.println("Received jokes:");
        response.getJokeList().forEach(joke -> System.out.println("  -> " + joke));
        assertEquals("Expected 5 jokes", 5, response.getJokeCount());
        assertEquals("How do you get a squirrel to like you? Act like a nut.", response.getJoke(3));
        assertEquals("I am out of jokes...", response.getJoke(4));

        // Adding a joke without a joke field
        System.out.println("Attempting to add a joke with no joke field...");
        JokeSetReq req2 = JokeSetReq.newBuilder().build();
        JokeSetRes res2 = blockingStub2.setJoke(req2);
        System.out.println("Add joke response: " + res2.getMessage());
        assertFalse("Expected failure for missing joke field", res2.getOk());

        // Adding an empty joke
        System.out.println("Attempting to add an empty joke...");
        req2 = JokeSetReq.newBuilder().setJoke("").build();
        res2 = blockingStub2.setJoke(req2);
        System.out.println("Add joke response: " + res2.getMessage());
        assertFalse("Expected failure for empty joke", res2.getOk());

        // Adding a valid joke
        System.out.println("Adding a valid joke...");
        req2 = JokeSetReq.newBuilder().setJoke("whoop").build();
        res2 = blockingStub2.setJoke(req2);
        System.out.println("Add joke response: " + res2.getMessage());
        assertTrue("Expected success for valid joke", res2.getOk());

        // Fetching the newly added joke
        System.out.println("Requesting 1 joke (should include the new joke)...");
        request = JokeReq.newBuilder().setNumber(1).build();
        response = blockingStub2.getJoke(request);
        System.out.println("Received jokes:");
        response.getJokeList().forEach(joke -> System.out.println("  -> " + joke));
        assertEquals("Expected 1 joke", 1, response.getJokeCount());
        assertEquals("whoop", response.getJoke(0));
    }

    @Test
    public void flowers() {
        System.out.println("\n=== Testing Flowers Service ===");

        // Instantiate the FlowersImpl with time manipulation enabled
        FlowersImpl flowersService = new FlowersImpl();
        FlowersGrpc.FlowersBlockingStub flowersStub = FlowersGrpc.newBlockingStub(channel);

        // Test: Plant a new flower
        System.out.println("Planting a new flower...");
        FlowerReq plantReq = FlowerReq.newBuilder()
                .setName("Rose")
                .setWaterTimes(3)
                .setBloomTime(1) // Bloom time is in minutes for testing
                .build();
        FlowerRes plantRes = flowersStub.plantFlower(plantReq);
        System.out.println("Response: " + (plantRes.getIsSuccess() ? "Success" : "Error: " + plantRes.getError()));
        assertTrue("Expected success when planting a valid flower", plantRes.getIsSuccess());

        // Test: View flowers
        System.out.println("Viewing all flowers...");
        FlowerViewRes viewRes = flowersStub.viewFlowers(com.google.protobuf.Empty.getDefaultInstance());
        System.out.println("Flowers in the garden:");
        viewRes.getFlowersList().forEach(flower -> System.out.println("  -> " + flower));
        assertTrue("Expected success when viewing flowers", viewRes.getIsSuccess());
        assertEquals("Expected one flower in the garden", 1, viewRes.getFlowersCount());
        assertEquals("Rose", viewRes.getFlowers(0).getName());

        // Test: Water the flower
        System.out.println("Watering the flower 'Rose'...");
        FlowerCare waterReq = FlowerCare.newBuilder().setName("Rose").build();
        WaterRes waterRes = flowersStub.waterFlower(waterReq);
        System.out.println("Response: " + (waterRes.getIsSuccess() ? "Success" : "Error: " + waterRes.getError()));
        assertTrue("Expected success when watering a planted flower", waterRes.getIsSuccess());

        // Test: Water the flower until it blooms
        System.out.println("Watering the flower again to trigger blooming...");
        flowersStub.waterFlower(waterReq); // Second watering
        flowersStub.waterFlower(waterReq); // Third watering
        FlowerViewRes updatedViewRes = flowersStub.viewFlowers(com.google.protobuf.Empty.getDefaultInstance());
        Flower updatedFlower = updatedViewRes.getFlowersList().get(0);
        assertEquals("Expected flower to transition to BLOOMING state", State.BLOOMING, updatedFlower.getFlowerState());

        // Test: Care for a blooming flower
        System.out.println("Caring for the blooming flower 'Rose'...");
        CareRes careRes = flowersStub.careForFlower(waterReq);
        System.out.println("Response: " + (careRes.getIsSuccess() ? "Success: Bloom Time = " + careRes.getBloomTime() : "Error: " + careRes.getError()));
        assertTrue("Expected success when caring for a blooming flower", careRes.getIsSuccess());

        // Test: Care for a flower that is not blooming
        System.out.println("Attempting to care for a non-blooming flower...");
        FlowerReq daisyReq = FlowerReq.newBuilder()
                .setName("Daisy")
                .setWaterTimes(1)
                .setBloomTime(3)
                .build();
        flowersStub.plantFlower(daisyReq);
        FlowerCare daisyCareReq = FlowerCare.newBuilder().setName("Daisy").build();
        CareRes invalidCareRes = flowersStub.careForFlower(daisyCareReq);
        System.out.println("Response: " + (invalidCareRes.getIsSuccess() ? "Success" : "Error: " + invalidCareRes.getError()));
        assertFalse("Expected failure when caring for a non-blooming flower", invalidCareRes.getIsSuccess());

        // Test: View flowers after bloom expires
        System.out.println("Simulating expired bloom...");
        FlowerViewRes finalViewRes = flowersStub.viewFlowers(com.google.protobuf.Empty.getDefaultInstance());
        Flower finalFlower = finalViewRes.getFlowersList().get(0);

        // Assert state transitions to DEAD after bloom time expires
        assertEquals("Expected flower to transition to DEAD state after bloom expires", State.DEAD, finalFlower.getFlowerState());
    }

    @Test
    public void follow() {
        System.out.println("\n=== Testing Follow Service ===");

        // Test: Add a user
        System.out.println("Adding user 'Alice'...");
        UserReq addUserReq = UserReq.newBuilder().setName("Alice").build();
        UserRes addUserRes = followStub.addUser(addUserReq);
        assertTrue("Expected success when adding a new user", addUserRes.getIsSuccess());

        // Test: Add another user
        System.out.println("Adding user 'Bob'...");
        addUserReq = UserReq.newBuilder().setName("Bob").build();
        addUserRes = followStub.addUser(addUserReq);
        assertTrue("Expected success when adding a new user", addUserRes.getIsSuccess());

        // Test: Add an existing user
        System.out.println("Adding user 'Alice' again...");
        addUserRes = followStub.addUser(UserReq.newBuilder().setName("Alice").build());
        assertFalse("Expected failure when adding an existing user", addUserRes.getIsSuccess());
        assertEquals("User 'Alice' already exists.", addUserRes.getError());

        // Test: Follow a user
        System.out.println("'Alice' follows 'Bob'...");
        UserReq followReq = UserReq.newBuilder().setName("Alice").setFollowName("Bob").build();
        UserRes followRes = followStub.follow(followReq);
        assertTrue("Expected success when following a user", followRes.getIsSuccess());

        // Test: Follow a non-existent user
        System.out.println("'Alice' tries to follow 'Charlie'...");
        followReq = UserReq.newBuilder().setName("Alice").setFollowName("Charlie").build();
        followRes = followStub.follow(followReq);
        assertFalse("Expected failure when following a non-existent user", followRes.getIsSuccess());
        assertEquals("User 'Charlie' is not registered.", followRes.getError());

        // Test: View following list
        System.out.println("Viewing who 'Alice' is following...");
        UserReq viewFollowingReq = UserReq.newBuilder().setName("Alice").build();
        UserRes viewFollowingRes = followStub.viewFollowing(viewFollowingReq);
        assertTrue("Expected success when viewing following list", viewFollowingRes.getIsSuccess());
        assertTrue("Expected 'Alice' to be following 'Bob'", viewFollowingRes.getFollowedUserList().contains("Bob"));

        // Test: View following list for a user not following anyone
        System.out.println("Viewing who 'Bob' is following...");
        viewFollowingReq = UserReq.newBuilder().setName("Bob").build();
        viewFollowingRes = followStub.viewFollowing(viewFollowingReq);
        assertTrue("Expected success when viewing following list", viewFollowingRes.getIsSuccess());
        assertEquals("Expected 'Bob' to not be following anyone", 0, viewFollowingRes.getFollowedUserList().size());

        // Test: View following for a non-existent user
        System.out.println("Viewing who 'Charlie' is following...");
        viewFollowingReq = UserReq.newBuilder().setName("Charlie").build();
        viewFollowingRes = followStub.viewFollowing(viewFollowingReq);
        assertFalse("Expected failure when viewing following list for a non-existent user", viewFollowingRes.getIsSuccess());
        assertEquals("User 'Charlie' is not registered.", viewFollowingRes.getError());
    }

    @Test
    public void trivia() {
        System.out.println("\n=== Testing TriviaMaster Service ===");

        // Instantiate the TriviaImpl with preloaded questions
        TriviaMasterGrpc.TriviaMasterBlockingStub triviaStub = TriviaMasterGrpc.newBlockingStub(channel);

        // Test: Get preloaded trivia questions
        System.out.println("Retrieving all preloaded trivia questions...");
        GetQuestionReq getAllQuestionsReq = GetQuestionReq.newBuilder()
                .setRandom(false)
                .setCount(10) // Arbitrary number larger than the preloaded count
                .build();
        GetQuestionRes getAllQuestionsRes = triviaStub.getQuestion(getAllQuestionsReq);

        System.out.println("Questions retrieved:");
        getAllQuestionsRes.getQuestionsList().forEach(q -> System.out.println("  -> " + q));
        assertEquals("Expected 4 preloaded questions", 4, getAllQuestionsRes.getQuestionsCount());

        // Test: Add a new trivia question
        System.out.println("Adding a new trivia question...");
        AddQuestionReq addQuestionReq = AddQuestionReq.newBuilder()
                .setQuestion("What is the largest planet in our solar system?")
                .setAnswer("Jupiter")
                .build();
        AddQuestionRes addQuestionRes = triviaStub.addQuestion(addQuestionReq);
        assertTrue("Expected success when adding a new trivia question", addQuestionRes.getIsSuccess());
        assertEquals("Question added successfully.", addQuestionRes.getMessage());

        // Test: Retrieve a random trivia question
        System.out.println("Retrieving a random trivia question...");
        GetQuestionReq randomQuestionReq = GetQuestionReq.newBuilder()
                .setRandom(true)
                .build();
        GetQuestionRes randomQuestionRes = triviaStub.getQuestion(randomQuestionReq);
        System.out.println("Random Question: " + randomQuestionRes.getQuestions(0));
        assertEquals("Expected 1 random question", 1, randomQuestionRes.getQuestionsCount());

        // Test: Answer a trivia question correctly
        System.out.println("Answering a trivia question correctly...");
        AnswerQuestionReq correctAnswerReq = AnswerQuestionReq.newBuilder()
                .setQuestion("What is the largest planet in our solar system?")
                .setAnswer("Jupiter")
                .build();
        AnswerQuestionRes correctAnswerRes = triviaStub.answerQuestion(correctAnswerReq);
        assertTrue("Expected correct answer to be acknowledged", correctAnswerRes.getIsCorrect());
        assertEquals("Correct! Well done.", correctAnswerRes.getMessage());

        // Test: Answer a trivia question incorrectly
        System.out.println("Answering a trivia question incorrectly...");
        AnswerQuestionReq incorrectAnswerReq = AnswerQuestionReq.newBuilder()
                .setQuestion("What is the largest planet in our solar system?")
                .setAnswer("Mars")
                .build();
        AnswerQuestionRes incorrectAnswerRes = triviaStub.answerQuestion(incorrectAnswerReq);
        assertFalse("Expected incorrect answer to be acknowledged", incorrectAnswerRes.getIsCorrect());
        assertEquals("Incorrect. The correct answer is: Jupiter", incorrectAnswerRes.getMessage());

        // Test: Add duplicate trivia question
        System.out.println("Adding a duplicate trivia question...");
        AddQuestionRes duplicateAddQuestionRes = triviaStub.addQuestion(addQuestionReq);
        assertFalse("Expected failure when adding duplicate trivia question", duplicateAddQuestionRes.getIsSuccess());
        assertEquals("Question already exists.", duplicateAddQuestionRes.getMessage());

        // Test: Answer a non-existent trivia question
        System.out.println("Answering a non-existent trivia question...");
        AnswerQuestionReq nonExistentAnswerReq = AnswerQuestionReq.newBuilder()
                .setQuestion("What is the square root of -1?")
                .setAnswer("Imaginary")
                .build();
        AnswerQuestionRes nonExistentAnswerRes = triviaStub.answerQuestion(nonExistentAnswerReq);
        assertFalse("Expected failure when answering a non-existent trivia question", nonExistentAnswerRes.getIsCorrect());
        assertEquals("Question not found.", nonExistentAnswerRes.getMessage());
    }


}