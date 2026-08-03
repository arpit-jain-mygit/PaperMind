# UPDATE Flow - Student Management System

## Overview
Complete end-to-end flow for updating an existing student record.

---

## Sequence Diagram

```
USER BROWSER                    ANGULAR (4200)              SPRING BOOT (8080)          POSTGRESQL (5432)
     │                              │                            │                           │
     │ 1. Clicks Edit button        │                            │                           │
     ├─────────────────────────────►│                            │                           │
     │                              │                            │                           │
     │                              │ 2. Loads student data      │                           │
     │                              │    GET /api/students/{id}  │                           │
     │                              ├───────────────────────────►│                           │
     │                              │                            │                           │
     │                              │◄───────────────────────────┤                           │
     │                              │ Returns student data       │                           │
     │                              │                            │                           │
     │ 3. Shows edit form with data │                            │                           │
     │◄─────────────────────────────                            │                           │
     │                              │                            │                           │
     │ 4. Modifies fields           │                            │                           │
     │    (name, email, phone, gpa) │                            │                           │
     ├─────────────────────────────►│                            │                           │
     │                              │                            │                           │
     │ 5. Clicks "Update Student"   │                            │                           │
     ├─────────────────────────────►│                            │                           │
     │                              │                            │                           │
     │                              │ 6. Validates form          │                           │
     │                              │    - Same as CREATE        │                           │
     │                              │                            │                           │
     │                              │ 7. HTTP PUT request        │                           │
     │                              │    /api/students/{id}      │                           │
     │                              ├───────────────────────────►│                           │
     │                              │                            │                           │
     │                              │                            │ 8. StudentController      │
     │                              │                            │    receives PUT           │
     │                              │                            │    id=1                   │
     │                              │                            │                           │
     │                              │                            │ 9. Fetches existing      │
     │                              │                            │    student by id          │
     │                              │                            │                           │
     │                              │                            │ 10. Updates fields       │
     │                              │                            │     if provided           │
     │                              │                            │                           │
     │                              │                            │ 11. Calls repo.save()    │
     │                              │                            ├──────────────────────────►│
     │                              │                            │                           │
     │                              │                            │                           │ 12. UPDATE executed
     │                              │                            │                           │     WHERE id = 1
     │                              │                            │                           │
     │                              │                            │ 13. Returns updated      │
     │                              │                            │     Student              │
     │                              │◄──────────────────────────┤                           │
     │                              │                            │                           │
     │                              │ 14. Returns 200 OK         │                           │
     │                              │     with updated student   │                           │
     │◄──────────────────────────────                            │                           │
     │                              │                            │                           │
     │ 15. Display success message  │                            │                           │
     │ 16. Update students list     │                            │                           │
     │ 17. Close edit form          │                            │                           │
```

---

## Step-by-Step Flow

### Step 1: User Clicks Edit Button
**File:** `frontend/src/app/app.component.ts`
```html
<button (click)="editStudent(student)">Edit</button>
```

### Step 2: Get Student Details
```typescript
editStudent(student: Student) {
  this.http.get<Student>(`http://localhost:8080/api/students/${student.id}`)
    .subscribe({
      next: (data) => {
        this.editingStudent = { ...data };  // Copy for editing
        this.showEditForm = true;
      }
    });
}
```

### Step 3: Display Edit Form
Form pre-filled with student data:
```html
<form *ngIf="showEditForm" (ngSubmit)="updateStudent()">
  <input [(ngModel)]="editingStudent.name" name="name" />
  <input [(ngModel)]="editingStudent.email" name="email" />
  <input [(ngModel)]="editingStudent.phoneNumber" name="phoneNumber" />
  <input type="number" [(ngModel)]="editingStudent.gpa" name="gpa" />
  <button type="submit">Update Student</button>
  <button type="button" (click)="cancelEdit()">Cancel</button>
</form>
```

### Step 4: User Modifies Fields
Original values:
```
name: "John Doe"
email: "john@example.com"
phoneNumber: "9876543210"
gpa: 3.8
```

Updated values:
```
name: "John Doe Jr."
email: "john.jr@example.com"
phoneNumber: "9876543210"
gpa: 3.9
```

### Step 5: User Clicks "Update Student"
Triggers `updateStudent()` method

### Step 6: Frontend Validates
```typescript
private isValidStudent(student: Student): boolean {
  return student.name?.trim().length > 0 &&
         student.email?.trim().length > 0 &&
         student.phoneNumber?.trim().length > 0 &&
         student.gpa >= 0 && student.gpa <= 4;
}

if (!this.isValidStudent(this.editingStudent)) {
  this.errorMessage = 'Please fill all fields correctly';
  return;
}
```

### Step 7: HTTP PUT Request
**File:** `frontend/src/app/app.component.ts`
```typescript
this.http.put<Student>(
  `http://localhost:8080/api/students/${this.editingStudent.id}`,
  this.editingStudent
)
```

**Request Details:**
- **Method:** PUT
- **URL:** http://localhost:8080/api/students/1
- **Headers:** Content-Type: application/json
- **Body:**
```json
{
  "name": "John Doe Jr.",
  "email": "john.jr@example.com",
  "phoneNumber": "9876543210",
  "gpa": 3.9
}
```

### Step 8: Backend Receives PUT Request
**File:** `backend/src/main/java/com/example/controller/StudentController.java`
```java
@PutMapping("/{id}")
public ResponseEntity<Student> updateStudent(
  @PathVariable Long id,
  @RequestBody Student studentDetails
) {
```

- `@PathVariable Long id` extracts id from URL (1)
- `@RequestBody Student studentDetails` deserializes JSON
- CORS validation passes

### Step 9: Fetch Existing Student
```java
Optional<Student> existingStudent = studentRepository.findById(id);

if (existingStudent.isPresent()) {
  Student student = existingStudent.get();
```

**Database Query:**
```sql
SELECT * FROM student WHERE id = 1;
```

**Returns:**
```
Student {
  id: 1,
  name: "John Doe",
  email: "john@example.com",
  phoneNumber: "9876543210",
  gpa: 3.8
}
```

### Step 10: Update Fields (Selective Update)
```java
if (studentDetails.getName() != null) 
  student.setName(studentDetails.getName());
if (studentDetails.getEmail() != null) 
  student.setEmail(studentDetails.getEmail());
if (studentDetails.getPhoneNumber() != null) 
  student.setPhoneNumber(studentDetails.getPhoneNumber());
if (studentDetails.getGpa() != null) 
  student.setGpa(studentDetails.getGpa());
```

**After Updates:**
```
Student {
  id: 1,
  name: "John Doe Jr.",
  email: "john.jr@example.com",
  phoneNumber: "9876543210",
  gpa: 3.9
}
```

### Step 11: Save Updated Student
```java
Student updatedStudent = studentRepository.save(student);
```

### Step 12: Hibernate Generates SQL
```sql
UPDATE student
SET name = 'John Doe Jr.',
    email = 'john.jr@example.com',
    phone_number = '9876543210',
    gpa = 3.9
WHERE id = 1;
```

### Step 13: Database Executes UPDATE
**PostgreSQL** executes the UPDATE query

**Before:**
```
 id │ name     │ email            │ phone_number │ gpa 
────┼──────────┼──────────────────┼──────────────┼─────
  1 │ John Doe │ john@example.com │ 9876543210   │ 3.8
```

**After:**
```
 id │ name          │ email              │ phone_number │ gpa 
────┼───────────────┼────────────────────┼──────────────┼─────
  1 │ John Doe Jr.  │ john.jr@example.com│ 9876543210   │ 3.9
```

### Step 14: Database Returns Updated Record
Row count: 1 updated

### Step 15: Hibernate Retrieves Updated Student
```java
Student updatedStudent {
  id: 1,
  name: "John Doe Jr.",
  email: "john.jr@example.com",
  phoneNumber: "9876543210",
  gpa: 3.9
}
```

### Step 16: Controller Returns Response
```java
return ResponseEntity.ok(updatedStudent);
```

**Response Details:**
- **Status:** 200 OK
- **Headers:** Content-Type: application/json
- **Body:**
```json
{
  "id": 1,
  "name": "John Doe Jr.",
  "email": "john.jr@example.com",
  "phoneNumber": "9876543210",
  "gpa": 3.9
}
```

### Step 17: Frontend Receives Response
```typescript
.subscribe({
  next: (student) => {
    // Update local list
    const index = this.students.findIndex(s => s.id === student.id);
    if (index !== -1) {
      this.students[index] = student;
    }
    this.successMessage = 'Student updated successfully!';
    this.showEditForm = false;
    this.editingStudent = null;
  },
  error: (err) => {
    this.errorMessage = 'Failed to update student';
  }
})
```

### Step 18: Update Component State
```typescript
// Find and update in local array
const index = this.students.findIndex(s => s.id === student.id);
if (index !== -1) {
  this.students[index] = student;  // Update in list
}

this.successMessage = 'Student updated successfully!';
this.showEditForm = false;  // Close edit form
this.editingStudent = null;  // Clear edit object
```

### Step 19: Trigger Change Detection
Angular re-renders with updated data

### Step 20: Display Success
- Green success message: "Student updated successfully!"
- Edit form closes
- Updated student appears in table with new values

---

## Files Involved

| Layer | File | Method/Component |
|-------|------|------------------|
| **Frontend** | `app.component.ts` | `editStudent()` |
| **Frontend** | `app.component.ts` | `updateStudent()` |
| **Frontend** | `app.component.ts` | `cancelEdit()` |
| **Backend** | `StudentController.java` | `updateStudent(@PathVariable, @RequestBody)` |
| **Backend** | `StudentRepository.java` | `findById(Long)` |
| **Backend** | `StudentRepository.java` | `save(Student)` |
| **Backend** | `Student.java` | Entity setters |
| **Config** | `application.yml` | Database connection |
| **Database** | PostgreSQL | UPDATE statement |

---

## SQL Generated

### UPDATE Single Student
```sql
UPDATE student
SET name = 'John Doe Jr.',
    email = 'john.jr@example.com',
    phone_number = '9876543210',
    gpa = 3.9
WHERE id = 1;
```

### FETCH Before Update
```sql
SELECT * FROM student WHERE id = 1;
```

---

## API Response

### PUT /api/students/1
**Status:** 200 OK
```json
{
  "id": 1,
  "name": "John Doe Jr.",
  "email": "john.jr@example.com",
  "phoneNumber": "9876543210",
  "gpa": 3.9
}
```

### PUT /api/students/999 (Not Found)
**Status:** 404 Not Found
```json
{
  "timestamp": "2024-01-15T11:00:00.000+00:00",
  "status": 404,
  "error": "Not Found"
}
```

---

## Error Scenarios

### Student Not Found
```java
if (existingStudent.isPresent()) {
  // ... update
} else {
  return ResponseEntity.notFound().build();  // 404
}
```

**Frontend:** Shows error message

### Validation Failed
```typescript
if (!this.isValidStudent(this.editingStudent)) {
  this.errorMessage = 'Please fill all fields correctly';
  return;  // Don't send request
}
```

### Concurrent Update
Two users update same student simultaneously
- Last write wins (current implementation)
- Future: Add version/timestamp for optimistic locking

### Database Constraint Violation
Example: Unique email constraint
```sql
UPDATE student SET email = 'john.jr@example.com' WHERE id = 1;
-- Error: Duplicate key value violates unique constraint
```
**Response:** 409 Conflict

---

## Database Activity

### Before UPDATE
```
 id │ name     │ email            │ phone_number │ gpa 
────┼──────────┼──────────────────┼──────────────┼─────
  1 │ John Doe │ john@example.com │ 9876543210   │ 3.8
```

### During UPDATE
```
UPDATE student
SET name = 'John Doe Jr.',
    email = 'john.jr@example.com',
    gpa = 3.9
WHERE id = 1;
```

### After UPDATE
```
 id │ name          │ email              │ phone_number │ gpa 
────┼───────────────┼────────────────────┼──────────────┼─────
  1 │ John Doe Jr.  │ john.jr@example.com│ 9876543210   │ 3.9
```

---

## Summary

**User Action:** Click "Edit" → modify fields → Click "Update"

**Path Through System:**
1. Frontend HTTP GET to fetch current data
2. Display edit form with pre-filled values
3. User modifies fields
4. Frontend validates form
5. HTTP PUT request with updated data
6. Backend fetches existing student
7. Backend updates only provided fields
8. JPA/Hibernate generates UPDATE SQL
9. PostgreSQL executes UPDATE
10. Updated record returned
11. Frontend updates local list
12. UI displays success message and updated values

**Status Codes:**
- ✅ 200 OK - Student successfully updated
- ❌ 404 Not Found - Student ID doesn't exist
- ❌ 409 Conflict - Data constraint violation
- ❌ 500 INTERNAL SERVER ERROR - Database error

---

## Improvement Opportunities

### Partial Updates (Current)
Only provided fields are updated
```java
if (studentDetails.getName() != null)
  student.setName(studentDetails.getName());
```

### Full Replacement (Alternative)
All fields replaced regardless
```java
student.setName(studentDetails.getName());
student.setEmail(studentDetails.getEmail());
// ... all fields always updated
```

### Optimistic Locking (Future)
Prevent concurrent update conflicts
```java
@Version
private Long version;

// Update only if version matches
```

### Batch Updates (Future)
Update multiple students at once
```
PATCH /api/students
[
  { "id": 1, "gpa": 3.9 },
  { "id": 2, "gpa": 4.0 }
]
```
