# BoxShare — Frontend

The UI for BoxShare, themed after the Pokémon Colosseum PC Box storage screen. A dark teal grid-based interface where each file occupies a slot, inspired by the GameCube-era box management screen.

## Features

- **File grid** — 30-slot box view and 6-slot party view, keyboard navigable with arrow keys
- **Upload** — press X (or click Upload) to pick a file; it's uploaded directly to S3 via a pre-signed URL
- **View** — press A on a selected file to preview images, video, audio, PDFs, and text files inline
- **Delete** — press Y with a file selected; a confirmation dialog prevents accidental deletes
- **Share**
  - *Link share* — generate a public `/view/:token` URL anyone can open without logging in; revoke it to make the file private again
  - *Email share* — share directly with another BoxShare user by their email address; shared files appear with a purple tint and a "shared" badge in the recipient's grid
- **Profile** — click your email in the top bar to update your name and email
- **Auth** — register, log in, log out; email verification gate before accessing the app

## Tech Stack

- **React 19** + **TypeScript**
- **Vite** — dev server and build
- **Axios** — API client with a response interceptor for dev-mode error logging
- CSS Modules — scoped styles per component, no CSS framework

## Running Locally

The frontend expects the BoxShare backend running at `http://localhost:8080`. See the backend README for setup.

```bash
npm install
npm run dev       # http://localhost:5173
```

## Environment

Create `.env.local` in the project root:

```
VITE_API_BASE_URL=http://localhost:8080
```

## Keyboard Controls

| Key | Action |
|-----|--------|
| Arrow keys | Navigate the file grid |
| A | Open file viewer |
| B | Deselect / close viewer |
| X | Upload a file |
| Y | Delete selected file |
| Z | Share selected file |

## Testing

```bash
# Unit tests (pure logic — fileUtils, error handling)
npm test

# Unit tests with coverage report
npm run test:coverage

# E2E tests — Playwright, mocks the API, no backend needed
npm run test:e2e

# E2E with interactive UI
npm run test:e2e:ui
```

Unit tests cover `src/utils/fileUtils.ts` and `src/api/errors.ts`.
E2E tests cover the full UI: auth, file list, upload, delete, share (link + email), profile, and the public viewer page.

## Project Structure

```
src/
  api/          # Axios API functions (auth, files, users)
  components/
    auth/       # Login, register, profile, email gate modals
    button/     # GcButton — game-controller style button
    layout/     # TopBar, ControlsBar
    modal/      # Base Modal wrapper
    pc/         # BoxArea, SlotGrid, Slot, FileDataPanel, ShareModal, PublicViewer
  context/      # AuthContext + useAuth hook
  data/         # Mock slots for logged-out demo view
  types/        # FileSlot type
  utils/        # fileUtils — emoji, type label, size formatting
e2e/            # Playwright E2E tests
```

## Public File Viewer

Shared files are accessible at `/view/:shareToken` without logging in. The page loads the file via the public API endpoint and renders the same viewer (image, video, audio, PDF, text). The link is revocable — once the owner makes the file private, the token returns a 404.
