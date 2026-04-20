# Movify Backend

## Run locally

1. Copy `.env.example` to `.env`
2. Replace `[YOUR-PASSWORD]` in `DATABASE_URL` and `DIRECT_URL`
3. Run `npm install`
4. Run `npm run db:generate`
5. Run `npm run db:push`
6. Run `npm run db:seed`
7. Run `npm run dev`

The server listens on `0.0.0.0:${PORT}` and exposes:

- `GET /health`
- `GET /api/places/autocomplete?q=...`
- `GET /api/places/geocode?q=...`
- `GET /api/routes/preview?pickup=...&destination=...`

## Database

- Prisma schema: `db/schema.prisma`
- Mock seed source: `db/mock-data.js`
- Generate client: `npm run db:generate`
- Validate schema: `npm run db:validate`
- Pull schema from Supabase: `npm run db:pull`
- Push schema to database: `npm run db:push`
- Create dev migration: `npm run db:migrate`
- Seed mock data: `npm run db:seed`
- Open Prisma Studio: `npm run db:studio`

`GET /health` now reports whether Prisma can connect to PostgreSQL and whether the schema/mock data are ready.

If you test the Android app on a real phone, set `com.movify.BACKEND_BASE_URL` to your computer's LAN IP, for example `http://192.168.1.10:3000`.
