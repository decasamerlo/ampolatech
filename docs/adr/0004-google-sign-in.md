# Staff sign in with Google; AmpolaTech keeps its own users

Staff sign in with their Google accounts through OpenID Connect, and AmpolaTech stores no passwords. It keeps its own user records (name, role, pharmacy), and only an email a pharmacy has registered can sign in: nobody signs up on their own. Google proves who someone is; AmpolaTech decides what they may do. The first pharmacy's staff all have Google accounts.

## Considered options

- **The app's own accounts.** More code, but not more correct: password storage, reset by email, lockout after failed attempts and multi-factor authentication are where a mistake costs the most, and each needs its own screens and an email provider.
- **A managed identity provider** (e.g., Auth0). Another vendor and its pricing for a one-developer product, to provide what the staff's Google accounts already give.

## Consequences

- The backend runs the sign-in and holds the session. The browser gets only an HttpOnly, SameSite session cookie, and no token ever reaches it.
- Who is signed in comes through a port from the first slice. Until the sign-in slice ships, a local-only adapter signs everyone in as a configured user and pharmacy; it runs only under the local profile, and Google sign-in replaces it before any real data goes in.
- The identity provider sits behind a port. A pharmacy whose staff don't use Google gets a new adapter (the app's own accounts or another OpenID Connect provider), not a rewrite.
- With personal Google accounts, the pharmacy can't require two-step verification, and removing someone means deactivating their AmpolaTech user.
