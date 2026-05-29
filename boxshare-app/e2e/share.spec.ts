import { test, expect } from '@playwright/test'
import { bootAsLoggedIn, mockFile, mockPublicFile } from './helpers'

async function openShareModal(page: import('@playwright/test').Page) {
  await bootAsLoggedIn(page)
  await page.getByTitle(mockFile.name).click()
  await page.keyboard.press('z')
  await expect(page.getByText(/share — document\.pdf/i)).toBeVisible()
}

// Scope share modal actions to the modal panel to avoid the ControlsBar "Share" button
const modal = (page: import('@playwright/test').Page) =>
  page.locator('[class*="panel"]').filter({ hasText: /share/i }).last()

test.describe('Share — modal', () => {
  test('opens share modal with Z key', async ({ page }) => {
    await openShareModal(page)
    await expect(page.getByRole('button', { name: /link share/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /email share/i })).toBeVisible()
  })

  test('Link Share tab is active by default', async ({ page }) => {
    await openShareModal(page)
    await expect(page.getByText('Generate a public link')).toBeVisible()
  })

  test('switching to Email Share tab shows email input', async ({ page }) => {
    await openShareModal(page)
    await page.getByRole('button', { name: /email share/i }).click()
    await expect(page.getByPlaceholder('user@example.com')).toBeVisible()
  })
})

test.describe('Share — link share', () => {
  test('Generate Link makes file public and shows the site link', async ({ page }) => {
    await page.route('**/api/file/1/visibility', route =>
      route.fulfill({ json: { ...mockPublicFile, id: 1 } })
    )
    await openShareModal(page)
    await page.getByRole('button', { name: /generate link/i }).click()
    await expect(page.getByText(/\/view\//)).toBeVisible()
    await expect(page.getByRole('button', { name: /copy/i })).toBeVisible()
  })

  test('Revoke Link shows on already-public file', async ({ page }) => {
    await bootAsLoggedIn(page, [{ ...mockPublicFile }])
    await page.getByTitle(mockPublicFile.name).click()
    await page.keyboard.press('z')
    await expect(page.getByRole('button', { name: /revoke link/i })).toBeVisible()
  })

  test('Revoke Link makes file private', async ({ page }) => {
    // Register specific override AFTER bootAsLoggedIn so it wins (Playwright LIFO routing)
    await bootAsLoggedIn(page, [{ ...mockPublicFile }])
    await page.route('**/api/file/2/visibility', route =>
      route.fulfill({ json: { ...mockFile, id: 2 } })
    )
    await page.getByTitle(mockPublicFile.name).click()
    await page.keyboard.press('z')
    await page.getByRole('button', { name: /revoke link/i }).click()
    await expect(page.getByRole('button', { name: /generate link/i })).toBeVisible()
  })

  test('Copy button copies site link to clipboard', async ({ page, context }) => {
    await context.grantPermissions(['clipboard-read', 'clipboard-write'])
    await page.route('**/api/file/1/visibility', route =>
      route.fulfill({ json: { ...mockPublicFile, id: 1 } })
    )
    await openShareModal(page)
    await page.getByRole('button', { name: /generate link/i }).click()
    await page.getByRole('button', { name: /copy/i }).click()

    const clipboardText = await page.evaluate(() => navigator.clipboard.readText())
    expect(clipboardText).toContain('/view/')
    expect(clipboardText).not.toContain('localhost:8080')
  })
})

test.describe('Share — email share', () => {
  test('sends email share request and shows success', async ({ page }) => {
    await page.route('**/api/file/1/share**', route =>
      route.fulfill({ status: 204 })
    )
    await openShareModal(page)
    await page.getByRole('button', { name: /email share/i }).click()
    await page.getByPlaceholder('user@example.com').fill('friend@example.com')
    // Scope to modal panel to avoid the ControlsBar Share button
    await modal(page).getByRole('button', { name: /^share$/i }).click()
    await expect(modal(page).getByRole('button', { name: /shared!/i })).toBeVisible()
  })

  test('Share button is disabled with empty email', async ({ page }) => {
    await openShareModal(page)
    await page.getByRole('button', { name: /email share/i }).click()
    await expect(modal(page).getByRole('button', { name: /^share$/i })).toBeDisabled()
  })

  test('pressing Enter in email input submits the share', async ({ page }) => {
    await page.route('**/api/file/1/share**', route =>
      route.fulfill({ status: 204 })
    )
    await openShareModal(page)
    await page.getByRole('button', { name: /email share/i }).click()
    await page.getByPlaceholder('user@example.com').fill('friend@example.com')
    await page.getByPlaceholder('user@example.com').press('Enter')
    await expect(modal(page).getByRole('button', { name: /shared!/i })).toBeVisible()
  })

  test('shows error if email share request fails', async ({ page }) => {
    await openShareModal(page)
    // Register AFTER openShareModal so this override wins (Playwright LIFO routing)
    await page.route('**/api/file/1/share**', route =>
      route.fulfill({ status: 404 })
    )
    await page.getByRole('button', { name: /email share/i }).click()
    await page.getByPlaceholder('user@example.com').fill('nobody@example.com')
    await modal(page).getByRole('button', { name: /^share$/i }).click()
    await expect(page.getByText('Something went wrong')).toBeVisible()
  })
})
