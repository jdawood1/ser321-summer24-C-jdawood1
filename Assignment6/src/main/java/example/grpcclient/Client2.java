package example.grpcclient;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import service.*;
import com.google.protobuf.Empty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Client2 that dynamically connects to services through the Registry.
 */
public class Client2 {
    private final RegistryGrpc.RegistryBlockingStub registryStub;

    // Constructor for service and registry channels
    public Client2(Channel serviceChannel, Channel registryChannel) {
        this.registryStub = RegistryGrpc.newBlockingStub(registryChannel);
    }

    // Fetch all available services from Registry
    public void getServices() {
        GetServicesReq request = GetServicesReq.newBuilder().build();
        ServicesListRes response;
        try {
            response = registryStub.getServices(request);
            if (response.getIsSuccess()) {
                System.out.println("Available services from Registry:");
                response.getServicesList().forEach(System.out::println);
            } else {
                System.out.println("Error fetching services: " + response.getError());
            }
        } catch (Exception e) {
            System.err.println("RPC failed while fetching services: " + e.getMessage());
        }
    }

    // Dynamic selection based on user input
    public void invokeServiceDynamically(String serviceName, String userInput) {
        // Find server providing this service
        FindServerReq findRequest = FindServerReq.newBuilder().setServiceName(serviceName).build();
        SingleServerRes serverResponse;
        try {
            serverResponse = registryStub.findServer(findRequest);
            if (serverResponse.getIsSuccess()) {
                Connection connection = serverResponse.getConnection();
                System.out.printf("Connecting to %s at %s:%d%n", serviceName, connection.getUri(), connection.getPort());

                // Create new channel with the discovered service node
                ManagedChannel serviceChannel = ManagedChannelBuilder.forAddress(connection.getUri(), connection.getPort()).usePlaintext().build();

                // Invoke the appropriate service based on serviceName
                switch (serviceName) {
                    case "services.Echo/parrot":
                        EchoGrpc.EchoBlockingStub echoStub = EchoGrpc.newBlockingStub(serviceChannel);
                        ClientRequest echoRequest = ClientRequest.newBuilder().setMessage(userInput.trim()).build();
                        ServerResponse echoResponse = echoStub.parrot(echoRequest);
                        System.out.println("Response from Echo service: " + (echoResponse.getIsSuccess() ? echoResponse.getMessage() : echoResponse.getError()));
                        break;

                    case "services.Joke/getJoke":
                        JokeGrpc.JokeBlockingStub jokeStub = JokeGrpc.newBlockingStub(serviceChannel);
                        try {
                            int numJokes = Integer.parseInt(userInput.trim());
                            JokeReq jokeRequest = JokeReq.newBuilder().setNumber(numJokes).build();
                            JokeRes jokeResponse = jokeStub.getJoke(jokeRequest);
                            System.out.println("Jokes:");
                            jokeResponse.getJokeList().forEach(joke -> System.out.println(" - " + joke));
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input for number of jokes. Please enter a valid integer.");
                        }
                        break;

                    case "services.Joke/setJoke":
                        JokeGrpc.JokeBlockingStub jokeStubSet = JokeGrpc.newBlockingStub(serviceChannel);
                        JokeSetReq jokeSetRequest = JokeSetReq.newBuilder().setJoke(userInput.trim()).build();
                        JokeSetRes jokeSetResponse = jokeStubSet.setJoke(jokeSetRequest);
                        System.out.println("Set Joke Response: " + jokeSetResponse.getMessage());
                        break;

                    case "services.Flowers/plantFlower":
                        FlowersGrpc.FlowersBlockingStub flowerStub = FlowersGrpc.newBlockingStub(serviceChannel);
                        String[] flowerParams = userInput.split(" ");
                        if (flowerParams.length == 3) {
                            try {
                                String flowerName = flowerParams[0];
                                int waterTimes = Integer.parseInt(flowerParams[1]);
                                int bloomTime = Integer.parseInt(flowerParams[2]);
                                FlowerReq flowerRequest = FlowerReq.newBuilder()
                                        .setName(flowerName)
                                        .setWaterTimes(waterTimes)
                                        .setBloomTime(bloomTime)
                                        .build();
                                FlowerRes flowerResponse = flowerStub.plantFlower(flowerRequest);
                                System.out.println("Flower Response: " + (flowerResponse.getIsSuccess() ? flowerResponse.getMessage() : flowerResponse.getError()));
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid input. Please ensure water times and bloom time are integers.");
                            }
                        } else {
                            System.out.println("Invalid input format. Please provide: <name> <waterTimes> <bloomTime>");
                        }
                        break;

                    case "services.Flowers/careForFlower":
                        FlowersGrpc.FlowersBlockingStub flowerStubCare = FlowersGrpc.newBlockingStub(serviceChannel);
                        FlowerCare careRequest = FlowerCare.newBuilder().setName(userInput.trim()).build();
                        CareRes careResponse = flowerStubCare.careForFlower(careRequest);
                        System.out.println(careResponse.getIsSuccess() ? careResponse.getMessage() : "Error: " + careResponse.getError());
                        break;

                    case "services.Flowers/viewFlowers":
                        FlowersGrpc.FlowersBlockingStub flowerStubView = FlowersGrpc.newBlockingStub(serviceChannel);
                        FlowerViewRes viewRes = flowerStubView.viewFlowers(Empty.newBuilder().build());
                        if (viewRes.getIsSuccess()) {
                            System.out.println("Flowers in your garden:");
                            viewRes.getFlowersList().forEach(flower ->
                                    System.out.printf("  -> %s (State: %s, Water Times: %d, Bloom Time: %d hours)%n",
                                            flower.getName(), flower.getFlowerState(), flower.getWaterTimes(), flower.getBloomTime()));
                        } else {
                            System.out.println("Error: " + viewRes.getError());
                        }
                        break;

                    case "services.Flowers/waterFlower":
                        FlowersGrpc.FlowersBlockingStub flowerStubWater = FlowersGrpc.newBlockingStub(serviceChannel);
                        FlowerCare waterRequest = FlowerCare.newBuilder().setName(userInput.trim()).build();
                        WaterRes waterRes = flowerStubWater.waterFlower(waterRequest);
                        System.out.println(waterRes.getIsSuccess() ? waterRes.getMessage() : "Error: " + waterRes.getError());
                        break;

                    case "services.Follow/viewFollowing":
                        FollowGrpc.FollowBlockingStub followStubView = FollowGrpc.newBlockingStub(serviceChannel);
                        UserReq viewFollowingReq = UserReq.newBuilder().setName(userInput.trim()).build();
                        UserRes viewFollowingRes = followStubView.viewFollowing(viewFollowingReq);
                        if (viewFollowingRes.getIsSuccess()) {
                            System.out.println("You are following: " + viewFollowingRes.getFollowedUserList());
                        } else {
                            System.out.println("Error: " + viewFollowingRes.getError());
                        }
                        break;

                    case "services.Follow/addUser":
                        FollowGrpc.FollowBlockingStub followStubAdd = FollowGrpc.newBlockingStub(serviceChannel);
                        UserReq addUserReq = UserReq.newBuilder().setName(userInput.trim()).build();
                        UserRes addUserRes = followStubAdd.addUser(addUserReq);
                        System.out.println(addUserRes.getIsSuccess() ? "User added successfully!" : "Error: " + addUserRes.getError());
                        break;

                    case "services.Follow/follow":
                        FollowGrpc.FollowBlockingStub followStubFollow = FollowGrpc.newBlockingStub(serviceChannel);
                        String[] followParams = userInput.split(" ");
                        if (followParams.length == 2) {
                            UserReq followReq = UserReq.newBuilder().setName(followParams[0]).setFollowName(followParams[1]).build();
                            UserRes followRes = followStubFollow.follow(followReq);
                            System.out.println(followRes.getIsSuccess() ? "Followed successfully!" : "Error: " + followRes.getError());
                        } else {
                            System.out.println("Invalid input format. Please provide: <follower> <followee>");
                        }
                        break;

                    case "services.TriviaMaster/answerQuestion":
                        TriviaMasterGrpc.TriviaMasterBlockingStub triviaStubAnswer = TriviaMasterGrpc.newBlockingStub(serviceChannel);
                        String[] triviaAnswerParams = userInput.split("\\|", 2);
                        if (triviaAnswerParams.length == 2) {
                            AnswerQuestionReq answerReq = AnswerQuestionReq.newBuilder()
                                    .setQuestion(triviaAnswerParams[0].trim())
                                    .setAnswer(triviaAnswerParams[1].trim())
                                    .build();
                            AnswerQuestionRes answerRes = triviaStubAnswer.answerQuestion(answerReq);
                            System.out.println(answerRes.getIsCorrect() ? "Correct! " + answerRes.getMessage() : "Incorrect! " + answerRes.getMessage());
                        } else {
                            System.out.println("Invalid input format. Please provide: <question>|<answer>");
                        }
                        break;

                    case "services.TriviaMaster/getQuestion":
                        TriviaMasterGrpc.TriviaMasterBlockingStub triviaStubGet = TriviaMasterGrpc.newBlockingStub(serviceChannel);
                        try {
                            int questionCount = Integer.parseInt(userInput.trim());
                            GetQuestionReq getQuestionReq = GetQuestionReq.newBuilder().setRandom(true).setCount(questionCount).build();
                            GetQuestionRes getQuestionRes = triviaStubGet.getQuestion(getQuestionReq);
                            System.out.println("Trivia Questions:");
                            getQuestionRes.getQuestionsList().forEach(question -> System.out.println("  -> " + question));
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input for number of questions. Please enter a valid integer.");
                        }
                        break;

                    case "services.TriviaMaster/addQuestion":
                        TriviaMasterGrpc.TriviaMasterBlockingStub triviaStubAdd = TriviaMasterGrpc.newBlockingStub(serviceChannel);
                        String[] triviaParams = userInput.split("\\|", 2);
                        if (triviaParams.length == 2) {
                            AddQuestionReq addQuestionReq = AddQuestionReq.newBuilder()
                                    .setQuestion(triviaParams[0].trim())
                                    .setAnswer(triviaParams[1].trim())
                                    .build();
                            AddQuestionRes addQuestionRes = triviaStubAdd.addQuestion(addQuestionReq);
                            System.out.println(addQuestionRes.getIsSuccess() ? "Success: " + addQuestionRes.getMessage() : "Error: " + addQuestionRes.getMessage());
                        } else {
                            System.out.println("Invalid input format. Please provide: <question>|<answer>");
                        }
                        break;

                    default:
                        System.out.println("Service invocation not implemented for: " + serviceName);
                        break;
                }

                // Clean up
                serviceChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            } else {
                System.out.println("Service not found: " + serverResponse.getError());
            }
        } catch (Exception e) {
            System.err.println("Failed to find or connect to the service: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            System.out.println("Expected arguments: <host> <port> <regHost> <regPort> <message> <regOn>");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String regHost = args[2];
        int regPort = Integer.parseInt(args[3]);
        boolean regOn = Boolean.parseBoolean(args[5]);

        ManagedChannel serviceChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        ManagedChannel registryChannel = regOn ? ManagedChannelBuilder.forAddress(regHost, regPort).usePlaintext().build() : null;

        try {
            Client2 client = new Client2(serviceChannel, registryChannel);

            if (regOn) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String selectedService;
                do {
                    // Step 3: Get available services from the Registry
                    client.getServices();

                    // Prompt user to select a service
                    System.out.println("Enter the name of the service you want to use (or type 'exit' to quit):");
                    selectedService = reader.readLine().trim();

                    if (!selectedService.equalsIgnoreCase("exit")) {
                        // Prompt for user input based on the service selected
                        System.out.println("Enter input for the selected service:");
                        String userInput = reader.readLine().trim();

                        // Step 4: Invoke the service dynamically
                        client.invokeServiceDynamically(selectedService, userInput);
                    }
                } while (!selectedService.equalsIgnoreCase("exit"));
            } else {
                System.out.println("Registry is not enabled.");
            }
        } finally {
            serviceChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            if (registryChannel != null) {
                registryChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            }
        }
    }
}
