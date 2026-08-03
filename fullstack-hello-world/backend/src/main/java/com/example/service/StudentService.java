package com.example.service;

import com.example.entity.Student;
import java.util.List;
import java.util.Optional;

public interface StudentService {

    // CREATE - Save new student
    Student createStudent(Student student);

    // READ - Get all students
    List<Student> getAllStudents();

    // READ - Get student by ID
    Optional<Student> getStudentById(Long id);

    // UPDATE - Update existing student
    Optional<Student> updateStudent(Long id, Student studentDetails);

    // DELETE - Delete student
    boolean deleteStudent(Long id);

    // UTILITY - Check if student exists
    boolean studentExists(Long id);
}
