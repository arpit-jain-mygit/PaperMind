# SQL Interview Guide - Oracle ANSI SQL

## Table of Contents
1. [Top 10 Easiest Interview Questions](#top-10-easiest-interview-questions)
2. [Next 10 Intermediate Problems](#next-10-intermediate-problems)
3. [Deep Dive: 3 Essential Concepts](#deep-dive-3-essential-concepts)
   - 3.1 [Employee-Manager Queries (Self Join)](#1-employee-manager-queries-self-join)
   - 3.2 [Find nth Largest/Smallest Value](#2-find-nth-largestsmallest-value)
   - 3.3 [GROUP BY vs HAVING](#3-group-by-vs-having)
4. [Oracle ANSI SQL - All Queries Rewritten](#oracle-ansi-sql---all-queries-rewritten)
   - 4.1 [Self Join Queries](#1-employee-manager-queries-self-join-oracle)
   - 4.2 [nth Largest/Smallest Methods](#2-find-nth-largestsmallest-value-oracle)
   - 4.3 [GROUP BY vs HAVING Examples](#3-group-by-vs-having-oracle)
   - 4.4 [Date Functions](#complete-examples-with-date-functions-oracle)
   - 4.5 [Advanced CTE](#advanced-cte-with-oracle-ansi)
5. [Oracle-Specific ANSI Syntax](#oracle-specific-ansi-syntax-features)
6. [Quick Reference](#summary)

---

# Top 10 Easiest Interview Questions

## 1. Simple SELECT and WHERE
Retrieve all employees with salary > 50,000
```sql
SELECT * FROM employees WHERE salary > 50000;
```
**Why asked:** Tests basic query understanding

---

## 2. SELECT Specific Columns
Get employee names and salaries only
```sql
SELECT name, salary FROM employees;
```
**Why asked:** Tests column selection and result formatting

---

## 3. ORDER BY (Sorting)
List all employees sorted by salary in descending order
```sql
SELECT * FROM employees ORDER BY salary DESC;
```
**Why asked:** Tests understanding of sorting and ASC/DESC

---

## 4. DISTINCT (Remove Duplicates)
Find all unique job titles in the company
```sql
SELECT DISTINCT job_title FROM employees;
```
**Why asked:** Tests duplicate removal and data uniqueness concept

---

## 5. COUNT Aggregate Function
Count total number of employees
```sql
SELECT COUNT(*) AS total_employees FROM employees;
```
**Why asked:** Tests aggregate functions and aliasing

---

## 6. GROUP BY and SUM
Find total salary expense per department
```sql
SELECT department, SUM(salary) AS total_salary 
FROM employees 
GROUP BY department;
```
**Why asked:** Tests grouping and aggregation together

---

## 7. INNER JOIN
Get employee names with their department names
```sql
SELECT e.name, d.department_name 
FROM employees e 
INNER JOIN departments d ON e.dept_id = d.id;
```
**Why asked:** Most critical join type; tests relationship understanding

---

## 8. LEFT JOIN
List all departments and count of employees (even if no employees)
```sql
SELECT d.department_name, COUNT(e.id) AS emp_count
FROM departments d
LEFT JOIN employees e ON d.id = e.dept_id
GROUP BY d.department_name;
```
**Why asked:** Tests outer joins and handling NULL/missing data

---

## 9. LIKE and Pattern Matching
Find all employees whose name starts with 'A'
```sql
SELECT * FROM employees WHERE name LIKE 'A%';
```
**Why asked:** Tests string pattern matching

---

## 10. CASE Statement (Conditional Logic)
Categorize employees by salary range
```sql
SELECT name, salary,
  CASE 
    WHEN salary < 40000 THEN 'Junior'
    WHEN salary BETWEEN 40000 AND 70000 THEN 'Mid-level'
    ELSE 'Senior'
  END AS level
FROM employees;
```
**Why asked:** Tests conditional logic in SQL

---

# Next 10 Intermediate Problems

## 11. HAVING Clause (Filter Groups)
Find departments with average salary > 55,000
```sql
SELECT department, AVG(salary) AS avg_salary
FROM employees
GROUP BY department
HAVING AVG(salary) > 55000;
```
**Why asked:** Tests filtering at aggregate level (WHERE filters rows, HAVING filters groups)

---

## 12. Multiple JOINs
Get employee name, department name, and manager name
```sql
SELECT e.name, d.department_name, m.name AS manager_name
FROM employees e
INNER JOIN departments d ON e.dept_id = d.id
LEFT JOIN employees m ON e.manager_id = m.id;
```
**Why asked:** Tests complex relationship navigation

---

## 13. IN and NOT IN Operators
Find employees who work in Sales or Marketing departments
```sql
SELECT * FROM employees 
WHERE department IN ('Sales', 'Marketing');
```
**Why asked:** Tests list-based filtering

---

## 14. BETWEEN Operator
Find employees hired between Jan 1, 2020 and Dec 31, 2022
```sql
SELECT * FROM employees
WHERE hire_date BETWEEN '2020-01-01' AND '2022-12-31';
```
**Why asked:** Tests range filtering (cleaner than >= AND <=)

---

## 15. Subqueries (Simple)
Find employees earning more than the average salary
```sql
SELECT * FROM employees
WHERE salary > (SELECT AVG(salary) FROM employees);
```
**Why asked:** Tests nested query thinking and data comparison

---

## 16. Subqueries in FROM Clause
Get department-wise salary statistics
```sql
SELECT department, avg_sal, max_sal, min_sal
FROM (
  SELECT department,
    AVG(salary) AS avg_sal,
    MAX(salary) AS max_sal,
    MIN(salary) AS min_sal
  FROM employees
  GROUP BY department
) AS dept_stats;
```
**Why asked:** Tests derived tables and query complexity

---

## 17. UNION (Combine Results)
List all managers and all senior developers (from different tables or same table)
```sql
SELECT name, 'Manager' AS role FROM managers
UNION
SELECT name, 'Senior Developer' AS role FROM developers WHERE level = 'Senior';
```
**Why asked:** Tests combining multiple queries + understanding UNION vs UNION ALL

---

## 18. Self JOIN (Table Joins Itself)
Find all employee pairs in the same department
```sql
SELECT e1.name AS employee1, e2.name AS employee2, e1.department
FROM employees e1
INNER JOIN employees e2 ON e1.department = e2.department
WHERE e1.id < e2.id;
```
**Why asked:** Tests aliasing and relationship logic within same table

---

## 19. Date Functions
Find employees hired in the last 6 months
```sql
-- Oracle ANSI
SELECT * FROM employees
WHERE hire_date >= ADD_MONTHS(TRUNC(SYSDATE), -6);
```
**Why asked:** Tests date manipulation (very common in real data)

---

## 20. Conditional Aggregation (SUM with CASE)
Get total salary by gender for each department
```sql
SELECT department,
  SUM(CASE WHEN gender = 'M' THEN salary ELSE 0 END) AS male_salary,
  SUM(CASE WHEN gender = 'F' THEN salary ELSE 0 END) AS female_salary
FROM employees
GROUP BY department;
```
**Why asked:** Tests combining CASE with aggregates (very practical)

---

# Deep Dive: 3 Essential Concepts

## 1. Employee-Manager Queries (Self Join)

### The Concept
A **self join** means joining a table to itself. This is useful when data references itself (like employees and their managers, who are also employees).

### Simple Example Setup
```
employees table:
id | name      | manager_id
---|-----------|------------
1  | John      | NULL
2  | Alice     | 1
3  | Bob       | 1
4  | Carol     | 2
5  | David     | 2
```

### Problem 1: Get Each Employee with Their Manager's Name
**Query:**
```sql
SELECT 
  e.name AS employee_name,
  m.name AS manager_name
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id;
```

**Result:**
```
employee_name | manager_name
--------------|-------------
John          | NULL
Alice         | John
Bob           | John
Carol         | Alice
David         | Alice
```

**Why LEFT JOIN?** Because John has no manager (manager_id = NULL), we use LEFT JOIN to still show John.

---

### Problem 2: Get Manager's Manager (Chain of Command)
**Query:**
```sql
SELECT 
  e.name AS employee,
  m.name AS manager,
  mm.name AS manager_of_manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id
LEFT JOIN employees mm ON m.manager_id = mm.id;
```

**Result:**
```
employee | manager | manager_of_manager
---------|---------|-------------------
John     | NULL    | NULL
Alice    | John    | NULL
Bob      | John    | NULL
Carol    | Alice   | John
David    | Alice   | John
```

---

### Problem 3: Count Employees Under Each Manager
**Query:**
```sql
SELECT 
  m.name AS manager_name,
  COUNT(e.id) AS employee_count
FROM employees e
RIGHT JOIN employees m ON e.manager_id = m.id
GROUP BY m.id, m.name
ORDER BY employee_count DESC;
```

**Result:**
```
manager_name | employee_count
-------------|---------------
John         | 2
Alice        | 2
Bob          | 0
Carol        | 0
David        | 0
```

**Key Point:** Use RIGHT JOIN here so we see all managers, even those with 0 employees.

---

### Problem 4: Find Employees with Same Manager
**Query:**
```sql
SELECT 
  e1.name AS employee1,
  e2.name AS employee2,
  m.name AS shared_manager
FROM employees e1
INNER JOIN employees e2 ON e1.manager_id = e2.manager_id
INNER JOIN employees m ON e1.manager_id = m.id
WHERE e1.id < e2.id;  -- Avoid duplicate pairs
```

**Result:**
```
employee1 | employee2 | shared_manager
----------|-----------|---------------
Alice     | Bob       | John
Carol     | David     | Alice
```

---

## 2. Find nth Largest/Smallest Value

### The Problem
Find the 2nd highest salary, 3rd lowest salary, etc.

### Method 1: Using ORDER BY + LIMIT + OFFSET (Simplest)

**Find 2nd Highest Salary:**
```sql
SELECT DISTINCT salary
FROM employees
ORDER BY salary DESC
LIMIT 1 OFFSET 1;  -- Skip 1 (the highest), get the next one
```

**Find 3rd Lowest Salary:**
```sql
SELECT DISTINCT salary
FROM employees
ORDER BY salary ASC
LIMIT 1 OFFSET 2;  -- Skip 2, get the 3rd
```

**Why DISTINCT?** If two employees have the same highest salary, we want to count that as one salary level.

---

### Method 2: Using Window Functions (More Powerful)

**Find Employee with 2nd Highest Salary:**
```sql
SELECT 
  name,
  salary,
  DENSE_RANK() OVER (ORDER BY salary DESC) AS salary_rank
FROM employees
WHERE DENSE_RANK() OVER (ORDER BY salary DESC) = 2;
```

**Result:**
```
name  | salary | salary_rank
------|--------|------------
Alice | 75000  | 2
Bob   | 75000  | 2
```

**Why DENSE_RANK?** 
- `RANK()` leaves gaps (1, 1, 3...)
- `DENSE_RANK()` has no gaps (1, 1, 2...)
- Use DENSE_RANK for "2nd salary level" questions

---

### Method 3: Using Subquery (Works Everywhere)

**Find 2nd Highest Salary:**
```sql
SELECT MAX(salary)
FROM employees
WHERE salary < (SELECT MAX(salary) FROM employees);
```

**Find 3rd Highest Salary:**
```sql
SELECT MAX(salary)
FROM employees
WHERE salary NOT IN (
  SELECT MAX(salary) FROM employees
  UNION ALL
  SELECT MAX(salary) FROM employees WHERE salary < (SELECT MAX(salary) FROM employees)
);
```

---

### Quick Comparison Table

| Method | Best For | Complexity |
|--------|----------|-----------|
| OFFSET/LIMIT | Simple, clean | Easy (⭐) |
| Window Functions | Multiple ranks needed | Medium (⭐⭐) |
| Subquery | Old databases, clarity | Medium (⭐⭐) |

---

## 3. GROUP BY vs HAVING: When to Use What

### The Fundamental Difference

| Aspect | GROUP BY | HAVING |
|--------|----------|--------|
| **Filters** | Individual **rows** | **Grouped** results |
| **When applied** | Before aggregation | After aggregation |
| **With functions** | Regular columns | Aggregate functions (COUNT, SUM, AVG) |

---

### Simple Analogy
Think of it like:
- **WHERE** = Filter people before entering a store
- **GROUP BY** = Organize people into groups
- **HAVING** = Filter groups before showing results

---

### Example 1: Using WHERE (Before Grouping)

**Question: What's the average salary per department for employees hired after 2020?**

```sql
SELECT 
  department,
  AVG(salary) as avg_salary
FROM employees
WHERE hire_date > '2020-01-01'  -- ← WHERE filters rows FIRST
GROUP BY department;
```

**Process:**
1. ✅ Filter rows (WHERE hire_date > '2020-01-01')
2. ✅ Group by department
3. ✅ Calculate AVG for each group

---

### Example 2: Using HAVING (After Grouping)

**Question: Which departments have average salary > 60,000?**

```sql
SELECT 
  department,
  AVG(salary) as avg_salary
FROM employees
GROUP BY department
HAVING AVG(salary) > 60000;  -- ← HAVING filters groups AFTER aggregation
```

**Process:**
1. ✅ Group by department
2. ✅ Calculate AVG for each group
3. ✅ Filter groups (HAVING avg_salary > 60000)

---

### Example 3: Using Both WHERE and HAVING

**Question: For employees hired after 2020, find departments with average salary > 60,000?**

```sql
SELECT 
  department,
  AVG(salary) as avg_salary,
  COUNT(*) as emp_count
FROM employees
WHERE hire_date > '2020-01-01'         -- ← Filter ROWS first
GROUP BY department
HAVING AVG(salary) > 60000             -- ← Filter GROUPS second
  AND COUNT(*) >= 3;                   -- ← Multiple group conditions
```

**Process:**
1. ✅ WHERE: Filter rows by hire date
2. ✅ GROUP BY: Organize into departments
3. ✅ HAVING: Keep only groups with avg_salary > 60000 AND count >= 3

---

### Decision Tree: When to Use What?

```
Question: "Do I need to filter based on..."

├─ Column values? (name, salary, date, etc.)
│  └─ Use WHERE
│     Example: WHERE salary > 50000

├─ Aggregate result? (COUNT, SUM, AVG, etc.)
│  └─ Use HAVING
│     Example: HAVING COUNT(*) > 5

└─ Both?
   └─ Use WHERE + HAVING
      WHERE filters rows → GROUP BY → HAVING filters groups
```

---

### Common Mistakes

❌ **Wrong: Using HAVING without GROUP BY**
```sql
SELECT * FROM employees
HAVING salary > 50000;  -- Error! HAVING needs GROUP BY
```

❌ **Wrong: Using WHERE for aggregate conditions**
```sql
SELECT department, COUNT(*) as cnt
FROM employees
WHERE COUNT(*) > 5  -- Error! Can't use COUNT in WHERE
GROUP BY department;
```

✅ **Right:**
```sql
SELECT department, COUNT(*) as cnt
FROM employees
GROUP BY department
HAVING COUNT(*) > 5;  -- Correct!
```

---

### Real-World Example: Sales Analysis

**Find sales representatives who made more than 50 sales calls and have average deal size > $10,000**

```sql
SELECT 
  sales_rep_id,
  sales_rep_name,
  COUNT(*) as call_count,           -- Total calls
  AVG(deal_size) as avg_deal_size,  -- Average deal
  SUM(deal_size) as total_deals     -- Total revenue
FROM sales_calls
WHERE call_date >= '2024-01-01'     -- ← Filter calls from this year
GROUP BY sales_rep_id, sales_rep_name
HAVING COUNT(*) > 50                 -- ← At least 50 calls
  AND AVG(deal_size) > 10000         -- ← Avg deal > $10k
ORDER BY total_deals DESC;
```

**What this does:**
1. **WHERE:** Keeps only 2024 calls
2. **GROUP BY:** Organizes by sales rep
3. **HAVING:** Filters reps with 50+ calls AND avg deal > $10k
4. **SELECT:** Shows their stats

---

### Quick Reference Card

| Scenario | Use | Example |
|----------|-----|---------|
| Filter by salary | WHERE | `WHERE salary > 50000` |
| Filter by department name | WHERE | `WHERE dept IN ('Sales', 'IT')` |
| Keep groups with 5+ people | HAVING | `HAVING COUNT(*) >= 5` |
| Keep groups with avg > 60k | HAVING | `HAVING AVG(salary) > 60000` |
| Both row and group filter | WHERE + HAVING | See sales example above |

---

# Oracle ANSI SQL - All Queries Rewritten

---

## 1. Employee-Manager Queries (Self Join) - Oracle

### Problem 1: Employee with Manager Name
```sql
SELECT 
  e.name AS employee_name,
  m.name AS manager_name
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id;
```
✅ Same (Oracle supports LEFT JOIN)

---

### Problem 2: Manager's Manager (Chain of Command)
```sql
SELECT 
  e.name AS employee,
  m.name AS manager,
  mm.name AS manager_of_manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id
LEFT JOIN employees mm ON m.manager_id = mm.id;
```
✅ Same (ANSI standard)

---

### Problem 3: Count Employees Under Each Manager
```sql
SELECT 
  m.name AS manager_name,
  COUNT(e.employee_id) AS employee_count
FROM employees e
RIGHT JOIN employees m ON e.manager_id = m.employee_id
GROUP BY m.employee_id, m.name
ORDER BY employee_count DESC;
```
✅ Oracle supports RIGHT JOIN

---

### Problem 4: Employees with Same Manager
```sql
SELECT 
  e1.name AS employee1,
  e2.name AS employee2,
  m.name AS shared_manager
FROM employees e1
INNER JOIN employees e2 
  ON e1.manager_id = e2.manager_id 
  AND e1.employee_id < e2.employee_id
INNER JOIN employees m 
  ON e1.manager_id = m.employee_id;
```
✅ Oracle ANSI JOIN syntax

---

## 2. Find nth Largest/Smallest Value - Oracle

### Method 1: Using OFFSET/FETCH (Oracle 12c+, ANSI Standard)

**Find 2nd Highest Salary:**
```sql
SELECT DISTINCT salary
FROM employees
ORDER BY salary DESC
FETCH FIRST 1 ROWS ONLY
OFFSET 1 ROWS;
```

**Find 3rd Lowest Salary:**
```sql
SELECT DISTINCT salary
FROM employees
ORDER BY salary ASC
FETCH FIRST 1 ROWS ONLY
OFFSET 2 ROWS;
```

**Note:** This is the ANSI SQL standard (works in Oracle 12c+)

---

### Method 2: Using Window Functions (Best for Oracle)

**Find Employee with 2nd Highest Salary:**
```sql
WITH ranked_salary AS (
  SELECT 
    name,
    salary,
    DENSE_RANK() OVER (ORDER BY salary DESC) AS salary_rank
  FROM employees
)
SELECT name, salary
FROM ranked_salary
WHERE salary_rank = 2;
```

**Find Top 3 Salaries with Employee Names:**
```sql
WITH ranked_salary AS (
  SELECT 
    name,
    salary,
    ROW_NUMBER() OVER (ORDER BY salary DESC) AS rank
  FROM employees
)
SELECT name, salary, rank
FROM ranked_salary
WHERE rank <= 3;
```

---

### Method 3: Using Subquery (Works in All Oracle Versions)

**Find 2nd Highest Salary:**
```sql
SELECT MAX(salary)
FROM employees
WHERE salary < (SELECT MAX(salary) FROM employees);
```

**Find 3rd Highest Salary Using NOT IN:**
```sql
SELECT MAX(salary)
FROM employees
WHERE salary NOT IN (
  SELECT DISTINCT salary
  FROM employees
  ORDER BY salary DESC
  FETCH FIRST 2 ROWS ONLY
);
```

---

### Method 4: Using ROW_NUMBER for All Ranks

**Get Employee with nth Highest Salary (n=5):**
```sql
WITH ranked_employees AS (
  SELECT 
    employee_id,
    name,
    salary,
    DENSE_RANK() OVER (ORDER BY salary DESC) AS salary_rank
  FROM employees
)
SELECT employee_id, name, salary
FROM ranked_employees
WHERE salary_rank = 5;
```

---

## 3. GROUP BY vs HAVING - Oracle

### Example 1: WHERE Before Grouping

**Average salary per department for employees hired after 2020:**
```sql
SELECT 
  department,
  AVG(salary) AS avg_salary,
  COUNT(*) AS emp_count
FROM employees
WHERE hire_date > TO_DATE('2020-01-01', 'YYYY-MM-DD')
GROUP BY department;
```

**Oracle Date Note:** Use `TO_DATE()` for explicit date conversion

---

### Example 2: HAVING After Grouping

**Departments with average salary > 60,000:**
```sql
SELECT 
  department,
  AVG(salary) AS avg_salary,
  COUNT(*) AS emp_count
FROM employees
GROUP BY department
HAVING AVG(salary) > 60000;
```

---

### Example 3: WHERE + HAVING Combined

**Employees hired after 2020, departments with avg > 60k and 3+ employees:**
```sql
SELECT 
  department,
  AVG(salary) AS avg_salary,
  COUNT(*) AS emp_count,
  MIN(salary) AS min_sal,
  MAX(salary) AS max_sal
FROM employees
WHERE hire_date > TO_DATE('2020-01-01', 'YYYY-MM-DD')
GROUP BY department
HAVING AVG(salary) > 60000 
  AND COUNT(*) >= 3
ORDER BY avg_salary DESC;
```

---

### Example 4: GROUP BY with Multiple Conditions

**Sales rep performance (2024):**
```sql
SELECT 
  sales_rep_id,
  sales_rep_name,
  COUNT(*) AS call_count,
  AVG(deal_size) AS avg_deal_size,
  SUM(deal_size) AS total_deals,
  TRUNC(AVG(deal_size), 2) AS avg_deal_rounded
FROM sales_calls
WHERE call_date >= TO_DATE('2024-01-01', 'YYYY-MM-DD')
  AND deal_size > 0
GROUP BY sales_rep_id, sales_rep_name
HAVING COUNT(*) > 50 
  AND AVG(deal_size) > 10000
ORDER BY total_deals DESC;
```

---

## Complete Examples with Date Functions - Oracle

### Using SYSDATE (Current Date/Time)

**Employees hired in last 6 months:**
```sql
SELECT 
  employee_id,
  name,
  hire_date
FROM employees
WHERE hire_date >= ADD_MONTHS(TRUNC(SYSDATE), -6);
```

**Employees hired in last 365 days (Alternative):**
```sql
SELECT 
  employee_id,
  name,
  hire_date
FROM employees
WHERE hire_date >= SYSDATE - 365;
```

---

### Date Range Query

**Employees hired between specific dates:**
```sql
SELECT 
  employee_id,
  name,
  hire_date,
  department
FROM employees
WHERE hire_date BETWEEN TO_DATE('2020-01-01', 'YYYY-MM-DD') 
                    AND TO_DATE('2022-12-31', 'YYYY-MM-DD')
ORDER BY hire_date DESC;
```

---

### Group By with Date Trunc

**Monthly hire count:**
```sql
SELECT 
  TRUNC(hire_date, 'MONTH') AS hire_month,
  COUNT(*) AS emp_count
FROM employees
WHERE hire_date >= ADD_MONTHS(SYSDATE, -12)
GROUP BY TRUNC(hire_date, 'MONTH')
ORDER BY hire_month DESC;
```

---

## Advanced: CTE with Oracle ANSI

### Problem: Find Managers and Their Top Performer

```sql
WITH manager_info AS (
  SELECT 
    m.employee_id,
    m.name AS manager_name,
    m.department,
    e.name AS employee_name,
    e.salary,
    ROW_NUMBER() OVER (PARTITION BY m.employee_id ORDER BY e.salary DESC) AS rank
  FROM employees m
  INNER JOIN employees e ON m.employee_id = e.manager_id
)
SELECT 
  manager_name,
  department,
  employee_name AS top_performer,
  salary
FROM manager_info
WHERE rank = 1
ORDER BY manager_name;
```

---

### Problem: Sales Performance with Running Total

```sql
WITH monthly_sales AS (
  SELECT 
    sales_rep_id,
    TRUNC(sale_date, 'MONTH') AS sale_month,
    SUM(sale_amount) AS monthly_total
  FROM sales
  WHERE TRUNC(sale_date, 'YYYY') = TRUNC(SYSDATE, 'YYYY')
  GROUP BY sales_rep_id, TRUNC(sale_date, 'MONTH')
)
SELECT 
  sales_rep_id,
  sale_month,
  monthly_total,
  SUM(monthly_total) OVER (
    PARTITION BY sales_rep_id 
    ORDER BY sale_month
  ) AS running_total
FROM monthly_sales
ORDER BY sales_rep_id, sale_month;
```

---

# Oracle-Specific ANSI Syntax Features

| Feature | Oracle Syntax | Example |
|---------|--------------|---------|
| **OFFSET/FETCH** | ANSI Standard | `FETCH FIRST 10 ROWS ONLY OFFSET 5 ROWS` |
| **Window Functions** | ANSI Standard | `ROW_NUMBER() OVER (PARTITION BY dept ORDER BY salary)` |
| **CTE (WITH)** | ANSI Standard | `WITH cte_name AS (SELECT ...) SELECT ...` |
| **Date Current** | `SYSDATE` | `WHERE hire_date >= SYSDATE - 30` |
| **Date Add** | `ADD_MONTHS()` | `ADD_MONTHS(SYSDATE, -6)` |
| **Date Format** | `TO_DATE()` | `TO_DATE('2024-01-01', 'YYYY-MM-DD')` |
| **Date Truncate** | `TRUNC()` | `TRUNC(hire_date, 'MONTH')` |
| **String Concat** | `||` operator | `name || ' - ' || department` |
| **NULL Handling** | `NVL()` | `NVL(commission, 0)` |
| **Rounding** | `TRUNC()` or `ROUND()` | `TRUNC(salary, 2)` |

---

# Summary

✅ **Use OFFSET/FETCH** instead of LIMIT (ANSI Standard, Oracle 12c+)  
✅ **Use CTE with WITH** for complex queries (ANSI Standard)  
✅ **Use ROW_NUMBER/DENSE_RANK** for ranking (ANSI Standard)  
✅ **Use TO_DATE()** for date conversions  
✅ **Use ADD_MONTHS()** for date arithmetic  
✅ **Use || operator** for string concatenation  
✅ **Use TRUNC()** for date truncation  
✅ **Use NVL()** for NULL handling  

All these queries are **ANSI SQL compliant** and work perfectly in Oracle! 🎯
