# How to Discover URLs from Code

## Overview
Complete guide on how to trace through the codebase to find what URLs to call from the browser.

---

## Quick Reference - Finding URLs

### Method 1: Start from Angular Component
**Question:** "What API should I call from the browser?"

**Solution:** Look at `app.component.ts` → Find `this.studentService` calls

```typescript
// In app.component.ts
constructor(private studentService: StudentService) {}

createStudent() {
  this.studentService.createStudent(this.newStudent)  // ← Look here
    .subscribe({...});
}
```

### Method 2: Look in Angular Service
**File:** `frontend/src/app/services/student.service.ts`

```typescript
export class StudentService {
  private apiUrl = 'http://localhost:8080/api/students';  // ← URL IS HERE
  
  createStudent(student: Student): Observable<Student> {
    return this.http.post<Student>(this.apiUrl, student);
  }
  
  getAllStudents(): Observable<Student[]> {
    return this.http.get<Student[]>(this.apiUrl);  // ← Or here
  }
  
  getStudentById(id: number): Observable<Student> {
    return this.http.get<Student>(`${this.apiUrl}/${id}`);  // ← Or here
  }
}
```

**URLs Discovered:**
- `http://localhost:8080/api/students` (POST, GET)
- `http://localhost:8080/api/students/{id}` (GET, PUT, DELETE)

---

## Step-by-Step URL Discovery Process

### Step 1: Start with the Browser
**What user types in browser:** `http://localhost:4200`

### Step 2: Find the HTML Entry Point
**File to check:** `frontend/src/index.html`

```html
<!doctype html>
<html>
<head>
  <title>Student Management System</title>
</head>
<body>
  <app-root></app-root>  <!-- ← This is the component root -->
</body>
</html>
```

**Finding:** App root is `<app-root>` which corresponds to `AppComponent`

### Step 3: Find the Component
**File to check:** `frontend/src/main.ts`

```typescript
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, {
  providers: [provideHttpClient()],  // ← HTTP provider added
})
```

**Finding:** Main application component is `AppComponent`

### Step 4: Examine Component Methods
**File to check:** `frontend/src/app/app.component.ts`

```typescript
export class AppComponent implements OnInit {
  constructor(private studentService: StudentService) {}  // ← Service injected
  
  ngOnInit() {
    this.loadStudents();  // ← Called on load
  }
  
  loadStudents() {
    this.studentService.getAllStudents()  // ← Calls service method
      .subscribe({...});
  }
}
```

**Finding:** Component uses `StudentService.getAllStudents()`

### Step 5: Find the Service
**File to check:** `frontend/src/app/services/student.service.ts`

```typescript
@Injectable({
  providedIn: 'root'
})
export class StudentService {
  private apiUrl = 'http://localhost:8080/api/students';  // ← BASE URL
  
  getAllStudents(): Observable<Student[]> {
    return this.http.get<Student[]>(this.apiUrl);  // ← FULL URL
  }
}
```

**Discovery:**
- Base URL: `http://localhost:8080/api/students`
- Method: GET
- Full URL: `http://localhost:8080/api/students`

---

## Complete URL Map - Discovered Through Code

### CREATE Student
**From Component:**
```typescript
createStudent() {
  this.studentService.createStudent(this.newStudent)
}
```

**From Service:**
```typescript
createStudent(student: Student): Observable<Student> {
  return this.http.post<Student>(this.apiUrl, student);
  //                     ↑ Method    ↑ URL from apiUrl
}
```

**Final URL:** 
```
POST http://localhost:8080/api/students
```

### READ All Students
**From Component:**
```typescript
loadStudents() {
  this.studentService.getAllStudents()
}
```

**From Service:**
```typescript
getAllStudents(): Observable<Student[]> {
  return this.http.get<Student[]>(this.apiUrl);
}
```

**Final URL:**
```
GET http://localhost:8080/api/students
```

### READ Single Student
**From Component:**
```typescript
editStudent(student: Student) {
  // Would call getStudentById
}
```

**From Service:**
```typescript
getStudentById(id: number): Observable<Student> {
  return this.http.get<Student>(`${this.apiUrl}/${id}`);
  //                            ↑ Template string: apiUrl + "/" + id
}
```

**Final URL (if id=1):**
```
GET http://localhost:8080/api/students/1
```

### UPDATE Student
**From Component:**
```typescript
updateStudent() {
  this.studentService.updateStudent(id, student)
}
```

**From Service:**
```typescript
updateStudent(id: number, student: Student): Observable<Student> {
  return this.http.put<Student>(`${this.apiUrl}/${id}`, student);
}
```

**Final URL (if id=1):**
```
PUT http://localhost:8080/api/students/1
```

### DELETE Student
**From Component:**
```typescript
deleteStudent(id: number | undefined) {
  this.studentService.deleteStudent(id)
}
```

**From Service:**
```typescript
deleteStudent(id: number): Observable<void> {
  return this.http.delete<void>(`${this.apiUrl}/${id}`);
}
```

**Final URL (if id=1):**
```
DELETE http://localhost:8080/api/students/1
```

---

## Tracing Backend URLs

### Method 1: Find in Spring Boot Controller
**File to check:** `backend/src/main/java/com/example/controller/StudentController.java`

```java
@RestController
@RequestMapping("/api/students")  // ← BASE PATH
public class StudentController {
  
  @PostMapping                     // ← POST /api/students
  public ResponseEntity<Student> createStudent(@RequestBody Student student) {
  
  @GetMapping                      // ← GET /api/students
  public ResponseEntity<List<Student>> getAllStudents() {
  
  @GetMapping("/{id}")             // ← GET /api/students/{id}
  public ResponseEntity<Student> getStudentById(@PathVariable Long id) {
  
  @PutMapping("/{id}")             // ← PUT /api/students/{id}
  public ResponseEntity<Student> updateStudent(@PathVariable Long id, ...) {
  
  @DeleteMapping("/{id}")          // ← DELETE /api/students/{id}
  public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
}
```

**Discovered URLs:**
```
POST   /api/students
GET    /api/students
GET    /api/students/{id}
PUT    /api/students/{id}
DELETE /api/students/{id}
```

### Method 2: Check Application Configuration
**File to check:** `backend/src/main/java/com/example/HelloWorldApplication.java`

```java
@SpringBootApplication
public class HelloWorldApplication {
  
  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200")  // ← Which browser URLs allowed
                .allowedMethods("GET", "POST", "PUT", "DELETE");
      }
    };
  }
}
```

**Discovered:**
- Backend listens on: `http://localhost:8080` (default Spring Boot)
- Allows requests from: `http://localhost:4200` (Angular)
- Allowed methods: GET, POST, PUT, DELETE

### Method 3: Check Server Configuration
**File to check:** `backend/src/main/resources/application.yml`

```yaml
server:
  port: 8080            # ← Server runs on 8080
  servlet:
    context-path: /     # ← Base path is /
```

**Discovered:**
- Backend URL: `http://localhost:8080`

---

## Code Tracing Examples

### Example 1: "I want to create a student. What URL do I call?"

**Step 1:** Find Component
```bash
grep -r "createStudent" frontend/src/app/
# Output: app.component.ts
```

**Step 2:** Open Component
```typescript
// app.component.ts
createStudent() {
  this.studentService.createStudent(this.newStudent)
}
```

**Step 3:** Find Service
```bash
grep -r "StudentService" frontend/src/app/
# Output: services/student.service.ts
```

**Step 4:** Check Service
```typescript
// services/student.service.ts
private apiUrl = 'http://localhost:8080/api/students';

createStudent(student: Student): Observable<Student> {
  return this.http.post<Student>(this.apiUrl, student);
}
```

**Answer:** `POST http://localhost:8080/api/students`

---

### Example 2: "What's the full backend flow for reading a student?"

**Step 1:** Find Component method
```typescript
// app.component.ts
loadStudents() {
  this.studentService.getAllStudents()
}
```

**Step 2:** Find Service method
```typescript
// student.service.ts
getAllStudents(): Observable<Student[]> {
  return this.http.get<Student[]>(this.apiUrl);
}
```

**Step 3:** Find Controller method
```java
// StudentController.java
@GetMapping
public ResponseEntity<List<Student>> getAllStudents() {
  List<Student> students = studentService.getAllStudents();
  return ResponseEntity.ok(students);
}
```

**Step 4:** Find Service method
```java
// StudentServiceImpl.java
@Override
public List<Student> getAllStudents() {
  return studentRepository.findAll();
}
```

**Step 5:** Find Repository method
```java
// StudentRepository.java
public interface StudentRepository extends JpaRepository<Student, Long> {
  // findAll() inherited from JpaRepository
  // Generates SQL: SELECT * FROM student
}
```

**Full Flow:**
```
Browser → GET http://localhost:8080/api/students
  ↓
StudentController.getAllStudents()
  ↓
StudentService.getAllStudents()
  ↓
StudentRepository.findAll()
  ↓
SELECT * FROM student
  ↓
Returns List<Student>
  ↓
Browser receives JSON array
```

---

## Finding URLs - Quick Checklist

### For READ Operations
- [ ] Open `app.component.ts`
- [ ] Find method that loads data (e.g., `ngOnInit()`, `loadStudents()`)
- [ ] Find service call (e.g., `this.studentService.getAllStudents()`)
- [ ] Open `student.service.ts`
- [ ] Find the method (e.g., `getAllStudents()`)
- [ ] Look for `this.http.get(...)` or similar
- [ ] Extract the URL: `this.apiUrl` or computed URL

### For CREATE Operations
- [ ] Open `app.component.ts`
- [ ] Find submit handler (e.g., `createStudent()`, `onSubmit()`)
- [ ] Find service call (e.g., `this.studentService.createStudent(...)`)
- [ ] Open `student.service.ts`
- [ ] Find the method
- [ ] Look for `this.http.post(...)`
- [ ] Extract the URL and method

### For UPDATE Operations
- [ ] Open `app.component.ts`
- [ ] Find update handler (e.g., `updateStudent()`, `saveEdit()`)
- [ ] Find service call (e.g., `this.studentService.updateStudent(...)`)
- [ ] Open `student.service.ts`
- [ ] Look for `this.http.put(...)` or `patch(...)`
- [ ] Extract URL pattern: usually `${this.apiUrl}/${id}`

### For DELETE Operations
- [ ] Open `app.component.ts`
- [ ] Find delete handler (e.g., `deleteStudent()`)
- [ ] Find service call (e.g., `this.studentService.deleteStudent(...)`)
- [ ] Open `student.service.ts`
- [ ] Look for `this.http.delete(...)`
- [ ] Extract URL pattern: usually `${this.apiUrl}/${id}`

---

## Verifying URL with Backend Code

### Step 1: Check Controller Routing
```java
@RestController
@RequestMapping("/api/students")  // ← Base path
public class StudentController {
  
  @GetMapping("/{id}")           // ← Full path: /api/students/{id}
  public ResponseEntity<Student> getStudentById(@PathVariable Long id) {
    // ...
  }
}
```

### Step 2: Verify in Configuration
```yaml
server:
  port: 8080                     # ← Full URL: http://localhost:8080
```

### Step 3: Check CORS Settings
```java
registry.addMapping("/**")
        .allowedOrigins("http://localhost:4200")  // ← Angular can call backend
        .allowedMethods("GET", "POST", "PUT", "DELETE");
```

**Result:** `http://localhost:8080/api/students/{id}` is correct endpoint

---

## Complete URL Discovery Map

```
Browser User
    ↓
Enters: http://localhost:4200
    ↓
index.html loads
    ↓
app.component.ts initialized
    ↓
ngOnInit() → loadStudents()
    ↓
studentService.getAllStudents()
    ↓
private apiUrl = 'http://localhost:8080/api/students'
    ↓
this.http.get<Student[]>(this.apiUrl)
    ↓
HTTP GET → http://localhost:8080/api/students
    ↓
Spring Boot port 8080 receives
    ↓
@RequestMapping("/api/students")
@GetMapping
    ↓
HTTP GET /api/students matches
    ↓
StudentController.getAllStudents()
    ↓
studentService.getAllStudents()
    ↓
studentRepository.findAll()
    ↓
SELECT * FROM student
    ↓
Returns List<Student>
    ↓
HTTP 200 OK with JSON
    ↓
Browser receives response
```

---

## Tools to Help Discover URLs

### 1. IDE Search
**Search for HTTP methods:**
```bash
# In VSCode/IntelliJ
Ctrl+Shift+F → Search: "this.http.get"
Ctrl+Shift+F → Search: "this.http.post"
```

### 2. Browser DevTools
**After running the app:**
1. Open `http://localhost:4200` in browser
2. Open DevTools (F12)
3. Go to Network tab
4. Perform action (create, read, update, delete)
5. See the actual URL called

**Example Network Tab shows:**
```
GET http://localhost:8080/api/students 200 OK
POST http://localhost:8080/api/students 201 Created
PUT http://localhost:8080/api/students/1 200 OK
DELETE http://localhost:8080/api/students/1 204 No Content
```

### 3. cURL Commands
**After discovering URL, test with cURL:**

```bash
# Read all students
curl http://localhost:8080/api/students

# Read single student
curl http://localhost:8080/api/students/1

# Create student
curl -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -d '{"name":"John","email":"john@example.com","phoneNumber":"9876543210","gpa":3.8}'

# Update student
curl -X PUT http://localhost:8080/api/students/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane","email":"jane@example.com",...}'

# Delete student
curl -X DELETE http://localhost:8080/api/students/1
```

### 4. Postman/Insomnia
**REST Client tools:**
1. Create requests for each endpoint
2. Test locally
3. Save for documentation
4. Share with team

---

## Summary: Finding URLs

**Priority Order:**
1. ✅ Check `frontend/src/app/services/student.service.ts` (easiest)
2. ✅ Check `frontend/src/app/app.component.ts` (how it's used)
3. ✅ Check `backend/src/main/java/com/example/controller/StudentController.java` (verify)
4. ✅ Use Browser DevTools Network tab (confirm it works)

**Key Pattern:**
```
Service private apiUrl = 'BASE_URL'
  ↓
Service method: this.http.METHOD('URL', data)
  ↓
Full URL = BASE_URL + METHOD + PATH + ID
```

**Always verify:**
- ✅ Service URL matches controller mapping
- ✅ HTTP method matches (GET, POST, PUT, DELETE)
- ✅ CORS allows this origin
- ✅ Port is correct (8080)
- ✅ Path is correct (/api/students)
