# CNPM Web Frontend

This package hosts the customer/admin experience originally built in `CNPM/web` and wired into the FastfoodDelivery microservice backend.

- React 19 + Vite 7
- Axios client preconfigured for the API gateway
- Ant Design, Leaflet, Swiper, Recharts
- Mock data + marketing assets live in `shared/`

## Requirements

- Node.js 20+
- API gateway running locally on `http://localhost:8080/api/v1` (or override via env)

## Getting Started

```bash
cd frontend
npm install

# start vite only (expects real backend)
npm run dev

# start vite + JSON mock server (shared/db.json on port 5002)
npm run start
```

Build & lint:

```bash
npm run build
npm run lint
```

## Configuration

| Name                 | Default                        | Description                              |
| -------------------- | ------------------------------ | ---------------------------------------- |
| `VITE_API_BASE_URL`  | `http://localhost:8080/api/v1` | Gateway endpoint for all HTTP requests   |

## Data Sources

- **Backend (preferred):** all `firebase/firestore` calls are shimmed to the FastfoodDelivery APIs.
- **Mock data:** `npm run server` exposes `shared/db.json` on port `5002` for demos without backend access.
- **Static assets:** copied from `shared/Images` into `public/Images`.

## Notes

- Legacy Firebase scripts (`serviceAccountKey`, import jobs) have been removed.
- Drones and restaurant creation still persist locally (no backend endpoints yet).
