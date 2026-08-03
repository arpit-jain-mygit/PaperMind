# UI Browser URL vs API Backend URL - Complete Guide

## Visual Overview

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         USER'S BROWSER                                   │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  Address Bar: http://localhost:4200    ← UI Browser URL                  │
│                      ↑        ↑                                           │
│               From: default   Angular CLI default port                    │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │ Student Management System                                           │ │
│  │                                                                     │ │
│  │ Add New Student                                                   │ │
│  │ Name: [          ]                                                │ │
│  │ Email: [         ]                                                │ │
│  │ Phone: [         ]                                                │ │
│  │ GPA: [    ]                                                       │ │
│  │ [Create Student]  ← Click                                         │ │
│  │                                                                     │ │
│  │ Students List                                                     │ │
│  │ [Load Students]  ← Click                                          │ │
│  │                                                                     │ │
│  │ ┌─ ID ─ Name ─ Email ────────────────────────────────────────┐   │ │
│  │ │  1  │ John │ john@example.com  │  9876543210  │ 3.8 │ ⚙ ✕ │   │ │
│  │ └────────────────────────────────────────────────────────────────┘   │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                           ↓                                               │
│                When user clicks button or loads page                      │
│                Angular Service makes HTTP call                            │
│                                                                           │
│  TO: http://localhost:8080/api/students    ← API Backend URL             │
│      ↑        ↑         ↑                                                 │
│   Protocol   Host       Port                                              │
│                         (Backend port, NOT browser port)                  │
│                                                                           │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Side-by-Side Comparison

```
┌─────────────────────────────────────┬──────────────────────────────────────┐
│ UI BROWSER URL                      │ API BACKEND URL                      │
├─────────────────────────────────────┼──────────────────────────────────────┤
│ http://localhost:4200               │ http://localhost:8080/api/students   │
│                                     │                                      │
│ What it is:                         │ What it is:                          │
│ ✓ HTML page rendered in browser     │ ✓ REST API endpoint                  │
│ ✓ User can see and interact         │ ✓ Server receives data               │
│ ✓ Angular components running        │ ✓ Stores/retrieves from database    │
│                                     │                                      │
│ Why 4200?                           │ Why 8080?                            │
│ Angular CLI default port            │ Spring Boot default port             │
│ Defined in: angular.json or default │ Defined in: application.yml (line 19)│
│                                     │                                      │
│ How to start?                       │ How to start?                        │
│ $ npm start (in frontend/)          │ $ mvn spring-boot:run (in backend/)  │
│ OR                                  │                                      │
│ $ cd frontend && ng serve           │                                      │
│                                     │                                      │
│ Where configured?                   │ Where configured?                    │
│ ✓ angular.json (line 56-67)         │ ✓ application.yml (line 19)          │
│ ✓ package.json (line 6)             │ ✓ HelloWorldApplication.java (CORS)  │
│ ✓ index.html (entry point)          │ ✓ StudentController.java (routing)   │
│                                     │                                      │
│ Can user access directly? YES       │ Can user access directly? NO         │
│ Browser shows HTML/CSS/JS           │ Browser shows JSON only              │
│ User can click, type, submit        │ Only programmatic access            │
│ $ curl http://localhost:4200        │ $ curl http://localhost:8080/api/... │
│ Output: HTML page                   │ Output: JSON data                    │
└─────────────────────────────────────┴──────────────────────────────────────┘
```

---

## Where Each URL is Defined

### UI Browser URL: http://localhost:4200

#### Location 1: Default in Angular CLI
```
Default Port: 4200 (no file needed)
```

#### Location 2: angular.json (if you want custom port)
**File:** `frontend/angular.json`
```json
{
  "architect": {
    "serve": {                    ← LINE 56
      "builder": "@angular-devkit/build-angular:dev-server",
      "options": {
        "port": 4200              ← CUSTOM PORT (if specified)
      }
    }
  }
}
```
**Current state:** NOT specified → uses default 4200

#### Location 3: package.json (start script)
**File:** `frontend/package.json`
```json
{
  "scripts": {
    "start": "ng serve"           ← LINE 6: This starts the dev server
  }
}
```
**When you run:** `npm start` or `npm run start`
**It executes:** `ng serve`
**Which starts:** Development server on port 4200

#### Location 4: index.html (entry point)
**File:** `frontend/src/index.html`
```html
<!doctype html>
<html>
  <head>
    <base href="/">              ← Line 6: Base path for routing
  </head>
  <body>
    <app-root></app-root>        ← Line 11: Angular bootstraps here
  </body>
</html>
```
**When you visit:** http://localhost:4200
**Browser loads:** This HTML file
**Shows:** `<app-root>` component rendered by Angular

---

### API Backend URL: http://localhost:8080/api/students

#### Part 1: Host + Port (http://localhost:8080)

**Location:** `backend/src/main/resources/application.yml`
```yaml
server:
  port: 8080                     ← LINE 19: Server port
  servlet:
    context-path: /              ← Line 20: Base path
```

#### Part 2: Base Path (/api/students)

**Location:** `backend/src/main/java/com/example/controller/StudentController.java`
```java
@RestController
@RequestMapping("/api/students")  ← LINE 14: Base path for all methods
public class StudentController {
```

#### Part 3: Frontend knows the same URL

**Location:** `frontend/src/app/services/student.service.ts`
```typescript
export class StudentService {
  private apiUrl = 'http://localhost:8080/api/students';  ← LINE 17: HARDCODED!
  
  // All methods use this URL:
  getAllStudents(): Observable<Student[]> {
    return this.http.get<Student[]>(this.apiUrl);  ← Uses apiUrl
  }
}
```

---

## How They Connect

### Process 1: User Visits Browser

```
1. User opens browser
2. Types: http://localhost:4200
         (4200 comes from Angular default)

3. Browser sends HTTP GET to port 4200

4. Angular dev server responds with index.html
   
5. Browser renders <app-root></app-root>
   
6. Angular JavaScript loads and runs app.component.ts
```

### Process 2: User Clicks "Create Student"

```
1. app.component.ts: createStudent() method triggered

2. Calls: this.studentService.createStudent(student)

3. student.service.ts: 
   return this.http.post<Student>(this.apiUrl, student)
   
   this.apiUrl = 'http://localhost:8080/api/students'
                                 ↑
                          Backend URL (8080)!

4. Browser makes HTTP POST to http://localhost:8080/api/students
   (Note: Different port! 8080, not 4200)

5. Spring Boot backend receives on port 8080

6. StudentController processes request

7. Response sent back with JSON

8. Angular displays result
```

---

## Why Two Different Ports?

```
┌──────────────────────────────────────────────────────────────────┐
│                      DIFFERENT SERVICES                          │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  PORT 4200                          PORT 8080                    │
│  ──────────────                     ─────────────                │
│  Angular Development Server         Spring Boot Server           │
│                                                                  │
│  Serves:                            Serves:                      │
│  • HTML                             • JSON APIs                  │
│  • CSS                              • Database operations        │
│  • JavaScript                       • Business logic             │
│  • User Interface                   • Data persistence           │
│                                                                  │
│  Purpose:                           Purpose:                     │
│  Show UI to user                    Process data                 │
│  Handle user interactions           Manage database              │
│                                                                  │
│  Started by:                        Started by:                  │
│  npm start (frontend dir)           mvn spring-boot:run (backend)│
│                                                                  │
│  Configuration:                     Configuration:               │
│  angular.json                       application.yml              │
│  package.json                       Spring Boot annotation       │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## Step-by-Step: Finding Both URLs

### Finding UI Browser URL (4200)

```
Step 1: Go to frontend directory
        $ cd frontend

Step 2: Look at package.json
        "start": "ng serve"
        
Step 3: When you run it
        $ npm start
        
Step 4: Angular CLI outputs:
        "Angular Live Development Server is listening on 
         localhost:4200"
        
Step 5: Open browser and visit:
        http://localhost:4200
```

### Finding API Backend URL (8080/api/students)

```
Step 1: Look at student.service.ts
        private apiUrl = 'http://localhost:8080/api/students'
        
Step 2: Verify port in application.yml
        server:
          port: 8080
        
Step 3: Verify path in StudentController.java
        @RequestMapping("/api/students")
        
Step 4: Result:
        http://localhost:8080 + /api/students
        = http://localhost:8080/api/students
```

---

## Common Confusion

### ❌ WRONG: User thinks they should visit http://localhost:8080

```
Why this is wrong:
• Port 8080 is for API/REST calls only
• Returns JSON, not HTML
• User will see raw JSON like:
  [{"id":1,"name":"John","email":"john@example.com",...}]
• No UI, no buttons, no forms
```

### ✅ CORRECT: User visits http://localhost:4200

```
Why this is right:
• Port 4200 is for UI
• Returns HTML with Angular app
• User sees:
  - Form to create students
  - Button to load students
  - Table of students
  - Can interact with the app
  
• Behind the scenes:
  - Angular calls http://localhost:8080 (API port)
  - Gets JSON from backend
  - Displays it nicely in the browser
```

---

## Testing Both URLs

### Test UI URL (Should show HTML)
```bash
$ curl http://localhost:4200
# Output: HTML (first line will be <!doctype html>)
```

### Test API URL (Should show JSON)
```bash
$ curl http://localhost:8080/api/students
# Output: JSON array
# [
#   {"id":1,"name":"John","email":"john@example.com",...},
#   {"id":2,"name":"Jane","email":"jane@example.com",...}
# ]
```

### Test with Browser DevTools
1. Open http://localhost:4200
2. Press F12 (DevTools)
3. Go to Network tab
4. Click "Load Students" button
5. See in Network tab:
   ```
   GET http://localhost:8080/api/students  200 OK
   ```

---

## Summary

| Question | Answer | Where to Find |
|----------|--------|-----------------|
| What URL do I visit in browser? | http://localhost:4200 | Angular default port |
| How do I start that? | npm start (in frontend/) | package.json line 6 |
| What does port 4200 do? | Serves HTML/UI | Angular dev server |
| What URL does the app call? | http://localhost:8080/api/students | student.service.ts line 17 |
| How do I start the backend? | mvn spring-boot:run (in backend/) | Spring Boot |
| What does port 8080 do? | Serves JSON/API | Spring Boot |
| Why two ports? | Different services | UI vs API |
| Can I change ports? | Yes | angular.json and application.yml |

---

## Final Answer

**User Question:** "This is API URL, what about UI browser URL?"

**Complete Answer:**

| URL | Purpose | Where Defined | Port |
|-----|---------|-----------------|------|
| **UI: http://localhost:4200** | What user visits in browser | Angular default | 4200 |
| **API: http://localhost:8080/api/students** | What Angular app calls | student.service.ts | 8080 |

**To find UI URL:**
1. Check `frontend/package.json` → "start": "ng serve"
2. Check `frontend/angular.json` → serve configuration
3. Check `frontend/src/index.html` → entry point
4. Default Angular CLI port: **4200**

**To find API URL:**
1. Check `frontend/src/app/services/student.service.ts` → line 17
2. Verify in `backend/src/main/resources/application.yml` → line 19
3. Verify in `backend/src/main/java/com/example/controller/StudentController.java` → line 14
4. Result: **http://localhost:8080/api/students**
