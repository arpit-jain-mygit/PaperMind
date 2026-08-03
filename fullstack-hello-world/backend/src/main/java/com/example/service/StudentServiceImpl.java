package com.example.service;

import com.example.entity.Student;
import com.example.repository.StudentRepository;
import com.example.mongodb.StudentDoc;
import com.example.mongodb.StudentDocRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class StudentServiceImpl implements StudentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentServiceImpl.class);

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentDocRepository mongoRepository;

    @Override
    public Student createStudent(Student student) {
        logger.info("CREATE: Starting to create student with name: {}", student.getName());

        if (student.getName() == null || student.getName().trim().isEmpty()) {
            logger.error("CREATE: Validation failed - Student name cannot be empty");
            throw new IllegalArgumentException("Student name cannot be empty");
        }
        if (student.getEmail() == null || student.getEmail().trim().isEmpty()) {
            logger.error("CREATE: Validation failed - Student email cannot be empty");
            throw new IllegalArgumentException("Student email cannot be empty");
        }
        if (student.getGpa() == null || student.getGpa() < 0 || student.getGpa() > 4.0) {
            logger.error("CREATE: Validation failed - Invalid GPA: {}", student.getGpa());
            throw new IllegalArgumentException("GPA must be between 0 and 4.0");
        }

        Student savedStudent = studentRepository.save(student);
        logger.info("CREATE: Student saved to PostgreSQL with ID: {}", savedStudent.getId());

        StudentDoc mongoDoc = new StudentDoc(
                savedStudent.getId(),
                savedStudent.getName(),
                savedStudent.getEmail(),
                savedStudent.getPhoneNumber(),
                savedStudent.getGpa()
        );
        mongoRepository.save(mongoDoc);
        logger.info("CREATE: Student saved to MongoDB with ID: {}", savedStudent.getId());

        return savedStudent;
    }

    @Override
    public List<Student> getAllStudents() {
        logger.info("READ: Fetching all students from PostgreSQL");
        List<Student> students = studentRepository.findAll();
        logger.info("READ: Retrieved {} students from PostgreSQL", students.size());
        return students;
    }

    @Override
    public Optional<Student> getStudentById(Long id) {
        logger.info("READ: Fetching student with ID: {} from PostgreSQL", id);

        if (id == null || id <= 0) {
            logger.error("READ: Invalid student ID: {}", id);
            throw new IllegalArgumentException("Invalid student ID");
        }

        Optional<Student> student = studentRepository.findById(id);
        if (student.isPresent()) {
            logger.info("READ: Student found with ID: {}, Name: {}", id, student.get().getName());
        } else {
            logger.warn("READ: Student not found with ID: {}", id);
        }
        return student;
    }

    @Override
    public Optional<Student> updateStudent(Long id, Student studentDetails) {
        logger.info("UPDATE: Starting update for student ID: {}", id);

        if (id == null || id <= 0) {
            logger.error("UPDATE: Invalid student ID: {}", id);
            throw new IllegalArgumentException("Invalid student ID");
        }

        return studentRepository.findById(id).map(student -> {
            logger.info("UPDATE: Found student ID {} ({}), applying changes", id, student.getName());

            if (studentDetails.getName() != null && !studentDetails.getName().trim().isEmpty()) {
                logger.debug("UPDATE: Changing name from {} to {}", student.getName(), studentDetails.getName());
                student.setName(studentDetails.getName());
            }
            if (studentDetails.getEmail() != null && !studentDetails.getEmail().trim().isEmpty()) {
                logger.debug("UPDATE: Changing email to {}", studentDetails.getEmail());
                student.setEmail(studentDetails.getEmail());
            }
            if (studentDetails.getPhoneNumber() != null && !studentDetails.getPhoneNumber().trim().isEmpty()) {
                logger.debug("UPDATE: Changing phone to {}", studentDetails.getPhoneNumber());
                student.setPhoneNumber(studentDetails.getPhoneNumber());
            }
            if (studentDetails.getGpa() != null && studentDetails.getGpa() >= 0 && studentDetails.getGpa() <= 4.0) {
                logger.debug("UPDATE: Changing GPA to {}", studentDetails.getGpa());
                student.setGpa(studentDetails.getGpa());
            }

            Student updated = studentRepository.save(student);
            logger.info("UPDATE: Student ID {} updated in PostgreSQL", id);

            StudentDoc mongoDoc = mongoRepository.findByPostgresId(id);
            if (mongoDoc != null) {
                mongoDoc.setName(updated.getName());
                mongoDoc.setEmail(updated.getEmail());
                mongoDoc.setPhoneNumber(updated.getPhoneNumber());
                mongoDoc.setGpa(updated.getGpa());
                mongoRepository.save(mongoDoc);
                logger.info("UPDATE: Student ID {} updated in MongoDB", id);
            } else {
                logger.warn("UPDATE: MongoDB document not found for student ID {}", id);
            }

            return updated;
        }).or(() -> {
            logger.warn("UPDATE: Student not found with ID: {}", id);
            return Optional.empty();
        });
    }

    @Override
    public boolean deleteStudent(Long id) {
        logger.info("DELETE: Starting delete for student ID: {}", id);

        if (id == null || id <= 0) {
            logger.error("DELETE: Invalid student ID: {}", id);
            throw new IllegalArgumentException("Invalid student ID");
        }

        if (studentRepository.existsById(id)) {
            studentRepository.deleteById(id);
            logger.info("DELETE: Student ID {} deleted from PostgreSQL", id);

            StudentDoc mongoDoc = mongoRepository.findByPostgresId(id);
            if (mongoDoc != null) {
                mongoRepository.delete(mongoDoc);
                logger.info("DELETE: Student ID {} deleted from MongoDB", id);
            } else {
                logger.warn("DELETE: MongoDB document not found for student ID {}", id);
            }

            return true;
        } else {
            logger.warn("DELETE: Student not found with ID: {}", id);
            return false;
        }
    }

    @Override
    public boolean studentExists(Long id) {
        if (id == null || id <= 0) {
            return false;
        }
        return studentRepository.existsById(id);
    }
}
