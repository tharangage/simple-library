# Self-registration page

- **Figma:** [Simple Library – Self-Registration](https://www.figma.com/design/napnNlVd3hy0LtHqzMtQtA) (file key `napnNlVd3hy0LtHqzMtQtA`)
- **Status:** Draft, desktop only (1440×900). No mobile layout yet.
- **Implemented** in code as E02 (US-02.1–02.3, merged in PR #11). The Figma frames match the built form: a **required Mobile number** (E.164, with a hint) and no address field.
- **Frontend:** `frontend/src/features/registration/RegisterPage.jsx` follows the frames (two panels, 440 px form, same sizes, spacing, colours and wording such as "Sign in" / "Go to sign in"). `/register` is full-screen, outside the app header (`Layout`); the brand name links home. Below 900 px the panels stack and only the brand name stays on top (the design is desktop only). The blue is scoped to this page (`.auth-layout` in `index.css`), so the rest of the app keeps its green theme.
- **Font:** Inter. Colours are plain fills (no Figma variables or design system yet). The Figma file uses Inter; the app uses the system font stack, so letter shapes differ slightly (add Inter as a web font if an exact match matters).

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
| Email | Yes | `Borrower.email` (unique; duplicate gives 409 `EMAIL_ALREADY_REGISTERED`) |
| Mobile number | Yes | `Borrower.mobile` (E.164, e.g. `+14155550100`) and the Keycloak attribute `mobile` |
| Password, Confirm password | Yes | Held by Keycloak, never by the API |

## Open questions
- Should sign-up stay a custom React page, or move to Keycloak's own registration page later (option b in D-01)?
- Add a mobile layout and a matching sign-in screen?
- Password rules: the form says "At least 8 characters"; keep the Keycloak password policy in step.
