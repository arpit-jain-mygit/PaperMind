# DELETE Flow - Student Management System

## Overview
Complete end-to-end flow for deleting a student record.

---

## Sequence Diagram

```
USER BROWSER                    ANGULAR (4200)              SPRING BOOT (8080)          POSTGRESQL (5432)
     │                              │                            │                           │
     │ 1. Clicks Delete button      │                            │                           │
     ├─────────────────────────────►│                            │                           │
     │                              │                            │                           │
     │                              │ 2. Shows confirmation      │                           │
     │                              │    "Are you sure?"         │                           │
     │◄─────────────────────────────                            │                           │
     │                              │                            │                           │
     │ 3. Clicks OK on confirmation │                            │                           │
     ├─────────────────────────────►│                            │                           │
     │                              │                            │                           │
     │                              │ 4. HTTP DELETE request     │                           │
     │                              │    /api/students/1         │                           │
     │                              ├───────────────────────────►│                           │
     │                              │                            │                           │
     │                              │                            │ 5. StudentController      │
     │                              │                            │    receives DELETE       │
     │                              │                            │    id=1                   │
     │                              │                            │                           │
     │                              │                            │ 6. Check if exists       │
     │                              │                            │    repo.existsById(1)    │
     │                              │                            │                           │
     │                              │                            │ 7. Delete by id          │
     │                              │                            │    repo.deleteById(1)    │
     │                              │                            ├──────────────────────────►│
     │                              │                            │                           │
     │                              │                            │                           │ 8. DELETE executed
     │                              │                            │                           │    WHERE id = 1
     │                              │                            │                           │
     │                              │                            │ 9. Returns row count 1  │
     │                              │                            │    (1 row deleted)       │
     │                              │                            │◄──────────────────────────┤
     │                              │                            │                           │
     │                              │ 10. Returns 204 No Content │                           │
     │                              │     (Success, no body)     │                           │
     │◄──────────────────────────────                            │                           │
     │                              │                            │                           │
     │ 11. Remove from list         │                            │                           │
     │ 12. Display success message  │                            │                           │
     │ 13. Update UI                │                            │                           │
```

---

## Step-by-Step Flow

### Step 1: User Clicks Delete Button
**File:** `frontend/src/app/app.component.ts`
```html
<button (click)="deleteStudent(student.id)">Delete</button>
```

### Step 2: Show Confirmation Dialog
```typescript
deleteStudent(id: number | undefined) {
  if (!id || !confirm('Are you sure you want to delete this student?')) 
    return;
```

**Browser Alert:**
```
┌─────────────────────────────────────┐
│ Are you sure you want to delete     │
│ this student?                       │
│                                     │
│    [OK]  [Cancel]                   │
└─────────────────────────────────────┘
```

### Step 3: User Confirms Deletion
If user clicks "OK", proceeds with deletion

### Step 4: HTTP DELETE Request
**File:** `frontend/src/app/app.component.ts`
```typescript
this.http.delete(`http://localhost:8080/api/students/${id}`)
```

**Request Details:**
- **Method:** DELETE
- **URL:** http://localhost:8080/api/students/1
- **Headers:** Content-Type: application/json
- **Body:** None (DELETE doesn't have body)

### Step 5: Backend Receives DELETE Request
**File:** `backend/src/main/java/com/example/controller/StudentController.java`
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
```

- `@DeleteMapping` maps to DELETE /api/students/{id}
- `@PathVariable Long id` extracts "1" from URL
- `@ResponseBody` returns empty body
- CORS validation passes

### Step 6: Check Student Exists
```java
if (studentRepository.existsById(id)) {
  studentRepository.deleteById(id);
  return ResponseEntity.noContent().build();
}
return ResponseEntity.notFound().build();
```

**Database Query:**
```sql
SELECT EXISTS(SELECT 1 FROM student WHERE id = 1);
```

**Result:** true (student exists)

### Step 7: Delete by ID
```java
studentRepository.deleteById(id);
```

**File:** `backend/src/main/java/com/example/repository/StudentRepository.java`
- `deleteById(Long id)` inherited from JpaRepository
- JPA generates DELETE SQL

### Step 8: Hibernate Generates SQL
```sql
DELETE FROM student WHERE id = 1;
```

### Step 9: Database Executes DELETE
**PostgreSQL** executes the DELETE query

**Before:**
```
 id │ name      │ email            │ phone_number │ gpa 
────┼───────────┼──────────────────┼──────────────┼─────
  1 │ John Doe  │ john@example.com │ 9876543210   │ 3.8
  2 │ Jane Smith│ jane@example.com │ 8765432109   │ 3.9
```

**After:**
```
 id │ name      │ email            │ phone_number │ gpa 
────┼───────────┼──────────────────┼──────────────┼─────
  2 │ Jane Smith│ jane@example.com │ 8765432109   │ 3.9
```

Row count: 1 deleted

### Step 10: Controller Returns Response
```java
return ResponseEntity.noContent().build();
```

**Response Details:**
- **Status:** 204 No Content
- **Headers:** None
- **Body:** Empty (null)

### Step 11: Frontend Receives Response
```typescript
.subscribe({
  next: () => {
    // Remove from local list
    this.students = this.students.filter(s => s.id !== id);
    this.successMessage = 'Student deleted successfully!';
  },
  error: (err) => {
    this.errorMessage = 'Failed to delete student';
  }
})
```

### Step 12: Update Local List
```typescript
this.students = this.students.filter(s => s.id !== id);
```

**Before:**
```
[
  { id: 1, name: "John Doe", ... },
  { id: 2, name: "Jane Smith", ... }
]
```

**After:**
```
[
  { id: 2, name: "Jane Smith", ... }
]
```

### Step 13: Display Success Message
```typescript
this.successMessage = 'Student deleted successfully!';
```

**UI Changes:**
- Green success message appears
- Student row removed from table
- Table updated with remaining students

### Step 14: Trigger Change Detection
Angular re-renders the table

### Step 15: User Sees Updated UI
- Student no longer in list
- Success message displayed
- Only Jane Smith remains in table

---

## Alternative Scenario: Student Not Found

### Step 1-3: Same as Above

### Step 4-5: Request Sent

### Step 6: Check Exists
```sql
SELECT EXISTS(SELECT 1 FROM student WHERE id = 999);
```
**Result:** false (student doesn't exist)

### Step 7: Return 404
```java
return ResponseEntity.notFound().build();
```

**Response:**
- **Status:** 404 Not Found

### Step 8: Frontend Error
```typescript
error: (err) => {
  this.errorMessage = 'Failed to delete student';
}
```

**UI:** Shows error message, student remains in list

---

## Files Involved

| Layer | File | Method/Component |
|-------|------|------------------|
| **Frontend** | `app.component.ts` | `deleteStudent(id)` |
| **Frontend** | `app.component.ts` | HTML delete button |
| **Backend** | `StudentController.java` | `deleteStudent(@PathVariable)` |
| **Backend** | `StudentRepository.java` | `existsById(Long)` |
| **Backend** | `StudentRepository.java` | `deleteById(Long)` |
| **Config** | `application.yml` | Database connection |
| **Database** | PostgreSQL | DELETE statement |

---

## SQL Generated

### Check Existence
```sql
SELECT EXISTS(SELECT 1 FROM student WHERE id = 1);
```

### Delete Record
```sql
DELETE FROM student WHERE id = 1;
```

---

## API Response

### DELETE /api/students/1 (Success)
**Status:** 204 No Content
```
(Empty body)
```

### DELETE /api/students/999 (Not Found)
**Status:** 404 Not Found
```json
{
  "timestamp": "2024-01-15T11:30:00.000+00:00",
  "status": 404,
  "error": "Not Found"
}
```

---

## Error Scenarios

### Student Not Found
```java
if (studentRepository.existsById(id)) {
  // ... delete
} else {
  return ResponseEntity.notFound().build();  // 404
}
```

### User Cancels Confirmation
```typescript
deleteStudent(id: number | undefined) {
  if (!id || !confirm('Are you sure...')) 
    return;  // Stops here, no HTTP request
}
```

### Database Connection Failed
```
Exception: Could not get a connection to the database
```
**Frontend:** Shows "Failed to delete student" message

### Foreign Key Constraint (Future)
If other tables reference student:
```sql
DELETE FROM student WHERE id = 1;
-- Error: Update or delete on table "student" violates foreign key
```

---

## Database Activity

### Before DELETE
```
Total records: 2
┌────┬────────────┬──────────────────┐
│ id │ name       │ email            │
├────┼────────────┼──────────────────┤
│ 1  │ John Doe   │ john@example.com │
│ 2  │ Jane Smith │ jane@example.com │
└────┴────────────┴──────────────────┘
```

### During DELETE
```sql
DELETE FROM student WHERE id = 1;
```

### After DELETE
```
Total records: 1
┌────┬────────────┬──────────────────┐
│ id │ name       │ email            │
├────┼────────────┼──────────────────┤
│ 2  │ Jane Smith │ jane@example.com │
└────┴────────────┴──────────────────┘
```

**Note:** Row with id=2 keeps its id (no auto-renumbering)

---

## Summary

**User Action:** Click "Delete" → Confirm → OK

**Path Through System:**
1. Frontend shows confirmation dialog
2. User confirms deletion
3. HTTP DELETE request sent to backend
4. Backend checks if student exists
5. Backend calls repository delete method
6. JPA/Hibernate generates DELETE SQL
7. PostgreSQL executes DELETE
8. 204 No Content response returned
9. Frontend removes student from local list
10. UI updates with success message
11. Table refreshed without deleted student

**Status Codes:**
- ✅ 204 No Content - Student successfully deleted
- ❌ 404 Not Found - Student ID doesn't exist
- ❌ 500 INTERNAL SERVER ERROR - Database error

---

## Safety Features

### Confirmation Dialog
```typescript
if (!confirm('Are you sure you want to delete this student?'))
  return;
```
Prevents accidental deletions

### Exists Check
```java
if (studentRepository.existsById(id)) {
  // Only delete if exists
}
```
Prevents unnecessary errors

### Proper HTTP Status
- 204 No Content for success (no body to parse)
- 404 Not Found for missing record (clear error)

---

## Performance Notes

### Soft Delete (Alternative)
Instead of physical delete, mark as inactive:
```java
@Column(name = "is_active")
private Boolean isActive;

// Instead of delete, update:
student.setIsActive(false);
repository.save(student);
```

**Advantages:**
- Preserves historical data
- Can restore if needed
- Audit trail remains

**Current Implementation:** Hard delete (permanent)

### Cascade Delete (Future)
If student has related records (enrollments, grades):
```java
@OneToMany(mappedBy = "student", cascade = CascadeType.DELETE)
private List<Enrollment> enrollments;
```

Auto-delete related records when student deleted

### Batch Delete (Future)
Delete multiple students at once:
```
DELETE /api/students
[1, 2, 3]
```

### Soft Delete Implementation
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
  Optional<Student> student = studentRepository.findById(id);
  if (student.isPresent()) {
    student.get().setIsActive(false);
    studentRepository.save(student.get());
    return ResponseEntity.noContent().build();
  }
  return ResponseEntity.notFound().build();
}
```
