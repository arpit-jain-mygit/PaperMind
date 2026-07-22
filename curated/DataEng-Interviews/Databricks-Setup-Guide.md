# Databricks Free Tier Setup

## 1. Create Account & Workspace

1. Go to https://databricks.com/try-databricks
2. Sign up (email, password)
3. Select **Community Edition** (free)
4. Create workspace (takes ~2 min)

---

## 2. Launch Workspace

1. Once live, click "Launch Workspace"
2. You're in the Databricks UI

---

## 3. Create a Cluster

1. Left sidebar → **Compute**
2. Click **Create Cluster**
3. Name: `learning-cluster`
4. Runtime: Latest (e.g., `15.4 LTS`)
5. Worker Type: `i3.xlarge` (or whatever's available)
6. Workers: `1` (free tier)
7. Click **Create**

*Wait 1-2 minutes for it to start*

---

## 4. Import a Notebook

1. Left sidebar → **Workspace**
2. Click your username folder
3. Right-click → **Import**
4. Paste the notebook code from the markdown files
5. Or upload as `.py` file and Databricks converts it

**Alternative: Create from scratch**
1. Click **Create** → **Notebook**
2. Name: `Spark-Basics`
3. Language: **Python**
4. Cluster: Select `learning-cluster`
5. Copy-paste code cells

---

## 5. Run Code

1. Click **Attach** → select `learning-cluster`
2. Type code in cells
3. Press `Shift+Enter` to run cell
4. Results show below

---

## 6. Watch the Spark UI

During a query:
1. In the notebook output, click **Spark UI**
2. Look at **Stages** tab — see your query's DAG
3. Look at **Tasks** tab — see individual task times
4. Look at **Executors** tab — see memory usage

---

## 7. Key Databricks Shortcuts

| Action | Shortcut |
|--------|----------|
| Run cell | `Shift + Enter` |
| Add cell below | `B` |
| Delete cell | `D + D` |
| View execution history | Top right icon |
| Attach to cluster | Click **Attach** |

---

## 8. File Storage

- **DBFS** (Databricks File System) — `dbfs:/path`
  - Use for saving data, models
  - Example: `df.write.parquet("dbfs:/user/output")`

- **Tables** (Delta format) — use `.table("name")`
  - Example: `df.write.mode("overwrite").saveAsTable("my_table")`

---

## 9. Helpful Databricks Features

**Display Results Beautifully**
```python
display(df)  # Much better than .show()
```

**Magic Commands**
```python
%sql
SELECT * FROM my_table LIMIT 10
```

**Markdown Cells**
```
%md
# My Heading
This explains my analysis
```

---

## Tips

- **First notebook slow?** Cluster takes time to initialize. Subsequent cells run fast.
- **Run code multiple times?** Cluster caches libraries after first run.
- **Out of memory?** Cluster is single-worker free tier. Adjust data size in notebooks.
- **Need data?** Databricks includes sample datasets: `/databricks-datasets/`

---

## Next Steps

1. Create cluster
2. Import Notebook 1 (Basics)
3. Run each cell, see output
4. Move to Notebook 2 (Performance), watch Spark UI
5. Move to Notebook 3 (Streaming)

