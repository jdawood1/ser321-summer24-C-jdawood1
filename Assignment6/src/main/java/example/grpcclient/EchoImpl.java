package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.*;

public class EchoImpl extends EchoGrpc.EchoImplBase {
    @Override
    public void parrot(ClientRequest req, StreamObserver<ServerResponse> responseObserver) {
        System.out.println("Received from client: " + req.getMessage());
        System.out.println("Starting to process the message...");
        ServerResponse.Builder response = ServerResponse.newBuilder();

        if (req.getMessage().isEmpty()) {
            System.out.println("Error: No message provided by client.");
            response.setIsSuccess(false).setError("No message provided");
        } else {
            System.out.println("Message received is valid. Preparing echo response...");
            response.setIsSuccess(true).setMessage("Echo: " + req.getMessage());
        }

        System.out.println("Sending response to client: " + response.getMessage() + " | Success: " + response.getIsSuccess() + " | Error: " + response.getError());
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
        System.out.println("Response sent to client.");
    }
}
