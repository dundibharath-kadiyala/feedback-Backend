package com.feedback.backend.controller;

import com.feedback.backend.entity.Course;
import com.feedback.backend.entity.Feedback;
import com.feedback.backend.entity.User;
import com.feedback.backend.repository.CourseRepository;
import com.feedback.backend.repository.FeedbackRepository;
import com.feedback.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allows React to connect
public class DashboardController {

    @Autowired
    private FeedbackRepository feedbackRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseRepository courseRepository;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        
        return userRepository.findByEmail(email)
            .filter(u -> u.getPassword().equals(password))
            .map(u -> {
                Map<String, Object> res = new HashMap<>();
                res.put("success", true);
                res.put("user", u);
                return res;
            })
            .orElseGet(() -> {
                Map<String, Object> res = new HashMap<>();
                res.put("success", false);
                res.put("message", "Invalid credentials");
                return res;
            });
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> res = new HashMap<>();
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            res.put("success", false);
            res.put("message", "Email already exists");
            return res;
        }
        User saved = userRepository.save(user);
        res.put("success", true);
        res.put("user", saved);
        return res;
    }

    /* ================= STUDENT ================= */
    @GetMapping("/student/{studentId}/dashboard")
    public Map<String, Object> getStudentDashboard(@PathVariable Long studentId) {
        List<Feedback> allFeedbacks = feedbackRepository.findByStudentId(studentId);
        
        long pendingCount = allFeedbacks.stream().filter(f -> "PENDING".equals(f.getStatus())).count();
        long submittedCount = allFeedbacks.stream().filter(f -> "SUBMITTED".equals(f.getStatus())).count();

        Map<String, Object> response = new HashMap<>();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingFeedback", pendingCount);
        stats.put("feedbackSubmitted", submittedCount);
        stats.put("attendance", "Top 10%");
        response.put("stats", stats);

        response.put("pendingList", allFeedbacks.stream().filter(f -> "PENDING".equals(f.getStatus())).toList());
        response.put("historyList", allFeedbacks.stream().filter(f -> "SUBMITTED".equals(f.getStatus())).toList());

        return response;
    }

    @PutMapping("/student/feedback/{feedbackId}")
    public Feedback submitFeedback(@PathVariable Long feedbackId, @RequestBody Map<String, Object> request) {
        return feedbackRepository.findById(feedbackId).map(f -> {
            f.setStatus("SUBMITTED");
            if (request.containsKey("rating")) {
                f.setRating(Integer.parseInt(request.get("rating").toString()));
            }
            if (request.containsKey("comments")) {
                f.setComments(request.get("comments").toString());
            }
            return feedbackRepository.save(f);
        }).orElseThrow();
    }

    /* ================= INSTRUCTOR ================= */
    @GetMapping("/instructor/{instructorId}/dashboard")
    public Map<String, Object> getInstructorDashboard(@PathVariable Long instructorId) {
        // Find all courses taught by this instructor
        List<Course> allCourses = courseRepository.findAll().stream()
                .filter(c -> c.getInstructor().getId().equals(instructorId)).toList();
        
        List<Feedback> allFeedbacks = feedbackRepository.findAll().stream()
                .filter(f -> "SUBMITTED".equals(f.getStatus()))
                .filter(f -> f.getCourse().getInstructor().getId().equals(instructorId))
                .toList();
        
        double avgRating = allFeedbacks.stream().mapToInt(Feedback::getRating).average().orElse(0.0);
        long totalStudents = allFeedbacks.size(); // Simplified
        
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> stats = new HashMap<>();
        stats.put("avgRating", String.format("%.1f", avgRating));
        stats.put("totalStudents", totalStudents);
        stats.put("participation", "85%"); // Mock logic
        
        response.put("stats", stats);
        response.put("courses", allCourses);
        // Map feedback to sentiment mock for UI
        var sentiments = allFeedbacks.stream().map(f -> {
            Map<String, Object> map = new HashMap<>();
            map.put("course", f.getCourse().getTitle());
            map.put("text", f.getComments() != null ? f.getComments() : "No comments");
            map.put("score", f.getRating());
            map.put("sentiment", f.getRating() >= 4 ? "Positive" : (f.getRating() == 3 ? "Neutral" : "Negative"));
            return map;
        }).toList();
        
        response.put("sentiments", sentiments);

        return response;
    }

    /* ================= ADMIN ================= */
    @GetMapping("/admin/dashboard")
    public Map<String, Object> getAdminDashboard() {
        List<User> students = userRepository.findByRole("STUDENT");
        List<User> instructors = userRepository.findByRole("INSTRUCTOR");
        List<Feedback> submittedFeedbacks = feedbackRepository.findAll().stream().filter(f -> "SUBMITTED".equals(f.getStatus())).toList();
        
        double avgRating = submittedFeedbacks.stream().mapToInt(Feedback::getRating).average().orElse(0.0);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", students.size());
        stats.put("totalInstructors", instructors.size());
        stats.put("averageRating", String.format("%.1f", avgRating));
        stats.put("activeForms", feedbackRepository.findAll().size() - submittedFeedbacks.size());
        
        // Find top instructors
        var topInstructors = instructors.stream().map(inst -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", inst.getName());
            map.put("dept", "Computer Science"); // Mock
            // Calculate avg
            double rating = feedbackRepository.findAll().stream()
                .filter(f -> "SUBMITTED".equals(f.getStatus()))
                .filter(f -> f.getCourse().getInstructor().getId().equals(inst.getId()))
                .mapToInt(Feedback::getRating).average().orElse(4.0); // Default to 4.0 if no ratings
            map.put("rating", String.format("%.1f", rating));
            return map;
        }).sorted((a,b) -> Double.compare(Double.parseDouble((String)b.get("rating")), Double.parseDouble((String)a.get("rating"))))
          .limit(5).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("stats", stats);
        response.put("topInstructors", topInstructors);
        return response;
    }

    /* ================= USER MANAGEMENT ================= */
    @PostMapping("/admin/deploy-feedback")
    public Map<String, String> deployFeedback(@RequestBody Map<String, Object> request) {
        List<User> students = userRepository.findByRole("STUDENT");
        List<Course> courses = courseRepository.findAll();
        
        String schema = request.containsKey("schema") ? request.get("schema").toString() : null;
        
        int count = 0;
        for (User student : students) {
            for (Course course : courses) {
                // Find if a pending feedback already exists
                Feedback existingPending = feedbackRepository.findByStudentId(student.getId()).stream()
                    .filter(f -> f.getCourse().getId().equals(course.getId()) && "PENDING".equals(f.getStatus()))
                    .findFirst().orElse(null);
                
                if (existingPending != null) {
                    // Update the schema of the existing pending form
                    existingPending.setFormSchema(schema);
                    feedbackRepository.save(existingPending);
                } else {
                    // Create a brand new pending form
                    Feedback pending = new Feedback();
                    pending.setStudent(student);
                    pending.setCourse(course);
                    pending.setStatus("PENDING");
                    pending.setFormSchema(schema);
                    feedbackRepository.save(pending);
                    count++;
                }
            }
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("message", count + " new feedback forms deployed successfully with custom schema!");
        return response;
    }

    @GetMapping("/admin/current-schema")
    public Map<String, Object> getCurrentSchema() {
        // Fetch any pending feedback to get the latest schema
        return feedbackRepository.findAll().stream()
            .filter(f -> f.getFormSchema() != null && "PENDING".equals(f.getStatus()))
            .findFirst()
            .map(f -> {
                Map<String, Object> res = new HashMap<>();
                res.put("schema", f.getFormSchema());
                return res;
            })
            .orElseGet(() -> {
                Map<String, Object> empty = new HashMap<>();
                empty.put("schema", "[]");
                return empty;
            });
    }

    @GetMapping("/admin/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @PostMapping("/admin/users")
    public Object createUser(@RequestBody User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return Map.of("error", "User with this email already exists");
        }
        return userRepository.save(user);
    }
    
    @PutMapping("/admin/users/{id}")
    public Object updateUser(@PathVariable Long id, @RequestBody User request) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            u.setRole(request.getRole());
            u.setName(request.getName());
            u.setEmail(request.getEmail());
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                u.setPassword(request.getPassword());
            }
            return userRepository.save(u);
        }
        return Map.of("error", "User not found");
    }
}
