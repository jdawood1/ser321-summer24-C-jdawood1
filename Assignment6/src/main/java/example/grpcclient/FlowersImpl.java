package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.*;
import java.util.Map;
import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

public class FlowersImpl extends FlowersGrpc.FlowersImplBase {
    private final Map<String, FlowerState> flowers = new ConcurrentHashMap<>();
    private Instant currentTime = Instant.now();
    private static class FlowerState {
        Flower.Builder flower;
        Instant bloomUntil; // Time when the flower stops blooming

        FlowerState(Flower.Builder flower) {
            this.flower = flower;
        }
    }

    @Override
    public void plantFlower(FlowerReq req, StreamObserver<FlowerRes> responseObserver) {
        System.out.println("Received request to plant flower: " + req.getName());

        if (flowers.containsKey(req.getName())) {
            responseObserver.onNext(FlowerRes.newBuilder()
                    .setIsSuccess(false)
                    .setError("A flower with the name '" + req.getName() + "' already exists.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        if (req.getWaterTimes() > 6 || req.getBloomTime() > 6) {
            responseObserver.onNext(FlowerRes.newBuilder()
                    .setIsSuccess(false)
                    .setError("Water times and bloom time must not exceed 6.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        // Add flower to the map
        Flower.Builder flower = Flower.newBuilder()
                .setName(req.getName())
                .setWaterTimes(req.getWaterTimes())
                .setBloomTime(req.getBloomTime())
                .setFlowerState(State.PLANTED);

        flowers.put(req.getName(), new FlowerState(flower));

        responseObserver.onNext(FlowerRes.newBuilder()
                .setIsSuccess(true)
                .setMessage("Flower '" + req.getName() + "' planted successfully!")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void viewFlowers(com.google.protobuf.Empty req, StreamObserver<FlowerViewRes> responseObserver) {
        System.out.println("Received request to view flowers.");

        if (flowers.isEmpty()) {
            responseObserver.onNext(FlowerViewRes.newBuilder()
                    .setIsSuccess(false)
                    .setError("No flowers in the garden.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        FlowerViewRes.Builder response = FlowerViewRes.newBuilder().setIsSuccess(true);

        for (FlowerState flowerState : flowers.values()) {
            Flower.Builder flower = flowerState.flower;

            // Check and update state dynamically
            if (flower.getFlowerState() == State.BLOOMING && flowerState.bloomUntil != null) {
                System.out.println("Checking bloom expiration...");
                System.out.println("Current time: " + now());
                System.out.println("Bloom until: " + flowerState.bloomUntil);

                if (now().isAfter(flowerState.bloomUntil)) {
                    System.out.println("Flower '" + flower.getName() + "' has stopped blooming and is now DEAD.");
                    flower.setFlowerState(State.DEAD);
                    flower.setBloomTime(0);
                    flowerState.bloomUntil = null;
                } else {
                    System.out.println("Flower '" + flower.getName() + "' is still blooming.");
                }
            }

            response.addFlowers(flower.build());
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }




    @Override
    public void waterFlower(FlowerCare req, StreamObserver<WaterRes> responseObserver) {
        System.out.println("Received request to water flower: " + req.getName());

        FlowerState flowerState = flowers.get(req.getName());
        if (flowerState == null) {
            responseObserver.onNext(WaterRes.newBuilder()
                    .setIsSuccess(false)
                    .setError("Flower '" + req.getName() + "' not found.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        Flower.Builder flower = flowerState.flower;

        if (flower.getFlowerState() == State.DEAD) {
            responseObserver.onNext(WaterRes.newBuilder()
                    .setIsSuccess(false)
                    .setError("Flower '" + req.getName() + "' is DEAD and cannot be watered.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        if (flower.getFlowerState() != State.PLANTED) {
            responseObserver.onNext(WaterRes.newBuilder()
                    .setIsSuccess(false)
                    .setError("Flower '" + req.getName() + "' is not in the PLANTED state.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        flower.setWaterTimes(flower.getWaterTimes() - 1);
        if (flower.getWaterTimes() <= 0) {
            flower.setFlowerState(State.BLOOMING);
            flowerState.bloomUntil = now().plus(Duration.ofMinutes(flower.getBloomTime()));
        }

        responseObserver.onNext(WaterRes.newBuilder()
                .setIsSuccess(true)
                .setMessage("Flower '" + req.getName() + "' watered successfully.")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void careForFlower(FlowerCare req, StreamObserver<CareRes> responseObserver) {
        System.out.println("Received request to care for flower: " + req.getName());

        FlowerState flowerState = flowers.get(req.getName());
        if (flowerState == null) {
            responseObserver.onNext(CareRes.newBuilder()
                    .setIsSuccess(false)
                    .setError("Flower '" + req.getName() + "' not found.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        Flower.Builder flower = flowerState.flower;

        if (flower.getFlowerState() == State.DEAD) {
            responseObserver.onNext(CareRes.newBuilder()
                    .setIsSuccess(false)
                    .setError("Flower '" + req.getName() + "' is DEAD and cannot be cared for.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        if (flower.getFlowerState() != State.BLOOMING || flowerState.bloomUntil == null) {
            responseObserver.onNext(CareRes.newBuilder()
                    .setIsSuccess(false)
                    .setError("Flower '" + req.getName() + "' is not blooming.")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        // Extend blooming time by 1 minute
        flower.setBloomTime(flower.getBloomTime() + 1);
        flowerState.bloomUntil = flowerState.bloomUntil.plus(Duration.ofMinutes(-2)); // -2 just for testing

        responseObserver.onNext(CareRes.newBuilder()
                .setIsSuccess(true)
                .setMessage("Flower '" + req.getName() + "' cared for successfully.")
                .setBloomTime(flower.getBloomTime())
                .build());
        responseObserver.onCompleted();
    }

    private Instant now() {
        return currentTime;
    }
}