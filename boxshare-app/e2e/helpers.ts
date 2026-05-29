import type { Page } from '@playwright/test'

// ── Fixtures ─────────────────────────────────────────────────────────────────

export const mockUser = {
  email: 'ash@example.com',
  role: 'ROLE_USER',
  emailVerified: true,
}

export const mockProfile = {
  id: 1,
  firstName: 'Ash',
  lastName: 'Ketchum',
  email: 'ash@example.com',
}

export const mockFile = {
  id: 1,
  name: 'document.pdf',
  size: 102400,
  mimeType: 'application/pdf',
  status: 'COMPLETED',
  visibility: 'PRIVATE',
  shareToken: null,
  presignedUrl: 'https://fake-s3.example.com/document.pdf',
  shared: false,
}

export const mockPublicFile = {
  ...mockFile,
  id: 2,
  name: 'public-image.png',
  mimeType: 'image/png',
  visibility: 'PUBLIC',
  shareToken: 'abc123-token',
  presignedUrl: 'https://fake-s3.example.com/public-image.png',
}

// ── Route mocking helpers ─────────────────────────────────────────────────────

export async function mockLoggedOut(page: Page) {
  await page.route('**/api/auth/me', route =>
    route.fulfill({ status: 401, json: {} })
  )
}

export async function mockFileRoutes(page: Page, files = [mockFile]) {
  await page.route('**/api/file', route => {
    if (route.request().method() === 'GET')
      return route.fulfill({ json: files })
    if (route.request().method() === 'POST')
      return route.fulfill({ json: { ...mockFile, id: Date.now() } })
    return route.continue()
  })

  await page.route('**/api/file/**', route => {
    const method = route.request().method()
    if (method === 'DELETE') return route.fulfill({ status: 204 })
    if (method === 'PATCH')  return route.fulfill({ json: { ...mockFile, visibility: 'PUBLIC', shareToken: 'new-token' } })
    if (method === 'POST')   return route.fulfill({ status: 204 })
    if (method === 'GET')    return route.fulfill({ json: mockFile })
    return route.continue()
  })
}

/**
 * Boot the app as a logged-in, verified user.
 * Waits for the Log Out button to confirm auth has settled before returning.
 */
export async function bootAsLoggedIn(page: Page, files = [mockFile]) {
  await mockFileRoutes(page, files)
  await page.route('**/api/auth/me', route =>
    route.fulfill({ json: mockUser })
  )
  await page.addInitScript(() => {
    localStorage.setItem('bs_session', 'true')
  })
  await page.goto('/')
  // Wait for auth to settle before test proceeds
  await page.waitForSelector('button:has-text("Log Out")')
}
