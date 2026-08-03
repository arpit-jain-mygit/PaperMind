import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StudentService, Student } from './services/student.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container">
      <h1>Student Management System</h1>

      <!-- Batch Agent Section -->
      <div class="batch-section">
        <h2>🤖 Batch Create Agent</h2>
        <p class="batch-info">Paste CSV data (name, email, phone, gpa) to bulk import students. Agent validates, detects duplicates, and generates AI summaries for high performers (GPA ≥ 3.7).</p>
        <textarea
          [(ngModel)]="batchCsvData"
          placeholder="name,email,phone,gpa&#10;Alice,alice@test.com,9876543210,3.9&#10;Bob,bob@test.com,9876543211,3.1"
          rows="6"
          class="batch-textarea"
        ></textarea>
        <button
          (click)="processBatch()"
          [disabled]="isBatchProcessing || !batchCsvData.trim()"
          class="btn-batch"
        >
          {{ isBatchProcessing ? 'Processing...' : '▶ Process Batch' }}
        </button>
      </div>

      <!-- Agent Feedback Section -->
      <div class="feedback-section">
        <h2>🧠 Agent Learning Dashboard</h2>

        <!-- Analytics Card -->
        <div class="analytics-card" *ngIf="agentAnalytics">
          <div class="accuracy-display">
            <div class="accuracy-number">{{ agentAnalytics.accuracy }}</div>
            <div class="accuracy-label">Agent Accuracy</div>
          </div>
          <div class="stats-grid">
            <div class="stat">
              <div class="stat-value">{{ agentAnalytics.decisionBreakdown.successful }}</div>
              <div class="stat-label">✅ Correct</div>
            </div>
            <div class="stat">
              <div class="stat-value">{{ agentAnalytics.decisionBreakdown.failed }}</div>
              <div class="stat-label">❌ Wrong</div>
            </div>
            <div class="stat">
              <div class="stat-value">{{ agentAnalytics.decisionBreakdown.pendingFeedback }}</div>
              <div class="stat-label">⏳ Pending</div>
            </div>
          </div>
          <div class="insight">{{ agentAnalytics.insight }}</div>
        </div>

        <!-- Pending Decisions -->
        <div class="pending-decisions" *ngIf="pendingDecisions && pendingDecisions.length > 0">
          <h3>⏳ Decisions Needing Your Feedback ({{ pendingDecisions.length }})</h3>
          <div class="decision-list">
            <div *ngFor="let decision of pendingDecisions" class="decision-item">
              <div class="decision-header">
                <strong>{{ decision.studentName || decision.studentEmail }}</strong>
                <span class="decision-type">{{ decision.decisionType }}</span>
              </div>
              <div class="decision-info">
                <div class="info-row">
                  <span class="label">Email:</span>
                  <span class="value">{{ decision.studentEmail }}</span>
                </div>
                <div class="info-row">
                  <span class="label">Reason:</span>
                  <span class="value">{{ decision.decisionReason }}</span>
                </div>
              </div>
              <div class="decision-actions">
                <button
                  class="btn-feedback btn-correct"
                  (click)="provideFeedback(decision, true)"
                  [disabled]="feedbackSubmitting"
                >
                  ✅ Correct
                </button>
                <button
                  class="btn-feedback btn-wrong"
                  (click)="provideFeedback(decision, false)"
                  [disabled]="feedbackSubmitting"
                >
                  ❌ Wrong
                </button>
              </div>
            </div>
          </div>
        </div>

        <div *ngIf="!pendingDecisions || pendingDecisions.length === 0" class="no-pending">
          ✅ No pending feedback! All recent decisions have been reviewed.
        </div>

        <!-- Learning Analysis Section -->
        <div class="learning-section" *ngIf="learningAnalysis">
          <h3>🧠 AI Agent Learning Analysis</h3>
          <div class="analysis-card">
            <p class="analysis-summary">{{ learningAnalysis.summary }}</p>

            <div *ngIf="learningAnalysis.patternPercentages && (learningAnalysis.patternPercentages | keyvalue).length > 0" class="patterns-breakdown">
              <h4>Failure Patterns Detected:</h4>
              <div class="pattern-items">
                <div *ngFor="let p of getPatternItems()" class="pattern-item">
                  <span class="pattern-name">{{ p.pattern }}</span>
                  <span class="pattern-percentage">{{ p.percentage }}%</span>
                </div>
              </div>
            </div>

            <div *ngIf="learningAnalysis.recommendedRule" class="recommendation">
              <h4>💡 Claude's Recommendation:</h4>
              <div class="recommendation-text">
                {{ learningAnalysis.recommendedRule.recommendation }}
              </div>
              <div class="recommendation-pattern">
                <strong>Pattern:</strong> {{ learningAnalysis.recommendedRule.failurePattern }}
              </div>
            </div>
          </div>
        </div>

        <!-- Learned Rules Section -->
        <div class="learned-rules-section" *ngIf="learnedRules && learnedRules.length > 0">
          <h3>📚 Learned Validation Rules</h3>
          <div class="rules-list">
            <div *ngFor="let rule of learnedRules" class="rule-card" [class.active]="rule.active">
              <div class="rule-header">
                <span class="rule-pattern">{{ rule.failurePattern }}</span>
                <span class="rule-status" [class.active]="rule.active">{{ rule.active ? '✅ Active' : '⏳ Pending' }}</span>
              </div>
              <div class="rule-content">
                <p class="rule-description">{{ rule.ruleDescription }}</p>
                <p class="rule-meta">Suggested by: {{ rule.recommendedBy }}</p>
              </div>
              <div class="rule-actions">
                <button *ngIf="!rule.active" class="btn-activate" (click)="activateRule(rule.id)">
                  ✅ Activate
                </button>
                <button *ngIf="rule.active" class="btn-deactivate" (click)="deactivateRule(rule.id)">
                  ❌ Deactivate
                </button>
              </div>
            </div>
          </div>
        </div>

        <button (click)="loadFeedbackData()" class="btn-refresh">🔄 Refresh Analytics</button>
        <button (click)="analyzeLearning()" class="btn-analyze">🤖 Analyze & Learn</button>
      </div>

      <!-- Batch Results Modal -->
      <div class="modal" *ngIf="showBatchResults">
        <div class="modal-content batch-modal">
          <div class="modal-header">
            <h3>🤖 Batch Agent Results</h3>
            <button class="close-btn" (click)="closeBatchResults()">&times;</button>
          </div>
          <div class="modal-body">
            <div *ngIf="batchProcessing" class="loading">Processing with AI Agent...</div>

            <div *ngIf="!batchProcessing && batchResult">
              <!-- Summary -->
              <div class="batch-summary">
                <pre>{{ batchResult.summary }}</pre>
              </div>

              <!-- Success Records -->
              <div *ngIf="batchResult.successes.length > 0" class="batch-section-result">
                <h4>✅ Created ({{ batchResult.successes.length }})</h4>
                <ul>
                  <li *ngFor="let success of batchResult.successes">
                    <strong>{{ success.name }}</strong> (ID: {{ success.id }}) - {{ success.action }}
                  </li>
                </ul>
              </div>

              <!-- Duplicate Records -->
              <div *ngIf="batchResult.duplicates.length > 0" class="batch-section-result warning">
                <h4>⚠️ Duplicates ({{ batchResult.duplicates.length }})</h4>
                <ul>
                  <li *ngFor="let dup of batchResult.duplicates">
                    <strong>Row {{ dup.row }}:</strong> {{ dup.name }} ({{ dup.email }}) - Already exists
                  </li>
                </ul>
              </div>

              <!-- Error Records -->
              <div *ngIf="batchResult.errors.length > 0" class="batch-section-result error">
                <h4>❌ Errors ({{ batchResult.errors.length }})</h4>
                <ul>
                  <li *ngFor="let err of batchResult.errors">
                    <strong>Row {{ err.row }}:</strong> {{ err.name }} - {{ err.error }}
                  </li>
                </ul>
              </div>
            </div>

            <div *ngIf="!batchProcessing && batchError" class="error">
              {{ batchError }}
            </div>
          </div>
          <div class="modal-footer">
            <button (click)="closeBatchResults()" class="btn-close">Close</button>
          </div>
        </div>
      </div>

      <!-- Create Form -->
      <div class="form-section">
        <h2>Add New Student</h2>
        <form (ngSubmit)="createStudent()" #studentForm="ngForm">
          <div class="form-group">
            <label>Name:</label>
            <input
              type="text"
              [(ngModel)]="newStudent.name"
              name="name"
              required
              placeholder="Enter student name"
            />
          </div>

          <div class="form-group">
            <label>Email:</label>
            <input
              type="email"
              [(ngModel)]="newStudent.email"
              name="email"
              required
              placeholder="Enter student email"
            />
          </div>

          <div class="form-group">
            <label>Phone Number:</label>
            <input
              type="tel"
              [(ngModel)]="newStudent.phoneNumber"
              name="phoneNumber"
              required
              placeholder="Enter phone number"
            />
          </div>

          <div class="form-group">
            <label>GPA:</label>
            <input
              type="number"
              [(ngModel)]="newStudent.gpa"
              name="gpa"
              step="0.1"
              min="0"
              max="4"
              required
              placeholder="Enter GPA (0-4)"
            />
          </div>

          <button type="submit" [disabled]="isCreating">
            {{ isCreating ? 'Creating...' : 'Create Student' }}
          </button>
        </form>

        <div class="message success" *ngIf="successMessage">
          {{ successMessage }}
        </div>
        <div class="message error" *ngIf="errorMessage">
          {{ errorMessage }}
        </div>
      </div>

      <!-- Students List -->
      <div class="students-section">
        <h2>Students List</h2>
        <button (click)="loadStudents()">Load Students</button>

        <div class="loading" *ngIf="isLoading">Loading students...</div>

        <div *ngIf="students.length > 0" class="students-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>GPA</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let student of students">
                <td>{{ student.id }}</td>
                <td>{{ student.name }}</td>
                <td>{{ student.email }}</td>
                <td>{{ student.phoneNumber }}</td>
                <td>{{ student.gpa }}</td>
                <td>
                  <button class="btn-ai" (click)="generateAISummary(student)">✨ AI Summary</button>
                  <button class="btn-edit" (click)="editStudent(student)">Edit</button>
                  <button class="btn-delete" (click)="deleteStudent(student.id)">Delete</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div *ngIf="!isLoading && students.length === 0" class="no-data">
          No students found. Click "Load Students" to fetch.
        </div>
      </div>

      <!-- Failure Pattern Selection Modal -->
      <div class="modal" *ngIf="showFeedbackModal">
        <div class="modal-content">
          <div class="modal-header">
            <h3>Why was this decision wrong?</h3>
            <button class="close-btn" (click)="closeFeedbackModal()">&times;</button>
          </div>
          <div class="modal-body">
            <p class="modal-info">Select the failure pattern to help the agent learn:</p>

            <div class="pattern-select">
              <div *ngFor="let pattern of failurePatterns" class="pattern-option">
                <input
                  type="radio"
                  [id]="'pattern-' + pattern.id"
                  [value]="pattern.id"
                  [(ngModel)]="selectedFailurePattern"
                  name="failurePattern"
                />
                <label [for]="'pattern-' + pattern.id">{{ pattern.label }}</label>
              </div>
            </div>

            <div class="form-group">
              <label for="explanation">Additional explanation (optional):</label>
              <textarea
                id="explanation"
                [(ngModel)]="feedbackExplanation"
                placeholder="Explain why this decision was wrong..."
                rows="3"
                class="feedback-textarea"
              ></textarea>
            </div>

            <div *ngIf="errorMessage" class="error">{{ errorMessage }}</div>
          </div>
          <div class="modal-footer">
            <button (click)="closeFeedbackModal()" class="btn-close">Cancel</button>
            <button (click)="submitFeedback()" class="btn-submit" [disabled]="feedbackSubmitting">
              {{ feedbackSubmitting ? 'Submitting...' : 'Submit Feedback' }}
            </button>
          </div>
        </div>
      </div>

      <!-- AI Summary Modal -->
      <div class="modal" *ngIf="showSummaryModal">
        <div class="modal-content">
          <div class="modal-header">
            <h3>✨ AI Summary - {{ selectedStudent?.name }}</h3>
            <button class="close-btn" (click)="closeSummaryModal()">&times;</button>
          </div>
          <div class="modal-body">
            <div *ngIf="summaryLoading" class="loading">Generating AI summary...</div>
            <div *ngIf="!summaryLoading && aiSummary" class="summary-text">
              {{ aiSummary }}
            </div>
            <div *ngIf="!summaryLoading && summaryError" class="error">
              {{ summaryError }}
            </div>
          </div>
          <div class="modal-footer">
            <button (click)="closeSummaryModal()" class="btn-close">Close</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .container {
      max-width: 1000px;
      margin: 20px auto;
      padding: 20px;
      font-family: Arial, sans-serif;
    }

    h1 {
      color: #333;
      text-align: center;
    }

    h2 {
      color: #555;
      border-bottom: 2px solid #007bff;
      padding-bottom: 10px;
    }

    .form-section {
      background: #f9f9f9;
      padding: 20px;
      border-radius: 8px;
      margin-bottom: 30px;
      border: 1px solid #ddd;
    }

    form {
      display: grid;
      gap: 15px;
    }

    .form-group {
      display: flex;
      flex-direction: column;
    }

    label {
      font-weight: bold;
      margin-bottom: 5px;
      color: #333;
    }

    input {
      padding: 10px;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 14px;
    }

    input:focus {
      outline: none;
      border-color: #007bff;
      box-shadow: 0 0 5px rgba(0, 123, 255, 0.3);
    }

    button {
      padding: 12px 20px;
      font-size: 16px;
      background-color: #007bff;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      transition: background-color 0.3s;
    }

    button:hover:not(:disabled) {
      background-color: #0056b3;
    }

    button:disabled {
      background-color: #cccccc;
      cursor: not-allowed;
    }

    .btn-edit {
      background-color: #28a745;
      padding: 8px 15px;
      font-size: 14px;
      margin-right: 5px;
    }

    .btn-edit:hover {
      background-color: #218838;
    }

    .btn-delete {
      background-color: #dc3545;
      padding: 8px 15px;
      font-size: 14px;
    }

    .btn-delete:hover {
      background-color: #c82333;
    }

    .message {
      padding: 15px;
      margin-top: 15px;
      border-radius: 4px;
      text-align: center;
    }

    .success {
      background-color: #d4edda;
      color: #155724;
      border: 1px solid #c3e6cb;
    }

    .error {
      background-color: #f8d7da;
      color: #721c24;
      border: 1px solid #f5c6cb;
    }

    .students-section {
      background: #f9f9f9;
      padding: 20px;
      border-radius: 8px;
      border: 1px solid #ddd;
    }

    .loading {
      text-align: center;
      color: #666;
      padding: 20px;
    }

    .no-data {
      text-align: center;
      color: #999;
      padding: 20px;
    }

    .students-table {
      overflow-x: auto;
      margin-top: 20px;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      background: white;
    }

    thead {
      background-color: #007bff;
      color: white;
    }

    th {
      padding: 12px;
      text-align: left;
      font-weight: bold;
    }

    td {
      padding: 12px;
      border-bottom: 1px solid #ddd;
    }

    tbody tr:hover {
      background-color: #f5f5f5;
    }

    .btn-ai {
      background-color: #9c27b0;
      padding: 8px 12px;
      font-size: 13px;
      margin-right: 5px;
    }

    .btn-ai:hover {
      background-color: #7b1fa2;
    }

    .modal {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-color: rgba(0, 0, 0, 0.5);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 1000;
    }

    .modal-content {
      background: white;
      border-radius: 8px;
      max-width: 600px;
      width: 90%;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    }

    .modal-header {
      padding: 20px;
      border-bottom: 1px solid #ddd;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .modal-header h3 {
      margin: 0;
      color: #333;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 28px;
      cursor: pointer;
      color: #999;
      padding: 0;
      width: 30px;
      height: 30px;
    }

    .close-btn:hover {
      color: #333;
    }

    .modal-body {
      padding: 20px;
      max-height: 400px;
      overflow-y: auto;
    }

    .summary-text {
      font-size: 15px;
      line-height: 1.6;
      color: #333;
      background: #f9f9f9;
      padding: 15px;
      border-radius: 4px;
    }

    .modal-footer {
      padding: 20px;
      border-top: 1px solid #ddd;
      text-align: right;
    }

    .btn-close {
      background-color: #6c757d;
      padding: 10px 20px;
    }

    .btn-close:hover {
      background-color: #5a6268;
    }

    .batch-section {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      padding: 20px;
      border-radius: 8px;
      margin-bottom: 30px;
      color: white;
      border: 1px solid #764ba2;
    }

    .batch-section h2 {
      color: white;
      border-bottom-color: rgba(255, 255, 255, 0.3);
    }

    .batch-info {
      color: rgba(255, 255, 255, 0.9);
      font-size: 14px;
      margin-bottom: 15px;
    }

    .batch-textarea {
      width: 100%;
      padding: 12px;
      border: 1px solid rgba(255, 255, 255, 0.3);
      border-radius: 4px;
      font-family: 'Courier New', monospace;
      font-size: 13px;
      background: rgba(255, 255, 255, 0.1);
      color: white;
      resize: vertical;
      margin-bottom: 15px;
    }

    .batch-textarea::placeholder {
      color: rgba(255, 255, 255, 0.6);
    }

    .batch-textarea:focus {
      outline: none;
      border-color: rgba(255, 255, 255, 0.8);
      background: rgba(255, 255, 255, 0.15);
    }

    .btn-batch {
      background-color: #28a745;
      padding: 12px 24px;
      font-size: 16px;
      font-weight: bold;
      width: 100%;
    }

    .btn-batch:hover:not(:disabled) {
      background-color: #218838;
    }

    .btn-batch:disabled {
      background-color: #cccccc;
      cursor: not-allowed;
    }

    .batch-modal {
      max-width: 700px;
      max-height: 600px;
    }

    .batch-summary {
      background: #f0f0f0;
      padding: 15px;
      border-radius: 4px;
      margin-bottom: 20px;
      border-left: 4px solid #667eea;
    }

    .batch-summary pre {
      margin: 0;
      color: #333;
      font-size: 13px;
      line-height: 1.5;
    }

    .batch-section-result {
      margin-bottom: 20px;
      padding: 15px;
      border-radius: 4px;
      background: #f9f9f9;
      border-left: 4px solid #28a745;
    }

    .batch-section-result.warning {
      background: #fff3cd;
      border-left-color: #ffc107;
    }

    .batch-section-result.error {
      background: #f8d7da;
      border-left-color: #dc3545;
    }

    .batch-section-result h4 {
      margin: 0 0 10px 0;
      color: #333;
      font-size: 14px;
    }

    .batch-section-result ul {
      list-style: none;
      padding: 0;
      margin: 0;
    }

    .batch-section-result li {
      padding: 8px 0;
      font-size: 13px;
      color: #333;
      border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    }

    .batch-section-result li:last-child {
      border-bottom: none;
    }

    .feedback-section {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      padding: 20px;
      border-radius: 8px;
      margin-bottom: 30px;
      color: white;
      border: 1px solid #764ba2;
    }

    .feedback-section h2 {
      color: white;
      border-bottom-color: rgba(255, 255, 255, 0.3);
      margin-top: 0;
    }

    .feedback-section h3 {
      color: white;
      font-size: 16px;
      margin-bottom: 15px;
    }

    .analytics-card {
      background: rgba(255, 255, 255, 0.15);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 8px;
      padding: 20px;
      margin-bottom: 20px;
    }

    .accuracy-display {
      text-align: center;
      margin-bottom: 20px;
    }

    .accuracy-number {
      font-size: 48px;
      font-weight: bold;
      color: #4ade80;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    }

    .accuracy-label {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.8);
      margin-top: 5px;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 15px;
      margin-bottom: 15px;
    }

    .stat {
      background: rgba(255, 255, 255, 0.1);
      border-radius: 6px;
      padding: 15px;
      text-align: center;
    }

    .stat-value {
      font-size: 24px;
      font-weight: bold;
      color: #fbbf24;
    }

    .stat-label {
      font-size: 12px;
      color: rgba(255, 255, 255, 0.7);
      margin-top: 5px;
    }

    .insight {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.9);
      text-align: center;
      padding: 10px;
      background: rgba(255, 255, 255, 0.05);
      border-radius: 4px;
    }

    .pending-decisions {
      margin-bottom: 20px;
    }

    .decision-list {
      display: grid;
      gap: 15px;
    }

    .decision-item {
      background: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 6px;
      padding: 15px;
      backdrop-filter: blur(10px);
    }

    .decision-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
      font-size: 15px;
      color: white;
    }

    .decision-type {
      background: rgba(255, 255, 255, 0.2);
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: bold;
    }

    .decision-info {
      margin-bottom: 12px;
      font-size: 13px;
    }

    .info-row {
      display: flex;
      gap: 10px;
      margin-bottom: 5px;
      color: rgba(255, 255, 255, 0.8);
    }

    .info-row .label {
      font-weight: bold;
      min-width: 60px;
    }

    .info-row .value {
      word-break: break-all;
    }

    .decision-actions {
      display: flex;
      gap: 10px;
    }

    .btn-feedback {
      flex: 1;
      padding: 10px 15px;
      border: none;
      border-radius: 4px;
      font-weight: bold;
      cursor: pointer;
      font-size: 13px;
      transition: all 0.3s;
    }

    .btn-correct {
      background-color: #4ade80;
      color: #1a3a1a;
    }

    .btn-correct:hover:not(:disabled) {
      background-color: #22c55e;
      transform: translateY(-2px);
      box-shadow: 0 4px 8px rgba(74, 222, 128, 0.3);
    }

    .btn-wrong {
      background-color: #f87171;
      color: #3a1a1a;
    }

    .btn-wrong:hover:not(:disabled) {
      background-color: #ef4444;
      transform: translateY(-2px);
      box-shadow: 0 4px 8px rgba(248, 113, 113, 0.3);
    }

    .btn-feedback:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .no-pending {
      background: rgba(74, 222, 128, 0.2);
      border: 1px solid rgba(74, 222, 128, 0.4);
      border-radius: 6px;
      padding: 15px;
      text-align: center;
      color: #c8e6c9;
      margin-bottom: 15px;
    }

    .btn-refresh {
      width: 100%;
      background-color: rgba(255, 255, 255, 0.2);
      color: white;
      border: 1px solid rgba(255, 255, 255, 0.3);
      padding: 12px;
      border-radius: 4px;
      font-weight: bold;
      cursor: pointer;
      transition: all 0.3s;
    }

    .btn-refresh:hover {
      background-color: rgba(255, 255, 255, 0.3);
      border-color: rgba(255, 255, 255, 0.5);
    }

    .btn-analyze {
      background-color: #8b5cf6;
      margin-top: 10px;
    }

    .btn-analyze:hover:not(:disabled) {
      background-color: #7c3aed;
    }

    .learning-section {
      background: linear-gradient(135deg, rgba(139, 92, 246, 0.1) 0%, rgba(59, 130, 246, 0.1) 100%);
      border: 1px solid rgba(139, 92, 246, 0.3);
      border-radius: 8px;
      padding: 20px;
      margin-bottom: 20px;
      backdrop-filter: blur(10px);
    }

    .learning-section h3 {
      color: #c4b5fd;
      margin-top: 0;
      margin-bottom: 15px;
    }

    .learning-section h4 {
      color: #ddd6fe;
      font-size: 14px;
      margin-top: 15px;
      margin-bottom: 10px;
    }

    .analysis-card {
      background: rgba(255, 255, 255, 0.05);
      border-radius: 6px;
      padding: 15px;
      border: 1px solid rgba(139, 92, 246, 0.2);
    }

    .analysis-summary {
      color: #e9d5ff;
      font-size: 14px;
      margin: 0 0 15px 0;
      line-height: 1.5;
    }

    .patterns-breakdown {
      margin-bottom: 15px;
    }

    .pattern-items {
      display: grid;
      gap: 8px;
    }

    .pattern-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: rgba(139, 92, 246, 0.1);
      padding: 10px 12px;
      border-radius: 4px;
      border-left: 3px solid #8b5cf6;
    }

    .pattern-name {
      color: #c4b5fd;
      font-weight: 500;
      font-size: 13px;
    }

    .pattern-percentage {
      background: rgba(139, 92, 246, 0.3);
      color: #f3e8ff;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: bold;
    }

    .recommendation {
      background: rgba(34, 197, 94, 0.1);
      border-left: 3px solid #22c55e;
      padding: 12px;
      border-radius: 4px;
      margin-top: 15px;
    }

    .recommendation-text {
      color: #d1fae5;
      font-size: 13px;
      line-height: 1.5;
      margin-bottom: 8px;
      font-style: italic;
    }

    .recommendation-pattern {
      color: #a7f3d0;
      font-size: 12px;
    }

    .pattern-select {
      display: grid;
      gap: 10px;
      margin-bottom: 20px;
      background: rgba(255, 255, 255, 0.05);
      padding: 15px;
      border-radius: 4px;
    }

    .pattern-option {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .pattern-option input[type="radio"] {
      cursor: pointer;
      width: 18px;
      height: 18px;
    }

    .pattern-option label {
      cursor: pointer;
      color: #e5e7eb;
      font-weight: normal;
      margin: 0;
    }

    .pattern-option input[type="radio"]:checked + label {
      color: #60a5fa;
      font-weight: bold;
    }

    .modal-info {
      color: #d1d5db;
      font-size: 14px;
      margin-bottom: 15px;
    }

    .feedback-textarea {
      width: 100%;
      padding: 10px;
      border: 1px solid #555;
      border-radius: 4px;
      background: rgba(255, 255, 255, 0.05);
      color: #e5e7eb;
      font-family: Arial, sans-serif;
      resize: vertical;
    }

    .feedback-textarea::placeholder {
      color: rgba(255, 255, 255, 0.5);
    }

    .feedback-textarea:focus {
      outline: none;
      border-color: #60a5fa;
      box-shadow: 0 0 5px rgba(96, 165, 250, 0.3);
    }

    .btn-submit {
      background-color: #10b981;
      padding: 10px 20px;
    }

    .btn-submit:hover:not(:disabled) {
      background-color: #059669;
    }

    .learned-rules-section {
      background: linear-gradient(135deg, rgba(34, 197, 94, 0.1) 0%, rgba(16, 185, 129, 0.1) 100%);
      border: 1px solid rgba(34, 197, 94, 0.3);
      border-radius: 8px;
      padding: 20px;
      margin-bottom: 20px;
      backdrop-filter: blur(10px);
    }

    .learned-rules-section h3 {
      color: #a7f3d0;
      margin-top: 0;
      margin-bottom: 15px;
    }

    .rules-list {
      display: grid;
      gap: 12px;
    }

    .rule-card {
      background: rgba(255, 255, 255, 0.05);
      border-left: 4px solid #fbbf24;
      border-radius: 6px;
      padding: 12px 15px;
      transition: all 0.3s;
    }

    .rule-card.active {
      border-left-color: #22c55e;
      background: rgba(34, 197, 94, 0.1);
    }

    .rule-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
    }

    .rule-pattern {
      font-weight: bold;
      color: #f3e8ff;
      font-size: 14px;
      background: rgba(139, 92, 246, 0.2);
      padding: 2px 8px;
      border-radius: 4px;
    }

    .rule-status {
      font-size: 12px;
      background: rgba(251, 191, 36, 0.2);
      color: #fbbf24;
      padding: 2px 8px;
      border-radius: 4px;
    }

    .rule-status.active {
      background: rgba(34, 197, 94, 0.2);
      color: #22c55e;
    }

    .rule-content {
      margin-bottom: 10px;
    }

    .rule-description {
      color: #d1d5db;
      font-size: 13px;
      margin: 0 0 4px 0;
      line-height: 1.4;
    }

    .rule-meta {
      color: #9ca3af;
      font-size: 11px;
      margin: 0;
    }

    .rule-actions {
      display: flex;
      gap: 8px;
    }

    .btn-activate, .btn-deactivate {
      padding: 6px 12px;
      font-size: 12px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-weight: bold;
      transition: all 0.2s;
    }

    .btn-activate {
      background-color: #22c55e;
      color: white;
    }

    .btn-activate:hover {
      background-color: #16a34a;
      transform: scale(1.05);
    }

    .btn-deactivate {
      background-color: #ef4444;
      color: white;
    }

    .btn-deactivate:hover {
      background-color: #dc2626;
      transform: scale(1.05);
    }

  `]
})
export class AppComponent implements OnInit {
  students: Student[] = [];
  newStudent: Student = {
    name: '',
    email: '',
    phoneNumber: '',
    gpa: 0
  };

  isLoading = false;
  isCreating = false;
  successMessage = '';
  errorMessage = '';

  showSummaryModal = false;
  selectedStudent: Student | null = null;
  aiSummary = '';
  summaryLoading = false;
  summaryError = '';

  batchCsvData = '';
  showBatchResults = false;
  isBatchProcessing = false;
  batchProcessing = false;
  batchResult: any = null;
  batchError = '';

  agentAnalytics: any = null;
  pendingDecisions: any[] = [];
  feedbackSubmitting = false;
  learningAnalysis: any = null;
  learnedRules: any[] = [];

  showFeedbackModal = false;
  selectedFeedbackDecision: any = null;
  selectedFailurePattern: string = '';
  feedbackExplanation: string = '';

  failurePatterns = [
    { id: 'missing_at_symbol', label: 'Email missing @ symbol' },
    { id: 'invalid_gpa', label: 'GPA out of range' },
    { id: 'empty_field', label: 'Required field empty' },
    { id: 'duplicate_email', label: 'Email already exists' },
    { id: 'invalid_format', label: 'Invalid data format' },
    { id: 'other', label: 'Other reason' }
  ];

  constructor(private studentService: StudentService) {}

  ngOnInit() {
    this.loadStudents();
    this.loadFeedbackData();
    this.loadLearnedRules();
  }

  loadFeedbackData() {
    this.studentService.getAgentAnalytics().subscribe({
      next: (analytics) => {
        this.agentAnalytics = analytics;
        this.loadPendingDecisions();
      },
      error: (err) => {
        console.error('Failed to load analytics', err);
      }
    });
  }

  loadPendingDecisions() {
    this.studentService.getPendingDecisions().subscribe({
      next: (response) => {
        this.pendingDecisions = response.pendingDecisions || [];
        console.log('Pending decisions loaded:', this.pendingDecisions.length);
      },
      error: (err) => {
        console.error('Failed to load pending decisions', err);
        this.pendingDecisions = [];
      }
    });
  }

  provideFeedback(decision: any, successful: boolean) {
    if (successful) {
      // Mark as correct
      this.feedbackSubmitting = true;
      this.studentService.provideFeedback(decision.id, true, 'Decision was correct').subscribe({
        next: () => {
          this.feedbackSubmitting = false;
          this.successMessage = 'Feedback recorded! Agent will learn from this.';
          setTimeout(() => this.loadFeedbackData(), 500);
        },
        error: (err) => {
          this.feedbackSubmitting = false;
          this.errorMessage = 'Failed to record feedback. Please try again.';
          console.error(err);
        }
      });
    } else {
      // Mark as wrong - show failure pattern selection
      this.selectedFeedbackDecision = decision;
      this.showFeedbackModal = true;
      this.selectedFailurePattern = '';
      this.feedbackExplanation = '';
    }
  }

  submitFeedback() {
    if (!this.selectedFailurePattern) {
      this.errorMessage = 'Please select a failure pattern';
      return;
    }

    this.feedbackSubmitting = true;
    this.studentService.provideFeedback(
      this.selectedFeedbackDecision.id,
      false,
      this.feedbackExplanation,
      this.selectedFailurePattern
    ).subscribe({
      next: () => {
        this.feedbackSubmitting = false;
        this.successMessage = 'Feedback recorded with pattern! Agent will analyze and learn.';
        this.showFeedbackModal = false;
        this.selectedFeedbackDecision = null;
        setTimeout(() => this.loadFeedbackData(), 500);
      },
      error: (err) => {
        this.feedbackSubmitting = false;
        this.errorMessage = 'Failed to record feedback. Please try again.';
        console.error(err);
      }
    });
  }

  closeFeedbackModal() {
    this.showFeedbackModal = false;
    this.selectedFeedbackDecision = null;
    this.selectedFailurePattern = '';
    this.feedbackExplanation = '';
  }

  analyzeLearning() {
    this.studentService.analyzeLearning().subscribe({
      next: (analysis) => {
        this.learningAnalysis = analysis;
        this.successMessage = 'Learning analysis complete! OpenAI recommendations are ready.';
        setTimeout(() => this.loadLearnedRules(), 500);
      },
      error: (err) => {
        this.errorMessage = 'Failed to analyze learning patterns. Please try again.';
        console.error(err);
      }
    });
  }

  loadLearnedRules() {
    this.studentService.getAllRules().subscribe({
      next: (response) => {
        this.learnedRules = response.allRules || [];
        console.log('Learned rules loaded:', this.learnedRules.length);
      },
      error: (err) => {
        console.error('Failed to load rules', err);
      }
    });
  }

  activateRule(ruleId: number) {
    this.studentService.activateRule(ruleId).subscribe({
      next: () => {
        this.successMessage = 'Rule activated! Agent will use this rule for next batch.';
        this.loadLearnedRules();
      },
      error: (err) => {
        this.errorMessage = 'Failed to activate rule';
        console.error(err);
      }
    });
  }

  deactivateRule(ruleId: number) {
    this.studentService.deactivateRule(ruleId).subscribe({
      next: () => {
        this.successMessage = 'Rule deactivated.';
        this.loadLearnedRules();
      },
      error: (err) => {
        this.errorMessage = 'Failed to deactivate rule';
        console.error(err);
      }
    });
  }

  getPatternItems() {
    if (!this.learningAnalysis?.patternPercentages) {
      return [];
    }
    return Object.entries(this.learningAnalysis.patternPercentages).map(([pattern, percentage]) => ({
      pattern,
      percentage: (percentage as number).toFixed(1)
    }));
  }

  createStudent() {
    if (!this.isValidStudent(this.newStudent)) {
      this.errorMessage = 'Please fill all fields correctly';
      return;
    }

    this.isCreating = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.studentService.createStudent(this.newStudent)
      .subscribe({
        next: (student) => {
          this.students.push(student);
          this.successMessage = `Student "${student.name}" created successfully!`;
          this.resetForm();
          this.isCreating = false;
        },
        error: (err) => {
          this.errorMessage = 'Failed to create student. Please try again.';
          this.isCreating = false;
          console.error(err);
        }
      });
  }

  loadStudents() {
    this.isLoading = true;
    this.errorMessage = '';

    this.studentService.getAllStudents()
      .subscribe({
        next: (data) => {
          this.students = data;
          this.isLoading = false;
        },
        error: (err) => {
          this.errorMessage = 'Failed to load students';
          this.isLoading = false;
          console.error(err);
        }
      });
  }

  editStudent(student: Student) {
    this.errorMessage = 'Edit functionality coming soon...';
  }

  deleteStudent(id: number | undefined) {
    if (!id || !confirm('Are you sure you want to delete this student?')) return;

    this.studentService.deleteStudent(id)
      .subscribe({
        next: () => {
          this.students = this.students.filter(s => s.id !== id);
          this.successMessage = 'Student deleted successfully!';
        },
        error: (err) => {
          this.errorMessage = 'Failed to delete student';
          console.error(err);
        }
      });
  }

  private isValidStudent(student: Student): boolean {
    return student.name?.trim().length > 0 &&
           student.email?.trim().length > 0 &&
           student.phoneNumber?.trim().length > 0 &&
           student.gpa >= 0 && student.gpa <= 4;
  }

  generateAISummary(student: Student) {
    this.selectedStudent = student;
    this.showSummaryModal = true;
    this.summaryLoading = true;
    this.aiSummary = '';
    this.summaryError = '';

    this.studentService.getStudentSummary(student.id!)
      .subscribe({
        next: (summary) => {
          this.aiSummary = summary;
          this.summaryLoading = false;
        },
        error: (err) => {
          this.summaryError = 'Failed to generate AI summary. Please check your OpenAI API key.';
          this.summaryLoading = false;
          console.error(err);
        }
      });
  }

  closeSummaryModal() {
    this.showSummaryModal = false;
    this.selectedStudent = null;
    this.aiSummary = '';
    this.summaryError = '';
  }

  processBatch() {
    if (!this.batchCsvData.trim()) {
      this.batchError = 'Please enter CSV data';
      return;
    }

    const students = this.parseCsvData(this.batchCsvData);
    if (students.length === 0) {
      this.batchError = 'No valid student data found. Use format: name,email,phone,gpa';
      return;
    }

    this.showBatchResults = true;
    this.isBatchProcessing = true;
    this.batchProcessing = true;
    this.batchError = '';
    this.batchResult = null;

    this.studentService.processBatch(students)
      .subscribe({
        next: (response: any) => {
          this.batchResult = response;
          this.batchProcessing = false;
          this.isBatchProcessing = false;
          this.loadStudents(); // Refresh student list
        },
        error: (err) => {
          this.batchError = 'Failed to process batch. Please check your data and try again.';
          this.batchProcessing = false;
          this.isBatchProcessing = false;
          console.error(err);
        }
      });
  }

  private parseCsvData(csv: string): any[] {
    const lines = csv.trim().split('\n');
    const students = [];

    for (const line of lines) {
      const parts = line.split(',').map(p => p.trim());
      if (parts.length === 4) {
        const [name, email, phone, gpa] = parts;
        if (name && email && phone && gpa) {
          students.push({
            name: name,
            email: email,
            phoneNumber: phone,
            gpa: parseFloat(gpa)
          });
        }
      }
    }

    return students;
  }

  closeBatchResults() {
    this.showBatchResults = false;
    this.batchResult = null;
    this.batchError = '';
  }

  private resetForm() {
    this.newStudent = {
      name: '',
      email: '',
      phoneNumber: '',
      gpa: 0
    };
  }
}
