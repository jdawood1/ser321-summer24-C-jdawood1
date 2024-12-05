package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.FollowGrpc;
import service.UserReq;
import service.UserRes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
public class FollowImpl extends FollowGrpc.FollowImplBase {
    // Store registered users and their following lists
    private final Map<String, Set<String>> userFollowingMap = new HashMap<>();

    @Override
    public void addUser(UserReq req, StreamObserver<UserRes> responseObserver) {
        System.out.println("Received addUser request for: " + req.getName());

        synchronized (userFollowingMap) {
            if (userFollowingMap.containsKey(req.getName())) {
                System.out.println("AddUser failed: User '" + req.getName() + "' already exists.");
                responseObserver.onNext(UserRes.newBuilder()
                        .setIsSuccess(false)
                        .setError("User '" + req.getName() + "' already exists.")
                        .build());
            } else {
                userFollowingMap.put(req.getName(), new HashSet<>());
                System.out.println("User '" + req.getName() + "' added successfully.");
                responseObserver.onNext(UserRes.newBuilder().setIsSuccess(true).build());
            }
            responseObserver.onCompleted();
        }
    }


    @Override
    public void follow(UserReq req, StreamObserver<UserRes> responseObserver) {
        System.out.println(req.getName() + " requested to follow " + req.getFollowName());

        synchronized (userFollowingMap) {
            if (!userFollowingMap.containsKey(req.getName())) {
                System.out.println("Follow failed: User '" + req.getName() + "' is not registered.");
                responseObserver.onNext(UserRes.newBuilder()
                        .setIsSuccess(false)
                        .setError("User '" + req.getName() + "' is not registered.")
                        .build());
            } else if (!userFollowingMap.containsKey(req.getFollowName())) {
                System.out.println("Follow failed: User '" + req.getFollowName() + "' is not registered.");
                responseObserver.onNext(UserRes.newBuilder()
                        .setIsSuccess(false)
                        .setError("User '" + req.getFollowName() + "' is not registered.")
                        .build());
            } else {
                userFollowingMap.get(req.getName()).add(req.getFollowName());
                System.out.println("User '" + req.getName() + "' is now following '" + req.getFollowName() + "'.");
                responseObserver.onNext(UserRes.newBuilder().setIsSuccess(true).build());
            }
            responseObserver.onCompleted();
        }
    }


    @Override
    public void viewFollowing(UserReq req, StreamObserver<UserRes> responseObserver) {
        String name = req.getName();
        System.out.println("Received viewFollowing request for: " + name);

        synchronized (userFollowingMap) {
            if (!userFollowingMap.containsKey(name)) {
                // User is not registered
                responseObserver.onNext(UserRes.newBuilder()
                        .setIsSuccess(false)
                        .setError("User '" + name + "' is not registered.")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // Get the list of users being followed
            Set<String> following = userFollowingMap.get(name);
            responseObserver.onNext(UserRes.newBuilder()
                    .setIsSuccess(true)
                    .addAllFollowedUser(following)
                    .build());
            responseObserver.onCompleted();
        }
    }
}
