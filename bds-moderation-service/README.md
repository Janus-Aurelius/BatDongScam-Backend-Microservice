# Moderation Service

Standalone microservice extracted from monolith `violation` module.

## Current phase

- Phase 1 completed:
  - Decoupled from `User` and `Property` domains.
  - Uses primitive IDs (`reporterId`, `relatedEntityId`) only.
  - No direct repository/service dependency to external domains.
- Phase 2 pending:
  - Validate IDs via HTTP call to Core Service.

## Main endpoints

- `POST /violations` (multipart form-data)
  - `payload`: `ViolationCreateRequest`
  - `evidenceFiles`: optional files
- `GET /violations/admin`
- `GET /violations/admin/{id}`
- `PUT /violations/admin/{id}`
- `GET /violations/my-violations`
- `GET /violations/my-violations/{id}`

## Authentication bridge for local development

This service currently accepts optional header:

- `X-User-Id: <uuid>`

If provided, it is mapped into Spring Security context and used by:

- `GET /violations/my-violations`
- `GET /violations/my-violations/{id}`
- reporter ID fallback in `POST /violations` if `reporterId` is omitted.

## Notes

- File uploads are currently stored by `MockFileStorageService` and return `mock://` URLs.
- Replace `MockFileStorageService` with real adapter (S3/Cloudinary/etc.) when integrating.
