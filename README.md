# Java Public Records Due-Diligence Website

A lightweight Java website for **ethical public-records discovery**. It helps a user prepare searches across official/public sources for:

- Court and criminal-case research leads
- Business/corporate registration profiles
- Professional license checks
- School/academic credential verification routes
- Other public/open datasets such as sanctions/watchlist aggregators and open knowledge bases

The app intentionally **does not** scrape restricted websites, bypass access controls, collect private identifiers, or claim that a person has a criminal record. It returns source links and verification guidance so a human reviewer can confirm results lawfully.

## Quick local demo

Requires JDK 11+.

```bash
./run.sh 8080
```

Open:

```text
http://localhost:8080
```

Click **Try demo** in the UI to auto-fill a safe sample search.

Manual run:

```bash
javac src/PublicRecordsSearchServer.java
java -cp src PublicRecordsSearchServer 8080
```

## Smoke test

```bash
./ci-smoke-test.sh
```

This compiles the Java server, starts it on a temporary port, checks `/health`, loads the UI, and runs a sample `/api/search` request.

## API endpoints

- `GET /` — website UI
- `POST /api/search` — form-urlencoded search request
- `GET /api/sources` — configured public-source registry
- `GET /health` — health check

Example API request:

```bash
curl -X POST http://localhost:8080/api/search \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'name=Juan Dela Cruz' \
  --data-urlencode 'country=PH' \
  --data-urlencode 'scope=court' \
  --data-urlencode 'scope=business' \
  --data-urlencode 'scope=credentials' \
  --data-urlencode 'purpose=Consented vendor due diligence' \
  --data-urlencode 'consent=yes' \
  --data-urlencode 'notMinor=yes' \
  --data-urlencode 'fairUse=yes'
```

## Make a ZIP for GitHub upload

```bash
make zip
```

This creates:

```text
public-records-search-java-github-ready.zip
```

You can also create the ZIP manually:

```bash
rm -f src/*.class public-records-search-java-github-ready.zip
zip -r public-records-search-java-github-ready.zip . -x '.git/*' './.git/*' '*.class' 'public-records-search-java-github-ready.zip'
```

## Upload to GitHub

Option A — upload ZIP in GitHub web UI:

1. Create a new GitHub repository.
2. Upload the ZIP contents, not the ZIP file itself.
3. Commit to `main`.
4. Open the **Actions** tab. The included workflow will compile and smoke-test the app.

Option B — push with Git:

```bash
git init
git add .
git commit -m "Initial Java public records due-diligence website"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git push -u origin main
```

## Deployment options

> GitHub Pages hosts static files only, so it cannot run this Java server directly. Use GitHub for source control and CI, then deploy with Docker or a Java-capable host.

### Docker

```bash
docker build -t public-records-search .
docker run --rm -p 8080:8080 public-records-search
```

### Render

This repo includes `render.yaml` and a `Dockerfile`.

1. Push the repo to GitHub.
2. In Render, choose **New Web Service**.
3. Connect your GitHub repo.
4. Render should detect Docker automatically.
5. Health check path: `/health`.

### Railway/Fly.io/other Docker hosts

Use the included `Dockerfile`. The app reads the `PORT` environment variable, so it works with hosts that assign dynamic ports.

### GitHub Actions

The included workflow is at:

```text
.github/workflows/build.yml
```

It does three things on every push/pull request:

1. Sets up Java 11.
2. Runs `./ci-smoke-test.sh`.
3. Uploads a ready ZIP artifact.

## Project files

```text
src/PublicRecordsSearchServer.java   Java backend/server
public/index.html                    Frontend UI
run.sh                               Local run script
ci-smoke-test.sh                     Demo/smoke test script
DEMO.md                              Demo instructions and expected output
Dockerfile                           Docker deployment
render.yaml                          Render deployment config
.github/workflows/build.yml          GitHub Actions CI
SECURITY_AND_COMPLIANCE.md           Safety and compliance notes
```

## Compliance notes

Use only for lawful purposes such as self-checks, consented verification, vendor due diligence, fraud prevention, or compliance research. Do not use the tool for harassment, stalking, doxxing, discrimination, or unlawful employment/credit/tenant screening.

Important limitations:

1. A name match is not proof of identity.
2. Absence of records is not proof that no record exists.
3. Criminal/court public access varies by jurisdiction, case type, sealing/expungement, age, and portal rules.
4. School credentials usually require direct verification with the issuing school/registrar or an authorized verification service.
5. Aggregator data must be confirmed against official sources.

## Customizing sources

Edit the `SOURCES` list in:

```text
src/PublicRecordsSearchServer.java
```

Add source entries for your target country, state/province, city, professional boards, school registrars, and official business registries. Prefer official APIs or official manual portals. Avoid scraping websites that prohibit automated access.
