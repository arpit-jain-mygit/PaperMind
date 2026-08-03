# Docker Setup for Mac - Complete Guide

## Prerequisites
- Mac with Apple Silicon (M1/M2/M3) or Intel processor
- 2GB RAM free
- 5GB disk space free

---

## Step 1: Download Docker Desktop

### Option A: Direct Download (Easiest)
1. Go to: https://www.docker.com/products/docker-desktop
2. Click **"Download for Mac"**
3. Choose correct version:
   - **Apple Silicon (M1/M2/M3)**: Download `Docker.dmg for Apple Silicon`
   - **Intel Mac**: Download `Docker.dmg for Intel Chip`

**Not sure your Mac type?**
```bash
uname -m
# Result: arm64 = Apple Silicon (M1/M2/M3)
# Result: x86_64 = Intel
```

### Option B: Homebrew
```bash
brew install docker
brew install docker-compose
```

---

## Step 2: Install Docker

### From .dmg File (Recommended)
1. Wait for download to complete (~800MB)
2. Open **Downloads** folder
3. Double-click **Docker.dmg**
4. Drag **Docker.app** icon to **Applications** folder
5. Wait for copy to complete (takes ~2 minutes)

---

## Step 3: Launch Docker

### First Time Launch
1. Open **Applications** folder (Command+Shift+A)
2. Find **Docker.app**
3. Double-click to launch
4. Enter your **Mac password** when prompted
   - Docker needs elevated permissions
5. Wait for Docker daemon to start

**You'll see:**
- Docker icon appears in menu bar (top-right)
- Status changes from "Docker is starting..." to "Docker is running"
- Takes ~30 seconds to fully start

---

## Step 4: Verify Installation

Open Terminal and run:

```bash
docker --version
```

**Expected output:**
```
Docker version 24.0.5, build ced0996
```

If you see this, Docker is installed! ✅

---

## Step 5: Verify Docker Compose

```bash
docker compose version
```

**Expected output:**
```
Docker Compose version v2.20.2
```

If you see this, Docker Compose is ready! ✅

---

## Troubleshooting

### "command not found: docker"
**Problem:** Docker not in PATH

**Solution:**
```bash
# Restart Terminal/iTerm completely
# Close all Terminal windows and open a new one

# Or add Docker to PATH:
export PATH="$PATH:/Applications/Docker.app/Contents/Resources/bin"
echo 'export PATH="$PATH:/Applications/Docker.app/Contents/Resources/bin"' >> ~/.zshrc
source ~/.zshrc
```

### "Cannot connect to Docker daemon"
**Problem:** Docker daemon not running

**Solution:**
1. Check menu bar (top-right corner)
2. Look for Docker icon
3. If not there, open Applications → Docker.app
4. Wait 30 seconds for daemon to start
5. Try command again

### "Permission denied"
**Problem:** Need elevated permissions

**Solution:**
- Docker will ask for Mac password on first launch
- Enter your password
- Docker runs in privileged mode after that

### Docker icon shows error
**Problem:** Docker failed to start

**Solution:**
1. Quit Docker (Command+Q)
2. Restart Mac
3. Launch Docker again

---

## System Check

After installing, verify everything:

```bash
# Check Docker version
docker --version

# Check Docker Compose version
docker compose version

# Try running a test container
docker run hello-world
```

If all three work without errors, Docker is ready! ✅

---

## Storage Location

Docker images and containers stored in:
```
~/Library/Containers/com.docker.docker
```

This uses your Mac's storage, so keep it in mind if disk space is low.

---

## Enable Docker at Login

**Automatic start on Mac boot:**
1. Open Docker
2. Click Docker icon in menu bar
3. Click **Preferences**
4. Go to **General** tab
5. Check: "Start Docker Desktop when you log in"

---

## Next Steps

Once Docker is installed and running:

```bash
cd /Users/arpit/Documents/claude/transcription-workspace/fullstack-hello-world

# Start Kafka
docker compose up -d

# Verify Kafka is running
docker compose ps
```

---

## Quick Reference

| Action | Command |
|--------|---------|
| Check Docker version | `docker --version` |
| Check Compose version | `docker compose version` |
| Start Kafka | `docker compose up -d` |
| Check status | `docker compose ps` |
| View logs | `docker compose logs -f` |
| Stop Kafka | `docker compose down` |
| Test Docker | `docker run hello-world` |

---

## System Requirements

- **Minimum RAM:** 2GB (recommended 4GB+)
- **Disk Space:** 5GB free minimum
- **macOS Version:** 11 (Big Sur) or newer
- **Processor:** Any (Intel or Apple Silicon)

---

## Get Docker Info

After installation, view details:

```bash
docker info
```

Shows:
- Docker version
- Container count
- Image count
- Storage driver
- Runtime
- OS info

---

## Uninstall (If Needed)

```bash
# Quit Docker
# Remove from Applications
rm -rf /Applications/Docker.app

# Optional: Remove Docker data
rm -rf ~/Library/Containers/com.docker.docker
rm -rf ~/.docker
```

---

## Support

- **Docker Docs:** https://docs.docker.com
- **Docker Desktop for Mac:** https://docs.docker.com/desktop/install/mac-install/
- **Troubleshooting:** https://docs.docker.com/desktop/troubleshoot/

---

## Summary

1. ✅ Download Docker Desktop for Mac
2. ✅ Install from .dmg file
3. ✅ Launch Docker.app
4. ✅ Verify with `docker --version`
5. ✅ You're ready to use Kafka!

**Time needed:** ~10 minutes (5 min download + 5 min install)
