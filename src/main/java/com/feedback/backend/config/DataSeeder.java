package com.feedback.backend.config;

import com.feedback.backend.entity.Course;
import com.feedback.backend.entity.Feedback;
import com.feedback.backend.entity.User;
import com.feedback.backend.repository.CourseRepository;
import com.feedback.backend.repository.FeedbackRepository;
import com.feedback.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, CourseRepository courseRepository, FeedbackRepository feedbackRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                // Pre-seed some dummy data so the React frontend works immediately

                User admin = new User("Admin User", "admin@system.com", "admin123", "ADMIN");
                userRepository.save(admin);

                User student = new User("Student Person", "student@system.com", "student123", "STUDENT");
                userRepository.save(student);

                User drSmith = new User("Dr. Smith", "instructor@system.com", "instructor123", "INSTRUCTOR");
                User profJohnson = new User("Prof. Johnson", "johnson@system.com", "instructor123", "INSTRUCTOR");
                User drWilliams = new User("Dr. Williams", "williams@system.com", "instructor123", "INSTRUCTOR");
                userRepository.saveAll(List.of(drSmith, profJohnson, drWilliams));

                Course dbSystems = new Course("Advanced Database Systems", drSmith);
                Course cloudComp = new Course("Cloud Computing Concepts", profJohnson);
                Course mobileApp = new Course("Mobile App Development", drWilliams);
                courseRepository.saveAll(List.of(dbSystems, cloudComp, mobileApp));

                // Add Pending Feedbacks
                Feedback pending1 = new Feedback();
                pending1.setStudent(student);
                pending1.setCourse(dbSystems);
                pending1.setStatus("PENDING");
                
                Feedback pending2 = new Feedback();
                pending2.setStudent(student);
                pending2.setCourse(cloudComp);
                pending2.setStatus("PENDING");

                // Add Completed Feedback
                Feedback completed = new Feedback();
                completed.setStudent(student);
                completed.setCourse(mobileApp);
                completed.setStatus("SUBMITTED");
                completed.setRating(5);
                completed.setComments("Great course and excellent instructor!");

                feedbackRepository.saveAll(List.of(pending1, pending2, completed));

                System.out.println("====== DB Seeded Successfully ======");
            }
        };
    }
}
