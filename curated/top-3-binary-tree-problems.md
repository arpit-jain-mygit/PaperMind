# Top 17 High-Frequency Binary Search Tree (BST) Problems (MAANG)

## Table of Contents

### Binary Search Tree (BST) Problems - High to Low Frequency

#### With Code Solutions ✅
1. [LeetCode 98 - Validate Binary Search Tree [80%]](#1-leetcode-98---validate-binary-search-tree-80) ✅
2. [LeetCode 235 - LCA of Binary Search Tree [75%]](#2-leetcode-235---lca-of-binary-search-tree-75) ✅
3. [LeetCode 102 - Binary Tree Level Order Traversal [80%]](#3-leetcode-102---binary-tree-level-order-traversal-80) ✅
4. [LeetCode 94 - Binary Tree Inorder Traversal [78%]](#4-leetcode-94---binary-tree-inorder-traversal-78) ✅
5. [LeetCode 144 - Binary Tree Preorder Traversal [75%]](#5-leetcode-144---binary-tree-preorder-traversal-75) ✅
6. [LeetCode 145 - Binary Tree Postorder Traversal [72%]](#6-leetcode-145---binary-tree-postorder-traversal-72) ✅
7. [LeetCode 230 - Kth Smallest Element in BST [75%]](#7-leetcode-230---kth-smallest-element-in-bst-75) ✅

#### Additional Problems (To Be Added)
8. LeetCode 99 - Recover Binary Search Tree [75%]
9. LeetCode 427 - Serialize & Deserialize BST [75%]
10. LeetCode 215 - Kth Largest Element in BST [70%]
11. LeetCode 173 - Binary Search Tree Iterator [70%]
12. LeetCode 285 - Inorder Successor in BST [65%]
13. LeetCode 333 - Largest BST Subtree [65%]
14. LeetCode 108 - Convert Sorted Array to Balanced BST [60%]
15. LeetCode 510 - Inorder Predecessor in BST [60%]
16. LeetCode 270 - Closest Binary Search Tree Value [55%]
17. LeetCode 1305 - All Elements in Two Binary Search Trees [50%]

---

## 1. LeetCode 98 - Validate Binary Search Tree [75%]

### Reference Video
[![LeetCode 98 - Validate Binary Search Tree](https://img.youtube.com/vi/dSBcCynP1nA/maxresdefault.jpg)](https://youtu.be/dSBcCynP1nA?t=263)

**Video:** [LeetCode 98 - Validate Binary Search Tree](https://youtu.be/dSBcCynP1nA?t=263)

### Problem Statement
Given the root of a binary tree, determine if it is a valid Binary Search Tree (BST).

**Input:** Binary tree root node
**Output:** Boolean - true if valid BST, false otherwise

### Example
```
Valid BST:
    2
   / \
  1   3

Invalid BST:
    5
   / \
  1   4
 /     \
0       5

Expected Output: true, false
```

### Follow-up Problems (in the 19)
None

### Code Solution

```java
import java.util.*;

class Node {
    int value;
    Node left;
    Node right;

    public Node(int value) {
        this.value = value;
    }
    public String toString() {
        return "" + value;
    }
}

public class BSTValidity {
    // ============ VALID BST TEST CASES ============

    public static Node validSingleNode() {
        return new Node(5);
    }

    public static Node validBalancedBST() {
        Node root = new Node(4);
        root.left = new Node(2);
        root.right = new Node(6);
        root.left.left = new Node(1);
        root.left.right = new Node(3);
        root.right.left = new Node(5);
        root.right.right = new Node(7);
        return root;
    }

    public static Node validLeftSkewedBST() {
        Node root = new Node(5);
        root.left = new Node(4);
        root.left.left = new Node(3);
        root.left.left.left = new Node(2);
        root.left.left.left.left = new Node(1);
        return root;
    }

    public static Node validRightSkewedBST() {
        Node root = new Node(1);
        root.right = new Node(2);
        root.right.right = new Node(3);
        root.right.right.right = new Node(4);
        root.right.right.right.right = new Node(5);
        return root;
    }

    public static Node validComplexBST() {
        Node root = new Node(10);
        root.left = new Node(5);
        root.right = new Node(15);
        root.left.left = new Node(2);
        root.left.right = new Node(7);
        root.right.left = new Node(12);
        root.right.right = new Node(20);
        root.left.left.left = new Node(1);
        root.left.right.left = new Node(6);
        return root;
    }

    public static Node validWithNegativeNumbers() {
        Node root = new Node(0);
        root.left = new Node(-5);
        root.right = new Node(5);
        root.left.left = new Node(-8);
        root.left.right = new Node(-2);
        root.right.left = new Node(3);
        root.right.right = new Node(8);
        return root;
    }

    // ============ INVALID BST TEST CASES ============

    public static Node invalidRightChildTooSmall() {
        Node root = new Node(5);
        root.left = new Node(3);
        root.right = new Node(4);
        return root;
    }

    public static Node invalidLeftChildTooLarge() {
        Node root = new Node(5);
        root.left = new Node(6);
        root.right = new Node(8);
        return root;
    }

    public static Node invalidDeepLeftViolation() {
        Node root = new Node(10);
        root.left = new Node(5);
        root.right = new Node(15);
        root.left.left = new Node(2);
        root.left.right = new Node(7);
        root.left.left.left = new Node(12);
        return root;
    }

    public static Node invalidDeepRightViolation() {
        Node root = new Node(10);
        root.left = new Node(5);
        root.right = new Node(15);
        root.right.left = new Node(12);
        root.right.right = new Node(20);
        root.right.right.right = new Node(8);
        return root;
    }

    public static Node invalidMiddleNodeViolation() {
        Node root = new Node(5);
        root.left = new Node(3);
        root.right = new Node(8);
        root.left.left = new Node(2);
        root.left.right = new Node(6);
        root.left.right.right = new Node(9);
        return root;
    }

    public static Node invalidBothChildrenWrong() {
        Node root = new Node(5);
        root.left = new Node(6);
        root.right = new Node(4);
        return root;
    }

    public static Node invalidComplexTree() {
        Node root = new Node(50);
        root.left = new Node(30);
        root.right = new Node(70);
        root.left.left = new Node(20);
        root.left.right = new Node(40);
        root.right.left = new Node(60);
        root.right.right = new Node(80);
        root.left.right.left = new Node(35);
        root.left.right.left.left = new Node(75);
        return root;
    }

    public static Node invalidSingleNodeAsChild() {
        Node root = new Node(1);
        root.right = new Node(3);
        root.right.right = new Node(2);
        return root;
    }

    // ============ SOLUTION ============

    public static boolean isValidBST(Node root) {
        return isValidBST(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }
    
    public static boolean isValidBST(Node root, long min, long max) {
        if(root == null) return true;
        
        if(root.value <= min || root.value >= max)
            return false;
        
        return isValidBST(root.left, min, root.value) && 
               isValidBST(root.right, root.value, max);
    }

    // ============ TEST RUNNER ============

    public static void main(String[] args) {
        System.out.println("========== VALID BST CASES ==========\n");

        testCase("Valid: Single Node", validSingleNode(), true);
        testCase("Valid: Balanced BST", validBalancedBST(), true);
        testCase("Valid: Left-Skewed BST", validLeftSkewedBST(), true);
        testCase("Valid: Right-Skewed BST", validRightSkewedBST(), true);
        testCase("Valid: Complex BST", validComplexBST(), true);
        testCase("Valid: With Negative Numbers", validWithNegativeNumbers(), true);

        System.out.println("\n========== INVALID BST CASES ==========\n");

        testCase("Invalid: Right Child Too Small", invalidRightChildTooSmall(), false);
        testCase("Invalid: Left Child Too Large", invalidLeftChildTooLarge(), false);
        testCase("Invalid: Deep Left Violation", invalidDeepLeftViolation(), false);
        testCase("Invalid: Deep Right Violation", invalidDeepRightViolation(), false);
        testCase("Invalid: Middle Node Violation", invalidMiddleNodeViolation(), false);
        testCase("Invalid: Both Children Wrong", invalidBothChildrenWrong(), false);
        testCase("Invalid: Complex Tree Violation", invalidComplexTree(), false);
        testCase("Invalid: Single Node as Child", invalidSingleNodeAsChild(), false);
    }

    public static void testCase(String description, Node root, boolean expected) {
        boolean result = isValidBST(root);
        String status = result == expected ? "✅ PASS" : "❌ FAIL";
        System.out.println(status + " - " + description);
        System.out.println("   Expected: " + expected + ", Got: " + result + "\n");
    }
}
```

### Complexity Analysis
- **Time Complexity:** O(n) - visit each node once
- **Space Complexity:** O(h) - where h is height (recursion stack)

### MAANG Interview Questions
1. "What makes a tree a valid BST?" → Left < parent < right (recursively)
2. "Why can't you just check immediate children?" → Need to check entire subtree range
3. "Can you solve it iteratively?" → Use stack with min/max bounds
4. "How would you handle duplicate values?" → Decide if duplicates allowed (usually no)
5. "What about trees with very large/small values?" → Use Long instead of Integer

### Real-World Business Applications

**1. Database Indexing Validation (B-Tree Verification)**
- **Use Case:** After creating or modifying a B-Tree index in databases (PostgreSQL, MySQL, Oracle), verify the tree maintains BST properties
- **Business Impact:** Prevents query performance degradation from corrupted indexes
- **Example:** E-commerce platform validating product ID indexes after bulk imports

**2. Blockchain & Merkle Tree Validation**
- **Use Case:** Cryptocurrency wallets and blockchain explorers validate Merkle trees to ensure transaction integrity
- **Business Impact:** Detects tampering or corruption in transaction chains, critical for financial security
- **Example:** Bitcoin node verifying block merkle roots, Ethereum smart contract state proofs

**3. File System & Directory Tree Validation**
- **Use Case:** File systems (NTFS, ext4, APFS) validate directory tree structures during startup or repair
- **Business Impact:** Prevents data corruption and ensures file system consistency
- **Example:** macOS Disk Utility checking volume health, Linux fsck repair utility

---

## 2. LeetCode 235 - LCA of Binary Search Tree [65%]

### Reference Video
[![LeetCode 235 - LCA of BST](https://img.youtube.com/vi/cX_kPV_foZc/maxresdefault.jpg)](https://www.youtube.com/watch?v=cX_kPV_foZc&t=87s)

**Video:** [LeetCode 235 - LCA of Binary Search Tree](https://www.youtube.com/watch?v=cX_kPV_foZc&t=87s)

### Problem Statement
Given a Binary Search Tree (BST) and two nodes p and q, find their Lowest Common Ancestor (LCA).

**Key Difference from #4:** BST has ordering property (left < root < right), so you can navigate more efficiently.

**Input:** BST root, two nodes p and q
**Output:** The LCA node

### Examples (All 3 Scenarios)

```
Input BST:
      6
     / \
    2   8
   / \  / \
  0  4 7   9
    / \
   3   5

───────────────────────────────────────────────────────────

SCENARIO 1: p and q in DIFFERENT subtrees (split at root)
Input:  p = 2, q = 8
At node 6: p < 6 and q > 6 (split!)
Answer: 6 ✓

───────────────────────────────────────────────────────────

SCENARIO 2: Both p and q in LEFT subtree
Input:  p = 2, q = 4
At node 6: p < 6 and q < 6 (both left)
At node 2: p == 2 (found p!)
Answer: 2 ✓

───────────────────────────────────────────────────────────

SCENARIO 3: Both p and q in RIGHT subtree
Input:  p = 8, q = 9
At node 6: p > 6 and q > 6 (both right)
At node 8: p == 8 (found p!)
Answer: 8 ✓

───────────────────────────────────────────────────────────
```

### Logic Explanation

**Use BST property to navigate efficiently:**

```
findLCA_BST(root, p, q):
    if p < root AND q < root:
        return findLCA_BST(root.left, p, q)    // Both smaller → go left
    
    if p > root AND q > root:
        return findLCA_BST(root.right, p, q)   // Both larger → go right
    
    return root  // One on each side OR p/q equals root
```

**Key Insight:** Don't need to search both subtrees like regular tree!

### Follow-up Problems (in the 19)
None (follow-up to #4)

### Code Solution

```java
import java.util.*;
class Node{
    int value;
    Node left;
    Node right;
    
    public Node(int value){
        this.value = value;
    }
    public String toString(){
        return ""+value;
    }
}
public class BinarySearchTreeLCA{
    public static Node buildTree(){
        /*
               	  6
                 / \
                2   8
               / \  / \
              0  4 7   9
                / \
               3   5
        */
        
        Node root = new Node(6);
        
        root.left = new Node(2);
        root.right = new Node(8);
        
        root.left.left = new Node(0);
        root.left.right = new Node(4);
        
        root.left.right.left = new Node(3);
        root.left.right.right = new Node(5);
        
        root.right.left = new Node(7);
        root.right.right = new Node(9);
        
        return root;
    }
    
    public static Node findLCA(Node root, Node p, Node q){
        // p and q, both are in left subtree
        if(p.value < root.value && q.value < root.value){
            return findLCA(root.left, p, q);
        }
        
        // p and q, both are in right subtree
        if(p.value > root.value && q.value > root.value){
            return findLCA(root.right, p, q);
        }
        
        return root; // Split subtrees (p in left, q in right or vice versa)
    }
    
    public static void main (String[] args) {
        Node root = BinarySearchTreeLCA.buildTree();
        Node p;
        Node q;
        Node lca;
        
        /*
        SCENARIO 1: p and q in DIFFERENT subtrees (split at root)
        Input:  p = 2, q = 8
        At node 6: p < 6 and q > 6 (split!)
        Answer: 6 ✓
        ───────────────────────────────────────────────────────────
        */
        
        p = new Node(2);
        q = new Node(8);
        lca = BinarySearchTreeLCA.findLCA(root,p,q);
        System.out.println("Scenario 1 - LCA of 2 and 8: "+lca.value);
        
        /*
        SCENARIO 2: Both p and q in LEFT subtree
        Input:  p = 2, q = 4
        At node 6: p < 6 and q < 6 (both left)
        At node 2: p == 2 (found p!)
        Answer: 2 ✓
        ───────────────────────────────────────────────────────────
        */
        
        p = new Node(2);
        q = new Node(4);
        lca = BinarySearchTreeLCA.findLCA(root,p,q);
        System.out.println("Scenario 2 - LCA of 2 and 4: "+lca.value);
        
        /*
        SCENARIO 3: Both p and q in RIGHT subtree
        Input:  p = 8, q = 9
        At node 6: p > 6 and q > 6 (both right)
        At node 8: p == 8 (found p!)
        Answer: 8 ✓
        */
        p = new Node(8);
        q = new Node(9);
        
        lca = BinarySearchTreeLCA.findLCA(root,p,q);
        System.out.println("Scenario 3 - LCA of 8 and 9: "+lca.value);
    }
}
```

**Output:**
```
Scenario 1 - LCA of 2 and 8: 6
Scenario 2 - LCA of 2 and 4: 2
Scenario 3 - LCA of 8 and 9: 8
```

### Complexity Analysis
- **Time Complexity:** O(log n) average, O(n) worst case (unbalanced tree)
- **Space Complexity:** O(h) - where h is height (recursion stack)

### MAANG Interview Questions
1. "How is this different from regular tree LCA?" → Can use BST property to skip subtrees
2. "What if p or q don't exist?" → Behavior depends on problem statement
3. "Can you do it iteratively?" → Yes, navigate based on comparisons
4. "Why is BST version faster?" → Only search one path, not both sides
5. "What if tree is not balanced?" → Worst case becomes O(n), still better average case"

### Real-World Business Applications

**1. Gene/DNA Sequence Analysis (Phylogenetic Trees)**
- **Use Case:** Bioinformatics tools find the most recent common ancestor of two genes/organisms
- **Business Impact:** Pharmaceutical companies use this for drug target discovery and evolutionary studies
- **Example:** Finding common ancestor of two patient mutations in cancer genome research

**2. Organizational Hierarchy (Common Manager)**
- **Use Case:** HR systems find the common boss/manager of two employees
- **Business Impact:** Determines approval authority, reporting chain, and organizational boundaries
- **Example:** LinkedIn/Slack finding common team lead between two employees for project assignment

**3. Flight Route Optimization (Airport Networks)**
- **Use Case:** Airlines find the closest common hub airport for connecting two flights
- **Business Impact:** Optimizes connection times, reduces costs, and improves customer experience
- **Example:** Finding hub airport to route flights from rural airport to destination via major hubs

---

## 3. LeetCode 230 - Kth Smallest Element in BST [60%]

### Problem Statement
Given the root of a Binary Search Tree, return the kth smallest value in the BST.

**Input:** BST root, integer k
**Output:** Kth smallest value

### Example
```
Input BST:
      3
     / \
    1   4
     \
      2

k = 1: Expected Output: 1 (smallest)
k = 2: Expected Output: 2 (second smallest)
k = 3: Expected Output: 3
k = 4: Expected Output: 4 (largest)
```

### Follow-up Problems (in the 19)
None

### Code Solution

```java
// Write your solution here

```

### Complexity Analysis
- **Time Complexity:** O(n) - in-order traversal visits all nodes
- **Space Complexity:** O(h) - where h is height (recursion stack)

### MAANG Interview Questions
1. "How would you find kth largest?" → Reverse in-order traversal
2. "Can you optimize if k is small?" → Early termination when count == k
3. "What if tree is updated frequently?" → Use augmented tree with size info
4. "Can you do it iteratively?" → Yes, use explicit stack for in-order
5. "How would you find kth median?" → Find median position using tree sizes

---

## 4. LeetCode 94 - Binary Tree Inorder Traversal [78%]

### Problem Statement
Given the root of a binary tree, return the inorder traversal of its nodes' values.

**Input:** Binary tree root node
**Output:** List of node values in inorder sequence

### Example
```
Input Tree:
    4
   / \
  2   6
 / \ / \
1 3 5  7

Inorder Traversal: [1, 2, 3, 4, 5, 6, 7]
(Left → Root → Right)
```

### Follow-up Problems (in the 17)
- **#5: LeetCode 144 - Binary Tree Preorder Traversal [75%]** - Root → Left → Right
- **#6: LeetCode 145 - Binary Tree Postorder Traversal [72%]** - Left → Right → Root

### Code Solution

```java
import java.util.*;

class Node {
    int value;
    Node left;
    Node right;

    public Node(int value) {
        this.value = value;
    }
    public String toString() {
        return "" + value;
    }
}

public class BSTInOrderTraversal {
    // ============ VALID BST TEST CASES ============
    public static Node validBalancedBST() {
        /*
              4
             / \
            2   6
           / \ / \
          1 3 5  7
        */
        Node root = new Node(4);
        root.left = new Node(2);
        root.right = new Node(6);
        root.left.left = new Node(1);
        root.left.right = new Node(3);
        root.right.left = new Node(5);
        root.right.right = new Node(7);
        return root;
    }

    // ============ TEST RUNNER ============

    public static void main(String[] args) {
        Node root = validBalancedBST();
        in_order_traversal(root);
    }

    public static Node in_order_traversal(Node root){
        if (root == null) return null;
        in_order_traversal(root.left);
        System.out.println(root.value+",");
        in_order_traversal(root.right);
        return null;
    }
}
```

**Output:**
```
1,
2,
3,
4,
5,
6,
7,
```

### Complexity Analysis
- **Time Complexity:** O(n) - visit each node exactly once
- **Space Complexity:** O(h) - where h is height (recursion stack)

### MAANG Interview Questions
1. "Why is inorder useful for BST?" → Returns sorted order (Left < Root < Right)
2. "Can you do it iteratively?" → Yes, use explicit stack
3. "How to do preorder/postorder?" → Change the order of operations
4. "What if you need to store in a list?" → Collect values in List instead of printing
5. "Can you solve without recursion?" → Stack-based iterative approach

### Real-World Business Applications

**1. BST Validation & Verification**
- **Use Case:** Database systems verify B-tree/BST integrity using inorder traversal
- **Business Impact:** Ensures data structure validity after insertions/deletions
- **Example:** MySQL/PostgreSQL validating index tree structure

**2. Report Generation (Sorted Output)**
- **Use Case:** Financial systems generate sorted transaction reports
- **Business Impact:** Produces chronologically/numerically sorted reports efficiently
- **Example:** Bank statements showing transactions in date order

**3. Data Export & Migration**
- **Use Case:** Export database records in sorted order for audit trails
- **Business Impact:** Compliance with regulatory requirements for sorted data dumps
- **Example:** GDPR compliance exporting user data in sorted format

---

## 5. LeetCode 144 - Binary Tree Preorder Traversal [75%]

### Problem Statement
Given the root of a binary tree, return the preorder traversal of its nodes' values.

**Input:** Binary tree root node
**Output:** List of node values in preorder sequence

### Example
```
Input Tree:
    4
   / \
  2   6
 / \ / \
1 3 5  7

Preorder Traversal: [4, 2, 1, 3, 6, 5, 7]
(Root → Left → Right)
```

### Follow-up Problems (in the 17)
- **#4: LeetCode 94 - Binary Tree Inorder Traversal [78%]** - Left → Root → Right
- **#6: LeetCode 145 - Binary Tree Postorder Traversal [72%]** - Left → Right → Root

### Code Solution

```java
import java.util.*;

class Node {
    int value;
    Node left;
    Node right;

    public Node(int value) {
        this.value = value;
    }
    public String toString() {
        return "" + value;
    }
}

public class BSTPreOrderTraversal {
    // ============ VALID BST TEST CASES ============
    public static Node validBalancedBST() {
        /*
              4
             / \
            2   6
           / \ / \
          1 3 5  7
        */
        Node root = new Node(4);
        root.left = new Node(2);
        root.right = new Node(6);
        root.left.left = new Node(1);
        root.left.right = new Node(3);
        root.right.left = new Node(5);
        root.right.right = new Node(7);
        return root;
    }

    // ============ TEST RUNNER ============

    public static void main(String[] args) {
        Node root = validBalancedBST();
        pre_order_traversal(root);
    }

    public static Node pre_order_traversal(Node root){
        if (root == null) return null;
        System.out.println(root.value+",");
        pre_order_traversal(root.left);
        pre_order_traversal(root.right);
        return null;
    }
}
```

**Output:**
```
4,
2,
1,
3,
6,
5,
7,
```

### Complexity Analysis
- **Time Complexity:** O(n) - visit each node exactly once
- **Space Complexity:** O(h) - where h is height (recursion stack)

### MAANG Interview Questions
1. "What's the difference between preorder and inorder?" → Order of visiting node vs children
2. "Why preorder for serialization?" → Can reconstruct tree uniquely with preorder + inorder
3. "Can you do it iteratively?" → Yes, use explicit stack (push right then left)
4. "Where is preorder used?" → Tree copying, expression evaluation, XML/JSON parsing
5. "How to identify preorder from output?" → First element is always root

### Real-World Business Applications

**1. Tree Copying & Cloning**
- **Use Case:** File system or DOM tree cloning operations
- **Business Impact:** Efficient deep copy of hierarchical structures
- **Example:** JavaScript DOM cloning (React virtual DOM)

**2. Expression Tree Evaluation**
- **Use Case:** Mathematical expression parsing and evaluation
- **Business Impact:** Prefix notation (Polish notation) for calculators
- **Example:** Compiler parsing of arithmetic expressions

**3. Serialization & Deserialization**
- **Use Case:** Convert tree to string format for storage/transmission
- **Business Impact:** Combined with inorder to uniquely reconstruct tree
- **Example:** Database tree indexing, microservices communication

---

## 6. LeetCode 145 - Binary Tree Postorder Traversal [72%]

### Problem Statement
Given the root of a binary tree, return the postorder traversal of its nodes' values.

**Input:** Binary tree root node
**Output:** List of node values in postorder sequence

### Example
```
Input Tree:
    4
   / \
  2   6
 / \ / \
1 3 5  7

Postorder Traversal: [1, 3, 2, 5, 7, 6, 4]
(Left → Right → Root)
```

### Follow-up Problems (in the 17)
- **#4: LeetCode 94 - Binary Tree Inorder Traversal [78%]** - Left → Root → Right
- **#5: LeetCode 144 - Binary Tree Preorder Traversal [75%]** - Root → Left → Right

### Code Solution

```java
import java.util.*;

class Node {
    int value;
    Node left;
    Node right;

    public Node(int value) {
        this.value = value;
    }
    public String toString() {
        return "" + value;
    }
}

public class BSTPostOrderTraversal {
    // ============ VALID BST TEST CASES ============
    public static Node validBalancedBST() {
        /*
              4
             / \
            2   6
           / \ / \
          1 3 5  7
        */
        Node root = new Node(4);
        root.left = new Node(2);
        root.right = new Node(6);
        root.left.left = new Node(1);
        root.left.right = new Node(3);
        root.right.left = new Node(5);
        root.right.right = new Node(7);
        return root;
    }

    // ============ TEST RUNNER ============

    public static void main(String[] args) {
        Node root = validBalancedBST();
        post_order_traversal(root);
    }

    public static Node post_order_traversal(Node root){
        if (root == null) return null;
        post_order_traversal(root.left);
        post_order_traversal(root.right);
        System.out.println(root.value+",");
        return null;
    }
}
```

**Output:**
```
1,
3,
2,
5,
7,
6,
4,
```

### Complexity Analysis
- **Time Complexity:** O(n) - visit each node exactly once
- **Space Complexity:** O(h) - where h is height (recursion stack)

### MAANG Interview Questions
1. "When is postorder used?" → Tree deletion, dependency resolution, bottom-up computation
2. "How to convert to iterative?" → Two stacks or one stack with tracking
3. "Why postorder for deletion?" → Child nodes deleted before parent (no dangling pointers)
4. "Postorder vs others?" → Postorder processes data from bottom-up
5. "Real-world use cases?" → Undo/redo stacks, expression evaluation with operands first

### Real-World Business Applications

**1. Tree/Graph Deletion & Resource Cleanup**
- **Use Case:** Safely delete entire tree structure bottom-up
- **Business Impact:** Prevents memory leaks and dangling pointers
- **Example:** File system directory deletion, DOM cleanup in browsers

**2. Dependency Resolution & Build Systems**
- **Use Case:** Build projects in correct order (dependencies before dependents)
- **Business Impact:** Ensures correct compilation order, avoids circular dependencies
- **Example:** Maven/Gradle dependency resolution, build systems

**3. Postfix Notation & RPN Calculators**
- **Use Case:** Convert infix expressions to postfix for evaluation
- **Business Impact:** Efficient expression evaluation without parentheses
- **Example:** Scientific calculators, stack-based computation engines

---

## 7. LeetCode 230 - Kth Smallest Element in BST [75%]

### Problem Statement
Given the root of a Binary Search Tree, return the kth smallest value (1-indexed) in the tree.

**Input:** BST root, integer k
**Output:** Kth smallest value in BST

### Example
```
Input BST:
    4
   / \
  2   6
 / \ / \
1 3 5  7

k = 1: Expected Output: 1 (smallest)
k = 2: Expected Output: 2 (second smallest)
k = 3: Expected Output: 3
k = 4: Expected Output: 4
```

### Follow-up Problems (in the 17)
- **#10: LeetCode 215 - Kth Largest Element in BST [70%]** - Use reverse in-order
- **#12: LeetCode 285 - Inorder Successor in BST [65%]** - Similar navigation

### Code Solution - Naive Approach

```java
import java.util.*;

class Node {
    int value;
    Node left;
    Node right;

    public Node(int value) {
        this.value = value;
    }
    public String toString() {
        return "" + value;
    }
}

public class KthSmallestNaive {
    // ============ VALID BST TEST CASES ============
    public static Node validBalancedBST() {
        /*
              4
             / \
            2   6
           / \ / \
          1 3 5  7
        */
        Node root = new Node(4);
        root.left = new Node(2);
        root.right = new Node(6);
        root.left.left = new Node(1);
        root.left.right = new Node(3);
        root.right.left = new Node(5);
        root.right.right = new Node(7);
        return root;
    }

    // ============ TEST RUNNER ============

    public static void main(String[] args) {
        Node root = validBalancedBST();
        
        for (int k = 1; k <= 7; k++) {
            List<Integer> list = new ArrayList<Integer>();
            in_order_traversal(root, list);
            System.out.println("k = " + k + ": " + list.get(k-1));
        }
    }

    public static Node in_order_traversal(Node root, List<Integer> list){
        if (root == null) return null;
        in_order_traversal(root.left, list);
        list.add(root.value);
        in_order_traversal(root.right, list);
        return null;
    }
}
```

**Output:**
```
k = 1: 1
k = 2: 2
k = 3: 3
k = 4: 4
k = 5: 5
k = 6: 6
k = 7: 7
```

### Complexity Analysis - Naive Approach

| Metric | Complexity | Explanation |
|--------|-----------|-------------|
| **Time** | O(n) | Must traverse ALL nodes to build complete sorted list |
| **Space** | O(n) | Store all n values in ArrayList + O(h) recursion stack |

### Why is this Naive?

❌ **Problems:**
- Visits **every node** even if k is small (k=1 only needs 1 node!)
- Creates a **full list** of n elements when only kth is needed
- Wastes memory and time on unnecessary nodes

✅ **Better approach:**
- Use early termination: Stop after finding kth element
- Time: O(k) best case, O(n) worst case
- Space: O(h) recursion stack only (no list)

### MAANG Interview Questions
1. "Why is the naive approach inefficient?" → Visits all nodes when only k needed
2. "How to optimize with early termination?" → Stop inorder traversal after kth element
3. "What if k is close to 1?" → Naive: O(n), Optimized: O(k)
4. "How to find kth largest?" → Use reverse in-order (right → root → left)
5. "Can you do it iteratively?" → Yes, with explicit stack and counter

### Real-World Business Applications

**1. Database Query Optimization**
- **Use Case:** Find kth smallest value in sorted index without loading all data
- **Business Impact:** Reduces memory usage and query time for large datasets
- **Example:** SQL LIMIT/OFFSET with WHERE clause on B-tree index

**2. Median Finding in Streams**
- **Use Case:** Find median of continuously arriving numbers
- **Business Impact:** Efficient statistical analysis without storing all values
- **Example:** Real-time analytics, sensor data processing

**3. Percentile Calculations**
- **Use Case:** Find 95th percentile value in performance metrics
- **Business Impact:** Monitor SLA compliance and service health
- **Example:** Web server latency analysis, user engagement metrics

---

## Summary

| # | Problem | Frequency | Type | Status |
|---|---------|-----------|------|--------|
| 1 | Validate BST | 80% | Validation | ✅ |
| 2 | LCA BST | 75% | Traversal | ✅ |
| 3 | Level Order Traversal | 80% | Traversal | ✅ |
| 4 | Inorder Traversal | 78% | Traversal | ✅ |
| 5 | Preorder Traversal | 75% | Traversal | ✅ |
| 6 | Postorder Traversal | 72% | Traversal | ✅ |
| 7 | Kth Smallest (Naive) | 75% | Traversal | ✅ |
| 8 | Recover BST | 75% | Fixing | - |
| 9 | Serialize & Deserialize BST | 75% | Design | - |
| 10 | Kth Largest | 70% | Traversal | - |
| 11 | BST Iterator | 70% | Design | - |
| 12 | Inorder Successor | 65% | Navigation | - |
| 13 | Largest BST Subtree | 65% | Finding | - |
| 14 | Sorted Array to BST | 60% | Construction | - |
| 15 | Inorder Predecessor | 60% | Navigation | - |
| 16 | Closest Value | 55% | Search | - |
| 17 | All Elements in 2 BSTs | 50% | Merging | - |

**Master these 17 problems → Master 95%+ of all MAANG BST interview questions** 🎯
