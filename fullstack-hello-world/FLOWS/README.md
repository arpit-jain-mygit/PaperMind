# Student Management System - Flow Documentation

Complete end-to-end flow documentation for all CRUD operations and URL discovery.

---

## Quick Navigation

### 🚀 Application Bootstrap
- **[BOOTSTRAP-FLOW.md](BOOTSTRAP-FLOW.md)** - How URL loads HTML and bootstraps components ⭐ **START HERE**
  - Browser URL → Angular Dev Server → index.html → AppComponent
  - Component selector matching
  - Template rendering
  - How Angular finds and loads components
  - Complete file chain from URL to UI

### 🔄 CRUD Operation Flows
- **[CREATE-FLOW.md](CREATE-FLOW.md)** - Step-by-step flow for creating a student
  - User fills form → HTTP POST → Database INSERT → UI update
  - 20 detailed steps with code snippets
  - Shows service layer integration

- **[READ-FLOW.md](READ-FLOW.md)** - Step-by-step flow for reading students
  - Load all students → HTTP GET → Database SELECT → Table display
  - Both "Get All" and "Get by ID" flows
  - Shows filtering and error handling

- **[UPDATE-FLOW.md](UPDATE-FLOW.md)** - Step-by-step flow for updating a student
  - Edit form → Validation → HTTP PUT → Database UPDATE → UI refresh
  - Selective field updates
  - Concurrent update considerations

- **[DELETE-FLOW.md](DELETE-FLOW.md)** - Step-by-step flow for deleting a student
  - Delete button → Confirmation → HTTP DELETE → Database DELETE → Table update
  - Hard delete vs soft delete options
  - Error scenarios

### ⚡ Asynchronous Features
- **[KAFKA-NOTIFICATION-FLOW.md](KAFKA-NOTIFICATION-FLOW.md)** - Kafka notification flow
  - Student created → Event published to Kafka → Consumer processes → Notification sent
  - Event-driven architecture
  - Asynchronous messaging
  - Decoupled services

### 🤖 AI/GenAI Features
- **[AI-SUMMARY-FLOW.md](AI-SUMMARY-FLOW.md)** - AI-powered student summary ⭐ **NEW GenAI**
  - Click AI Summary button → Backend calls OpenAI → Generates insight → Display in modal
  - Integration with OpenAI GPT-3.5-turbo
  - Demonstrates GenAI in fullstack app
  - Example of prompt engineering

### 🔍 URL Discovery & Connection
- **[DISCOVER-URLs-FROM-CODE.md](DISCOVER-URLs-FROM-CODE.md)** - How to find API URLs from code
  - 5-step process to discover URLs
  - Complete code tracing examples
  - Tools to verify URLs
  - cURL and Postman examples

- **[DISCOVER-UI-Browser-URL.md](DISCOVER-UI-Browser-URL.md)** - How to find UI browser URL from code
  - Where is http://localhost:4200 defined
  - Angular CLI port configuration
  - Entry point HTML file
  - npm start command

- **[UI-vs-API-URLs.md](UI-vs-API-URLs.md)** - Comparison of UI and API URLs
  - Side-by-side comparison (4200 vs 8080)
  - Why two different ports
  - Configuration files for each
  - How they connect and communicate

---

## Flow Document Structure

Each flow document includes:

### 1. Overview
Brief description of the operation

### 2. Sequence Diagram
ASCII art showing data flow through layers:
```
Browser → Angular Component → Angular Service
   ↓
HTTP Request → Spring Boot Controller → Spring Boot Service
   ↓
Database Access → PostgreSQL → Result back through layers
   ↓
Browser UI Updated
```

### 3. Step-by-Step Breakdown
- Detailed numbered steps (15-20 steps per flow)
- Code snippets from actual files
- Variable values shown at each step
- SQL queries generated

### 4. Files Involved
Table showing:
- Layer (Frontend/Backend/Database)
- File path
- Method/Component name
- What changed

### 5. API Endpoints
- HTTP method and URL
- Request/response examples
- Status codes
- Error scenarios

### 6. Database Activity
- SQL queries executed
- Before/after table state
- Row counts

### 7. Error Scenarios
- Validation failures
- Database errors
- Network failures
- How errors are handled

---

## Quick Reference - URLs Discovered

| Operation | Method | URL | Code Location |
|-----------|--------|-----|------------------|
| Create Student | POST | `http://localhost:8080/api/students` | `StudentService.createStudent()` |
| Read All | GET | `http://localhost:8080/api/students` | `StudentService.getAllStudents()` |
| Read One | GET | `http://localhost:8080/api/students/{id}` | `StudentService.getStudentById()` |
| Update | PUT | `http://localhost:8080/api/students/{id}` | `StudentService.updateStudent()` |
| Delete | DELETE | `http://localhost:8080/api/students/{id}` | `StudentService.deleteStudent()` |

---

## Architecture Layers Involved

```
┌─────────────────────────────────────────────────────────────┐
│ BROWSER (http://localhost:4200)                             │
│ User Form → Angular Component                               │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ Service Layer
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ ANGULAR SERVICE (StudentService)                            │
│ Encapsulates HTTP calls + API URL                          │
│ Returns Observable<T>                                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ HTTP: POST, GET, PUT, DELETE
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ SPRING BOOT BACKEND (http://localhost:8080)                │
│ REST Controller → Service Layer → Repository               │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ JDBC / SQL
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ POSTGRESQL DATABASE (Port 5432)                             │
│ Tables, Queries, Data Persistence                          │
└─────────────────────────────────────────────────────────────┘
```

---

## How to Use These Docs

### For Understanding How the App Works (First Time)
1. Read BOOTSTRAP-FLOW.md (how URL loads the app)
2. Then read ARCHITECTURE.md (overall system design)
3. Then read CREATE-FLOW.md (simplest operation)

### For Understanding a Specific Operation
1. Open relevant flow doc (CREATE, READ, UPDATE, DELETE)
2. Read sequence diagram for high-level overview
3. Follow step-by-step breakdown for details
4. Check code snippets to see actual implementation

### For Finding Where to Make Changes
1. Identify the operation (create/read/update/delete)
2. Open that flow doc
3. Look at "Files Involved" table
4. Find the specific file and method to modify

### For Understanding URL Routing
1. Read BOOTSTRAP-FLOW.md (how http://localhost:4200 loads)
2. Read UI-vs-API-URLs.md (difference between 4200 and 8080)
3. Open `DISCOVER-URLs-FROM-CODE.md` (where URLs are defined)
4. Verify URL in Angular Service and Spring Boot Controller
5. Use Browser DevTools or cURL to test

### For Debugging Issues
1. Identify which operation fails (create/read/update/delete or page load)
2. Open that flow doc or BOOTSTRAP-FLOW.md
3. Look for "Error Scenarios" section
4. Check which validation or database operation failed
5. Look at corresponding code in the specified file

### For Onboarding New Team Members
1. Start with BOOTSTRAP-FLOW.md (how the page loads)
2. Read ARCHITECTURE.md (overall design)
3. Read CREATE-FLOW.md (simplest operation)
4. Check DISCOVER-URLs-FROM-CODE.md (where URLs come from)
5. Read other operation flows (READ, UPDATE, DELETE)
6. Explore actual source code with docs as reference

---

## File Locations

### Frontend
```
frontend/src/app/
├── app.component.ts                 (Presentation - UI & interactions)
├── services/
│   └── student.service.ts           (Service - HTTP & API)
├── index.html                       (Entry point)
└── main.ts                          (Bootstrap)
```

**Reference in flows:**
- CREATE-FLOW.md: Step 4-7, 17-20
- READ-FLOW.md: Step 2-4, 10-13
- UPDATE-FLOW.md: Step 2-5, 17-20
- DELETE-FLOW.md: Step 1-3, 11-15

### Backend
```
backend/src/main/java/com/example/
├── controller/
│   └── StudentController.java       (REST endpoints)
├── service/
│   ├── StudentService.java          (Interface)
│   └── StudentServiceImpl.java       (Implementation)
├── repository/
│   └── StudentRepository.java       (Data access)
├── entity/
│   └── Student.java                 (Model)
└── HelloWorldApplication.java       (App config)
```

**Reference in flows:**
- CREATE-FLOW.md: Step 8-16
- READ-FLOW.md: Step 4-8
- UPDATE-FLOW.md: Step 8-16
- DELETE-FLOW.md: Step 5-10

### Database
```
PostgreSQL (localhost:5432)
└── hello_world_db
    └── student table
```

**Reference in flows:**
- All flows: Step showing SQL execution
- Database Activity section

---

## Key Concepts Explained in Flows

### HTTP Methods
- **POST** - Create new resource
  - Ref: CREATE-FLOW.md Steps 7, 12
- **GET** - Read/retrieve resource
  - Ref: READ-FLOW.md Steps 3, 6
- **PUT** - Update entire resource
  - Ref: UPDATE-FLOW.md Steps 7, 12
- **DELETE** - Remove resource
  - Ref: DELETE-FLOW.md Steps 4, 8

### Status Codes
- **200 OK** - Successful GET/PUT
  - Ref: READ-FLOW.md, UPDATE-FLOW.md
- **201 CREATED** - Successful POST
  - Ref: CREATE-FLOW.md Step 16
- **204 No Content** - Successful DELETE
  - Ref: DELETE-FLOW.md Step 10
- **404 Not Found** - Resource doesn't exist
  - Ref: All flows, Error Scenarios

### Request/Response
- **Request Body** - Data sent to server (POST, PUT)
  - Ref: CREATE-FLOW.md Step 7, UPDATE-FLOW.md Step 7
- **Response Body** - Data sent back (GET, POST, PUT)
  - Ref: All flows, API Response section
- **Headers** - Metadata (Content-Type, CORS, etc.)
  - Ref: All flows, Communication section

### Validation
- **Frontend** - Client-side validation in component
  - Ref: CREATE-FLOW.md Step 5, UPDATE-FLOW.md Step 6
- **Backend** - Server-side validation in service
  - Ref: CREATE-FLOW.md Step 9, UPDATE-FLOW.md Step 10

### Error Handling
- **Try-Catch** - Catching exceptions
  - Ref: Backend service files
- **Subscribe Error** - RxJS error handling
  - Ref: CREATE-FLOW.md Step 10, Angular component
- **HTTP Status** - Error responses
  - Ref: All flows, Error Scenarios

---

## How Data Flows

### CREATE Flow
```
Form Data → Component State
  ↓
Service.createStudent(student)
  ↓
HTTP POST with JSON
  ↓
Controller receives @RequestBody
  ↓
Service validates & saves
  ↓
Repository.save()
  ↓
SQL INSERT
  ↓
Database generates ID
  ↓
Student object returned with ID
  ↓
HTTP 201 response
  ↓
Component adds to array
  ↓
UI updates with new student
```

### READ Flow
```
Component.loadStudents()
  ↓
Service.getAllStudents()
  ↓
HTTP GET request
  ↓
Controller routes to method
  ↓
Service calls repository
  ↓
SQL SELECT
  ↓
Results mapped to Student objects
  ↓
HTTP 200 with JSON array
  ↓
Component updates students array
  ↓
Template loops (*ngFor) and displays
```

### UPDATE Flow
```
Form pre-filled with current data
  ↓
User modifies fields
  ↓
Component validates
  ↓
Service.updateStudent(id, student)
  ↓
HTTP PUT with JSON
  ↓
Controller receives @PathVariable id
  ↓
Service fetches existing, updates fields
  ↓
Repository.save()
  ↓
SQL UPDATE WHERE id = X
  ↓
Updated Student returned
  ↓
HTTP 200 response
  ↓
Component updates array
  ↓
UI refreshes with new values
```

### DELETE Flow
```
User clicks Delete
  ↓
Confirmation dialog
  ↓
User confirms
  ↓
Service.deleteStudent(id)
  ↓
HTTP DELETE with id
  ↓
Controller receives @PathVariable id
  ↓
Service checks exists & deletes
  ↓
Repository.deleteById()
  ↓
SQL DELETE WHERE id = X
  ↓
HTTP 204 No Content
  ↓
Component filters out student
  ↓
Table updates without deleted row
```

---

## Code Examples in Flows

Each flow includes specific code examples:

### Service Layer Example (CREATE-FLOW.md)
```typescript
createStudent() {
  this.isCreating = true;
  this.studentService.createStudent(this.newStudent)
    .subscribe({
      next: (student) => {
        this.students.push(student);
        this.successMessage = `Student "${student.name}" created!`;
      }
    });
}
```

### Controller Example (READ-FLOW.md)
```java
@GetMapping("/{id}")
public ResponseEntity<Student> getStudentById(@PathVariable Long id) {
  Optional<Student> student = studentService.getStudentById(id);
  return student.map(ResponseEntity::ok)
          .orElseGet(() -> ResponseEntity.notFound().build());
}
```

### Service Example (UPDATE-FLOW.md)
```java
public Optional<Student> updateStudent(Long id, Student studentDetails) {
  return studentRepository.findById(id).map(student -> {
    if (studentDetails.getName() != null)
      student.setName(studentDetails.getName());
    return studentRepository.save(student);
  });
}
```

### SQL Example (DELETE-FLOW.md)
```sql
DELETE FROM student WHERE id = 1;
```

---

## Testing with Browser DevTools

Each flow can be verified using Browser DevTools:

1. **Open DevTools:** F12 or Right-click → Inspect
2. **Go to Network tab**
3. **Perform action:** Create, Read, Update, or Delete
4. **Observe:**
   - Request URL
   - HTTP Method
   - Status Code
   - Request/Response bodies

Example output:
```
POST /api/students                201 Created
GET /api/students                 200 OK
GET /api/students/1               200 OK
PUT /api/students/1               200 OK
DELETE /api/students/1            204 No Content
```

---

## Related Documentation

- **BOOTSTRAP-FLOW.md** - How URL loads HTML and bootstraps Angular components ⭐ **START HERE**
- **ARCHITECTURE.md** - Complete system architecture and layer responsibilities
- **E2E-FLOW.md** - Original comprehensive flow (now split into individual flows)
- **README.md** - Project overview and quick start guide
- **QUICKSTART.md** - Setup and run instructions

---

## Summary

These flow documents provide:
- ✅ Application bootstrap flow (URL → HTML → Component)
- ✅ Step-by-step tracing through all layers
- ✅ Code snippets showing actual implementation
- ✅ SQL queries generated by Hibernate
- ✅ HTTP requests/responses
- ✅ Error handling examples
- ✅ URL discovery guides (UI and API)
- ✅ Testing examples using browser tools
- ✅ Component selector matching
- ✅ Dependency injection flow

**Use them to:**
- Understand how the system works from page load to database
- Find where to make changes
- Discover API endpoints and UI configuration
- Debug issues at any layer
- Onboard new team members
- Document requirements
- Trace data flow through the entire stack

---

## Navigation Tips

- **First time learning?** → Start with BOOTSTRAP-FLOW.md (how page loads)
- **Confused about URLs?** → Read DISCOVER-URLs-FROM-CODE.md and UI-vs-API-URLs.md
- **Want to add a feature?** → Read relevant flow (CREATE, READ, UPDATE, DELETE)
- **System not working?** → Check Error Scenarios in relevant flow
- **Need to understand architecture?** → Read ARCHITECTURE.md first
- **Learning the codebase?** → Start with BOOTSTRAP-FLOW.md → CREATE-FLOW.md → others
- **Want to understand 4200 vs 8080?** → Read UI-vs-API-URLs.md

