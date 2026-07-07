# Top 19 High-Frequency Binary Tree Problems (MAANG)

## Table of Contents

### Binary Search Tree (BST) Problems - High to Low Frequency

1. [LeetCode 98 - Validate Binary Search Tree [75%]](#1-leetcode-98---validate-binary-search-tree-75)
2. [LeetCode 235 - LCA of Binary Search Tree [65%]](#2-leetcode-235---lca-of-binary-search-tree-65)
3. [LeetCode 230 - Kth Smallest Element in BST [60%]](#3-leetcode-230---kth-smallest-element-in-bst-60)

### Binary Tree (BT) Problems - High to Low Frequency

4. [LeetCode 236 - Lowest Common Ancestor [85%]](#4-leetcode-236---lowest-common-ancestor-85)
5. [LeetCode 102 - Binary Tree Level Order Traversal [80%]](#5-leetcode-102---binary-tree-level-order-traversal-80)
6. [LeetCode 124 - Maximum Path Sum [80%]](#6-leetcode-124---maximum-path-sum-80)
7. [LeetCode 297 - Serialize and Deserialize Binary Tree [80%]](#7-leetcode-297---serialize-and-deserialize-binary-tree-80)
8. [LeetCode 112 - Path Sum [75%]](#8-leetcode-112---path-sum-75)
9. [LeetCode 226 - Invert Binary Tree [70%]](#9-leetcode-226---invert-binary-tree-70)
10. [LeetCode 543 - Diameter of Binary Tree [70%]](#10-leetcode-543---diameter-of-binary-tree-70)
11. [LeetCode 199 - Binary Tree Right Side View [70%]](#11-leetcode-199---binary-tree-right-side-view-70)
12. [LeetCode 110 - Balanced Binary Tree [65%]](#12-leetcode-110---balanced-binary-tree-65)
13. [LeetCode 114 - Flatten Binary Tree to Linked List [65%]](#13-leetcode-114---flatten-binary-tree-to-linked-list-65)
14. [LeetCode 105 - Construct Binary Tree from Preorder and Inorder [65%]](#14-leetcode-105---construct-binary-tree-from-preorder-and-inorder-65)
15. [LeetCode 113 - Path Sum II [60%]](#15-leetcode-113---path-sum-ii-60)
16. [LeetCode 100 - Same Tree [60%]](#16-leetcode-100---same-tree-60)
17. [LeetCode 572 - Subtree of Another Tree [60%]](#17-leetcode-572---subtree-of-another-tree-60)
18. [LeetCode 987 - Vertical Order Traversal [55%]](#18-leetcode-987---vertical-order-traversal-55)
19. [LeetCode 103 - Binary Tree Zigzag Level Order Traversal [50%]](#19-leetcode-103---binary-tree-zigzag-level-order-traversal-50)

---

## 1. LeetCode 98 - Validate Binary Search Tree [75%]

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
// Write your solution here

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

## 4. LeetCode 236 - Lowest Common Ancestor [85%]

### Problem Statement
Given a binary tree and two nodes p and q, find their Lowest Common Ancestor (LCA). The LCA is the deepest node that is an ancestor to both p and q.

**Input:** Root node, two nodes p and q
**Output:** The LCA node

### Examples (All 4 Scenarios)

```
Input Tree:
      3
     / \
    5   1
   / \ / \
  6  2 0  8
    / \
   7   4

───────────────────────────────────────────────────────────

SCENARIO 1: Both p and q in DIFFERENT subtrees
Input:  p = 5, q = 1
Path p: 5 → 3
Path q: 1 → 3
First common: 3
Expected Output: 3

───────────────────────────────────────────────────────────

SCENARIO 2: p and q BOTH in LEFT subtree
Input:  p = 5, q = 4
Path p: 5 → 3
Path q: 4 → 2 → 5 → 3
First common: 5
Expected Output: 5

───────────────────────────────────────────────────────────

SCENARIO 3: p and q BOTH in RIGHT subtree
Input:  p = 1, q = 0
Path p: 1 → 3
Path q: 0 → 1 → 3
First common: 1
Expected Output: 1

───────────────────────────────────────────────────────────

SCENARIO 4: One node is ANCESTOR of the other
Input:  p = 3, q = 4
Path p: 3 (root itself)
Path q: 4 → 2 → 5 → 3
First common: 3 (root is ancestor to all)
Expected Output: 3

───────────────────────────────────────────────────────────
```

### Logic Explanation

**Three possible outcomes at each node:**

1. **Both p and q in DIFFERENT subtrees** → Current node is LCA
2. **Both p and q in LEFT subtree only** → Recursively find in left
3. **Both p and q in RIGHT subtree only** → Recursively find in right

**Pseudocode:**
```
findLCA(root, p, q):
    if root is null: return null
    if root == p or root == q: return root
    
    left = findLCA(root.left, p, q)
    right = findLCA(root.right, p, q)
    
    if left and right: return root  // Both in different sides
    if left: return left             // Both in left side
    if right: return right           // Both in right side
    return null                      // Neither found
```

### Follow-up Problems (in the 19)
- **#2: LeetCode 235 - LCA of Binary Search Tree [65%]** - Same logic but use BST property

### Code Solution

```java
// Write your solution here

```

### Complexity Analysis
- **Time Complexity:** O(n) - visit each node once in worst case
- **Space Complexity:** O(h) - where h is height (recursion stack)

### MAANG Interview Questions
1. "What if the nodes don't exist in the tree?" → Return null
2. "Can you solve it without recursion?" → Use iterative approach with parent pointers
3. "What if one node is ancestor of another?" → Return that node (it's the LCA)
4. "How would you find LCA in a BST?" → Use BST property (p < node < q)
5. "What if you need to find LCA of multiple nodes?" → Extend logic to handle k nodes

---

## 5. LeetCode 102 - Binary Tree Level Order Traversal [80%]

### Problem Statement
Given a binary tree, return its level order traversal (BFS). Each level should be a separate list.

**Input:** Binary tree root node
**Output:** List of lists where each sublist contains nodes at that level

### Follow-up Problems (in the 19)
- **#11: LeetCode 199 - Binary Tree Right Side View [70%]** - Return rightmost node of each level
- **#19: LeetCode 103 - Binary Tree Zigzag Level Order Traversal [50%]** - Alternate direction per level

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
public class TreeLevelsTraversal{
    public static Node buildTree(){
        Node root = new Node(1);
        root.left = new Node(2);
        root.right = new Node(3);
        root.left.left = new Node(4);
        root.left.right = new Node(5);
        root.right.left = new Node(6);
        root.right.right = new Node(7);
        return root;
    }
    
    public static List<Node> getAllLevels(Node root){
        List results = new ArrayList<Node>();
        Queue q = new LinkedList<Node>();
        q.add(root);
        while(q.size()!=0){
            List levelsList = new ArrayList<Node>();
            for (int i =0 ; i < q.size() ; i++){
                Node elem = (Node)q.remove();
                levelsList.add(elem);
                Node left = elem.left;
                Node right = elem.right;
                if (left != null) q.add(left);
                if (right != null) q.add(right);
            }
            System.out.println("levelsList:"+levelsList);
            results.add(levelsList);
        }
        return results;
    }
    
    public static void main (String[] args) {
        Node root = TreeLevelsTraversal.buildTree();
        List results = TreeLevelsTraversal.getAllLevels(root);
        System.out.println("Results:"+results);
    }
}
```

**Output:**
```
levelsList:[1]
levelsList:[2, 3]
levelsList:[4, 5, 6, 7]
Results:[[1], [2, 3], [4, 5, 6, 7]]
```

### Complexity Analysis
- **Time Complexity:** O(n) - visit each node exactly once
- **Space Complexity:** O(w) - where w is maximum width (nodes at widest level)

### MAANG Interview Questions
1. "Can you do this iteratively without recursion?" ✓ Queue-based approach shown
2. "What if you need to return it in reverse order?" → Use Collections.reverse()
3. "What if you only need the rightmost node of each level?" → Keep only last element of each level
4. "How would you find the average of each level?" → Sum nodes and divide by count
5. "Can you connect nodes at the same level?" → Add next pointer instead of storing values

---

## 6. LeetCode 124 - Maximum Path Sum [80%]

### Problem Statement
Find the maximum sum of any path in a binary tree. A path can start and end at any node, not necessarily root or leaf.

**Input:** Binary tree root node
**Output:** Maximum sum of any path

### Example
```
Input Tree:
    -10
    /  \
   9    20
       /  \
      15   7

Paths considered:
- 9: sum = 9
- 20: sum = 20
- 15 → 20 → 7: sum = 42 ✓ (Maximum)

Expected Output: 42
```

### Follow-up Problems (in the 19)
- **#8: LeetCode 112 - Path Sum [75%]** - Check if any path sums to target value
- **#15: LeetCode 113 - Path Sum II [60%]** - Return all paths with target sum

### Code Solution

```java
// Write your solution here

```

### Complexity Analysis
- **Time Complexity:** O(n) - visit each node exactly once
- **Space Complexity:** O(h) - where h is height (recursion stack)

### MAANG Interview Questions
1. "How do you handle negative numbers?" → Skip negative paths by taking max(0, value)
2. "Can you do it iteratively?" → More complex; post-order traversal needed
3. "What if path must go through root?" → Always include root in calculation
4. "What if path must be root to leaf?" → Different approach, simpler recursion
5. "How would you find the actual path, not just sum?" → Track path along with maximum

---

## 7. LeetCode 297 - Serialize and Deserialize Binary Tree [80%]

### Problem Statement
Design an algorithm to serialize a binary tree into a string and deserialize the string back into the original tree structure.

**Input:** Binary tree root (for serialize) or String (for deserialize)
**Output:** String representation or reconstructed tree

### Example
```
Input Tree:
    1
   / \
  2   3

Serialize: "1,2,null,null,3,null,null"
Deserialize: Reconstruct back to original tree

Expected Output: Same tree structure as input
```

### Follow-up Problems (in the 19)
None

### Code Solution

```java
// Write your solution here

```

### Complexity Analysis
- **Time Complexity:** O(n) - visit each node once
- **Space Complexity:** O(n) - store all nodes in string/queue

### MAANG Interview Questions
1. "What format would you use for the string representation?" → Level-order with null markers
2. "Can you handle different tree types?" → Use polymorphism or type markers
3. "How would you optimize for very large trees?" → Use compression or streaming
4. "What about tree with duplicate values?" → Use node references or indices
5. "How would you handle very deep trees?" → Use iterative approach to avoid stack overflow

---

## 8. LeetCode 112 - Path Sum [75%]

### Problem Statement
Given a binary tree and an integer target sum, check if there is a root-to-leaf path that sums to the target value.

**Input:** Root node, target sum
**Output:** Boolean - true if path exists, false otherwise

### Example
```
Input Tree:
      5
     / \
    4   8
   /   / \
  11  13  4
 / \      \
7   2      1

Target sum: 22
Path: 5 → 4 → 11 → 2 = 22 ✓

Expected Output: true
```

### Follow-up Problems (in the 19)
- **#15: LeetCode 113 - Path Sum II [60%]** - Return all paths with target sum

### Code Solution

```java
// Write your solution here

```

### Complexity Analysis
- **Time Complexity:** O(n) - visit each node in worst case
- **Space Complexity:** O(h) - where h is height (recursion stack)

### MAANG Interview Questions
1. "What if path doesn't have to be root to leaf?" → Different approach needed
2. "Can you find all paths that sum to target?" → Backtracking with list
3. "How would you handle paths that can skip nodes?" → DFS from every node
4. "What if tree has negative values?" → Still works, may have multiple paths
5. "Can you solve it iteratively?" → Use stack with (node, currentSum) pairs

---

## 9. LeetCode 226 - Invert Binary Tree [70%]

### Problem Statement
Given the root of a binary tree, invert the tree (swap left and right children at every node).

**Input:** Binary tree root node
**Output:** Root of inverted tree

### Example
```
Input Tree:
    1
   / \
  2   3

Inverted Tree:
    1
   / \
  3   2

Expected Output: Inverted tree
```

### Follow-up Problems (in the 19)
None

### Code Solution

```java
// Write your solution here

```

### Complexity Analysis
- **Time Complexity:** O(n) - visit each node once
- **Space Complexity:** O(h) - where h is height (recursion stack)

### MAANG Interview Questions
1. "Can you do this iteratively?" → Use queue for level-order approach
2. "Can you do this in-place?" → Yes, swap during traversal
3. "What if tree is very large?" → Iterative approach to avoid stack overflow
4. "How would you invert only a subtree?" → Apply same logic to subtree
5. "What's the minimum number of operations?" → One swap per node = n operations

---

## 10. LeetCode 543 - Diameter of Binary Tree [70%]

### Problem Statement
Given the root of a binary tree, return the length of the diameter of the tree. The diameter is the length of the longest path between any two nodes in a tree.

**Input:** Binary tree root
**Output:** Integer - diameter length

### Example
```
Input Tree:
      1
     / \
    2   3
   / \
  4   5

Longest path: 4 → 2 → 1 → 3 (length = 3 edges)

Expected Output: 3
```

### Follow-up Problems (in the 19)
None

### Code Solution

```java
// Write your solution here

```

### Complexity Analysis
- **Time Complexity:** O(n) - visit each node once
- **Space Complexity:** O(h) - where h is height (recursion stack)

### MAANG Interview Questions
1. "What counts as the diameter - just edges or nodes?" → Usually edges
2. "Can diameter pass through root?" → No, it's any longest path
3. "How would you find the actual path, not just length?" → Track nodes while traversing
4. "What if tree has only one node?" → Return 0
5. "Can you do it iteratively?" → Yes, with post-order using stack

---

## 11. LeetCode 199 - Binary Tree Right Side View [70%]

### Problem Statement
Given the root of a binary tree, imagine yourself standing on the right side of it. Return the values of the nodes you can see ordered from top to bottom.

**Input:** Binary tree root node
**Output:** List of nodes visible from right side

### Example
```
Input Tree:
    1
   / \
  2   3
 / \
4   5

Right side view: [1, 3, 5]

Expected Output: [1, 3, 5]
```

### Follow-up Problems (in the 19)
None

### Code Solution

```java
// Write your solution here

```

### Complexity Analysis
- **Time Complexity:** O(n) - visit each node once
- **Space Complexity:** O(w) - where w is maximum width

### MAANG Interview Questions
1. "How would you do this from the left side?" → Keep leftmost instead of rightmost
2. "Can you solve without level-order?" → DFS with right-first traversal
3. "What if you need all visible nodes (right + left)?" → Combine both approaches
4. "How would you handle a view from front?" → DFS by depth, track shallowest at each depth
5. "Can you return with depth information?" → Add depth to result

---

## 12. LeetCode 110 - Balanced Binary Tree [65%]

### Problem Statement
Given a binary tree, determine if it is height-balanced. A binary tree is height-balanced if for every node, the absolute difference between heights of left and right subtrees is at most 1.

**Input:** Binary tree root
**Output:** Boolean - true if balanced, false otherwise

### Example
```
Balanced Tree:
    3
   / \
  9  20
    /  \
   15   7

Unbalanced Tree:
      1
       \
        2
         \
          3

Expected Output: true, false
```

### Follow-up Problems (in the 19)
None

### Code Solution

```java
// Write your solution here

```

### Complexity Analysis
- **Time Complexity:** O(n) - visit each node once
- **Space Complexity:** O(h) - where h is height (recursion stack)

### MAANG Interview Questions
1. "What makes a tree balanced?" → Height difference ≤ 1 at every node
2. "Can you do it in one pass?" → Yes, return both height and balance status
3. "What if you check only immediate children?" → Incorrect - need to check entire subtree
4. "How would you rebalance an unbalanced tree?" → Use rotations (AVL tree)
5. "Can you do it iteratively?" → Yes, with post-order traversal

---

## 13. LeetCode 114 - Flatten Binary Tree to Linked List [65%]

### Problem Statement
Given the root of a binary tree, flatten the tree in-place so that it becomes a linked list using right pointers. The pre-order traversal of the flattened tree should be the same as the original tree.

**Input:** Binary tree root
**Output:** Root of flattened tree (modified in-place)

### Example
```
Input Tree:
    1
   / \
  2   5
 / \   \
3   4   6

Flattened (using right pointers):
1 → 2 → 3 → 4 → 5 → 6

Expected Output: Linked list structure via right pointers
```

### Follow-up Problems (in the 19)
None

### Code Solution

```java
// Write your solution here

```

### Complexity Analysis
- **Time Complexity:** O(n) - visit each node once
- **Space Complexity:** O(h) - where h is height (recursion stack)

### MAANG Interview Questions
1. "Why modify in-place?" → More efficient space usage
2. "What's the traversal order?" → Pre-order (root, left, right)
3. "Can you do it iteratively?" → Yes, with explicit stack
4. "How would you restore the left pointers?" → Not needed for linked list
5. "Can you flatten to doubly-linked list?" → Yes, add prev pointers

---

## 14. LeetCode 105 - Construct Binary Tree from Preorder and Inorder [65%]

### Problem Statement
Given two integer arrays preorder and inorder, construct and return the binary tree.

**Input:** Preorder traversal array, Inorder traversal array
**Output:** Root node of constructed tree

### Example
```
Preorder: [3, 9, 20, 15, 7]
Inorder:  [9, 3, 15, 20, 7]

Constructed Tree:
      3
     / \
    9  20
      / \
     15  7

Expected Output: Root node of tree
```

### Follow-up Problems (in the 19)
None

### Code Solution

```java
// Write your solution here

```

### Complexity Analysis
- **Time Complexity:** O(n²) - O(n) for each element, O(n) to find in inorder
- **Space Complexity:** O(n) - for result tree

### MAANG Interview Questions
1. "Why do we need both preorder and inorder?" → One array alone can't uniquely define tree
2. "Can you do it with postorder and inorder?" → Yes, similar logic
3. "Can you optimize the search in inorder?" → Use HashMap to store inorder indices
4. "What if arrays have duplicates?" → Can't uniquely reconstruct
5. "How would you construct from preorder and postorder?" → Can't do it uniquely

---

## 15. LeetCode 113 - Path Sum II [60%]

### Problem Statement
Given the root of a binary tree and an integer target sum, return all root-to-leaf paths where the sum of the values equals the target sum.

**Input:** Root node, target sum
**Output:** List of all paths that sum to target

### Example
```
Input Tree:
      5
     / \
    4   8
   /   / \
  11  13  4
 / \     / \
7   2   5   1

Target: 22

Paths: [[5, 4, 11, 2], [5, 8, 4, 5]]

Expected Output: [[5,4,11,2], [5,8,4,5]]
```

### Follow-up Problems (in the 19)
None

### Code Solution

```java
// Write your solution here

```

### Complexity Analysis
- **Time Complexity:** O(n) - visit each node, O(n) to copy path for each leaf
- **Space Complexity:** O(n) - for result and recursion stack

### MAANG Interview Questions
1. "What if path doesn't have to be root-to-leaf?" → More complex backtracking
2. "Can you return the number of paths instead?" → Yes, backtrack without storing
3. "What if paths can go in any direction?" → Use DFS from every node
4. "How would you find maximum path sum among these?" → Track max while backtracking
5. "Can you solve iteratively?" → Use stack with (node, path, sum) tuples

---

## 16. LeetCode 100 - Same Tree [60%]

### Problem Statement
Given the roots of two binary trees p and q, write a function to check if they are the same. Two binary trees are the same if they are structurally identical, and the nodes have the same value.

**Input:** Two binary tree roots p and q
**Output:** Boolean - true if same, false otherwise

### Example
```
Tree p:
  1
 / \
2   3

Tree q:
  1
 / \
2   3

Expected Output: true

Tree p:
  1
 /
2

Tree q:
  1
   \
    2

Expected Output: false
```

### Follow-up Problems (in the 19)
None

### Code Solution

```java
// Write your solution here

```

### Complexity Analysis
- **Time Complexity:** O(min(m, n)) - where m, n are sizes of trees
- **Space Complexity:** O(min(h1, h2)) - where h1, h2 are heights

### MAANG Interview Questions
1. "What makes two trees the same?" → Same structure and same values
2. "Can you do it iteratively?" → Yes, with queue for level-order
3. "What if only structure matters?" → Check structure without comparing values
4. "How would you compare trees with floating point values?" → Use epsilon for comparison
5. "Can you optimize for very large trees?" → Early termination on first mismatch

---

## 17. LeetCode 572 - Subtree of Another Tree [60%]

### Problem Statement
Given the roots of two binary trees root and subRoot, return true if there is a subtree of root with the same structure and node values of subRoot and false otherwise. A subtree of a binary tree is a tree that consists of a node in the original tree and all of this node's descendants.

**Input:** Two binary tree roots root and subRoot
**Output:** Boolean - true if subRoot is subtree of root

### Example
```
Root:
      3
     / \
    4   5
   / \
  1   2

SubRoot:
    4
   / \
  1   2

Expected Output: true (4 with children 1,2 is a subtree)

Root:
      3
     / \
    4   5
   / \
  1   2
 /
0

SubRoot:
    4
   / \
  1   2

Expected Output: false (structure doesn't match exactly)
```

### Follow-up Problems (in the 19)
None

### Code Solution

```java
// Write your solution here

```

### Complexity Analysis
- **Time Complexity:** O(m * n) - m, n are sizes of trees (check same tree for each node)
- **Space Complexity:** O(max(h1, h2)) - where h1, h2 are heights

### MAANG Interview Questions
1. "How is this different from same tree?" → Check if subRoot matches any subtree of root
2. "Can you optimize?" → Use tree hashing or serialization
3. "What if subRoot is larger than root?" → Return false immediately
4. "How would you handle very large trees?" → Use rolling hash for efficiency
5. "Can you do it with single pass?" → Yes, with hashing

---

## 18. LeetCode 987 - Vertical Order Traversal [55%]

### Problem Statement
Given the root of a binary tree, calculate the vertical order traversal of the binary tree.

**Input:** Binary tree root
**Output:** Vertical order as list of lists

### Example
```
Input Tree:
      3
     / \
    9  20
      / \
     15  7

Column assignment (column = col_index):
3 at col 0
9 at col -1, 20 at col 1
15 at col 0, 7 at col 2

Vertical Order: [[9], [3, 15], [20], [7]]

Expected Output: [[9], [3, 15], [20], [7]]
```

### Follow-up Problems (in the 19)
None

### Code Solution

```java
// Write your solution here

```

### Complexity Analysis
- **Time Complexity:** O(n log n) - sorting required for column order
- **Space Complexity:** O(n) - store all nodes

### MAANG Interview Questions
1. "What's the difference between vertical and level order?" → Vertical groups by column, not level
2. "How do you assign column values?" → Left child = col-1, right child = col+1
3. "What if multiple nodes have same column and row?" → Sort by node value
4. "Can you do it without sorting?" → Collect then sort, or use TreeMap
5. "How would you return columns in different order?" → Change sorting criteria

---

## 19. LeetCode 103 - Binary Tree Zigzag Level Order Traversal [50%]

### Problem Statement
Given the root of a binary tree, return the zigzag level order traversal of its nodes' values. (left to right for odd levels, right to left for even levels).

**Input:** Binary tree root
**Output:** Zigzag level order as list of lists

### Example
```
Input Tree:
      3
     / \
    9  20
      / \
     15  7

Level Order:
Level 0 (left→right): [3]
Level 1 (right→left): [20, 9]
Level 2 (left→right): [15, 7]

Expected Output: [[3], [20, 9], [15, 7]]
```

### Follow-up Problems (in the 19)
None

### Code Solution

```java
// Write your solution here

```

### Complexity Analysis
- **Time Complexity:** O(n) - visit each node once
- **Space Complexity:** O(w) - where w is maximum width

### MAANG Interview Questions
1. "How is this different from regular level order?" → Alternate direction per level
2. "Can you do it with recursion?" → Yes, track level parity
3. "Can you reverse in-place instead of during traversal?" → Yes, but less efficient
4. "What if you need zig but not zag?" → Just reverse even or odd levels
5. "How would you do triple-zag (3 directions)?" → Use modulo 3 for direction

---

## Summary

| # | Problem | Frequency | Difficulty | Type |
|---|---------|-----------|-----------|------|
| 1 | Validate BST | 75% | Medium | Recursion |
| 2 | LCA BST | 65% | Easy | Recursion |
| 3 | Kth Smallest | 60% | Medium | In-order |
| 4 | LCA | 85% | Medium | Recursion |
| 5 | Level Order | 80% | Medium | BFS |
| 6 | Max Path Sum | 80% | Hard | Recursion |
| 7 | Serialize | 80% | Hard | Design |
| 8 | Path Sum | 75% | Easy | Recursion |
| 9 | Invert Tree | 70% | Easy | Recursion |
| 10 | Diameter | 70% | Easy | Recursion |
| 11 | Right Side View | 70% | Medium | BFS |
| 12 | Balanced Tree | 65% | Easy | Recursion |
| 13 | Flatten Tree | 65% | Medium | Recursion |
| 14 | Construct Tree | 65% | Medium | Recursion |
| 15 | Path Sum II | 60% | Medium | Backtracking |
| 16 | Same Tree | 60% | Easy | Recursion |
| 17 | Subtree | 60% | Easy | Recursion |
| 18 | Vertical Order | 55% | Hard | BFS |
| 19 | Zigzag Order | 50% | Medium | BFS |

**Master these 19 problems → Master 98%+ of all MAANG tree interview questions**
