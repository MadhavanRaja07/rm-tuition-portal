package com.rmtuition.portal.controller;

import com.rmtuition.portal.model.*;
import com.rmtuition.portal.repository.*;
import com.rmtuition.portal.service.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

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
    private FileService fileService;

    @Autowired
    private AuthService authService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private QuizGeneratorService quizGeneratorService;

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Helper method to check if a subject exists in student's class
    public static boolean isSubjectAvailable(String classCode, String subject) {
        if ("10".equals(classCode)) {
            return List.of("Tamil", "English", "Mathematics", "Science", "Social Science").contains(subject);
        }
        if ("11-all".equals(classCode) || "12-all".equals(classCode)) {
            return List.of("Tamil", "English", "Mathematics", "Physics", "Chemistry", "Computer Science").contains(subject);
        }
        if ("11-pcm".equals(classCode) || "12-pcm".equals(classCode)) {
            return List.of("Physics", "Chemistry", "Mathematics").contains(subject);
        }
        if ("11-pc".equals(classCode) || "12-pc".equals(classCode)) {
            return List.of("Physics", "Chemistry").contains(subject);
        }
        if ("11-maths".equals(classCode) || "12-maths".equals(classCode)) {
            return List.of("Mathematics").contains(subject);
        }
        return false;
    }

    private boolean isGradeCompatible(String materialClass, String studentClass) {
        if (materialClass == null || studentClass == null) return false;
        if (materialClass.equals("10") && studentClass.equals("10")) return true;
        if (materialClass.startsWith("11") && studentClass.startsWith("11")) return true;
        if (materialClass.startsWith("12") && studentClass.startsWith("12")) return true;
        return false;
    }

    // 1. STATS
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long studentsCount = userRepository.findByRole(Role.ROLE_STUDENT).size();
        long materialsCount = materialRepository.count();
        long quizzesCount = quizRepository.count();
        long papersCount = questionPaperRepository.count();

        return ResponseEntity.ok(Map.of(
                "totalStudents", studentsCount,
                "totalMaterials", materialsCount,
                "totalQuizzes", quizzesCount,
                "totalPapers", papersCount
        ));
    }

    // 2. STUDENTS
    @GetMapping("/students")
    public ResponseEntity<?> getStudents(@RequestParam(required = false) String standard) {
        List<User> students;
        if (standard != null && !standard.trim().isEmpty()) {
            students = userRepository.findByRoleAndStandard(Role.ROLE_STUDENT, standard);
        } else {
            students = userRepository.findByRole(Role.ROLE_STUDENT);
        }
        return ResponseEntity.ok(students);
    }

    @PostMapping("/students")
    public ResponseEntity<?> createStudent(@RequestBody AuthController.RegisterRequest request) {
        try {
            User student = authService.adminCreateStudent(
                    request.getEmail(),
                    request.getPassword(),
                    request.getName(),
                    request.getPhone(),
                    request.getStandard()
            );
            return ResponseEntity.ok(student);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable String id) {
        Optional<User> studentOpt = userRepository.findById(id);
        if (studentOpt.isPresent() && studentOpt.get().getRole() == Role.ROLE_STUDENT) {
            // Delete student attempts too
            List<QuizResult> attempts = quizResultRepository.findByStudentId(id);
            quizResultRepository.deleteAll(attempts);
            
            userRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Student deleted successfully!"));
        }
        return ResponseEntity.notFound().build();
    }

    // 3. MATERIALS
    @PostMapping("/materials")
    public ResponseEntity<?> uploadMaterial(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("subject") String subject,
            @RequestParam("classCode") String classCode,
            @AuthenticationPrincipal UserDetails adminDetails) {
        try {
            String gridFsId = fileService.storeFile(file.getInputStream(), file.getOriginalFilename(), file.getContentType());
            
            Material material = Material.builder()
                    .classCode(classCode)
                    .subject(subject)
                    .title(title)
                    .fileName(file.getOriginalFilename())
                    .gridFsId(gridFsId)
                    .uploadedAt(new Date())
                    .uploadedBy(adminDetails.getUsername())
                    .build();

            materialRepository.save(material);

            // Notify compatible students
            List<User> students = userRepository.findByRole(Role.ROLE_STUDENT);
            for (User student : students) {
                if (isGradeCompatible(classCode, student.getStandard()) && isSubjectAvailable(student.getStandard(), subject)) {
                    notificationService.sendMaterialNotification(student.getEmail(), student.getStandard(), subject, title);
                }
            }

            return ResponseEntity.ok(material);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to upload file: " + e.getMessage()));
        }
    }

    @GetMapping("/materials")
    public ResponseEntity<?> getMaterials() {
        return ResponseEntity.ok(materialRepository.findAll());
    }

    @DeleteMapping("/materials/{id}")
    public ResponseEntity<?> deleteMaterial(@PathVariable String id) {
        Optional<Material> materialOpt = materialRepository.findById(id);
        if (materialOpt.isPresent()) {
            fileService.deleteFile(materialOpt.get().getGridFsId());
            materialRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Material deleted successfully!"));
        }
        return ResponseEntity.notFound().build();
    }

    // 4. PAPERS
    @PostMapping("/papers")
    public ResponseEntity<?> uploadPaper(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("subject") String subject,
            @RequestParam("classCode") String classCode,
            @AuthenticationPrincipal UserDetails adminDetails) {
        try {
            String gridFsId = fileService.storeFile(file.getInputStream(), file.getOriginalFilename(), file.getContentType());

            QuestionPaper paper = QuestionPaper.builder()
                    .classCode(classCode)
                    .subject(subject)
                    .title(title)
                    .fileName(file.getOriginalFilename())
                    .gridFsId(gridFsId)
                    .uploadedAt(new Date())
                    .uploadedBy(adminDetails.getUsername())
                    .build();

            questionPaperRepository.save(paper);

            // Notify compatible students
            List<User> students = userRepository.findByRole(Role.ROLE_STUDENT);
            for (User student : students) {
                if (isGradeCompatible(classCode, student.getStandard()) && isSubjectAvailable(student.getStandard(), subject)) {
                    notificationService.sendPaperNotification(student.getEmail(), student.getStandard(), subject, title);
                }
            }

            return ResponseEntity.ok(paper);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to upload practice paper: " + e.getMessage()));
        }
    }

    @GetMapping("/papers")
    public ResponseEntity<?> getPapers() {
        return ResponseEntity.ok(questionPaperRepository.findAll());
    }

    @DeleteMapping("/papers/{id}")
    public ResponseEntity<?> deletePaper(@PathVariable String id) {
        Optional<QuestionPaper> paperOpt = questionPaperRepository.findById(id);
        if (paperOpt.isPresent()) {
            fileService.deleteFile(paperOpt.get().getGridFsId());
            questionPaperRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Question paper deleted successfully!"));
        }
        return ResponseEntity.notFound().build();
    }

    // 5. QUIZZES
    @PostMapping("/quizzes")
    public ResponseEntity<?> createQuiz(@RequestBody Quiz quiz, @AuthenticationPrincipal UserDetails adminDetails) {
        quiz.setCreatedAt(new Date());
        quiz.setCreatedBy(adminDetails.getUsername());
        quiz.setPublished(false);
        quiz.setResultsReleased(false);
        Quiz savedQuiz = quizRepository.save(quiz);
        return ResponseEntity.ok(savedQuiz);
    }

    @PostMapping("/quizzes/generate")
    public ResponseEntity<?> generateQuizQuestions(@RequestBody QuizGenerateRequest request) {
        List<QuizQuestion> questions = quizGeneratorService.generateQuestions(
                request.getClassCode(),
                request.getSubject(),
                request.getTopic(),
                request.getQuestionCount()
        );
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/quizzes")
    public ResponseEntity<?> getQuizzes() {
        return ResponseEntity.ok(quizRepository.findAll());
    }

    @DeleteMapping("/quizzes/{id}")
    public ResponseEntity<?> deleteQuiz(@PathVariable String id) {
        if (quizRepository.existsById(id)) {
            // Delete attempts
            List<QuizResult> attempts = quizResultRepository.findByQuizId(id);
            quizResultRepository.deleteAll(attempts);

            quizRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Quiz deleted successfully!"));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/quizzes/{id}/publish")
    public ResponseEntity<?> publishQuiz(@PathVariable String id) {
        Optional<Quiz> quizOpt = quizRepository.findById(id);
        if (quizOpt.isPresent()) {
            Quiz quiz = quizOpt.get();
            quiz.setPublished(true);
            quizRepository.save(quiz);

            // Notify compatible students
            List<User> students = userRepository.findByRole(Role.ROLE_STUDENT);
            for (User student : students) {
                if (isGradeCompatible(quiz.getClassCode(), student.getStandard()) && isSubjectAvailable(student.getStandard(), quiz.getSubject())) {
                    notificationService.sendQuizNotification(student.getEmail(), student.getStandard(), quiz.getSubject(), quiz.getTitle());
                }
            }

            return ResponseEntity.ok(Map.of("message", "Quiz published successfully and notifications sent!"));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/quizzes/{id}/release-results")
    public ResponseEntity<?> releaseResults(@PathVariable String id) {
        Optional<Quiz> quizOpt = quizRepository.findById(id);
        if (quizOpt.isPresent()) {
            Quiz quiz = quizOpt.get();
            quiz.setResultsReleased(true);
            quizRepository.save(quiz);
            return ResponseEntity.ok(Map.of("message", "Quiz results released successfully!"));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/quizzes/{id}/results")
    public ResponseEntity<?> getQuizResults(@PathVariable String id) {
        return ResponseEntity.ok(quizResultRepository.findByQuizId(id));
    }

    @GetMapping("/quizzes/{id}/export")
    public ResponseEntity<byte[]> exportQuizAttempts(@PathVariable String id) {
        Optional<Quiz> quizOpt = quizRepository.findById(id);
        if (quizOpt.isPresent()) {
            Quiz quiz = quizOpt.get();
            List<QuizResult> results = quizResultRepository.findByQuizId(id);
            
            byte[] excelBytes = excelExportService.exportQuizResultsToExcel(results, quiz.getTitle());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename(quiz.getTitle().replaceAll("\\s+", "_") + "_results.xlsx")
                    .build());
            
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        }
        return ResponseEntity.notFound().build();
    }

    // 6. PROFILE
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileUpdateRequest request, @AuthenticationPrincipal UserDetails adminDetails) {
        Optional<User> adminOpt = userRepository.findByEmail(adminDetails.getUsername());
        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();
            if (request.getName() != null) admin.setName(request.getName());
            if (request.getPhone() != null) admin.setPhone(request.getPhone());
            if (request.getAvatarUrl() != null) admin.setAvatarUrl(request.getAvatarUrl());
            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                admin.setPassword(passwordEncoder.encode(request.getPassword()));
            }
            userRepository.save(admin);
            
            Map<String, Object> response = new HashMap<>();
            response.put("name", admin.getName());
            response.put("phone", admin.getPhone());
            response.put("avatarUrl", admin.getAvatarUrl());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).build();
    }

    @Data
    public static class QuizGenerateRequest {
        private String classCode;
        private String subject;
        private String topic;
        private int questionCount;
    }

    @Data
    public static class ProfileUpdateRequest {
        private String name;
        private String phone;
        private String avatarUrl;
        private String password;
        private String standard; // Student profile update standard
    }
}
