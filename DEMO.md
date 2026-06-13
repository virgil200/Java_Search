# Demo

## Browser demo

1. Run the app:

   ```bash
   ./run.sh 8080
   ```

2. Open:

   ```text
   http://localhost:8080
   ```

3. Click **Try demo**.

The demo fills in a sample Philippines public-source due-diligence search and returns source cards such as:

- Philippine Supreme Court Jurisprudence
- Supreme Court E-Library
- LawPhil Project
- Philippines SEC eFAST Public Search
- DTI Business Name Registration Search
- PRC License Verification
- CHED resources
- Global public-source aggregators where selected

## Command-line demo

```bash
./ci-smoke-test.sh
```

Expected result:

```text
Smoke test passed on http://localhost:18080
```

## Reminder

The demo returns source links and verification steps only. It does not decide whether someone has a criminal case, valid credential, or legitimate business. A human reviewer must verify all possible matches using official records and lawful procedures.
