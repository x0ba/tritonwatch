# Clerk authentication for Tritonwatch

The React frontend uses Clerk's prebuilt sign-in flow and sends the active Clerk session token as a bearer token. The
`user-service` and `watchlist-service` verify that JWT locally with Clerk's public keys and use its verified `sub`
claim as the application user ID.

The APIs validate the token signature, `exp`, `nbf`, exact issuer, non-empty subject, and `azp` authorized party. No
Clerk secret key is needed at runtime because these services do not call the Clerk Backend API.

## Environments

Every Clerk application has separate Development and Production instances. Use the Development instance locally and
activate the Production instance for `tritonwatch.app`. Their keys, users, sessions, and issuers are separate.

If a third long-lived environment is needed, create another Clerk application. Clerk currently documents Development
and Production as the built-in instance types; staging is modeled as a separate application.

## 1. Create and configure the Clerk application

Create an application in the [Clerk Dashboard](https://dashboard.clerk.com/). In the Development instance, configure
the sign-in methods the app should expose. The frontend renders Clerk's `<SignIn>` component, so enabled methods appear
without code changes.

For the existing passwordless experience, enable email sign-in and an email verification strategy. If access should
be limited to UCSD accounts, configure an appropriate sign-up restriction in Clerk rather than trusting the email
address supplied to the Tritonwatch profile API.

From **API keys**, copy:

- the Development Publishable Key (`pk_test_...`); and
- the Frontend API URL. This is the JWT issuer and normally resembles
  `https://<instance-slug>.clerk.accounts.dev`.

Do not add a trailing slash to the issuer.

## 2. Configure local development

Create `frontend/.env` from the example:

```bash
cp frontend/.env.example frontend/.env
```

Set the Development Publishable Key:

```dotenv
VITE_CLERK_PUBLISHABLE_KEY=pk_test_REPLACE_ME
```

Create the ignored root `.env` file from its example:

```bash
cp .env.example .env
```

Then replace `YOUR_INSTANCE` in `.env` with the Development Frontend API hostname shown in Clerk. Each Spring Boot
service imports the root `.env` directly, so start it normally from its service directory with
`./gradlew bootRun`. Shell exports and Mise are not required.

`CLERK_AUTHORIZED_PARTIES` is compared with the session token's `azp` claim. It is security-sensitive even though it
is not a secret. `CORS_ALLOWED_ORIGINS` controls browser CORS separately. Both accept comma-separated origins.

Run the frontend:

```bash
cd frontend
vp install
vp run dev
```

Clerk's standard session token is sufficient; do not create a custom JWT template for this integration. The frontend
uses `getToken()` and the APIs fetch and cache signing keys from
`<CLERK_ISSUER>/.well-known/jwks.json`.

## 3. Production

Activate the application's Production instance in Clerk and configure `tritonwatch.app` as its production domain.
Complete the DNS records shown by Clerk. Production uses different keys and an issuer from Development.

Set the Terraform values using the exact production Frontend API URL shown in Clerk:

```hcl
clerk_issuer             = "https://clerk.tritonwatch.app"
clerk_authorized_parties = "https://tritonwatch.app"
cors_allowed_origins     = "https://tritonwatch.app,http://localhost:5173"
```

Deploy the frontend with the production Publishable Key:

```bash
export VITE_CLERK_PUBLISHABLE_KEY='pk_live_REPLACE_ME'
./scripts/deploy-frontend.sh
```

Never put `CLERK_SECRET_KEY` or any `sk_...` value in a `VITE_` variable. This project does not currently need a
Clerk secret key.

## Authorization model

Every API route other than health checks requires a valid Clerk session. Application records are isolated by the
verified Clerk user ID in `sub`; clients cannot supply a different owner ID in request bodies.

The old Auth0 scope checks were removed because Clerk's standard session tokens are session credentials rather than
OAuth access tokens. If Tritonwatch later adds staff-only endpoints, use Clerk roles/permissions or a narrowly scoped
custom claim and add explicit Spring authorization rules for those endpoints.

Clerk authenticates browser/API requests, not Kafka. Production Kafka still needs network isolation and broker
authentication/ACLs.

## Existing Auth0 users

Clerk user IDs look like `user_...`, so they do not match existing Auth0 subjects. Existing database records will not
automatically follow a person to their new Clerk account.

- For disposable local data, recreate the volumes/databases after switching providers.
- For production data, export an Auth0-to-Clerk identity mapping and run an explicit, reviewed data migration before
  enabling Clerk sign-in.

The Flyway migrations named `use_auth0_subject_for_user_id` are intentionally left in place: they are historical,
already-applied migrations whose schema change (UUID to string user IDs) is also correct for Clerk IDs.

## References

- [Clerk React quickstart](https://clerk.com/docs/react/getting-started/quickstart)
- [Clerk session tokens](https://clerk.com/docs/guides/sessions/session-tokens)
- [Clerk manual JWT verification](https://clerk.com/docs/guides/sessions/manual-jwt-verification)
- [Clerk environments](https://clerk.com/docs/guides/development/managing-environments)
- [Deploy Clerk to production](https://clerk.com/docs/guides/development/deployment/production)
