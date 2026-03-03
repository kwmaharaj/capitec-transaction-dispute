# Transaction Dispute Portal (Frontend)

## Helpers for Local dev

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

The UI calls the backend using `VITE_API_BASE` (default `http://localhost:8080`).

## Docker
The root `docker-compose.yml` includes a `frontend` service.
