# Auth0 authentication for Tritonwatch

Tritonwatch's public boundaries are `watchlist-service` and `user-service`. They are OAuth 2.0 resource servers: clients
send an Auth0 access token, Spring Security verifies it, and each service takes the owner ID from the verified token's
`sub` claim. Clients must not send `X-User-Id`.

This setup uses Auth0 Universal Login for a browser client and Authorization Code with PKCE. The API is stateless; it does not create a server session and it does not need an Auth0 client secret.

## Authentication boundary

The browser-facing application endpoints are implemented by `watchlist-service` and `user-service`. Both validate Auth0
access tokens and derive ownership from `sub`. `ingestion-service` and `notification-service` are asynchronous workers;
they trust events from Kafka rather than accepting a user's bearer token.

Auth0 secures HTTP clients, not Kafka. In production, protect Kafka separately with network isolation plus broker authentication/ACLs so an attacker cannot publish a forged `UserCourseWatchCreated` event. If services later call one another over HTTP, create a separate Auth0 Machine to Machine application and narrow service scopes; do not reuse the browser client.

## 1. Create the Auth0 API

In **Auth0 Dashboard → Applications → APIs → Create API**, use:

| Setting | Value |
| --- | --- |
| Name | `Tritonwatch API` |
| Identifier (audience) | `https://api.tritonwatch.app` |
| Signing algorithm | `RS256` |

The identifier is a stable name, not a URL that has to resolve. If you choose a different identifier, use that exact value everywhere this guide uses `https://api.tritonwatch.app`.

Open the API's **Permissions** tab and add:

| Permission | Description |
| --- | --- |
| `create:watch-requests` | Create a course watch for the current user |
| `read:user-profile` | Read the current user's Tritonwatch profile |
| `update:user-profile` | Create, update, or delete the current user's profile and notification preferences |

Each endpoint requires its corresponding scope. Spring Security converts a JWT scope such as `update:user-profile` to
the authority `SCOPE_update:user-profile`.

If you turn on Auth0 RBAC, assign these permissions to the student role and assign users to that role. Keeping authorization as a scope check means the API works for both user tokens and authorized machine-to-machine test clients.

## 2. Create the browser application

Create an Auth0 **Single Page Application**. For a frontend running on Vite's default port, configure:

| Setting | Development value |
| --- | --- |
| Allowed Callback URLs | `http://localhost:5173` |
| Allowed Logout URLs | `http://localhost:5173` |
| Allowed Web Origins | `http://localhost:5173` |

Add the deployed HTTPS origin to all three lists for production. Do not put an SPA client secret in browser code; a SPA is a public client and uses PKCE.

## 3. Configure and run the APIs

Start the shared infrastructure, then export the tenant issuer and API identifier before starting both HTTP services:

```bash
export AUTH0_ISSUER='https://YOUR_TENANT.us.auth0.com/'
export AUTH0_AUDIENCE='https://api.tritonwatch.app'
export CORS_ALLOWED_ORIGINS='http://localhost:5173'

docker compose -f infra/docker-compose.yml up -d
```

Start `watchlist-service` in that terminal:

```bash
cd services/watchlist-service
./gradlew bootRun
```

In a second terminal, repeat the three exports above and start `user-service` from the repository root:

```bash
cd services/user-service
./gradlew bootRun
```

Use the issuer that Auth0 puts in the token's `iss` claim. Keep `https://` and preferably the trailing slash. If login uses an Auth0 custom domain, configure that same issuer here; tokens issued by the tenant domain and custom domain have different issuer values.

Multiple browser origins can be comma-separated:

```bash
export CORS_ALLOWED_ORIGINS='http://localhost:5173,https://tritonwatch.app'
```

`AUTH0_ISSUER` and `AUTH0_AUDIENCE` are identifiers, not secrets. No Auth0 client secret belongs in either service.

The security implementations are in:

- `services/watchlist-service/src/main/java/app/tritonwatch/watchlist_service/security/SecurityConfig.java`; and
- `services/user-service/src/main/java/app/tritonwatch/user_service/security/SecurityConfig.java`.

They:

- download and cache Auth0 public signing keys from `/.well-known/jwks.json` when a token must be verified;
- validate the JWT signature, expiration/not-before time, exact issuer, and audience;
- permit unauthenticated health probes;
- require `create:watch-requests` for `POST /api/v1/watch-requests`;
- require `read:user-profile` for `GET /api/v1/me`;
- require `update:user-profile` for `PUT /api/v1/me`, `PUT /api/v1/me/notification-preferences`, and `DELETE /api/v1/me`;
- disable CSRF and server sessions because authentication is a bearer token; and
- restrict browser CORS to configured origins.

## 4. Call the APIs from a frontend

There is no frontend package in this repository yet. In a React/Vite client, install the official React SDK:

```bash
npm install @auth0/auth0-react
```

Set public frontend variables (these are safe to expose):

```dotenv
VITE_AUTH0_DOMAIN=YOUR_TENANT.us.auth0.com
VITE_AUTH0_CLIENT_ID=YOUR_SPA_CLIENT_ID
VITE_AUTH0_AUDIENCE=https://api.tritonwatch.app
VITE_WATCHLIST_API_BASE_URL=http://localhost:8082
VITE_USER_API_BASE_URL=http://localhost:8081
```

Wrap the app at its entry point:

```tsx
import { Auth0Provider } from "@auth0/auth0-react";
import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <Auth0Provider
      domain={import.meta.env.VITE_AUTH0_DOMAIN}
      clientId={import.meta.env.VITE_AUTH0_CLIENT_ID}
      authorizationParams={{
        redirect_uri: window.location.origin,
        audience: import.meta.env.VITE_AUTH0_AUDIENCE,
        scope: "openid profile email create:watch-requests read:user-profile update:user-profile",
      }}
      cacheLocation="memory"
    >
      <App />
    </Auth0Provider>
  </React.StrictMode>,
);
```

A minimal login/logout control:

```tsx
import { useAuth0 } from "@auth0/auth0-react";

export function AccountButton() {
  const { isAuthenticated, isLoading, loginWithRedirect, logout, user } = useAuth0();

  if (isLoading) return <span>Loading…</span>;
  if (!isAuthenticated) return <button onClick={() => loginWithRedirect()}>Log in</button>;

  return (
    <div>
      <span>{user?.name ?? user?.email}</span>
      <button onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}>
        Log out
      </button>
    </div>
  );
}
```

Fetch an access token immediately before calling the API. Never send the ID token to this endpoint:

```tsx
import { useAuth0 } from "@auth0/auth0-react";

export function useWatchRequests() {
  const { getAccessTokenSilently } = useAuth0();

  return async (courseId: string, term: string) => {
    const accessToken = await getAccessTokenSilently({
      authorizationParams: {
        audience: import.meta.env.VITE_AUTH0_AUDIENCE,
        scope: "create:watch-requests",
      },
    });

    const response = await fetch(`${import.meta.env.VITE_WATCHLIST_API_BASE_URL}/api/v1/watch-requests`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ courseId, term }),
    });

    if (!response.ok) throw new Error(`Create watch failed (${response.status})`);
    return response.json();
  };
}
```

Auth0's `sub` claim is intentionally absent from the request body. The controller reads it from the verified JWT and passes it to persistence.

Calls to `user-service` use its base URL and the profile scope corresponding to the operation. For example:

```tsx
import { useAuth0 } from "@auth0/auth0-react";

export function useUserProfile() {
  const { getAccessTokenSilently } = useAuth0();

  return async () => {
    const accessToken = await getAccessTokenSilently({
      authorizationParams: {
        audience: import.meta.env.VITE_AUTH0_AUDIENCE,
        scope: "read:user-profile",
      },
    });

    const response = await fetch(`${import.meta.env.VITE_USER_API_BASE_URL}/api/v1/me`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });

    if (!response.ok) throw new Error(`Get profile failed (${response.status})`);
    return response.json();
  };
}
```

Use `update:user-profile` instead for profile creation/replacement, preference updates, and profile deletion. Those
requests still omit a user ID; `user-service` takes it from the verified access token.

## 5. Test without a frontend

Create an Auth0 **Machine to Machine Application**, authorize it for `Tritonwatch API`, and grant `create:watch-requests`. This client secret is only for local testing or a secure server, never a browser.

```bash
export AUTH0_DOMAIN='YOUR_TENANT.us.auth0.com'
export AUTH0_TEST_CLIENT_ID='YOUR_M2M_CLIENT_ID'
export AUTH0_TEST_CLIENT_SECRET='YOUR_M2M_CLIENT_SECRET'

ACCESS_TOKEN="$({
  curl --silent --request POST "https://${AUTH0_DOMAIN}/oauth/token" \
    --header 'content-type: application/json' \
    --data "{\"client_id\":\"${AUTH0_TEST_CLIENT_ID}\",\"client_secret\":\"${AUTH0_TEST_CLIENT_SECRET}\",\"audience\":\"${AUTH0_AUDIENCE}\",\"grant_type\":\"client_credentials\"}"
} | jq -r .access_token)"

curl --include --request POST 'http://localhost:8082/api/v1/watch-requests' \
  --header "Authorization: Bearer ${ACCESS_TOKEN}" \
  --header 'Content-Type: application/json' \
  --data '{"courseId":"CSE 100","term":"FA26"}'
```

Expected behavior:

| Request | Result |
| --- | --- |
| No bearer token, malformed token, wrong issuer/audience, expired token | `401 Unauthorized` |
| Valid token missing `create:watch-requests` | `403 Forbidden` |
| Valid token and scope, new watch | `201 Created` |
| Valid token and scope, duplicate watch for the same subject/course/term | `200 OK` |

An M2M token's subject identifies the application rather than a student. It is useful for transport/security testing, but use a real Universal Login user token to test user ownership.

## 6. Data and event contract changes

Auth0 subjects look like `auth0|6553...`; they are not UUIDs. `watch_requests.user_id`, `subscriptions.user_id`, and `UserCourseWatchCreated.userId` are therefore strings up to 255 characters. Flyway migrations preserve existing UUID values by converting them to text.

The event payload is now conceptually:

```json
{
  "eventId": "10ef6f62-3fd4-4f08-9708-7d69fb3b26ef",
  "occurredAt": "2026-08-30T12:00:00Z",
  "userId": "auth0|6553da60a54af58e29493993",
  "courseId": "CSE 100",
  "term": "FA26"
}
```

Because the Kafka payload changed incompatibly, deploy `notification-service` with the new shared contract before (or at the same time as) the authenticated `watchlist-service`. In a mature deployment, publish this as a v2 topic instead of changing a v1 payload in place.

## 7. Production checklist

- Use only HTTPS for the frontend and API.
- Keep access tokens in memory when possible; avoid `localStorage` unless persistent sessions are a deliberate tradeoff.
- Keep the access-token lifetime short enough for your risk model and use refresh-token rotation if persistent login is required.
- Allow only real frontend origins in `CORS_ALLOWED_ORIGINS`; never use `*` with credentials.
- Add a distinct permission for every future write/read boundary, such as `read:watch-requests` and `delete:watch-requests`.
- Never accept a user ID, email address, role, or permission from a request header/body when the verified token already provides the authoritative claim.
- Do not use an ID token as an API bearer token. Request an access token with the Tritonwatch API audience.
- Monitor `401`/`403` rates without logging raw bearer tokens.
- If accounts can be linked or migrated across Auth0 connections, define how ownership should migrate; the Auth0 `sub` is stable for one Auth0 identity, not necessarily across unrelated identities.

## References

- [Auth0: Java Spring Boot API quickstart](https://auth0.com/docs/quickstart/backend/java-spring-security5)
- [Auth0: validate access tokens](https://auth0.com/docs/secure/tokens/access-tokens/validate-access-tokens)
- [Auth0: access token profiles and subject format](https://auth0.com/docs/secure/tokens/access-tokens/access-token-profiles)
- [Auth0: React SDK for single-page applications](https://auth0.com/docs/libraries/auth0-react)
- [Spring Security: OAuth 2.0 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
