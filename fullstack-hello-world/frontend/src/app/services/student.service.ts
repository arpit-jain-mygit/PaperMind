import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Student {
  id?: number;
  name: string;
  email: string;
  phoneNumber: string;
  gpa: number;
}

@Injectable({
  providedIn: 'root'
})
export class StudentService {
  private apiUrl = 'http://localhost:8080/api/students';

  constructor(private http: HttpClient) {}

  // CREATE - Add new student
  createStudent(student: Student): Observable<Student> {
    return this.http.post<Student>(this.apiUrl, student);
  }

  // READ - Get all students
  getAllStudents(): Observable<Student[]> {
    return this.http.get<Student[]>(this.apiUrl);
  }

  // READ - Get student by ID
  getStudentById(id: number): Observable<Student> {
    return this.http.get<Student>(`${this.apiUrl}/${id}`);
  }

  // UPDATE - Update student
  updateStudent(id: number, student: Student): Observable<Student> {
    return this.http.put<Student>(`${this.apiUrl}/${id}`, student);
  }

  // DELETE - Delete student
  deleteStudent(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // AI - Get AI-generated summary for student
  getStudentSummary(id: number): Observable<string> {
    return this.http.get(`${this.apiUrl}/${id}/summary`, { responseType: 'text' });
  }

  // AGENT - Process batch student import
  processBatch(students: Student[]): Observable<any> {
    return this.http.post('http://localhost:8080/api/batch/create-students', { students });
  }

  // FEEDBACK - Get agent analytics
  getAgentAnalytics(): Observable<any> {
    return this.http.get('http://localhost:8080/api/agent/feedback/analytics');
  }

  // FEEDBACK - Provide feedback on a decision
  provideFeedback(decisionId: number, successful: boolean, feedback: string = '', failurePattern: string = ''): Observable<any> {
    const body: any = {
      successful,
      feedback
    };
    if (failurePattern) {
      body.failurePattern = failurePattern;
    }
    return this.http.post(`http://localhost:8080/api/agent/feedback/${decisionId}`, body);
  }

  // LEARNING - Analyze failure patterns and get AI recommendations
  analyzeLearning(): Observable<any> {
    return this.http.get('http://localhost:8080/api/agent/feedback/analyze');
  }

  // RULES - Get active validation rules
  getActiveRules(): Observable<any> {
    return this.http.get('http://localhost:8080/api/agent/rules/active');
  }

  // RULES - Get all validation rules
  getAllRules(): Observable<any> {
    return this.http.get('http://localhost:8080/api/agent/rules');
  }

  // RULES - Activate a rule
  activateRule(ruleId: number): Observable<any> {
    return this.http.post(`http://localhost:8080/api/agent/rules/${ruleId}/activate`, {});
  }

  // RULES - Deactivate a rule
  deactivateRule(ruleId: number): Observable<any> {
    return this.http.post(`http://localhost:8080/api/agent/rules/${ruleId}/deactivate`, {});
  }

  // FEEDBACK - Get pending decisions (no feedback yet)
  getPendingDecisions(): Observable<any> {
    return this.http.get('http://localhost:8080/api/agent/feedback/pending');
  }

  // FEEDBACK - Get feedback summary
  getFeedbackSummary(): Observable<any> {
    return this.http.get('http://localhost:8080/api/agent/feedback/summary');
  }

  // FEEDBACK - Get decisions for a student
  getStudentDecisions(email: string): Observable<any> {
    return this.http.get(`http://localhost:8080/api/agent/feedback/student/${email}`);
  }
}
