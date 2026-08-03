# How to Discover UI Browser URL from Code

## Overview
Complete guide on finding **http://localhost:4200** (the UI browser URL) from the codebase.

---

## Quick Answer

### Question: "Where do I type the URL in my browser to see the UI?"

### Answer: Look in these 3 places:

1. **`frontend/package.json`** - Line 6
2. **`frontend/angular.json`** - Line 56-67 (serve configuration)
3. **`frontend/src/index.html`** - Line 1 (entry point)

**Result:** `http://localhost:4200`

---

## Complete File Locations

### **File 1: frontend/package.json**

```json
{
  "name": "hello-world-frontend",
  "version": "1.0.0",
  "scripts": {
    "ng": "ng",
    "start": "ng serve",           // ← LINE 6: RUN THIS COMMAND
    "build": "ng build",
    "watch": "ng build --watch --configuration development",
    "test": "ng test"
  }
}
```

**What it means:**
```bash
npm start
# Runs: ng serve
# Which starts Angular dev server on default port: 4200
```

---

### **File 2: frontend/angular.json**

```json
{
  "architect": {
    "serve": {                              // ← LINE 56: SERVE CONFIGURATION
      "builder": "@angular-devkit/build-angular:dev-server",
      "configurations": {
        "production": {
          "buildTarget": "hello-world-frontend:build:production"
        },
        "development": {
          "buildTarget": "hello-world-frontend:build:development"
        }
      },
      "defaultConfiguration": "development"  // ← Uses development config
    }
  }
}
```

**What it means:**
- `"builder": "@angular-devkit/build-angular:dev-server"` - Starts dev server
- `"defaultConfiguration": "development"` - Uses development configuration
- **Port:** Not specified here = uses Angular CLI default = **4200**

---

### **File 3: frontend/src/index.html**

```html
<!doctype html>                             <!-- LINE 1: HTML ROOT -->
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Hello World Frontend</title>
  <base href="/">                           <!-- LINE 6: BASE URL PATH -->
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="icon" type="image/x-icon" href="favicon.ico">
</head>
<body>
  <app-root></app-root>                     <!-- LINE 11: COMPONENT ROOT -->
</body>
</html>
```

**What it means:**
- **Line 1:** This is the HTML entry point
- **Line 6:** `<base href="/">` - Application served at root path
- **Line 11:** `<app-root></app-root>` - Angular component bootstraps here

---

## Complete URL Breakdown

```
http://localhost:4200
│       │         │
│       │         └─ Port (default Angular CLI port)
│       └─ Host (localhost = this machine)
└─ Protocol (HTTP)

BROWSER URL = PROTOCOL + HOST + PORT + PATH
BROWSER URL = http + localhost + 4200 + / (root)
BROWSER URL = http://localhost:4200
```

---

## Step-by-Step: How to Find the UI URL

### **Step 1: Find the Start Command**

**File:** `frontend/package.json`

**Look for:**
```json
"start": "ng serve"
```

**What you found:**
- ✅ Start command runs `ng serve`
- ✅ This starts Angular development server

---

### **Step 2: Find What Port is Used**

**Option A: Check angular.json**

**File:** `frontend/angular.json`

```json
"serve": {
  "builder": "@angular-devkit/build-angular:dev-server"
  // No port specified = default port
}
```

**Option B: Check Angular CLI Documentation**

Angular CLI defaults:
- Port: **4200**
- Host: **localhost**

---

### **Step 3: Verify Entry Point**

**File:** `frontend/src/index.html`

**Look for:**
```html
<!doctype html>
<html>
  <app-root></app-root>
</html>
```

**What you found:**
- ✅ This HTML file is served when you visit the browser URL
- ✅ Angular bootstrap happens at `<app-root>`

---

## How Angular CLI Determines the URL

### **Default Behavior**

When you run `ng serve`:

```bash
$ cd frontend
$ npm start
# Output:
# Angular Live Development Server is listening on localhost:4200.
# ⠋ Building...
# ✔ Compiled successfully.
# 
# Application bundle generated successfully. (1.23 MB)
# 
# Initial Chunk Files | Names   | Size
# main.js             | main    | 256 kB
# polyfills.js        | polyfills | 45 kB
# styles.css          | styles  | 12 kB
# 
# Build complete. Watching for file changes...
```

**Result:** Visit `http://localhost:4200` in browser

---

### **Custom Port (If Configured)**

To change the port, you would modify `angular.json`:

```json
"serve": {
  "builder": "@angular-devkit/build-angular:dev-server",
  "options": {
    "port": 8000  // ← Custom port instead of 4200
  }
}
```

Or run with command line:
```bash
ng serve --port 8000
```

---

## How Frontend and Backend URLs Connect

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           BROWSER (User)                                │
│                                                                         │
│  User types: http://localhost:4200                                     │
│                                                                         │
└──────────────────────────┬──────────────────────────────────────────────┘
                           │
                ┌──────────┴──────────┐
                │                     │
                ▼                     ▼
        ┌───────────────┐   ┌──────────────────┐
        │ FRONTEND UI   │   │ BACKEND API      │
        │ Angular App   │   │ Spring Boot      │
        │ Port: 4200    │   │ Port: 8080       │
        │ Entry point:  │   │ Endpoints:       │
        │ index.html    │   │ /api/students    │
        └───────────────┘   └──────────────────┘
                │                     ▲
                │                     │
                │  When user clicks   │
                │  "Create Student"   │
                │                     │
                └─────────────────────┘
                
                HTTP Call:
                POST http://localhost:8080/api/students
                (Backend URL, defined in StudentService)
```

---

## File Tree - Where Everything Is

```
frontend/
├── package.json                    ← "start": "ng serve" (LINE 6)
├── angular.json                    ← serve configuration (LINE 56-67)
└── src/
    ├── index.html                  ← Entry point HTML (LINE 1)
    │                                 <app-root> (LINE 11)
    ├── main.ts                      ← Bootstrap Angular app
    ├── styles.css                   ← Global CSS
    └── app/
        ├── app.component.ts         ← Main component
        └── services/
            └── student.service.ts   ← API calls to backend
```

---

## Complete Journey: From Browser to Database

```
1. USER OPENS BROWSER
   └─ Types: http://localhost:4200
      ↑
      └─ Found in: angular.json (port 4200 = default)
                    package.json ("ng serve" = start command)

2. ANGULAR CLI STARTS
   └─ Runs: ng serve
      └─ Serves: frontend/src/index.html
         └─ Loads: <app-root></app-root> component

3. app.component.ts LOADED
   └─ ngOnInit() runs
      └─ Calls: studentService.getAllStudents()

4. student.service.ts RUNS
   └─ Makes HTTP call to:
      └─ http://localhost:8080/api/students
         ↑
         └─ Found in: student.service.ts (LINE 17)
                      apiUrl = 'http://localhost:8080/api/students'

5. SPRING BOOT BACKEND RECEIVES
   └─ Port 8080 from: application.yml (LINE 19)
      Path /api/students from: StudentController.java (LINE 14)

6. DATABASE EXECUTES
   └─ SQL: SELECT * FROM student

7. RESPONSE BACK TO BROWSER
   └─ JSON displayed in table
```

---

## Configuration Hierarchy

```
┌─────────────────────────────────────────────┐
│ WHAT THE USER TYPES IN BROWSER              │
│ http://localhost:4200                       │
│                    ↑        ↑               │
│           Host ─────┘        └─── Port      │
└──────────┬──────────────────────────────────┘
           │
           ├─ Host (localhost)
           │  └─ Default: localhost (this machine)
           │  └─ Can be changed: 127.0.0.1, 192.168.x.x, etc
           │
           ├─ Port (4200)
           │  └─ Default: 4200 (Angular CLI default)
           │  └─ Source: angular.json (if configured)
           │  └─ Fallback: Angular CLI hardcoded default
           │
           └─ Path (/)
              └─ Defined in: index.html <base href="/">
              └─ Default: / (root)
              └─ Can be changed: /myapp, /v2, etc
```

---

## How to Verify This Works

### **Step 1: Check What's Running**

```bash
# Terminal
cd frontend
npm start

# Output will show:
# ✔ Compiled successfully.
# Angular Live Development Server is listening on localhost:4200.
```

### **Step 2: Open Browser**

```
Navigate to: http://localhost:4200
```

### **Step 3: Verify Page Loads**

- ✅ Page appears
- ✅ Student form visible
- ✅ "Load Students" button clickable
- ✅ Check browser console (F12 → Console)

### **Step 4: Check Network Requests**

```
F12 → Network Tab → Click "Load Students"
```

You should see:
```
GET http://localhost:8080/api/students  200 OK
```

---

## Summary Table

| Component | What | Where | Value |
|-----------|------|-------|-------|
| **UI Browser URL** | Host | localhost (hardcoded) | localhost |
| **UI Browser URL** | Port | angular.json or default | 4200 |
| **UI Browser URL** | Path | index.html `<base>` | / (root) |
| **UI Start Command** | Script | package.json | "ng serve" |
| **UI Entry Point** | HTML File | index.html | http://localhost:4200 |
| **UI Root Component** | Tag | index.html | `<app-root></app-root>` |
| **API Base URL** | Full URL | student.service.ts | http://localhost:8080/api/students |
| **API Port** | Server Port | application.yml | 8080 |
| **API Path** | Controller Path | StudentController.java | /api/students |

---

## Quick Reference

### To Find: "What URL do I visit in browser?"

1. **Check:** `frontend/package.json` line 6
   - Shows: `"start": "ng serve"`

2. **Check:** `frontend/angular.json` line 56-67
   - Shows: serve configuration
   - Port not specified = uses default = 4200

3. **Result:** `http://localhost:4200`

### To Find: "What API does UI call?"

1. **Check:** `frontend/src/app/services/student.service.ts` line 17
   - Shows: `private apiUrl = 'http://localhost:8080/api/students'`

2. **Verify:** `backend/src/main/resources/application.yml` line 19
   - Shows: `port: 8080`

3. **Verify:** `backend/src/main/java/com/example/controller/StudentController.java` line 14
   - Shows: `@RequestMapping("/api/students")`

4. **Result:** `http://localhost:8080/api/students`

---

## Angular CLI Port Resolution

**Order of Priority:**

1. **Command line flag** (highest priority)
   ```bash
   ng serve --port 8000
   ```

2. **angular.json configuration**
   ```json
   "serve": {
     "options": {
       "port": 8000
     }
   }
   ```

3. **Default value** (lowest priority)
   ```
   4200
   ```

**In our case:** No CLI flag, no config → uses default **4200**

---

## Environment-Specific URLs

### Development
```
Frontend:  http://localhost:4200
Backend:   http://localhost:8080
```

### Production (Example)
```
Frontend:  https://myapp.com
Backend:   https://myapp.com/api
```

**To change:** Modify `student.service.ts` line 17:
```typescript
// Development
private apiUrl = 'http://localhost:8080/api/students';

// Production
private apiUrl = 'https://api.production.com/students';
```

---

## Answer to Original Question

**Q: Where did the user find the UI URL to call from browser?**

**A: The user doesn't need to find it - they just visit:**
```
http://localhost:4200
```

**Why?**
- Default Angular CLI port is 4200
- Defined in `angular.json` (or not, uses default)
- Started by `npm start` which runs `ng serve`
- Entry point is `frontend/src/index.html`

**The user doesn't type `http://localhost:4200` because they read code. They type it because:**
1. That's where the dev server starts by default
2. The output of `ng serve` tells them: "Angular Live Development Server is listening on localhost:4200"
