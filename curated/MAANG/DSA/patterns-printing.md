# Pattern Printing Problems

## Overview
This document contains 43 essential pattern printing problems with solutions using two different space complexity approaches:
- **O(n) Space Complexity**: Using StringBuilder to build strings before printing
- **O(1) Space Complexity**: Direct printing without intermediate data structures

All solutions have **O(n²) Time Complexity** (unavoidable due to output size).

---

## Difficulty Levels Guide

The 43 problems are organized into 5 difficulty levels to help with progressive learning:

- **Level 1 (Easiest)**: Basic nested loops - Pure multiplication loops without conditionals. Single loop structure repeated.
- **Level 2 (Easy-Medium)**: Simple leading/trailing spaces - Adding space complexity to basic loops. Single conditional for alignment.
- **Level 3 (Medium)**: Hollow patterns with conditionals - Border-only patterns requiring one or two conditionals per inner loop.
- **Level 4 (Hard)**: Complex patterns - Multiple conditionals, two-part logic (top/bottom), or arithmetic patterns (diamonds, arrows).
- **Level 5 (Advanced)**: Mathematical and complex multi-condition patterns - Pascal's triangle, palindromes, complex symmetric shapes.

---

## Table of Contents

### Level 1 (Easiest)
| # | Problem | Status | Link |
|---|---------|--------|------|
| 1 | Normal Square Pattern | ✅ | [View](#level-1-easiest) |
| 2 | Multiplication Table Pattern | ✅ | [View](#level-1-easiest) |
| 3 | Normal Right Triangle | ✅ | [View](#level-1-easiest) |
| 4 | Normal Inverted Triangle | ✅ | [View](#level-1-easiest) |
| 5 | Normal Number Staircase | ✅ | [View](#level-1-easiest) |
| 6 | Square Fill Pattern | ✅ | [View](#level-1-easiest) |
| 7 | Right Half Pyramid | ✅ | [View](#level-1-easiest) |

### Level 2 (Easy-Medium)
| # | Problem | Status | Link |
|---|---------|--------|------|
| 8 | Normal Left-Aligned Triangle | ✅ | [View](#level-2-easy-medium) |
| 9 | Normal Number Pyramid | ✅ | [View](#level-2-easy-medium) |
| 10 | Checkerboard Pattern | ✅ | [View](#level-2-easy-medium) |
| 11 | Number Triangular | ✅ | [View](#level-2-easy-medium) |
| 12 | Reverse Right Half Pyramid | ✅ | [View](#level-2-easy-medium) |

### Level 3 (Medium)
| # | Problem | Status | Link |
|---|---------|--------|------|
| 13 | Hollow Square Pattern | ✅ | [View](#level-3-medium) |
| 14 | Hollow Right Triangle | ✅ | [View](#level-3-medium) |
| 15 | Hollow Left-Aligned Triangle | ✅ | [View](#level-3-medium) |
| 16 | Hollow Inverted Triangle | ✅ | [View](#level-3-medium) |
| 17 | Hollow Rectangle | ✅ | [View](#level-3-medium) |
| 18 | Hollow Rectangle (Tall) | ✅ | [View](#level-3-medium) |
| 19 | Hollow Number Pyramid | ✅ | [View](#level-3-medium) |
| 20 | Hollow Number Staircase | ✅ | [View](#level-3-medium) |
| 21 | Left Half Pyramid | ✅ | [View](#level-3-medium) |
| 22 | Reverse Left Half Pyramid | ✅ | [View](#level-3-medium) |
| 23 | Triangle Star Pattern | ✅ | [View](#level-3-medium) |
| 24 | Number Increasing Pyramid | ✅ | [View](#level-3-medium) |
| 25 | Number Increasing Reverse Pyramid | ✅ | [View](#level-3-medium) |
| 26 | Mirror Image Triangle Pattern | ✅ | [View](#level-3-medium) |

### Level 4 (Hard)
| # | Problem | Status | Link |
|---|---------|--------|------|
| 27 | Normal Diamond Pattern | ✅ | [View](#level-4-hard) |
| 28 | Hollow Diamond Pattern | ✅ | [View](#level-4-hard) |
| 29 | Right Arrow Pattern | ✅ | [View](#level-4-hard) |
| 30 | Diamond/Arrow Pattern (Input 11) | ⏳ | [View](#level-4-hard) |
| 31 | Diamond/Arrow Pattern (Input 12) | ⏳ | [View](#level-4-hard) |
| 32 | Cross Pattern | ⏳ | [View](#level-4-hard) |
| 33 | Rhombus Pattern | ⏳ | [View](#level-4-hard) |
| 34 | Butterfly Star Pattern | ⏳ | [View](#level-4-hard) |
| 35 | K Pattern | ⏳ | [View](#level-4-hard) |
| 36 | Hollow Reverse Triangle Pattern | ⏳ | [View](#level-4-hard) |

### Level 5 (Advanced)
| # | Problem | Status | Link |
|---|---------|--------|------|
| 37 | Number Changing Pattern | ⏳ | [View](#level-5-advanced) |
| 38 | Zero-One Triangle | ⏳ | [View](#level-5-advanced) |
| 39 | Palindrome Triangular | ⏳ | [View](#level-5-advanced) |
| 40 | Reverse Number Triangle Pattern | ⏳ | [View](#level-5-advanced) |
| 41 | Hollow Hourglass Pattern | ⏳ | [View](#level-5-advanced) |
| 42 | Pascal's Triangle | ⏳ | [View](#level-5-advanced) |
| 43 | Right Pascal's Triangle | ⏳ | [View](#level-5-advanced) |

---

## Level 1 (Easiest)

### Problem 1: Normal Square Pattern

**Description**: Print a 5×5 square of asterisks.

**Expected Output**:
```
*****
*****
*****
*****
*****
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
```java
public class SquarePrinter {
    public static void main(String args[]) {
        int rows = 5, cols = 5;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                System.out.print("*");
            }
            System.out.println();
        }
    }
}
```

---

### Problem 2: Multiplication Table Pattern

**Description**: Print a 5×5 multiplication table.

**Expected Output**:
```
1 2 3 4 5
2 4 6 8 10
3 6 9 12 15
4 8 12 16 20
5 10 15 20 25
```

```java
public static void main(String args[]) {
    int rows = 5;
    int cols = 5;
    for(int i=1; i <=rows; i++){
        for(int j=1; j<=cols; j++){
            if(j>1) System.out.print(" ");                    
            System.out.print(i*j);                    
        }
        if(i!=rows)
         System.out.println();
    }
}
```

---

### Problem 3: Normal Right Triangle

**Description**: Print a right triangle with 5 rows.

**Expected Output**:
```
*
**
***
****
*****
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
```java
public class RightAlignedRightTriangle {
    public static void main(String args[]) {
        int rows = 5;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j <= i; j++) {
                System.out.print("*"); // Print i+1 asterisks per row
            }
            System.out.println();
        }
    }
}
```

---

### Problem 4: Normal Inverted Triangle

**Description**: Print an inverted triangle with 5 rows.

**Expected Output**:
```
*****
****
***
**
*
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
```java
public class InvertedTriangle {
    public static void main(String args[]) {
        int rows = 5;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < rows - i; j++) {
                System.out.print("*"); // Print rows-i asterisks
            }
            System.out.println();
        }
    }
}
```

---

### Problem 5: Normal Number Staircase

**Description**: Print a staircase of increasing numbers (5 steps).

**Expected Output**:
```
1
2 2
3 3 3
4 4 4 4
5 5 5 5 5
```

```java
public static void main(String args[]) {
    int rows = 5;
    for(int i=1; i<=rows; i++){
        for(int j = 1; j <= i; j++){
            System.out.print (i);
            if (j!=i) System.out.print (" ");//cosmetic spaces b/w stars except for the last one
        }
        System.out.println();
    }
}
```

---

### Problem 6: Square Fill Pattern

**Description**: Print a filled square pattern.

**Expected Output**:
```
* * * * *
* * * * *
* * * * *
* * * * *
* * * * *
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 7: Right Half Pyramid

**Description**: Print a right half pyramid.

**Expected Output**:
```
*
* *
* * *
* * * *
* * * * *
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

## Level 2 (Easy-Medium)

### Problem 8: Normal Left-Aligned Triangle

**Description**: Print a left-aligned triangle with 5 rows (leading spaces).

**Expected Output**:
```
    *
   **
  ***
 ****
*****
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
```java
public class LeftAlignedRightTriangle {
    public static void main(String args[]) {
        int rows = 5;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < rows - i - 1; j++) {
                System.out.print(" ");
            }
            for (int j = 0; j <= i; j++) {
                System.out.print("*"); // Asterisks for current row
            }
            System.out.println();
        }
    }
}
```

---

### Problem 9: Normal Number Pyramid

**Description**: Print a pyramid with numbers (5 rows).

**Expected Output**:
```
1
1 2
1 2 3
1 2 3 4
1 2 3 4 5
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
```java
public class NumberPyramid {
    public static void main(String args[]) {
        int rows = 5;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j <= i; j++) {
                if (j > 0) System.out.print(" "); // Space before each number (except first)
                System.out.print(j + 1); // Print numbers 1 to i+1
            }
            System.out.println();
        }
    }
}
```

---

### Problem 10: Checkerboard Pattern

**Description**: Print a 5×5 checkerboard pattern.

**Expected Output**:
```
* * * * *
 * * * * *
* * * * *
 * * * * *
* * * * *
```

```java
public static void main(String args[]) {
    int rows = 5, cols = 5;
    for(int i=1; i<=rows; i++){
        if(i%2 ==0) System.out.print (" ");
        for(int j = 1; j <= cols; j++){
            System.out.print ("*");
            if (j!=cols) System.out.print (" ");//cosmetic spaces b/w stars except for the last one
        }
        System.out.println();
    }
}
```

---

### Problem 11: Number Triangular

**Description**: Print a triangular pattern with numbers.

**Expected Output**:
```
1
2 2
3 3 3
4 4 4 4
5 5 5 5 5
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 12: Reverse Right Half Pyramid

**Description**: Print a reversed right half pyramid.

**Expected Output**:
```
* * * * *
* * * *
* * *
* *
*
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

## Level 3 (Medium)

### Problem 13: Hollow Square Pattern

**Description**: Print a hollow square (5 rows × 10 columns for visual square shape).

**Expected Output**:
```
**********
*        *
*        *
*        *
**********
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
```java
public static void main(String args[]) {
    int rows = 5;
    int cols = 10;
    for(int i=1; i <=rows; i++){
        for(int j=1; j<=cols; j++){
            if(i==1 || i==rows || j==1 || j==cols){
                System.out.print("*");                    
            }
            else{
                System.out.print(" ");                    
            }
        }
        if(i!=rows)
         System.out.println();
    }
}
```

---

### Problem 14: Hollow Right Triangle

**Description**: Print a hollow right triangle with 5 rows.

**Expected Output**:
```
*
**
* *
*  *
*   *
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
```java
public class RightAlignedRightTriangle {
    public static void main(String args[]) {
        int rows = 5;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j <= i; j++) {
                if (j == 0 || j == i) {
                    System.out.print("*"); // * denotes borders        
                } else {
                    System.out.print("x"); // x denotes hollow        
                }
            }
            System.out.println();
        }
    }
}
```

---

### Problem 15: Hollow Left-Aligned Triangle

**Description**: Print a hollow left-aligned triangle with 5 rows.

**Expected Output**:
```
    *
   * *
  *   *
 *     *
*       *
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
```java
public class LeftAlignedRightTriangle {
    public static void main(String args[]) {
        int rows = 5;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < rows - i - 1; j++) {
                System.out.print(" "); // Leading spaces for alignment
            }
            for (int j = 0; j <= i; j++) {
                if (j == 0 || j == i)
                    System.out.print("*"); // * denotes borders
                else
                    System.out.print("x"); // x denotes hollow
            }
            System.out.println();
        }
    }
}
```

---

### Problem 16: Hollow Inverted Triangle

**Description**: Print a hollow inverted triangle with 5 rows.

**Expected Output**:
```
*****
*  *
* *
**
*
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
```java
public class InvertedTriangle {
    public static void main(String args[]) {
        int rows = 5;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < rows - i; j++) {
                if (i == 0 || i == rows - 1) {
                    System.out.print("*"); // * denotes borders
                } else if (j == 0 || j == rows - i - 1) {
                    System.out.print("*"); // * denotes borders
                } else {
                    System.out.print("x"); // x denotes hollow
                }
            }
            System.out.println();
        }
    }
}
```

---

### Problem 17: Hollow Rectangle

**Description**: Print a hollow rectangle (3 rows × 9 columns - wider rectangle).

**Expected Output**:
```
*********
*       *
*********
```

```java
public static void main(String args[]) {
    int rows = 3;
    int cols = 9;
    for(int i=1; i <=rows; i++){
        for(int j=1; j<=cols; j++){
            if(i==1 || i==rows || j==1 || j==cols){
                System.out.print("*");                    
            }
            else{
                System.out.print(" ");                    
            }
        }
        if(i!=rows)
         System.out.println();
    }
}
```

---

### Problem 18: Hollow Rectangle (Tall)

**Description**: Print a tall hollow rectangle (7 rows × 5 columns).

**Expected Output**:
```
*****
*   *
*   *
*   *
*   *
*   *
*****
```

```java
public static void main(String args[]) {
    int rows = 7;
    int cols = 5;
    for(int i=1; i <=rows; i++){
        for(int j=1; j<=cols; j++){
            if(i==1 || i==rows || j==1 || j==cols){
                System.out.print("*");                    
            }
            else{
                System.out.print(" ");                    
            }
        }
        if(i!=rows)
         System.out.println();
    }
}
```

---

### Problem 19: Hollow Number Pyramid

**Description**: Print a hollow pyramid with numbers (5 rows).

**Expected Output**:
```
1
1 2
1   3
1     4
1       5
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
```java
public class HollowNumberPyramid {
    public static void main(String args[]) {
        int rows = 5;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j <= i; j++) {
                if (i == 0 || i == rows - 1 || j == 0 || j == i) {
                    System.out.print(j + 1); // Print number at edges or first/last rows
                } else {
                    System.out.print(" "); // Space for missing number
                }
                if (j < i) System.out.print(" "); // Space after each position (except last)
            }
            System.out.println();
        }
    }
}
```

---

### Problem 20: Hollow Number Staircase

**Description**: Print a hollow staircase of numbers (5 steps).

**Expected Output**:
```
1
2 2
3   3
4     4
5       5
```

```java
public static void main(String args[]) {
    int n = 5;
    for(int i=1; i<=n; i++){
        for(int j = 1; j <= i; j++){
            if(j==1 || j==i) System.out.print (i);
            if (j!=i) System.out.print (" ");//cosmetic spaces b/w stars except for the last one
        }
        System.out.println();
    }
}
```

---

### Problem 21: Left Half Pyramid

**Description**: Print a left half pyramid.

**Expected Output**:
```
    *
  * *
* * *
  * *
    *
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 22: Reverse Left Half Pyramid

**Description**: Print a reversed left half pyramid.

**Expected Output**:
```
* * * * *
  * * * *
    * * *
      * *
        *
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 23: Triangle Star Pattern

**Description**: Print a triangle made of stars.

**Expected Output**:
```
*
* *
* * *
* * * *
* * * * *
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 24: Number Increasing Pyramid

**Description**: Print an increasing pyramid with numbers.

**Expected Output**:
```
1
1 2
1 2 3
1 2 3 4
1 2 3 4 5
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 25: Number Increasing Reverse Pyramid

**Description**: Print an inverted pyramid with increasing numbers.

**Expected Output**:
```
1 2 3 4 5
1 2 3 4
1 2 3
1 2
1
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 26: Mirror Image Triangle Pattern

**Description**: Print a mirror image triangle pattern.

**Expected Output**:
```
1
1 2
1 2 3
1 2 3 4
1 2 3 4 5
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

## Level 4 (Hard)

### Problem 27: Normal Diamond Pattern

**Expected Output**:
```
  *
 ***
*****
 ***
  *
```

```java
public static void main(String args[]) {
    int n = 3;
    //1st half
    for (int i = 1; i <=n; i++) {
        //spaces
        for (int j = 1 ; j <= n-i; j++){
            System.out.print(" ");
        }
        //stars
        for (int j = 1; j <= 2*i-1; j++){
            System.out.print("*");               
        }
        System.out.println("");               
    }
    
    //1st half
    for (int i = n-1; i >=1; i--) {//start with n, if need symmetry for middle line, o/w n-1
        //spaces
        for (int j = 1 ; j <= n-i; j++){
            System.out.print(" ");
        }
        //stars
        for (int j = 1; j <= 2*i-1; j++){
            System.out.print("*");               
        }
        System.out.println("");               
    }
}
```

---

### Problem 28: Hollow Diamond Pattern

**Description**: Print a hollow diamond with 5 rows.

**Expected Output**:
```
    *
   * *
  *   *
   * *
    *
```

```java
public static void main(String args[]) {
    int n = 3;
    //1st half
    for (int i = 1; i <=n; i++) {
        //spaces
        for (int j = 1 ; j <= n-i; j++){
            System.out.print(" ");
        }
        //stars
        for (int j = 1; j <= 2*i-1; j++){
            if(j==1 || j==2*i-1)
                System.out.print("*");//for border stars 
            else
                System.out.print(" ");//for middle spaces
        }
        System.out.println("");               
    }
    
   //2nd half
    for (int i = n-1; i >= 1; i--) {
        //spaces
        for (int j = 1 ; j <= n-i; j++){
            System.out.print(" ");
        }
        //stars
        for (int j = 1; j <= 2*i-1; j++){
            if(j==1 || j==2*i-1)
                System.out.print("*");//for border stars 
            else
                System.out.print(" ");//for middle spaces
        }
        System.out.println("");               
    }
}
```

---

### Problem 29: Right Arrow Pattern

**Description**: Print a right-pointing arrow pattern (9 rows).

**Expected Output**:
```
* * * * *
  * * * *
    * * *
      * *
        *
      * *
    * * *
  * * * *
* * * * *
```

```java
public static void main(String args[]) {
    int n = 5;
    for(int i=0; i<n; i++){
        for(int j = 0; j < i; j++){
            System.out.print("  ");//my mind is blank, why 2 spaces and how to find in any problem
        }
        for(int j = 0; j < n-i; j++){
            System.out.print("*");
            if(j!=n-i-1) System.out.print(" ");
        }
        System.out.println();
    }
    for(int i=n-2; i>=0; i--){
        for(int j = 0; j < i; j++){
            System.out.print("  ");//my mind is blank, why 2 spaces and how to find in any problem
        }
        for(int j = 0; j < n-i; j++){
            System.out.print("*");
            if(j!=n-i-1) System.out.print(" ");
        }
        System.out.println();
    }
}
```

---

### Problem 30: Diamond/Arrow Pattern (Input 11)

**Description**: Print a diamond/arrow pattern with increasing and decreasing rows.

**Expected Output**:
```
*
* * * * *
* * * * * * * * *
* * * * * * * * * * * * *
* * * * * * * * * * *
* * * * *
*
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 31: Diamond/Arrow Pattern (Input 12)

**Description**: Print a diamond/arrow pattern with increasing and decreasing rows (starts with 2 asterisks).

**Expected Output**:
```
* *
* * * * * *
* * * * * * * * * * * *
* * * * * * * * * * * * * *
* * * * * * * * * * * *
* * * * * *
* *
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 32: Cross Pattern

**Description**: Print a cross/plus pattern.

**Expected Output**:
```
    *
  * * *
* * * * *
  * * *
    *
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 33: Rhombus Pattern

**Description**: Print a rhombus pattern with asterisks.

**Expected Output**:
```
    *
   * *
  * * *
 * * * *
* * * * *
 * * * *
  * * *
   * *
    *
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 34: Butterfly Star Pattern

**Description**: Print a butterfly pattern with stars.

**Expected Output**:
```
*           *
* *       * *
* * *   * * *
* * * * * * *
* * *   * * *
* *       * *
*           *
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 35: K Pattern

**Description**: Print a K pattern.

**Expected Output**:
```
*     *
*   *
* *
*   *
*     *
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 36: Hollow Reverse Triangle Pattern

**Description**: Print a hollow inverted triangle.

**Expected Output**:
```
* * * * *
*       *
*     *
*   *
* *
*
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

## Level 5 (Advanced)

### Problem 37: Number Changing Pattern

**Description**: Print a pattern with changing numbers.

**Expected Output**:
```
1
2 3
4 5 6
7 8 9 10
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 38: Zero-One Triangle

**Description**: Print a triangle with alternating 0s and 1s.

**Expected Output**:
```
1
0 1
1 0 1
0 1 0 1
1 0 1 0 1
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 39: Palindrome Triangular

**Description**: Print a palindromic triangular pattern.

**Expected Output**:
```
1
1 2 1
1 2 3 2 1
1 2 3 4 3 2 1
1 2 3 4 5 4 3 2 1
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 40: Reverse Number Triangle Pattern

**Description**: Print a reverse number triangle.

**Expected Output**:
```
1 2 3 4
2 3 4
3 4
4
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 41: Hollow Hourglass Pattern

**Description**: Print a hollow hourglass pattern.

**Expected Output**:
```
* * * * * * * * *
  * * * * * * *
    * * * * *
      * * *
        *
      * * *
    * * * * *
  * * * * * * *
* * * * * * * * *
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 42: Pascal's Triangle

**Description**: Print Pascal's triangle.

**Expected Output**:
```
1
1 1
1 2 1
1 3 3 1
1 4 6 4 1
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

---

### Problem 43: Right Pascal's Triangle

**Description**: Print Pascal's triangle on the right side.

**Expected Output**:
```
1
1 1
1 2 1
1 3 3 1
1 4 6 4 1
```

### Solution with O(n) Space Complexity
(To be added)

### Solution with O(1) Space Complexity
(To be added)

