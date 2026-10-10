# Self-registration page

- **Figma:** [Simple Library – Self-Registration](https://www.figma.com/design/napnNlVd3hy0LtHqzMtQtA) (file key `napnNlVd3hy0LtHqzMtQtA`)
- **Status:** Draft, desktop only (1440×900). No mobile layout yet.
- **Font:** Inter. Colours are plain fills (no Figma variables or design system yet).

## Frames
| Frame | Node ID | Shows |
|---|---|---|
| Sign up – Default | `1:2` | Empty form with placeholders |
| Sign up – Validation errors | `1:45` | Filled form, "This email is already registered" on Email, "Passwords do not match" on Confirm password |
| Sign up – Success | `1:90` | Green check, "Account created", "Go to sign in" |

Layout: blue brand panel on the left (720 px), form panel on the right (440 px form). Built with auto-layout.

## Fields and backend mapping
| Field | Required | Backend today |
|---|---|---|
| First name | Yes | `Borrower.firstname` |
| Last name | Yes | `Borrower.lastname` |
| Email | Yes | `Borrower.email` (unique; duplicate gives 409) |
| Phone | No | **Not stored.** Needs a new field or a Keycloak user attribute |
| Address | No | **Not stored.** Same as phone |
| Password, Confirm password | Yes | Held by Keycloak, never by the API |

## Open questions
- Is sign-up a custom React page, or Keycloak's own self-registration page (realm `library`, themed)?
  A custom page needs a Keycloak registration flow or admin API call; the API has no sign-up endpoint.
- How does a new Keycloak user become a `Borrower` row (today `POST /apis/v1/borrowers` needs a token)?
- Should phone and address be stored? If yes, add them to `Borrower` and the DTOs, and update `docs/architecture/README.md` (data model).
- Add a mobile layout and a matching sign-in screen?
- Password rules (the design says "At least 8 characters"; Keycloak policy must match).
