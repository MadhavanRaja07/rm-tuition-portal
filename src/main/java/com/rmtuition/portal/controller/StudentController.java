package com.rmtuition.portal.controller;

import com.rmtuition.portal.model.*;
import com.rmtuition.portal.repository.*;
import com.rmtuition.portal.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private QuestionPaperRepository questionPaperRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizResultRepository quizResultRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User getAuthenticatedStudent(UserDetails details) {
        return userRepository.findByEmail(details.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated student not found."));
    }

    // 1. MATERIALS (filtered to student's standard and matching subjects)
    @GetMapping("/materials")
    public ResponseEntity<?> getMaterials(@AuthenticationPrincipal UserDetails details) {
        User student = getAuthenticatedStudent(details);
        String standard = student.getStandard();
        if (standard == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Material> allMaterials = materialRepository.findByClassCode(standard);
        
        // Filter based on subjects available to this standard
        List<Material> filtered = allMaterials.stream()
                .filter(m -> AdminController.isSubjectAvailable(standard, m.getSubject()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(filtered);
    }

    // 2. PAPERS (filtered to student's standard and matching subjects)
    @GetMapping("/papers")
    public ResponseEntity<?> getPapers(@AuthenticationPrincipal UserDetails details) {
        User student = getAuthenticatedStudent(details);
        String standard = student.getStandard();
        if (standard == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<QuestionPaper> allPapers = questionPaperRepository.findByClassCode(standard);

        List<QuestionPaper> filtered = allPapers.stream()
                .filter(p -> AdminController.isSubjectAvailable(standard, p.getSubject()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(filtered);
    }

    // 3. QUIZZES (published quizzes matching student's standard and subjects)
    @GetMapping("/quizzes")
    public ResponseEntity<?> getQuizzes(@AuthenticationPrincipal UserDetails details) {
        User student = getAuthenticatedStudent(details);
        String standard = student.getStandard();
        if (standard == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Quiz> quizzes = quizRepository.findByClassCodeAndIsPublished(standard, true);
        
        List<Quiz> filtered = quizzes.stream()
                .filter(q -> AdminController.isSubjectAvailable(standard, q.getSubject()))
                .collect(Collectors.toList());

        // For list view, remove the actual questions details so they cannot scrape questions before starting!
        List<Map<String, Object>> listResponse = filtered.stream().map(q -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", q.getId());
            map.put("title", q.getTitle());
            map.put("subject", q.getSubject());
            map.put("durationMinutes", q.getDurationMinutes());
            map.put("startWindow", q.getStartWindow());
            map.put("endWindow", q.getEndWindow());
            map.put("questionCount", q.getQuestions() != null ? q.getQuestions().size() : 0);
            map.put("resultsReleased", q.isResultsReleased());
            
            // Check if already attempted
            boolean attempted = quizResultRepository.findByQuizIdAndStudentId(q.getId(), student.getId()).isPresent();
            map.put("attempted", attempted);
            
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(listResponse);
    }

    // GET QUIZ DETAILS FOR ATTEMPT (Only returns questions if within the timeline and not yet attempted)
    @GetMapping("/quizzes/{id}")
    public ResponseEntity<?> getQuizForAttempt(@PathVariable String id, @AuthenticationPrincipal UserDetails details) {
        User student = getAuthenticatedStudent(details);
        Optional<Quiz> quizOpt = quizRepository.findById(id);

        if (quizOpt.isPresent()) {
            Quiz quiz = quizOpt.get();
            if (!quiz.isPublished() || !quiz.getClassCode().equals(student.getStandard())) {
                return ResponseEntity.status(403).body(Map.of("message", "Access denied."));
            }

            // Check if already attempted
            if (quizResultRepository.findByQuizIdAndStudentId(id, student.getId()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "You have already attempted this quiz!"));
            }

            Date now = new Date();
            if (quiz.getStartWindow() != null && now.before(quiz.getStartWindow())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Quiz has not started yet."));
            }
            if (quiz.getEndWindow() != null && now.after(quiz.getEndWindow())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Quiz window has expired."));
            }

            // Return quiz metadata + questions list, but hide correct answers and explanations!
            Map<String, Object> response = new HashMap<>();
            response.put("id", quiz.getId());
            response.put("title", quiz.getTitle());
            response.put("subject", quiz.getSubject());
            response.put("durationMinutes", quiz.getDurationMinutes());

            List<Map<String, Object>> safeQuestions = new ArrayList<>();
            for (QuizQuestion qq : quiz.getQuestions()) {
                Map<String, Object> qMap = new HashMap<>();
                qMap.put("questionText", qq.getQuestionText());
                qMap.put("options", qq.getOptions());
                safeQuestions.add(qMap);
            }
            response.put("questions", safeQuestions);

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.notFound().build();
    }

    // SUBMIT ATTEMPT
    @PostMapping("/quizzes/{id}/attempt")
    public ResponseEntity<?> submitAttempt(
            @PathVariable String id,
            @RequestBody Map<Integer, Integer> answers, // Map of question index -> selected option index
            @AuthenticationPrincipal UserDetails details) {
        
        User student = getAuthenticatedStudent(details);
        Optional<Quiz> quizOpt = quizRepository.findById(id);

        if (quizOpt.isPresent()) {
            Quiz quiz = quizOpt.get();
            
            // Validate if already submitted
            if (quizResultRepository.findByQuizIdAndStudentId(id, student.getId()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Attempt already submitted."));
            }

            // Calculate Score
            int score = 0;
            List<QuizQuestion> questions = quiz.getQuestions();
            for (int i = 0; i < questions.size(); i++) {
                Integer selectedOption = answers.get(i);
                if (selectedOption != null && selectedOption == questions.get(i).getCorrectOptionIndex()) {
                    score++;
                }
            }

            QuizResult result = QuizResult.builder()
                    .quizId(quiz.getId())
                    .studentId(student.getId())
                    .studentName(student.getName())
                    .studentEmail(student.getEmail())
                    .score(score)
                    .totalQuestions(questions.size())
                    .answers(answers)
                    .submittedAt(new Date())
                    .build();

            quizResultRepository.save(result);

            return ResponseEntity.ok(Map.of(
                    "message", "Quiz attempt submitted successfully!",
                    "score", score,
                    "totalQuestions", questions.size()
            ));
        }

        return ResponseEntity.notFound().build();
    }

    // 4. ATTEMPT RESULTS (Accordion view by subject, only detailed if resultsReleased is true)
    @GetMapping("/results")
    public ResponseEntity<?> getResults(@AuthenticationPrincipal UserDetails details) {
        User student = getAuthenticatedStudent(details);
        List<QuizResult> attempts = quizResultRepository.findByStudentId(student.getId());

        List<Map<String, Object>> responseList = new ArrayList<>();

        for (QuizResult attempt : attempts) {
            Optional<Quiz> quizOpt = quizRepository.findById(attempt.getQuizId());
            if (quizOpt.isPresent()) {
                Quiz quiz = quizOpt.get();

                Map<String, Object> map = new HashMap<>();
                map.put("quizId", quiz.getId());
                map.put("quizTitle", quiz.getTitle());
                map.put("subject", quiz.getSubject());
                map.put("submittedAt", attempt.getSubmittedAt());
                map.put("resultsReleased", quiz.isResultsReleased());

                if (quiz.isResultsReleased()) {
                    // Safe to share scores and review details
                    map.put("score", attempt.getScore());
                    map.put("totalQuestions", attempt.getTotalQuestions());
                    
                    double percentage = (double) attempt.getScore() / attempt.getTotalQuestions() * 100;
                    map.put("percentage", Math.round(percentage * 100.0) / 100.0);

                    // Compile question details with student choice
                    List<Map<String, Object>> review = new ArrayList<>();
                    List<QuizQuestion> questions = quiz.getQuestions();
                    for (int i = 0; i < questions.size(); i++) {
                        QuizQuestion q = questions.get(i);
                        Map<String, Object> qReview = new HashMap<>();
                        qReview.put("questionText", q.getQuestionText());
                        qReview.put("options", q.getOptions());
                        qReview.put("correctOptionIndex", q.getCorrectOptionIndex());
                        qReview.put("explanation", q.getExplanation());
                        qReview.put("studentAnswer", attempt.getAnswers() != null ? attempt.getAnswers().get(i) : null);
                        review.add(qReview);
                    }
                    map.put("review", review);
                } else {
                    // Hide score and detail from payload
                    map.put("score", null);
                    map.put("totalQuestions", attempt.getTotalQuestions());
                    map.put("percentage", null);
                    map.put("review", Collections.emptyList());
                }

                responseList.add(map);
            }
        }

        // Group by subject
        Map<String, List<Map<String, Object>>> grouped = responseList.stream()
                .collect(Collectors.groupingBy(m -> (String) m.get("subject")));

        return ResponseEntity.ok(grouped);
    }

    // 5. PROFILE UPDATE
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody AdminController.ProfileUpdateRequest request, @AuthenticationPrincipal UserDetails details) {
        User student = getAuthenticatedStudent(details);
        
        if (request.getName() != null) student.setName(request.getName());
        if (request.getPhone() != null) student.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) student.setAvatarUrl(request.getAvatarUrl());
        if (request.getStandard() != null) student.setStandard(request.getStandard());
        
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            student.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(student);

        Map<String, Object> response = new HashMap<>();
        response.put("name", student.getName());
        response.put("phone", student.getPhone());
        response.put("standard", student.getStandard());
        response.put("avatarUrl", student.getAvatarUrl());
        
        return ResponseEntity.ok(response);
    }
}
