# AI Setup Guide for Student Project

## ✅ SOLUTION: Using OpenAI (RECOMMENDED)

**Status:** Working and tested ✓

OpenAI is the proven, working solution. Use this instead of Vertex AI.

### Quick Start

```bash
export AI_PROVIDER="openai"
export OPENAI_API_KEY="sk-your-real-key"
cd backend
mvn spring-boot:run
```

Test at http://localhost:4200 → Create student → "✨ AI Summary" button

---

# Google Vertex AI Setup Guide (Optional - For Reference)

## Prerequisites
- GCP Account with billing enabled
- gcloud CLI installed
- Service account with Vertex AI permissions

## Step 1: Set Project Variables

```bash
export GCP_PROJECT_ID="ultimate-bit-502715-k9"
export GCP_REGION="us-central1"
export SERVICE_ACCOUNT_EMAIL="your-service-account@ultimate-bit-502715-k9.iam.gserviceaccount.com"
```

## Step 2: Check Current Project & Enable APIs

```bash
# Set active project
gcloud config set project $GCP_PROJECT_ID

# Verify project
gcloud config get-value project

# Enable Vertex AI API
gcloud services enable aiplatform.googleapis.com

# Enable Cloud Resource Manager API (for authentication)
gcloud services enable cloudresourcemanager.googleapis.com

# Enable IAM API
gcloud services enable iam.googleapis.com
```

## Step 3: List Available Regions for Vertex AI

```bash
# Check which regions support Vertex AI
gcloud ai models list --project=$GCP_PROJECT_ID --filter="location:*" 2>/dev/null || echo "No models in current region"

# Try all regions with Vertex AI
for region in us-central1 us-west1 europe-west4 asia-southeast1; do
  echo "Checking $region..."
  gcloud ai models list --project=$GCP_PROJECT_ID --region=$region --format="table(name,displayName)" 2>/dev/null | head -5
done
```

## Step 4: Create/Use Service Account

```bash
# List existing service accounts
gcloud iam service-accounts list --project=$GCP_PROJECT_ID

# Or create new one
gcloud iam service-accounts create vertex-ai-sa \
  --project=$GCP_PROJECT_ID \
  --display-name="Vertex AI Service Account"

# Get the service account email
export SERVICE_ACCOUNT_EMAIL=$(gcloud iam service-accounts list \
  --project=$GCP_PROJECT_ID \
  --filter="displayName:Vertex AI Service Account" \
  --format="value(email)")

echo $SERVICE_ACCOUNT_EMAIL
```

## Step 5: Grant Vertex AI Permissions to Service Account

```bash
# Grant Vertex AI User role
gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
  --role="roles/aiplatform.user"

# Grant Service Account User role
gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
  --role="roles/iam.serviceAccountUser"

# Verify roles
gcloud projects get-iam-policy $GCP_PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
  --format="table(bindings.role)"
```

## Step 6: Create & Download Service Account Key

```bash
# Create key file (will be downloaded to current directory)
gcloud iam service-accounts keys create vertex-key.json \
  --iam-account=$SERVICE_ACCOUNT_EMAIL \
  --project=$GCP_PROJECT_ID

# Verify key file created
ls -lh vertex-key.json

# Set environment variable
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/vertex-key.json
echo $GOOGLE_APPLICATION_CREDENTIALS
```

## Step 7: Test Vertex AI API Access

```bash
# Test authentication
gcloud auth application-default print-access-token > /dev/null && echo "✓ Authentication OK" || echo "✗ Authentication FAILED"

# List available models
curl -H "Authorization: Bearer $(gcloud auth application-default print-access-token)" \
  "https://$GCP_REGION-aiplatform.googleapis.com/v1/projects/$GCP_PROJECT_ID/locations/$GCP_REGION/publishers/google/models" \
  2>/dev/null | jq '.models[].name' | head -10
```

## Step 8: Test Direct Model Call

```bash
# Test gemini-1.5-flash (most available model)
curl -X POST \
  -H "Authorization: Bearer $(gcloud auth application-default print-access-token)" \
  -H "Content-Type: application/json" \
  -d '{
    "contents": [{
      "role": "user",
      "parts": [{"text": "Say hello"}]
    }],
    "generationConfig": {
      "maxOutputTokens": 20,
      "temperature": 0.3
    }
  }' \
  "https://$GCP_REGION-aiplatform.googleapis.com/v1/projects/$GCP_PROJECT_ID/locations/$GCP_REGION/publishers/google/models/gemini-1.5-flash:generateContent"
```

## Step 9: Configure Spring Boot Application

**Update backend/src/main/resources/application.yml:**

```yaml
gcp:
  project-id: ${GCP_PROJECT_ID:ultimate-bit-502715-k9}
  location: ${GCP_LOCATION:us-central1}

ai:
  provider: ${AI_PROVIDER:vertex}
```

## Step 10: Set Environment Variables for Backend

```bash
# Navigate to backend directory
cd /Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world/backend

# Export all required variables
export GCP_PROJECT_ID="ultimate-bit-502715-k9"
export GCP_LOCATION="us-central1"
export GOOGLE_APPLICATION_CREDENTIALS="/Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world/backend/vertex-key.json"
export AI_PROVIDER="vertex"

# Verify variables are set
echo "Project: $GCP_PROJECT_ID"
echo "Location: $GCP_LOCATION"
echo "Credentials: $GOOGLE_APPLICATION_CREDENTIALS"
echo "Provider: $AI_PROVIDER"
```

## Step 11: Copy Service Account Key to Backend

```bash
# Copy the key file to backend directory
cp vertex-key.json /Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world/backend/

# Verify it's there
ls -lh /Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world/backend/vertex-key.json
```

## Step 12: Rebuild & Run Backend

```bash
cd /Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world/backend

# Clean build
mvn clean compile

# Run with Vertex AI
mvn spring-boot:run
```

## Step 13: Test in UI

1. Open http://localhost:4200
2. Create a new student
3. Click "✨ AI Summary" button
4. Check backend logs for Vertex AI API calls

## Troubleshooting

### Check if API is enabled
```bash
gcloud services list --enabled --project=$GCP_PROJECT_ID | grep aiplatform
```

### Check service account has proper roles
```bash
gcloud projects get-iam-policy $GCP_PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:serviceAccount:*" \
  --format="table(bindings.role)"
```

### Test credentials directly
```bash
python3 -c "
from google.auth import default
creds, project = default()
print(f'Authenticated: {creds is not None}')
print(f'Project: {project}')
"
```

### View Vertex AI logs
```bash
gcloud logging read "resource.type=api" \
  --project=$GCP_PROJECT_ID \
  --limit=20 \
  --format=json
```

## Available Models by Region

| Region | Models | Availability |
|--------|--------|--------------|
| us-central1 | gemini-1.5-flash, gemini-1.5-pro, gemini-2.0-flash-exp | Generally Available |
| us-west1 | gemini-1.5-flash, gemini-1.5-pro | Generally Available |
| europe-west4 | gemini-1.5-flash, gemini-1.5-pro | Generally Available |
| asia-southeast1 | gemini-1.5-flash, gemini-1.5-pro | Generally Available |

**Note:** If models return 404, try switching to `us-west1` or `europe-west4`

## Recommended Model Selection

- **Fast & Cheap:** `gemini-1.5-flash` (recommended for summaries)
- **Powerful:** `gemini-1.5-pro` (higher quality, higher cost)
- **Latest:** `gemini-2.0-flash-exp` (experimental, may not be available everywhere)

## Complete Setup Checklist

- [ ] GCP account with billing enabled
- [ ] Set project ID and region variables
- [ ] Enabled aiplatform.googleapis.com API
- [ ] Created/configured service account
- [ ] Granted Vertex AI User role to service account
- [ ] Downloaded vertex-key.json
- [ ] Tested authentication with gcloud
- [ ] Tested direct API call with curl
- [ ] Copied vertex-key.json to backend directory
- [ ] Updated application.yml with GCP config
- [ ] Set environment variables (GCP_PROJECT_ID, GCP_LOCATION, GOOGLE_APPLICATION_CREDENTIALS, AI_PROVIDER)
- [ ] Rebuilt backend with `mvn clean compile`
- [ ] Started backend with `mvn spring-boot:run`
- [ ] Tested AI Summary in UI
- [ ] Verified backend logs show successful Vertex AI calls
