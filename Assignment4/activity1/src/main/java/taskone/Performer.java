/**
  File: Performer.java
  Author: Student in Fall 2020B
  Description: Performer class in package taskone.
*/

package taskone;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

import static java.lang.Thread.sleep;

/**
 * Class: Performer 
 * Description: Threaded Performer for server tasks.
 */
class Performer {

    private StringList state;

    public Performer(StringList strings) {
        this.state = strings;
    }

    public JSONObject add(String str) throws InterruptedException {
        System.out.println("Start add");
        JSONObject json = new JSONObject();
        json.put("datatype", 1);
        json.put("type", "add");
        sleep(6000); // Simulate processing delay
        state.add(str);
        json.put("data", state.toString());
        System.out.println("End add");
        return json;
    }

    public JSONObject display() {
        JSONObject json = new JSONObject();
        json.put("type", "display");
        json.put("data", state.toString());
        return json;
    }

    public JSONObject count() {
        JSONObject json = new JSONObject();
        json.put("type", "count");
        json.put("data", Integer.toString(state.size()));
        return json;
    }

    public JSONObject quit() {
        JSONObject json = new JSONObject();
        json.put("type", "quit");
        json.put("data", "Server connection closed.");
        return json;
    }

    public static JSONObject error(String err) {
        JSONObject json = new JSONObject();
        json.put("error", err);
        return json;
    }

}
