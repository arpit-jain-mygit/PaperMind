# Vidyavahak 🎓

**Knowledge Carrier** — An AI-driven YouTube transcript ingestion pipeline for Jain content

Vidyavahak automatically discovers new videos on Jain YouTube channels and downloads their transcripts, organizing them for easy access and downstream processing.

[![GitHub](https://img.shields.io/badge/GitHub-arpit--jain--mygit%2FVidyavahak-blue?logo=github)](https://github.com/arpit-jain-mygit/Vidyavahak)

## 🚀 Quick Start

```bash
cd youtube-ingestion
pip install -r requirements.txt

cp .env.example .env
# Edit .env: add YOUTUBE_API_KEY

# Edit config.py: add your Jain YouTube channel IDs

python scheduler.py
```

See [youtube-ingestion/QUICKSTART.md](youtube-ingestion/QUICKSTART.md) for detailed setup.

## 📋 What It Does

**Morning & Evening (6 AM & 6 PM IST)**
- Monitor Agent checks your configured Jain YouTube channels
- Discovers new videos
- Creates processing jobs

**Continuous (Every 5 minutes)**
- Process Agent downloads transcripts
- Saves organized by channel: `transcripts/channel_id/video_id.json|txt`
- Updates processing status

## 📁 Project Structure

```
youtube-ingestion/
├── Core Agents
│   ├── monitor_agent.py      # Discovers new videos
│   ├── process_agent.py      # Downloads transcripts
│   ├── scheduler.py          # Orchestrates timing
│   └── database.py           # SQLite job queue
│
├── Configuration
│   ├── config.py             # Channels & schedule
│   ├── .env.example          # Environment template
│   └── requirements.txt       # Dependencies
│
├── CLI & Deployment
│   ├── cli.py                # Management interface
│   ├── Dockerfile            # Container image
│   └── docker-compose.yml    # Local deployment
│
└── Documentation
    ├── README.md             # Complete guide
    ├── QUICKSTART.md         # 5-minute setup
    ├── ARCHITECTURE.md       # System design
    ├── DEPLOYMENT.md         # Cloud options
    ├── SETUP_CHECKLIST.md    # Validation steps
    └── PROJECT_SUMMARY.md    # Overview
```

## 🎯 Features

✅ **Automatic Discovery** — Monitors multiple YouTube channels on schedule  
✅ **Transcript Download** — Uses YouTube's official API and transcript service  
✅ **Job Queue** — SQLite-based, prevents duplicate processing  
✅ **Organized Storage** — Transcripts saved by channel with metadata  
✅ **CLI Management** — Monitor progress and manage pipeline  
✅ **Production Ready** — Error handling, logging, Docker support  
✅ **Fully Documented** — 6 documentation files for every use case  

## 🔧 Configuration

### Add Your Channels

Edit `youtube-ingestion/config.py`:

```python
CHANNELS: List[ChannelConfig] = [
    ChannelConfig(
        name="Channel Name",
        channel_id="UCxxxxxxxxxxxxxx",
        description="Description"
    ),
]
```

### Adjust Schedule

```python
MORNING_CHECK_TIME = time(6, 0)    # 6 AM IST
EVENING_CHECK_TIME = time(18, 0)   # 6 PM IST
```

## 📊 Monitoring

```bash
# View statistics
python youtube-ingestion/cli.py stats

# List recent jobs
python youtube-ingestion/cli.py jobs

# Check configuration
python youtube-ingestion/cli.py config
```

## 🐳 Docker Deployment

```bash
cd youtube-ingestion

# Local development
docker-compose up -d
docker-compose logs -f

# Stop
docker-compose down
```

## ☁️ Cloud Deployment

See [youtube-ingestion/DEPLOYMENT.md](youtube-ingestion/DEPLOYMENT.md) for:
- AWS Lambda + SQS
- Google Cloud Functions + Pub/Sub
- Kubernetes

## 📚 Documentation

| Document | Purpose |
|----------|---------|
| [QUICKSTART.md](youtube-ingestion/QUICKSTART.md) | 5-minute setup guide |
| [README.md](youtube-ingestion/README.md) | Complete documentation |
| [SETUP_CHECKLIST.md](youtube-ingestion/SETUP_CHECKLIST.md) | Step-by-step validation |
| [ARCHITECTURE.md](youtube-ingestion/ARCHITECTURE.md) | System design & diagrams |
| [DEPLOYMENT.md](youtube-ingestion/DEPLOYMENT.md) | Cloud deployment options |
| [PROJECT_SUMMARY.md](youtube-ingestion/PROJECT_SUMMARY.md) | Project overview |

## 🔌 Integration

### With Your API

```python
# POST transcripts to your service
requests.post(
    'https://api.example.com/transcripts',
    json={
        'video_id': video_id,
        'channel_id': channel_id,
        'transcript_path': transcript_path
    }
)
```

### With Webhooks

```python
# Notify external systems
requests.post(
    webhook_url,
    json={'event': 'transcript_ready', 'video_id': video_id}
)
```

## 🛠 Technology Stack

- **Language**: Python 3.8+
- **Scheduling**: APScheduler
- **Database**: SQLite (upgradeable to PostgreSQL)
- **YouTube API**: google-api-python-client
- **Transcripts**: youtube-transcript-api
- **CLI**: Click
- **Deployment**: Docker, Kubernetes-ready

## 📝 Output Format

Transcripts are saved in organized structure:

```
transcripts/
├── UCxxxxxx_channel_1/
│   ├── video_1.json        # Full transcript + metadata
│   ├── video_1.txt         # Plain text
│   └── ...
└── UCyyyyyy_channel_2/
    └── ...
```

## 🚨 Troubleshooting

**API Key Error** → Check `.env` has `YOUTUBE_API_KEY` set  
**No Videos Found** → Verify channel IDs are correct  
**Transcript Failed** → Some videos don't have transcripts on YouTube  

See [QUICKSTART.md](youtube-ingestion/QUICKSTART.md) troubleshooting section for more.

## 📄 License

This project is available for educational and personal use.

## 🙏 About Vidyavahak

**Vidyavahak** (विद्यावाहक) — "Knowledge Carrier" in Sanskrit

The name reflects the purpose of preserving and distributing spiritual knowledge from Jain content creators.

---

**Ready to get started?** → See [youtube-ingestion/QUICKSTART.md](youtube-ingestion/QUICKSTART.md)
