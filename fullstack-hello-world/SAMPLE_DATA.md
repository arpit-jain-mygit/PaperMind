# Sample Data for First Run

Use this data to test the complete learning cycle: batch processing → feedback → learning → application.

---

## 🧪 Test Scenario: Email Validation Learning

### CSV Format (for Batch Upload)

Copy and paste this into the UI batch upload:

```csv
Alice Johnson,alice@example.com,5551234567,3.9
Bob Smith,bob.test.com,5559876543,3.1
Carol Davis,carol@gmail.com,5553334445,3.8
Diana Prince,diana.test.com,5559999999,3.2
Eve Wilson,eve@domain.co.uk,5556667778,3.7
Frank Brown,frank.invalid,5554445556,2.9
Grace Lee,grace@yahoo.com,5552223334,3.95
```

### SQL Format (for Direct DB Insert)

```sql
INSERT INTO student (name, email, phone_number, gpa) VALUES
('Alice Johnson', 'alice@example.com', '5551234567', 3.9),
('Bob Smith', 'bob.test.com', '5559876543', 3.1),
('Carol Davis', 'carol@gmail.com', '5553334445', 3.8),
('Diana Prince', 'diana.test.com', '5559999999', 3.2),
('Eve Wilson', 'eve@domain.co.uk', '5556667778', 3.7),
('Frank Brown', 'frank.invalid', '5554445556', 2.9),
('Grace Lee', 'grace@yahoo.com', '5552223334', 3.95);
```

---

## 📋 Record Details

| # | Name | Email | Phone | GPA | Notes |
|---|------|-------|-------|-----|-------|
| 1 | Alice Johnson | alice@example.com | 5551234567 | 3.9 | ✅ Valid - High performer (GPA ≥ 3.7) |
| 2 | Bob Smith | bob.test.com | 5559876543 | 3.1 | ❌ Invalid - Missing @ symbol |
| 3 | Carol Davis | carol@gmail.com | 5553334445 | 3.8 | ✅ Valid - High performer |
| 4 | Diana Prince | diana.test.com | 5559999999 | 3.2 | ❌ Invalid - Missing @ symbol |
| 5 | Eve Wilson | eve@domain.co.uk | 5556667778 | 3.7 | ✅ Valid - High performer (borderline) |
| 6 | Frank Brown | frank.invalid | 5554445556 | 2.9 | ❌ Invalid - No @ symbol AND no TLD |
| 7 | Grace Lee | grace@yahoo.com | 5552223334 | 3.95 | ✅ Valid - Highest performer |

---

## 🎯 Expected Learning Flow

### Step 1: Initial Batch (All created - no rules yet)
```
Upload 7 records
→ Agent has no rules, accepts everything
→ Creates 7 students
→ Tracks 7 decisions:
  - Alice: HIGH_PERFORMER (GPA 3.9)
  - Bob: CREATED
  - Carol: HIGH_PERFORMER (GPA 3.8)
  - Diana: CREATED
  - Eve: HIGH_PERFORMER (GPA 3.7)
  - Frank: CREATED
  - Grace: HIGH_PERFORMER (GPA 3.95)
```

### Step 2: User Provides Feedback

Mark decisions in UI:
```
✅ Alice - CORRECT
❌ Bob - WRONG (pattern: missing_at_symbol)
✅ Carol - CORRECT
❌ Diana - WRONG (pattern: missing_at_symbol)
✅ Eve - CORRECT
❌ Frank - WRONG (pattern: missing_at_symbol)
✅ Grace - CORRECT
```

**Pattern Analysis:**
- Total failures: 3
- Pattern: missing_at_symbol = 3 (100%)
- Dominant pattern: missing_at_symbol

### Step 3: Agent Learns

Click "Analyze & Learn":
```
System analyzes 3 wrong decisions
Calls OpenAI with prompt:
  "I found 3 failures, all missing_at_symbol pattern"
  "Failed examples: bob.test.com, diana.test.com, frank.invalid"

LLM generates:
  EXPRESSION: email != null && email.contains('@')
  EXPLANATION: Email must contain @ symbol

Rule created (inactive, requires approval)
```

### Step 4: User Activates Rule

Click "Activate" on learned rule:
```
Rule becomes active:
  Pattern: missing_at_symbol
  Expression: email != null && email.contains('@')
  Active: true
```

### Step 5: Test with New Batch

Upload 7 new records with same issues:
```
George Hayes,george.test.com,5551111111,3.5
Hannah King,hannah@microsoft.com,5552222222,3.4
Isaac Martinez,isaac.invalid,5553333333,3.6
Julia Anderson,julia@outlook.com,5554444444,3.8
Kevin Thomas,kevin.noemail,5555555555,2.8
Lisa Wright,lisa@github.com,5556666666,3.9
Mike Johnson,mike@example.com,5557777777,3.3
```

### Expected Result: Agent LEARNED! ✨

```
New batch processing:
→ Agent loads active rules (email validation)
→ Validates each student:
  ✅ George: INVALID (no @) → NOT created
  ✅ Hannah: VALID → Created
  ✅ Isaac: INVALID (no @) → NOT created
  ✅ Julia: VALID → Created
  ✅ Kevin: INVALID (no @) → NOT created
  ✅ Lisa: VALID → Created
  ✅ Mike: VALID → Created

Before: All 7 created (no intelligence)
After: 4 created, 3 rejected (learned from feedback!)
```

---

## 📊 Metrics After First Cycle

```
Decision Accuracy: 3/3 wrong decisions prevented
Rule Application: email validation rule works correctly
False Positives: 0 (no valid emails rejected)
False Negatives: 0 (no invalid emails accepted)
Learning Success: ✅ 100%
```

---

## 🚀 What This Demonstrates

✅ **Zero-Knowledge Learning** - Agent started with no rules  
✅ **LLM Integration** - OpenAI generated the rule  
✅ **User Feedback Loop** - Marked decisions as right/wrong  
✅ **Pattern Analysis** - Identified missing_at_symbol pattern  
✅ **Rule Persistence** - Stored expression in database  
✅ **Dynamic Enforcement** - Applied learned rule to new batch  
✅ **Continuous Improvement** - Accuracy improved on second batch  

---

## 💡 Next Testing Scenarios

After mastering email validation, try:

### Scenario 2: GPA Validation
```
Create records with invalid GPA values:
- gpa: -1.0 (below range)
- gpa: 5.0 (above range)
- gpa: 4.0 (valid - boundary)

Feedback pattern: invalid_gpa
Expected rule: gpa >= 0 && gpa <= 4.0
```

### Scenario 3: Phone Number Validation
```
Create records with various phone formats:
- 5551234567 (valid - 10 digits)
- 555-123-4567 (valid - formatted)
- (555) 123-4567 (valid - formatted)
- 555 (invalid - too short)

Feedback pattern: invalid_phone
Expected rule: phone.length() >= 10
```

### Scenario 4: Name Validation
```
Create records with empty or invalid names:
- "" (empty)
- "A" (single char)
- "Valid Name" (valid)

Feedback pattern: empty_field
Expected rule: name != null && !name.isEmpty()
```

---

## 🎓 Learning Progression

| Cycle | Batch | Pattern | Rule | Result |
|-------|-------|---------|------|--------|
| 1 | 7 students | None | None | ✅ Created 7 (learning starts) |
| 2 | Mark feedback | missing_at_symbol | email.contains('@') | ✅ Rule learned |
| 3 | 7 new students | Apply @rule | Active | ✅ 4 created, 3 rejected |
| 4 | Mark feedback | invalid_gpa | gpa validation | ✅ Second rule learned |
| 5 | 7 more students | Apply both rules | Active | ✅ Multi-rule enforcement |

---

## 📝 Quick Reference

### To use CSV format:
1. Copy records from CSV Format section above
2. Go to UI → "Batch Create Agent"
3. Paste into textarea
4. Click "Process Batch"
5. Wait for results modal

### To use SQL format:
1. Connect to PostgreSQL: `psql -U arpit -d hello_world_db`
2. Copy and paste SQL INSERT statements
3. Or run: `psql -U arpit -d hello_world_db -f sample_data.sql`

### To monitor database:
```sql
-- Check created students
SELECT id, name, email, gpa FROM student;

-- Check decisions
SELECT * FROM agent_decisions ORDER BY created_at DESC;

-- Check learned rules
SELECT * FROM validation_rules WHERE active = true;
```

---

**Status:** Ready to test the complete learning cycle! 🚀
