import { test, expect } from '@playwright/test'
import { mockPublicFile } from './helpers'

const VALID_TOKEN = 'abc123-token'

test.describe('Public viewer — /view/:token', () => {
  test('shows file name and download button for valid token', async ({ page }) => {
    await page.route(`**/api/public/file/${VALID_TOKEN}`, route =>
      route.fulfill({ json: mockPublicFile })
    )
    await page.goto(`/view/${VALID_TOKEN}`)
    await expect(page.getByText(mockPublicFile.name)).toBeVisible()
    await expect(page.getByRole('link', { name: /download/i })).toBeVisible()
  })

  test('shows lock icon and error message for invalid token', async ({ page }) => {
    await page.route('**/api/public/file/**', route =>
      route.fulfill({ status: 404 })
    )
    await page.goto('/view/bad-token')
    await expect(page.getByText(/not available or the link has expired/i)).toBeVisible()
  })

  test('shows Go to BoxShare link', async ({ page }) => {
    await page.route(`**/api/public/file/${VALID_TOKEN}`, route =>
      route.fulfill({ json: mockPublicFile })
    )
    await page.goto(`/view/${VALID_TOKEN}`)
    await expect(page.getByRole('link', { name: /go to boxshare/i })).toBeVisible()
  })

  test('Go to BoxShare navigates to root', async ({ page }) => {
    await page.route('**/api/public/file/**', route =>
      route.fulfill({ status: 404 })
    )
    await page.route('**/api/auth/me', route =>
      route.fulfill({ status: 401 })
    )
    await page.goto('/view/bad-token')
    await page.getByRole('link', { name: /go to boxshare/i }).first().click()
    await expect(page).toHaveURL('/')
  })

  test('displays image preview for image files', async ({ page }) => {
    await page.route(`**/api/public/file/${VALID_TOKEN}`, route =>
      route.fulfill({ json: mockPublicFile })
    )
    await page.goto(`/view/${VALID_TOKEN}`)
    await expect(page.locator(`img[alt="${mockPublicFile.name}"]`)).toBeVisible()
  })

  test('shows BoxShare branding in the header logo', async ({ page }) => {
    await page.route(`**/api/public/file/${VALID_TOKEN}`, route =>
      route.fulfill({ json: mockPublicFile })
    )
    await page.goto(`/view/${VALID_TOKEN}`)
    // Target the logo span specifically, not the "Go to BoxShare" link
    await expect(page.locator('[class*="logo"]')).toHaveText('BoxShare')
  })
})
