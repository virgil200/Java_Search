# Security and Compliance Guide

This project is designed as an ethical public-records discovery portal, not a private-investigation or surveillance tool.

## Allowed uses

- Self-checking your own public records
- Consented background or credential verification
- Vendor, contractor, or business due diligence
- Legal research using official court portals and public legal databases
- Compliance screening where your organization has a lawful basis and follows applicable rules

## Disallowed uses

- Harassment, stalking, doxxing, intimidation, or shaming
- Searching about minors
- Collecting private identifiers such as SSNs, passport numbers, bank details, private addresses, passwords, or account credentials
- Bypassing paywalls, logins, robots rules, CAPTCHAs, or access controls
- Making automated adverse decisions about employment, housing, credit, insurance, or benefits without legal compliance and human review
- Claiming a person has a criminal record based only on a name match

## Product safeguards included

- Consent/legitimate-purpose checkbox
- No search support for minors
- Sensitive identifier rejection for long number strings and obvious private-ID keywords
- No database or persistence layer
- No scraping code
- Official-source verification reminders
- Source reliability labels

## Recommended production safeguards

If you deploy this beyond a demo, add:

1. Authentication and role-based access control.
2. Strong audit logs that record purpose, user, timestamp, and sources checked. Protect logs as sensitive data.
3. Data-retention limits and deletion workflows.
4. Jurisdiction-specific legal review.
5. Clear user notices and consent capture.
6. Rate limiting and abuse monitoring.
7. A process for handling disputes, false positives, expunged/sealed records, and data subject requests.
8. Formal vendor contracts for any third-party data provider.
9. Human review before any adverse action.
10. Secure deployment with HTTPS, CSP, and secrets management.

## Verification principles

- Use at least two identifiers before treating records as a likely match.
- Confirm criminal/court records with the issuing court or agency.
- Confirm degrees with the school registrar or an authorized verification service.
- Confirm business records with the official registry and current permits/licenses.
- Record uncertainty. Never present public-source leads as proven facts without verification.
