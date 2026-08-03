package com.example.controller;

import com.example.entity.Student;
import com.example.service.StudentService;
import com.example.service.StudentSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentSummaryService summaryService;

    // CREATE - Add new student
    @PostMapping
    public ResponseEntity<Student> createStudent(@RequestBody Student student) {
        logger.info("API: POST /api/students - Creating student: {}", student.getName());
        try {
            Student savedStudent = studentService.createStudent(student);
            logger.info("API: POST /api/students - Student created successfully with ID: {}", savedStudent.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedStudent);
        } catch (Exception e) {
            logger.error("API: POST /api/students - Error creating student: {}", e.getMessage());
            throw e;
        }
    }

    // READ - Get all students
    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        logger.info("API: GET /api/students - Fetching all students");
        try {
            List<Student> students = studentService.getAllStudents();
            logger.info("API: GET /api/students - Retrieved {} students", students.size());
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            logger.error("API: GET /api/students - Error fetching students: {}", e.getMessage());
            throw e;
        }
    }

    // READ - Get student by ID
    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudentById(@PathVariable Long id) {
        logger.info("API: GET /api/students/{} - Fetching student", id);
        try {
            Optional<Student> student = studentService.getStudentById(id);
            if (student.isPresent()) {
                logger.info("API: GET /api/students/{} - Student found: {}", id, student.get().getName());
                return ResponseEntity.ok(student.get());
            } else {
                logger.warn("API: GET /api/students/{} - Student not found", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("API: GET /api/students/{} - Error fetching student: {}", id, e.getMessage());
            throw e;
        }
    }

    // UPDATE - Update student
    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable Long id, @RequestBody Student studentDetails) {
        logger.info("API: PUT /api/students/{} - Updating student", id);
        try {
            Optional<Student> updatedStudent = studentService.updateStudent(id, studentDetails);
            if (updatedStudent.isPresent()) {
                logger.info("API: PUT /api/students/{} - Student updated successfully", id);
                return ResponseEntity.ok(updatedStudent.get());
            } else {
                logger.warn("API: PUT /api/students/{} - Student not found for update", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("API: PUT /api/students/{} - Error updating student: {}", id, e.getMessage());
            throw e;
        }
    }

    // DELETE - Delete student
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        logger.info("API: DELETE /api/students/{} - Deleting student", id);
        try {
            if (studentService.deleteStudent(id)) {
                logger.info("API: DELETE /api/students/{} - Student deleted successfully", id);
                return ResponseEntity.noContent().build();
            } else {
                logger.warn("API: DELETE /api/students/{} - Student not found for deletion", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("API: DELETE /api/students/{} - Error deleting student: {}", id, e.getMessage());
            throw e;
        }
    }

    // AI SUMMARY - Generate AI summary for student
    @GetMapping("/{id}/summary")
    public ResponseEntity<String> generateStudentSummary(@PathVariable Long id) {
        logger.info("API: GET /api/students/{}/summary - Generating AI summary", id);
        try {
            Optional<Student> student = studentService.getStudentById(id);
            if (student.isEmpty()) {
                logger.warn("API: GET /api/students/{}/summary - Student not found", id);
                return ResponseEntity.notFound().build();
            }
            logger.info("API: GET /api/students/{}/summary - Calling OpenAI for student: {}", id, student.get().getName());
            String summary = summaryService.generateSummary(student.get());
            logger.info("API: GET /api/students/{}/summary - AI summary generated successfully", id);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("API: GET /api/students/{}/summary - Error generating summary: {}", id, e.getMessage());
            throw e;
        }
    }
}
