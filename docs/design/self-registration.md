# Self-registration page

- **Figma:** [Simple Library – Self-Registration](https://www.figma.com/design/napnNlVd3hy0LtHqzMtQtA) (file key `napnNlVd3hy0LtHqzMtQtA`)
- **Status:** Draft, desktop only (1440×900). No mobile layout yet.
- **Implemented** in code as E02 (US-02.1–02.3, merged in PR #11). The Figma frames match the built form: a **required Mobile number** (E.164, with a hint) and no address field.
- **Frontend:** `frontend/src/features/registration/RegisterPage.jsx` and the global styles in `frontend/src/index.css` follow these frames: blue header and accent (`#2563eb`), light grey page, a centred white form card (440 px) with 8 px input and button corners, Inter first in the font stack, bold headings. The same look is used on Home and Books. The frames show the page **inside the app shell**.
- **Font:** Inter. Colours are plain fills (no Figma variables or design system yet). The Figma file uses Inter; the app uses the system font stack, so letter shapes differ slightly (add Inter as a web font if an exact match matters).

## Frames
| Frame | Node ID | Shows |
|---|---|---|
| Sign up – Default | `1:2` | Empty form with placeholders |
| Sign up – Validation errors | `1:45` | Filled form, "This email is already registered" on Email, "Passwords do not match" on Confirm password |
| Sign up – Success | `1:90` | Green check, "Account created", "Go to sign in" |

Layout (all frames, 1440×900): blue **Header** (brand name, *Register* link, white *Log in* button) on top, then a grey **Page** area with a centred white **Card** (440 px, 12 px corners) that holds the form. Built with auto-layout. An earlier version of the frames had a full-screen two-panel layout with a blue brand panel; it was dropped in favour of the in-app layout.

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
