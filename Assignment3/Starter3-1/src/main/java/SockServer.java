import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.*;
import java.io.*;

import java.util.HashMap;
import java.util.Map;


/**
 * A class to demonstrate a simple client-server connection using sockets.
 *
 */
public class SockServer {
  static Socket sock;
  static DataOutputStream os;
  static ObjectInputStream in;
  static int port = 8888;

  // File Path for Inventory Storage
  private static final String INVENTORY_FILE = "inventory.json";

  private static final Map<String, Integer> DEFAULT_INVENTORY = Map.of(
          "Witch's Broomstick", 10,
          "Vampire Fangs", 25,
          "Ghost Lantern", 15,
          "Zombie Hand", 30,
          "Pumpkin Potion", 20
  );


  public static void main(String args[]) {
    // Load persistent inventory data on server start
    loadInventoryFromFile();

    if (args.length != 1) {
      System.out.println("Expected arguments: <port(int)>");
      System.exit(1);
    }

    try {
      port = Integer.parseInt(args[0]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port|sleepDelay] must be an integer");
      System.exit(2);
    }

    try {
      // Open server socket
      ServerSocket serv = new ServerSocket(port);
      System.out.println("Server ready for connections on port " + port);

      // Server loop to handle multiple client connections
      while (true) {
        System.out.println("Server waiting for a connection");
        Socket clientSocket = serv.accept(); // blocking wait for client connection
        System.out.println("Client connected");

        // Start a new thread for each client connection
        new Thread(() -> {
          try {
            // Set up object input and output streams for the client
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream());

            boolean connected = true;
            while (connected) {
              String s = "";
              try {
                s = (String) in.readObject(); // attempt to read string from client
              } catch (Exception e) { // Handle client disconnect or read error
                System.out.println("Client disconnect or read error.");
                connected = false;
                continue;
              }

              try {
                JSONObject res = isValid(s);

                if (res.has("ok")) {
                  os.writeUTF(res.toString());
                  os.flush();
                  continue;
                }

                JSONObject req = new JSONObject(s);

                res = testField(req, "type");
                if (!res.getBoolean("ok")) {
                  res = noType(req);
                  os.writeUTF(res.toString());
                  os.flush();
                  continue;
                }

                // Request handling based on type
                switch (req.getString("type")) {
                  case "echo":
                    res = echo(req);
                    break;
                  case "add":
                    res = add(req);
                    break;
                  case "addmany":
                    res = addmany(req);
                    break;
                  case "inventory":
                    res = inventory(req);
                    break;
                  case "charcount":
                    res = charCount(req);
                    break;
                  default:
                    res = wrongType(req);
                }
                os.writeUTF(res.toString());
                os.flush();

              } catch (Exception e) {
                e.printStackTrace();
                JSONObject errorRes = new JSONObject();
                errorRes.put("ok", false);
                errorRes.put("message", "Internal server error occurred");
                os.writeUTF(errorRes.toString());
                os.flush();
              }
            }

            // Close the connection when done
            in.close();
            os.close();
            clientSocket.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }).start(); // Start the thread immediately
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  /**
   * Checks if a specific field exists
   *
   */
  static JSONObject testField(JSONObject req, String key){
    JSONObject res = new JSONObject();

    // field does not exist
    if (!req.has(key)){
      res.put("ok", false);
      res.put("message", "Field " + key + " does not exist in request");
      return res;
    }
    return res.put("ok", true);
  }

  // handles the simple echo request
  static JSONObject echo(JSONObject req){
    System.out.println("Echo request: " + req.toString());
    JSONObject res = testField(req, "data");
    if (res.getBoolean("ok")) {
      if (!req.get("data").getClass().getName().equals("java.lang.String")){
        res.put("ok", false);
        res.put("message", "Field data needs to be of type: String");
        return res;
      }

      res.put("type", "echo");
      res.put("echo", "Here is your echo: " + req.getString("data"));
    }
    return res;
  }

  // handles the simple add request with two numbers
  static JSONObject add(JSONObject req){
    System.out.println("Add request: " + req.toString());
    JSONObject res1 = testField(req, "num1");
    if (!res1.getBoolean("ok")) {
      return res1;
    }

    JSONObject res2 = testField(req, "num2");
    if (!res2.getBoolean("ok")) {
      return res2;
    }

    JSONObject res = new JSONObject();
    res.put("ok", true);
    res.put("type", "add");
    try {
      res.put("result", req.getInt("num1") + req.getInt("num2"));
    } catch (JSONException e){
      res.put("ok", false);
      res.put("message", "Field num1/num2 needs to be of type: int");
    }
    return res;
  }

  // implement me in assignment 3 -- static JSONObject inventory(JSONObject req);
  // Inventory data structure
  private static Map<String, Integer> inventory = new HashMap<>();

  static JSONObject inventory(JSONObject req) {
    System.out.println("Processing inventory request: " + req.toString());  // Log the initial request

    JSONObject response = new JSONObject();
    response.put("type", "inventory");

    String task = req.optString("task", null);
    if (task == null) {
      response.put("ok", false);
      response.put("message", "Field task does not exist in request");
      System.out.println("inventory error: " + response.toString());  // Log the error response
      return response;
    }

    switch (task) {
      case "add":
        response = addToInventory(req);
        System.out.println("Inventory add task result: " + response.toString());  // Log add result
        return response;
      case "view":
        response = viewInventory(req);
        System.out.println("Inventory view task result: " + response.toString());  // Log view result
        return response;
      case "buy":
        response = buyFromInventory(req);
        System.out.println("Inventory buy task result: " + response.toString());  // Log buy result
        return response;
      default:
        response.put("ok", false);
        response.put("message", "Task " + task + " is not supported");
        System.out.println("inventory error: " + response.toString());  // Log unsupported task error
        return response;
    }
  }

  private static JSONObject addToInventory(JSONObject req) {
    JSONObject response = new JSONObject();
    response.put("type", "inventory");
    response.put("task", "add");

    // Validate request fields
    String productName = req.optString("productName", null);
    if (productName == null || productName.isEmpty()) {
      response.put("ok", false);
      response.put("message", "Field productName does not exist in request or is empty");
      return response;
    }

    int quantity;
    try {
      quantity = req.getInt("quantity");
    } catch (JSONException e) {
      response.put("ok", false);
      response.put("message", "Field quantity needs to be of type: int");
      return response;
    }

    // Update inventory
    inventory.put(productName, inventory.getOrDefault(productName, 0) + quantity);

    // Construct success response with an explicit JSONArray
    response.put("ok", true);
    JSONArray inventoryArray = new JSONArray();
    for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
      JSONObject item = new JSONObject();
      item.put("product", entry.getKey());
      item.put("quantity", entry.getValue());
      inventoryArray.put(item);
    }
    response.put("inventory", inventoryArray);
    saveInventoryToFile();
    checkAndResetInventoryIfNeeded();
    return response;
  }

  private static JSONObject viewInventory(JSONObject req) {
    JSONObject response = new JSONObject();
    response.put("type", "inventory");
    response.put("task", "view");
    response.put("ok", true);

    // Construct the inventory list as a JSONArray
    JSONArray inventoryArray = new JSONArray();
    for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
      JSONObject product = new JSONObject();
      product.put("product", entry.getKey());
      product.put("quantity", entry.getValue());
      inventoryArray.put(product);
    }

    response.put("inventory", inventoryArray);

    return response;
  }

  public static void clearInventory() {
    inventory.clear();
  }

  private static JSONObject buyFromInventory(JSONObject req) {
    JSONObject response = new JSONObject();
    response.put("type", "inventory");
    response.put("task", "buy");

    // Validate product name
    String productName = req.optString("productName", null);
    if (productName == null || productName.isEmpty()) {
      response.put("ok", false);
      response.put("message", "Field productName does not exist in request or is empty");
      return response;
    }

    // Validate quantity
    int quantity;
    try {
      quantity = req.getInt("quantity");
    } catch (JSONException e) {
      response.put("ok", false);
      response.put("message", "Field quantity needs to be of type: int");
      return response;
    }

    // Check if the product exists in the inventory
    if (!inventory.containsKey(productName)) {
      response.put("ok", false);
      response.put("message", "Product " + productName + " not in inventory");
      return response;
    }

    // Check if sufficient quantity is available
    int availableQuantity = inventory.get(productName);
    if (availableQuantity < quantity) {
      response.put("ok", false);
      response.put("message", "Product " + productName + " not available in quantity " + quantity);
      return response;
    }

    // Reduce quantity and update inventory
    inventory.put(productName, availableQuantity - quantity);
    response.put("ok", true);

    // Return the updated inventory list
    JSONArray inventoryArray = new JSONArray();
    for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
      JSONObject item = new JSONObject();
      item.put("product", entry.getKey());
      item.put("quantity", entry.getValue());
      inventoryArray.put(item);
    }
    response.put("inventory", inventoryArray);
    saveInventoryToFile();
    checkAndResetInventoryIfNeeded();
    return response;
  }

  // Method to Save Inventory (Persistent Storage Solution)
  private static void saveInventoryToFile() {
    try (FileWriter file = new FileWriter(INVENTORY_FILE)) {
      JSONObject jsonInventory = new JSONObject();
      for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
        jsonInventory.put(entry.getKey(), entry.getValue());
      }
      file.write(jsonInventory.toString());
      file.flush();
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Failed to save inventory to file.");
    }
  }


  // Method to Load Inventory
  private static void loadInventoryFromFile() {
    File file = new File(INVENTORY_FILE);

    // If the file doesn't exist or is empty, use default inventory
    if (!file.exists() || file.length() == 0) {
      inventory.putAll(DEFAULT_INVENTORY);
      saveInventoryToFile();  // Save the default inventory to the file
      return;
    }

    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      StringBuilder jsonString = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        jsonString.append(line);
      }

      JSONObject jsonInventory = new JSONObject(jsonString.toString());
      for (String key : jsonInventory.keySet()) {
        inventory.put(key, jsonInventory.getInt(key));
      }
    } catch (IOException | JSONException e) {
      e.printStackTrace();
      System.out.println("Failed to load inventory from file.");
    }
  }

  // Method to Reset Inventory to Default
  private static void resetInventoryToDefault() {
    inventory.clear();
    inventory.putAll(DEFAULT_INVENTORY);
    saveInventoryToFile();  // Save the default inventory to file
    System.out.println("Inventory reset to default.");
  }

  // Method to Check Total Inventory Quantity
  private static void checkAndResetInventoryIfNeeded() {
    // Reset if the number of unique items exceeds 25
    if (inventory.size() > 25) {
      resetInventoryToDefault();
    }
  }

  // implement me in assignment 3 -- static JSONObject charCount(JSONObject req);
  static JSONObject charCount(JSONObject req) {
    System.out.println("Processing charCount request: " + req.toString());  // Log the initial request

    JSONObject response = new JSONObject();
    response.put("type", "charcount");

    // Validate `count` field
    String countString = req.optString("count", null);
    if (countString == null) {
      response.put("ok", false);
      response.put("message", "Field count does not exist in request");
      System.out.println("charCount error: " + response.toString());  // Log the error response
      return response;
    }

    boolean findChar = req.optBoolean("findchar", false);

    // Check if we are counting a specific character or the entire string
    if (findChar) {
      String find = req.optString("find", null);
      if (find == null || find.length() != 1) {
        response.put("ok", false);
        response.put("message", "Field find must be a single character");
        System.out.println("charCount error: " + response.toString());  // Log the error response
        return response;
      }
      // Count occurrences of the specified character
      long count = countString.chars().filter(ch -> ch == find.charAt(0)).count();
      response.put("ok", true);
      response.put("result", (int) count);
      System.out.println("charCount result (specific char): " + response.toString());  // Log the successful result
    } else {
      // Count total characters
      response.put("ok", true);
      response.put("result", countString.length());
      System.out.println("charCount result (total count): " + response.toString());  // Log the successful result
    }

    return response;
  }

  // handles the simple addmany request
  static JSONObject addmany(JSONObject req) {
    System.out.println("Add many request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("type", "addmany");  // Set the type initially for all responses

    // Check if 'nums' field exists
    if (!req.has("nums")) {
      res.put("ok", false);
      res.put("message", "Field nums does not exist in request");
      return res;
    }

    int result = 0;
    JSONArray array;
    try {
      array = req.getJSONArray("nums");
    } catch (JSONException e) {
      res.put("ok", false);
      res.put("message", "Field nums must be an array");
      return res;
    }

    // Sum the integers in the array, handling any non-integer values
    for (int i = 0; i < array.length(); i++) {
      try {
        result += array.getInt(i);
      } catch (JSONException e) {
        res.put("ok", false);
        res.put("message", "Values in array need to be ints");
        return res;
      }
    }

    // If successful, populate the response with the result
    res.put("ok", true);
    res.put("result", result);
    return res;
  }

  // creates the error message for wrong type
  static JSONObject wrongType(JSONObject req){
    System.out.println("Wrong type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "Type " + req.getString("type") + " is not supported.");
    return res;
  }

  // creates the error message for no given type
  static JSONObject noType(JSONObject req){
    System.out.println("No type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "No request type was given.");
    return res;
  }

  // From: https://www.baeldung.com/java-validate-json-string
  public static JSONObject isValid(String json) {
    try {
      new JSONObject(json);
    } catch (JSONException e) {
      try {
        new JSONArray(json);
      } catch (JSONException ne) {
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "req not JSON");
        return res;
      }
    }
    return new JSONObject();
  }

  // sends the response and closes the connection between client and server.
  static void overandout() {
    try {
      os.close();
      in.close();
      sock.close();
    } catch(Exception e) {e.printStackTrace();}

  }

  // sends the response and closes the connection between client and server.
  static void writeOut(JSONObject res) {
    try {
      os.writeUTF(res.toString());
      // make sure it wrote and doesn't get cached in a buffer
      os.flush();

    } catch(Exception e) {e.printStackTrace();}

  }
}