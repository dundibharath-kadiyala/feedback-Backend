package com.feedback.backend.repository;

import com.feedback.backend.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByStudentId(Long studentId);
    List<Feedback> findByStudentIdAndStatus(Long studentId, String status);
    List<Feedback> findByCourseId(Long courseId);
}
