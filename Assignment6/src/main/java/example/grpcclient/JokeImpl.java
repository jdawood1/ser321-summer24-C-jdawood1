package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.*;
import java.util.Stack;

public class JokeImpl extends JokeGrpc.JokeImplBase {
    Stack<String> jokes = new Stack<String>();
    
    public JokeImpl(){
        super();
        // copying some dad jokes
        jokes.add("How do you get a squirrel to like you? Act like a nut.");
        jokes.add("I don't trust stairs. They're always up to something.");
        jokes.add("What do you call someone with no body and no nose? Nobody knows.");
        jokes.add("Did you hear the rumor about butter? Well, I'm not going to spread it!");
        
    }

    @Override
    public void getJoke(JokeReq req, StreamObserver<JokeRes> responseObserver) {
        System.out.println("Received from client: " + req.getNumber());
        JokeRes.Builder response = JokeRes.newBuilder();
        Stack<String> tempStack = new Stack<>();
        tempStack.addAll(jokes); // Copy jokes to a temporary stack

        for (int i = 0; i < req.getNumber(); i++) {
            if (!tempStack.empty()) {
                response.addJoke(tempStack.pop());
            } else {
                response.addJoke("I am out of jokes...");
                break;
            }
        }
        JokeRes resp = response.build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void setJoke(JokeSetReq req, StreamObserver<JokeSetRes> responseObserver) {
        System.out.println("Received joke to set: " + req.getJoke());
        JokeSetRes.Builder response = JokeSetRes.newBuilder();

        if (req.getJoke().isEmpty()) {
            response.setOk(false).setMessage("Empty joke cannot be added!");
        } else {
            jokes.add(req.getJoke());
            response.setOk(true).setMessage("Joke added successfully.");
        }

        JokeSetRes resp = response.build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

}