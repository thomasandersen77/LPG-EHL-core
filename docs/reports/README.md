## Reports (time-sliced documentation)

This folder contains **historical writeups** (investigations, analyses, summaries, field notes) organized by **ISO year + week number**:

```
docs/reports/<ISOYEAR>/<ISOYEAR>-W<WW>/<filename>.md
```

### How the week is chosen

Files are bucketed by the **first commit where the file was added** (Git history), not by local filesystem timestamps.

If you need to find “when did this doc appear”, use:

```bash
git log --follow --diff-filter=A --reverse --format='%aI %h %s' -- path/to/doc.md | head -1
```

### What belongs here

- Analyses / reports
- One-off plans
- Summaries of changes
- Field testing notes

### What does *not* belong here

Canonical docs you expect to stay up-to-date should live under `docs/` in a **topic folder** (e.g. `docs/deployment/`, `docs/development/`, `docs/testing/`).

