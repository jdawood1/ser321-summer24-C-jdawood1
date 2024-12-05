package example.grpcclient;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import service.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import com.google.protobuf.Empty; // needed to use Empty


/**
 * Client that requests `parrot` method from the `EchoServer`.
 */
public class Client {
  private final EchoGrpc.EchoBlockingStub blockingStub;
  private final JokeGrpc.JokeBlockingStub blockingStub2;
  private final RegistryGrpc.RegistryBlockingStub blockingStub3;
  private final RegistryGrpc.RegistryBlockingStub blockingStub4;
  private final FlowersGrpc.FlowersBlockingStub flowersStub;
  private final FollowGrpc.FollowBlockingStub followStub;
  private final TriviaMasterGrpc.TriviaMasterBlockingStub triviaStub;


  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel, Channel regChannel) {
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = RegistryGrpc.newBlockingStub(regChannel);
    blockingStub4 = RegistryGrpc.newBlockingStub(channel);

    flowersStub = FlowersGrpc.newBlockingStub(channel);
    followStub = FollowGrpc.newBlockingStub(channel);
    triviaStub = TriviaMasterGrpc.newBlockingStub(channel);
  }

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel) {
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = null;
    blockingStub4 = null;

    flowersStub = FlowersGrpc.newBlockingStub(channel);
    followStub = FollowGrpc.newBlockingStub(channel);
    triviaStub = TriviaMasterGrpc.newBlockingStub(channel);
  }

  public void askServerToParrot(String message) {
    ClientRequest request = ClientRequest.newBuilder().setMessage(message).build();
    ServerResponse response;
    try {
      response = blockingStub.parrot(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }
    if (response.getIsSuccess()) {
      System.out.println("Received from server: " + response.getMessage());
    } else {
      System.out.println("Error: " + response.getError());
    }
  }


  public void askForJokes(int num) {
    JokeReq request = JokeReq.newBuilder().setNumber(num).build();
    JokeRes response;

    // just to show how to use the empty in the protobuf protocol
    Empty empt = Empty.newBuilder().build();

    try {
      response = blockingStub2.getJoke(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
    System.out.println("Your jokes: ");
    for (String joke : response.getJokeList()) {
      System.out.println("--- " + joke);
    }
  }

  public void setJoke(String joke) {
    JokeSetReq request = JokeSetReq.newBuilder().setJoke(joke).build();
    JokeSetRes response;

    try {
      response = blockingStub2.setJoke(request);
      System.out.println(response.getOk());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void getNodeServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub4.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void getServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub3.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServer(String name) {
    FindServerReq request = FindServerReq.newBuilder().setServiceName(name).build();
    SingleServerRes response;
    try {
      response = blockingStub3.findServer(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServers(String name) {
    FindServersReq request = FindServersReq.newBuilder().setServiceName(name).build();
    ServerListRes response;
    try {
      response = blockingStub3.findServers(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void plantFlower(String name, int waterTimes, int bloomTime) {
    FlowerReq request = FlowerReq.newBuilder()
            .setName(name)
            .setWaterTimes(waterTimes)
            .setBloomTime(bloomTime)
            .build();
    FlowerRes response;
    try {
      response = flowersStub.plantFlower(request);
      if (response.getIsSuccess()) {
        System.out.println(response.getMessage());
      } else {
        System.out.println("Error: " + response.getError());
      }
    } catch (Exception e) {
      System.out.println("Failed to plant flower: " + e.getMessage());
    }
  }


  public void viewFlowers() {
    FlowerViewRes response;
    try {
      response = flowersStub.viewFlowers(com.google.protobuf.Empty.newBuilder().build());
      if (response.getIsSuccess()) {
        System.out.println("Flowers in your garden:");
        for (Flower flower : response.getFlowersList()) {
          System.out.println("  -> " + flower.getName() +
                  " (State: " + flower.getFlowerState() +
                  ", Water Times: " + flower.getWaterTimes() +
                  ", Bloom Time: " + flower.getBloomTime() + " hours)");
        }
      } else {
        System.out.println("Error: " + response.getError());
      }
    } catch (Exception e) {
      System.out.println("Failed to view flowers: " + e.getMessage());
    }
  }


  public void waterFlower(String name) {
    FlowerCare request = FlowerCare.newBuilder().setName(name).build();
    WaterRes response;
    try {
      response = flowersStub.waterFlower(request);
      if (response.getIsSuccess()) {
        System.out.println(response.getMessage());
      } else {
        System.out.println("Error: " + response.getError());
      }
    } catch (Exception e) {
      System.out.println("Failed to water flower: " + e.getMessage());
    }
  }

  public void addUser(String name) {
    UserReq req = UserReq.newBuilder().setName(name).build();
    UserRes res = followStub.addUser(req);
    if (res.getIsSuccess()) {
      System.out.println("User added successfully!");
    } else {
      System.out.println("Error: " + res.getError());
    }
  }

  public void followUser(String follower, String followee) {
    UserReq req = UserReq.newBuilder().setName(follower).setFollowName(followee).build();
    UserRes res = followStub.follow(req);
    if (res.getIsSuccess()) {
      System.out.println("Followed successfully!");
    } else {
      System.out.println("Error: " + res.getError());
    }
  }

  public void viewFollowing(String name) {
    UserReq req = UserReq.newBuilder().setName(name).build();
    UserRes res = followStub.viewFollowing(req);
    if (res.getIsSuccess()) {
      System.out.println("You are following: " + res.getFollowedUserList());
    } else {
      System.out.println("Error: " + res.getError());
    }
  }

  public void careForFlower(String name) {
    FlowerCare request = FlowerCare.newBuilder().setName(name).build();
    CareRes response;
    try {
      response = flowersStub.careForFlower(request);
      if (response.getIsSuccess()) {
        System.out.println(response.getMessage() + " New bloom time: " + response.getBloomTime() + " hours.");
      } else {
        System.out.println("Error: " + response.getError());
      }
    } catch (Exception e) {
      System.out.println("Failed to care for flower: " + e.getMessage());
    }
  }

  public void addTriviaQuestion(String question, String answer) {
    // Build the request
    AddQuestionReq req = AddQuestionReq.newBuilder()
            .setQuestion(question)
            .setAnswer(answer)
            .build();

    // Call the server
    AddQuestionRes res = triviaStub.addQuestion(req);

    // Handle the response
    if (res.getIsSuccess()) {
      System.out.println("Success: " + res.getMessage());
    } else {
      System.out.println("Error: " + res.getMessage());
    }
  }

  public void getTriviaQuestions(boolean random, int count) {
    // Build the request
    GetQuestionReq req = GetQuestionReq.newBuilder()
            .setRandom(random)
            .setCount(count)
            .build();

    // Call the server
    GetQuestionRes res = triviaStub.getQuestion(req);

    // Handle the response
    if (res.getQuestionsList().isEmpty() || res.getQuestions(0).equals("No questions available.")) {
      System.out.println("No questions available.");
    } else {
      System.out.println("Trivia Questions:");
      for (String question : res.getQuestionsList()) {
        System.out.println("  -> " + question);
      }
    }
  }

  public void answerTriviaQuestion(String question, String answer) {
    // Build the request
    AnswerQuestionReq req = AnswerQuestionReq.newBuilder()
            .setQuestion(question)
            .setAnswer(answer)
            .build();

    // Call the server
    AnswerQuestionRes res = triviaStub.answerQuestion(req);

    // Handle the response
    if (res.getIsCorrect()) {
      System.out.println("Correct! " + res.getMessage());
    } else {
      System.out.println("Incorrect! " + res.getMessage());
    }
  }

  private static String promptUser(String message, BufferedReader reader) throws IOException {
    System.out.print(message);
    return reader.readLine().trim();
  }


  public static void main(String[] args) throws Exception {
    if (args.length != 6) {
      System.out.println("Expected arguments: <host(String)> <port(int)> <regHost(string)> <regPort(int)> <message(String)> <regOn(bool)>");
      System.exit(1);
    }

    // Parse arguments
    int port = 9099;
    int regPort = 9003;
    String host = args[0];
    String regHost = args[2];
    String message = args[4];

    try {
      port = Integer.parseInt(args[1]);
      regPort = Integer.parseInt(args[3]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port] must be an integer");
      System.exit(2);
    }

    // Create communication channels
    String target = host + ":" + port;
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
            .usePlaintext() // Disable TLS for simplicity
            .build();

    String regTarget = regHost + ":" + regPort;
    ManagedChannel regChannel = ManagedChannelBuilder.forTarget(regTarget)
            .usePlaintext()
            .build();

    try {
      // Create the client
      Client client = new Client(channel, regChannel);

      // BufferedReader for user input
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

      while (true) {
        System.out.println("Choose a service:");
        System.out.println("1. Echo Service");
        System.out.println("2. Joke Service");
        System.out.println("3. Flowers Service");
        System.out.println("4. Follow Service");
        System.out.println("5. Trivia Master Service");
        System.out.println("6. Exit");

        // Read the user's choice
        String choice = reader.readLine();

        switch (choice) {
          case "1": // Echo Service
            System.out.print("Enter a message to echo: ");
            String echoMessage = reader.readLine();
            client.askServerToParrot(echoMessage);
            break;

          case "2": // Joke Service
            System.out.println("Joke Service Options:");
            System.out.println("1. Get Jokes");
            System.out.println("2. Add a Joke");

            String jokeChoice = reader.readLine();

            if ("1".equals(jokeChoice)) { // Get Jokes
              System.out.print("How many jokes would you like? ");
              try {
                int numJokes = Integer.parseInt(reader.readLine());
                client.askForJokes(numJokes);
              } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please enter a valid integer.");
              }
            } else if ("2".equals(jokeChoice)) { // Add a Joke
              System.out.print("Enter a joke to add: ");
              String newJoke = reader.readLine();
              client.setJoke(newJoke);
            } else {
              System.out.println("Invalid choice. Please select 1 or 2.");
            }
            break;

          case "3": // Flowers Service
            System.out.println("Flowers Service Options:");
            System.out.println("1. Plant a Flower");
            System.out.println("2. View Flowers");
            System.out.println("3. Water a Flower");
            System.out.println("4. Care for a Flower");
            System.out.println("5. Flower Info");
            System.out.println("6. Back to Main Menu");

            String flowerChoice = reader.readLine();

            switch (flowerChoice) {
              case "1": // Plant a Flower
                System.out.print("Enter flower name: ");
                String name = reader.readLine();
                System.out.print("Enter water times (1-6): ");
                int waterTimes = Integer.parseInt(reader.readLine());
                System.out.print("Enter bloom time (1-6 hours): ");
                int bloomTime = Integer.parseInt(reader.readLine());
                client.plantFlower(name, waterTimes, bloomTime);
                break;

              case "2": // View Flowers
                client.viewFlowers();
                break;

              case "3": // Water a Flower
                System.out.print("Enter the name of the flower to water: ");
                String flowerToWater = reader.readLine();
                client.waterFlower(flowerToWater);
                break;

              case "4": // Care for a Flower
                System.out.print("Enter the name of the flower to care for: ");
                String flowerToCare = reader.readLine();
                client.careForFlower(flowerToCare);
                break;

              case "5": // Flower Info
                System.out.println("\n=== Flower Info ===");
                System.out.println("1. Water Times:");
                System.out.println("   - The number of times a flower needs to be watered before it blooms.");
                System.out.println("   - For example, if Water Times is 3, you need to water the flower 3 times to transition it from PLANTED to BLOOMING.");
                System.out.println("\n2. Bloom Time:");
                System.out.println("   - The duration (in minutes for testing) that the flower remains in the BLOOMING state before it transitions to DEAD.");
                System.out.println("   - Caring for the flower while it is BLOOMING will extend its Bloom Time. (Currently reduces the time just for testing.)");
                System.out.println("\n3. State Transitions:");
                System.out.println("   - PLANTED: The flower has been planted but not yet blooming.");
                System.out.println("   - BLOOMING: The flower is blooming and requires care to extend its bloom time.");
                System.out.println("   - DEAD: The flower's bloom time has expired, and it can no longer be watered or cared for.\n");
                break;

              case "6": // Back to Main Menu
                System.out.println("Returning to main menu...");
                break;

              default:
                System.out.println("Invalid choice. Please select 1-6.");
                break;
            }
            break;

          case "4": // Follow Service
            System.out.println("Follow Service Options:");
            System.out.println("1. Add User");
            System.out.println("2. Follow User");
            System.out.println("3. View Following");
            System.out.println("4. Back to Main Menu");

            String followChoice = reader.readLine();

            switch (followChoice) {
              case "1": // Add User
                System.out.print("Enter user name: ");
                String userName = reader.readLine();
                client.addUser(userName);
                break;

              case "2": // Follow User
                System.out.print("Enter your name: ");
                String follower = reader.readLine();
                System.out.print("Enter the name of the user to follow: ");
                String followee = reader.readLine();
                client.followUser(follower, followee);
                break;

              case "3": // View Following
                System.out.print("Enter your name to see who you are following: ");
                String viewer = reader.readLine();
                client.viewFollowing(viewer);
                break;

              case "4": // Back to Main Menu
                System.out.println("Returning to main menu...");
                break;

              default:
                System.out.println("Invalid choice. Please select 1-4.");
                break;
            }
            break;

          case "5": // Trivia Master Service
            System.out.println("Trivia Master Service Options:");
            System.out.println("1. Add a Trivia Question");
            System.out.println("2. Get Trivia Questions");
            System.out.println("3. Answer a Trivia Question");
            System.out.println("4. Back to Main Menu");

            String triviaChoice = reader.readLine();

            switch (triviaChoice) {
              case "1": // Add a Trivia Question
                String question = promptUser("Enter a trivia question: ", reader);
                String answer = promptUser("Enter the answer: ", reader);
                client.addTriviaQuestion(question, answer);
                break;

              case "2": // Get Trivia Questions
                try {
                  String randomInput = promptUser("Get random question (true/false)? ", reader);
                  boolean random = !randomInput.isEmpty() && Boolean.parseBoolean(randomInput);
                  if (!random) {
                    int count = Integer.parseInt(promptUser("Enter the number of questions to retrieve: ", reader));
                    client.getTriviaQuestions(false, count);
                  } else {
                    client.getTriviaQuestions(true, 1);
                  }
                } catch (NumberFormatException e) {
                  System.out.println("Invalid number. Please enter a valid integer.");
                }
                break;

              case "3": // Answer a Trivia Question
                String triviaQ = promptUser("Enter the trivia question: ", reader);
                String triviaA = promptUser("Enter your answer: ", reader);
                client.answerTriviaQuestion(triviaQ, triviaA);
                break;

              case "4": // Back to Main Menu
                System.out.println("Returning to main menu...");
                break;

              default:
                System.out.println("Invalid choice. Please select 1-4.");
                break;
            }
            break;

          case "6": // Exit
            System.out.println("Exiting...");
            return;

          default:
            System.out.println("Invalid choice. Please select 1-6.");
        }
      }
    } finally {
      // Ensure channels are properly shut down
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      regChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
