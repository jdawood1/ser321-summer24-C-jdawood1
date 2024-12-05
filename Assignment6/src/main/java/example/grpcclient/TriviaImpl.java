package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TriviaImpl extends TriviaMasterGrpc.TriviaMasterImplBase {
    private final Map<String, String> triviaDB = new HashMap<>(); // Holds trivia questions and their answers
    private final Random random = new Random();

    // Constructor to preload trivia questions
    public TriviaImpl() {
        preloadTrivia();
    }

    private void preloadTrivia() {
        triviaDB.put("What is the capital of France?", "Paris");
        triviaDB.put("Who wrote 'To Kill a Mockingbird'?", "Harper Lee");
        triviaDB.put("What is the chemical symbol for water?", "H2O");
        triviaDB.put("How many continents are there?", "7");
        System.out.println("Trivia database preloaded with 4 questions.");
    }

    @Override
    public void addQuestion(AddQuestionReq req, StreamObserver<AddQuestionRes> responseObserver) {
        String question = req.getQuestion().trim();
        String answer = req.getAnswer().trim();

        System.out.println("Received addQuestion request.");
        System.out.println("Question: " + question);
        System.out.println("Answer: " + answer);

        if (question.isEmpty() || answer.isEmpty()) {
            System.out.println("Failed to add question: Question or answer is empty.");
            responseObserver.onNext(AddQuestionRes.newBuilder()
                    .setIsSuccess(false)
                    .setMessage("Question and answer cannot be empty.")
                    .build());
        } else if (triviaDB.containsKey(question)) {
            System.out.println("Failed to add question: Duplicate question.");
            responseObserver.onNext(AddQuestionRes.newBuilder()
                    .setIsSuccess(false)
                    .setMessage("Question already exists.")
                    .build());
        } else {
            triviaDB.put(question, answer);
            System.out.println("Question added successfully: " + question);
            responseObserver.onNext(AddQuestionRes.newBuilder()
                    .setIsSuccess(true)
                    .setMessage("Question added successfully.")
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getQuestion(GetQuestionReq req, StreamObserver<GetQuestionRes> responseObserver) {
        System.out.println("Received getQuestion request.");
        List<String> questions = new ArrayList<>(triviaDB.keySet());

        if (questions.isEmpty()) {
            System.out.println("No questions available.");
            responseObserver.onNext(GetQuestionRes.newBuilder()
                    .addQuestions("No questions available.")
                    .build());
        } else if (req.getRandom()) {
            String randomQuestion = questions.get(random.nextInt(questions.size()));
            System.out.println("Returning random question: " + randomQuestion);
            responseObserver.onNext(GetQuestionRes.newBuilder()
                    .addQuestions(randomQuestion)
                    .build());
        } else {
            int count = Math.min(req.getCount(), questions.size());
            System.out.println("Returning " + count + " questions.");
            responseObserver.onNext(GetQuestionRes.newBuilder()
                    .addAllQuestions(questions.subList(0, count))
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void answerQuestion(AnswerQuestionReq req, StreamObserver<AnswerQuestionRes> responseObserver) {
        String question = req.getQuestion().trim();
        String userAnswer = req.getAnswer().trim();

        System.out.println("Received answerQuestion request.");
        System.out.println("Question: " + question);
        System.out.println("User's Answer: " + userAnswer);

        String correctAnswer = triviaDB.get(question);
        if (correctAnswer == null) {
            System.out.println("Question not found: " + question);
            responseObserver.onNext(AnswerQuestionRes.newBuilder()
                    .setIsCorrect(false)
                    .setMessage("Question not found.")
                    .build());
        } else if (correctAnswer.equalsIgnoreCase(userAnswer)) {
            System.out.println("User answered correctly: " + userAnswer);
            responseObserver.onNext(AnswerQuestionRes.newBuilder()
                    .setIsCorrect(true)
                    .setMessage("Correct! Well done.")
                    .build());
        } else {
            System.out.println("User answered incorrectly. Correct answer: " + correctAnswer);
            responseObserver.onNext(AnswerQuestionRes.newBuilder()
                    .setIsCorrect(false)
                    .setMessage("Incorrect. The correct answer is: " + correctAnswer)
                    .build());
        }
        responseObserver.onCompleted();
    }
}

