import org.json.JSONArray;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.util.Scanner;

/**
 */
class SockClient {
  static Socket sock = null;
  static String host = "localhost";
  static int port = 8888;
  static OutputStream out;
  static ObjectOutputStream os;
  static DataInputStream in;

  public static void main(String args[]) {

    if (args.length != 2) {
      System.out.println("Expected arguments: <host(String)> <port(int)>");
      System.exit(1);
    }

    try {
      host = args[0];
      port = Integer.parseInt(args[1]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port|sleepDelay] must be an integer");
      System.exit(2);
    }

    try {
      connect(host, port); // connecting to server
      System.out.println("Client connected to server.");
      boolean requesting = true;
      while (requesting) {
        System.out.println("What would you like to do: 1 - echo, 2 - add, 3 - addmany, 4 - charCount, 5 - inventory (0 to quit)");
        Scanner scanner = new Scanner(System.in);
        int choice = Integer.parseInt(scanner.nextLine());
        JSONObject json = new JSONObject(); // request object

        switch (choice) {
          case 0:
            System.out.println("Choose quit. Thank you for using our services. Goodbye!");
            requesting = false;
            break;
          case 1:
            System.out.println("Choose echo, which String do you want to send?");
            String message = scanner.nextLine();
            json.put("type", "echo");
            json.put("data", message);
            break;
          case 2:
            System.out.println("Choose add, enter first number:");
            String num1 = scanner.nextLine();
            json.put("type", "add");
            json.put("num1", num1);

            System.out.println("Enter second number:");
            String num2 = scanner.nextLine();
            json.put("num2", num2);
            break;
          case 3:
            System.out.println("Choose addmany, enter as many numbers as you like, when done choose 0:");
            JSONArray array = new JSONArray();
            String num = "1";
            while (!num.equals("0")) {
              num = scanner.nextLine();
              array.put(num);
              System.out.println("Got your " + num);
            }
            json.put("type", "addmany");
            json.put("nums", array);
            break;
          case 4: // charCount
            System.out.println("Choose charCount. Do you want to search for a specific character? (yes or no)");
            String choiceChar = scanner.nextLine();
            json.put("type", "charcount");

            if (choiceChar.equalsIgnoreCase("yes")) {
              json.put("findchar", true);
              System.out.println("Enter the character to search for:");
              String find = scanner.nextLine();
              json.put("find", find);
            } else {
              json.put("findchar", false);
            }

            System.out.println("Enter the string to search through:");
            String countStr = scanner.nextLine();
            json.put("count", countStr);
            break;
          case 5: // inventory
            System.out.println("Choose inventory. What task would you like to perform? (add, view, or buy)");
            String task = scanner.nextLine().toLowerCase();
            json.put("type", "inventory");
            json.put("task", task);

            if (task.equals("add")) {
              System.out.println("Enter the product name:");
              String productName = scanner.nextLine();
              System.out.println("Enter the quantity to add:");
              int quantity = Integer.parseInt(scanner.nextLine());
              json.put("productName", productName);
              json.put("quantity", quantity);
            } else if (task.equals("buy")) {
              System.out.println("Enter the product name:");
              String productName = scanner.nextLine();
              System.out.println("Enter the quantity to buy:");
              int quantity = Integer.parseInt(scanner.nextLine());
              json.put("productName", productName);
              json.put("quantity", quantity);
            }
            break;
        }

        if (!requesting) {
          continue;
        }

        // write the whole message
        os.writeObject(json.toString());
        os.flush();

        // handle the response
        String i = in.readUTF();
        JSONObject res = new JSONObject(i);
        System.out.println("Got response: " + res);

        // Process response based on its type
        System.out.println("Raw response: " + res.toString()); // Debugging line to print the raw response

        if (res.getBoolean("ok")) {
          switch (res.getString("type")) {
            case "echo":
              System.out.println(res.optString("echo", "No echo message found"));
              break;
            case "add":
            case "addmany":
            case "charcount":
              System.out.println(res.optInt("result", 0)); // Print result directly without additional text
              break;
            case "inventory":
              JSONArray inventory = res.optJSONArray("inventory");
              if (inventory != null) {
                System.out.println("Current Inventory:");
                for (int j = 0; j < inventory.length(); j++) {
                  JSONObject item = inventory.optJSONObject(j);
                  if (item != null) {
                    System.out.println("- Product: " + item.optString("product", "Unknown") +
                            ", Quantity: " + item.optInt("quantity", 0));
                  }
                }
              } else {
                System.out.println("No inventory data found");
              }
              break;
          }
        } else {
          System.out.println("Error: " + res.optString("message", "Unknown error"));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void overandout() throws IOException {
    in.close();
    os.close();
    sock.close(); // close socket after sending
  }

  public static void connect(String host, int port) throws IOException {
    sock = new Socket(host, port); // connect to host and socket on port 8888
    out = sock.getOutputStream();
    os = new ObjectOutputStream(out);
    in = new DataInputStream(sock.getInputStream());
  }
}
