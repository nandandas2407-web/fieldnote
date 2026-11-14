# Planned server contract (not implemented in the app yet)

- `GET /health` -> 200 (used by the Groups screen today)
- `POST /auth/login`, `POST /auth/register`
- `GET/POST /groups`, `POST /groups/{id}/join`, `GET /groups/{id}/members`
- `WS /live` : presence messages `{userId, subjectId, startedAt}` while a session runs
- `GET /rankings?scope=global|group|friends&period=day|week|month`
- `POST /sync/sessions` : upload finished sessions (idempotent by device id + start time)
