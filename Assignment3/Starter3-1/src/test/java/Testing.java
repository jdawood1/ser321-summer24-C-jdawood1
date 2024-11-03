import org.junit.Test;
import static org.junit.Assert.*;
import org.json.JSONObject;
import org.json.JSONArray;



public class Testing {

    // some tests for locally testing methods in the server
    @Test
    public void typeWrong() {
        JSONObject req = new JSONObject();
        req.put("type1", "echo");

        JSONObject res = SockServer.testField(req, "type");

        assertEquals(res.getBoolean("ok"), false);
        assertEquals(res.getString("message"), "Field type does not exist in request");
    }

    @Test
    public void echoCorrect() {
        JSONObject req = new JSONObject();
        req.put("type", "echo");
        req.put("data", "whooooo");
        JSONObject res = SockServer.echo(req);

        assertEquals("echo", res.getString("type"));
        assertEquals(res.getBoolean("ok"), true);
        assertEquals(res.getString("echo"), "Here is your echo: whooooo");
    }

    @Test
    public void echoErrors() {
        JSONObject req = new JSONObject();
        req.put("type", "echo");
        req.put("data1", "whooooo");
        JSONObject res = SockServer.echo(req);

        assertEquals(res.getBoolean("ok"), false);
        assertEquals(res.getString("message"), "Field data does not exist in request");

        JSONObject req2 = new JSONObject();
        req2.put("type", "echo");
        req2.put("data", 33);
        JSONObject res2 = SockServer.echo(req2);

        assertEquals(false, res2.getBoolean("ok"));
        assertEquals(res2.getString("message"), "Field data needs to be of type: String");

        JSONObject req3 = new JSONObject();
        req3.put("type", "echo");
        req3.put("data", true);
        JSONObject res3 = SockServer.echo(req3);

        assertEquals(res3.getBoolean("ok"), false);
        assertEquals(res3.getString("message"), "Field data needs to be of type: String");
    }

    // INVENTORY
    @Test
    public void inventoryViewCorrect() {
        // Clear inventory to ensure fresh state
        SockServer.clearInventory();

        // Add a product first
        JSONObject addReq = new JSONObject();
        addReq.put("type", "inventory");
        addReq.put("task", "add");
        addReq.put("productName", "Road bike");
        addReq.put("quantity", 5);
        SockServer.inventory(addReq);

        // Now view the inventory
        JSONObject req = new JSONObject();
        req.put("type", "inventory");
        req.put("task", "view");

        JSONObject res = SockServer.inventory(req);

        assertEquals("inventory", res.getString("type"));
        assertEquals(true, res.getBoolean("ok"));
        assertNotNull(res.getJSONArray("inventory"));
        assertEquals(1, res.getJSONArray("inventory").length());

        // Additional check for correct product and quantity
        JSONObject item = res.getJSONArray("inventory").getJSONObject(0);
        assertEquals("Road bike", item.getString("product"));
        assertEquals(5, item.getInt("quantity"));
    }

    @Test
    public void inventoryViewMultipleItems() {
        // Add two products
        JSONObject addReq1 = new JSONObject();
        addReq1.put("type", "inventory");
        addReq1.put("task", "add");
        addReq1.put("productName", "Road bike");
        addReq1.put("quantity", 5);
        SockServer.inventory(addReq1);

        JSONObject addReq2 = new JSONObject();
        addReq2.put("type", "inventory");
        addReq2.put("task", "add");
        addReq2.put("productName", "Helmet");
        addReq2.put("quantity", 10);
        SockServer.inventory(addReq2);

        // View the inventory
        JSONObject req = new JSONObject();
        req.put("type", "inventory");
        req.put("task", "view");

        JSONObject res = SockServer.inventory(req);

        assertEquals("inventory", res.getString("type"));
        assertEquals(true, res.getBoolean("ok"));
        assertNotNull(res.getJSONArray("inventory"));
        assertEquals(2, res.getJSONArray("inventory").length());
    }

    @Test
    public void inventoryAddTest() {
        // Test adding a product to inventory
        JSONObject addReq = new JSONObject();
        addReq.put("type", "inventory");
        addReq.put("task", "add");
        addReq.put("productName", "helmet");
        addReq.put("quantity", 10);

        JSONObject res = SockServer.inventory(addReq);

        System.out.println("Inventory Add Response: " + res);  // Print response for debugging

        // Basic checks for type and task
        assertEquals("inventory", res.getString("type"));
        assertEquals("add", res.getString("task"));
        assertTrue(res.getBoolean("ok"));

        // Check if the inventory field exists and is an array
        assertTrue("Inventory field should exist", res.has("inventory"));
        assertTrue("Inventory should be a JSONArray", res.get("inventory") instanceof JSONArray);

        // Verify that "helmet" with quantity 10 is in the inventory array
        JSONArray inventoryArray = res.getJSONArray("inventory");
        boolean foundHelmet = false;
        for (int i = 0; i < inventoryArray.length(); i++) {
            JSONObject item = inventoryArray.getJSONObject(i);
            if (item.getString("product").equals("helmet") && item.getInt("quantity") == 10) {
                foundHelmet = true;
                break;
            }
        }
        assertTrue("Helmet with quantity 10 should be in the inventory", foundHelmet);
    }

    @Test
    public void inventoryBuyTest() {
        // Clear inventory to ensure fresh state
        SockServer.clearInventory();

        // Add a product to inventory
        JSONObject addReq = new JSONObject();
        addReq.put("type", "inventory");
        addReq.put("task", "add");
        addReq.put("productName", "Helmet");
        addReq.put("quantity", 10);
        SockServer.inventory(addReq);

        // Buy 5 of the product
        JSONObject buyReq = new JSONObject();
        buyReq.put("type", "inventory");
        buyReq.put("task", "buy");
        buyReq.put("productName", "Helmet");
        buyReq.put("quantity", 5);

        JSONObject res = SockServer.inventory(buyReq);

        assertEquals("inventory", res.getString("type"));
        assertEquals("buy", res.getString("task"));
        assertTrue(res.getBoolean("ok"));

        // Check the updated inventory to confirm quantity reduction
        JSONArray inventoryArray = res.getJSONArray("inventory");
        JSONObject item = inventoryArray.getJSONObject(0);
        assertEquals("Helmet", item.getString("product"));
        assertEquals(5, item.getInt("quantity")); // should have 5 left after buying 5

        // Attempt to buy more than available quantity
        buyReq.put("quantity", 6); // buying more than available

        res = SockServer.inventory(buyReq);

        assertFalse(res.getBoolean("ok"));
        assertEquals("Product Helmet not available in quantity 6", res.getString("message"));
    }

    @Test
    public void inventoryBuyInsufficientQuantityTest() {
        // Clear inventory to ensure a fresh state
        SockServer.clearInventory();

        // Add a product with a limited quantity
        JSONObject addReq = new JSONObject();
        addReq.put("type", "inventory");
        addReq.put("task", "add");
        addReq.put("productName", "Helmet");
        addReq.put("quantity", 5);
        SockServer.inventory(addReq);

        // Attempt to buy more than the available quantity
        JSONObject buyReq = new JSONObject();
        buyReq.put("type", "inventory");
        buyReq.put("task", "buy");
        buyReq.put("productName", "Helmet");
        buyReq.put("quantity", 10); // Requesting more than available

        JSONObject res = SockServer.inventory(buyReq);

        // Check that the server responds with an error and the appropriate message
        assertFalse(res.getBoolean("ok"));
        assertEquals("Product Helmet not available in quantity 10", res.getString("message"));
    }

    @Test
    public void inventoryBuyNonExistentProductTest() {
        // Clear inventory to ensure a fresh state
        SockServer.clearInventory();

        // Attempt to buy a product that doesn’t exist in the inventory
        JSONObject buyReq = new JSONObject();
        buyReq.put("type", "inventory");
        buyReq.put("task", "buy");
        buyReq.put("productName", "Gloves"); // Product not in inventory
        buyReq.put("quantity", 3);

        JSONObject res = SockServer.inventory(buyReq);

        // Check that the server responds with an error and the appropriate message
        assertFalse(res.getBoolean("ok"));
        assertEquals("Product Gloves not in inventory", res.getString("message"));
    }

    // ADD SERVICE
    @Test
    public void addServiceCorrectTest() {
        // Test correct input for add service
        JSONObject req = new JSONObject();
        req.put("type", "add");
        req.put("num1", "5");
        req.put("num2", "10");

        JSONObject res = SockServer.add(req);

        assertEquals("add", res.getString("type"));
        assertEquals(true, res.getBoolean("ok"));
        assertEquals(15, res.getInt("result"));  // 5 + 10 = 15
    }

    @Test
    public void addServiceErrorTest() {
        // Test error for non-integer inputs
        JSONObject req = new JSONObject();
        req.put("type", "add");
        req.put("num1", "five");
        req.put("num2", "10");

        JSONObject res = SockServer.add(req);

        assertEquals("add", res.getString("type"));
        assertEquals(false, res.getBoolean("ok"));
        assertEquals("Field num1/num2 needs to be of type: int", res.getString("message"));
    }

    // ADDMANY SERVICE
    @Test
    public void addManyServiceCorrectTest() {
        // Test correct input for addmany service
        JSONObject req = new JSONObject();
        req.put("type", "addmany");
        req.put("nums", new JSONArray("[1, 2, 3, 4]"));  // Sum = 10

        JSONObject res = SockServer.addmany(req);

        assertEquals("addmany", res.getString("type"));
        assertEquals(true, res.getBoolean("ok"));
        assertEquals(10, res.getInt("result"));
    }

    @Test
    public void addManyServiceErrorTest() {
        // Test error for non-integer input in the addmany service
        JSONObject req = new JSONObject();
        req.put("type", "addmany");
        req.put("nums", new JSONArray("[1, 'two', 3]"));  // Contains non-integer "two"

        JSONObject res = SockServer.addmany(req);

        assertEquals("addmany", res.getString("type"));
        assertEquals(false, res.getBoolean("ok"));
        assertEquals("Values in array need to be ints", res.getString("message"));
    }

    // CHAR COUNT TESTS
    @Test
    public void charCountMissingFindFieldTest() {
        // Test charCount with findchar set to true but without a find field
        JSONObject req = new JSONObject();
        req.put("type", "charcount");
        req.put("findchar", true);  // find field is missing
        req.put("count", "hello world");

        JSONObject res = SockServer.charCount(req);

        assertEquals("charcount", res.getString("type"));
        assertEquals(false, res.getBoolean("ok"));
        assertEquals("Field find must be a single character", res.getString("message"));
    }

    @Test
    public void charCountFindFieldTooLongTest() {
        // Test charCount with findchar set to true but find field has more than one character
        JSONObject req = new JSONObject();
        req.put("type", "charcount");
        req.put("findchar", true);
        req.put("find", "ll");  // find field should be a single character
        req.put("count", "hello world");

        JSONObject res = SockServer.charCount(req);

        assertEquals("charcount", res.getString("type"));
        assertEquals(false, res.getBoolean("ok"));
        assertEquals("Field find must be a single character", res.getString("message"));
    }

    @Test
    public void charCountTest() {
        // Test case for counting all characters in a string
        JSONObject req = new JSONObject();
        req.put("type", "charcount");
        req.put("findchar", false);
        req.put("count", "hello world");

        JSONObject res = SockServer.charCount(req);

        assertEquals("charcount", res.getString("type"));
        assertEquals(true, res.getBoolean("ok"));
        assertEquals(11, res.getInt("result"));  // "hello world" has 11 characters

        // Test case for counting occurrences of a specific character
        req = new JSONObject();
        req.put("type", "charcount");
        req.put("findchar", true);
        req.put("find", "l");
        req.put("count", "hello world");

        res = SockServer.charCount(req);

        assertEquals("charcount", res.getString("type"));
        assertEquals(true, res.getBoolean("ok"));
        assertEquals(3, res.getInt("result"));  // "l" appears 3 times in "hello world"
    }

    @Test
    public void inventoryExceedsLimitTest() {
        // Clear the inventory to start fresh
        SockServer.clearInventory();

        // Add 26 unique items to exceed the limit
        for (int i = 1; i <= 26; i++) {
            JSONObject addReq = new JSONObject();
            addReq.put("type", "inventory");
            addReq.put("task", "add");
            addReq.put("productName", "Product" + i);
            addReq.put("quantity", 1);
            SockServer.inventory(addReq);
        }

        // Now view the inventory to check if it has reset to default
        JSONObject viewReq = new JSONObject();
        viewReq.put("type", "inventory");
        viewReq.put("task", "view");
        JSONObject res = SockServer.inventory(viewReq);

        // Verify the response contains the default items (5 default items as per your setup)
        assertEquals("inventory", res.getString("type"));
        assertTrue(res.getBoolean("ok"));
        assertNotNull(res.getJSONArray("inventory"));
        assertEquals(5, res.getJSONArray("inventory").length());  // Assuming the default is set to 5 items

        // Check if the inventory matches the default items
        JSONArray inventoryArray = res.getJSONArray("inventory");
        boolean containsDefaultItems = false;
        for (int i = 0; i < inventoryArray.length(); i++) {
            JSONObject jsonItem = inventoryArray.getJSONObject(i);
            if (jsonItem.getString("product").equals("Witch's Broomstick") && jsonItem.getInt("quantity") == 10) {
                containsDefaultItems = true;
                break;
            }
        }
        assertTrue("Inventory should contain default items", containsDefaultItems);
    }


}