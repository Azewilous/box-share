import { test, expect } from '@playwright/test'
import { bootAsLoggedIn, mockProfile, mockUser } from './helpers'

test.describe('Profile — view and update', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/api/user', route => {
      if (route.request().method() === 'POST')
        return route.fulfill({ json: mockProfile })
      if (route.request().method() === 'PUT')
        return route.fulfill({ json: { ...mockProfile, firstName: 'Gary' } })
      return route.continue()
    })
    await bootAsLoggedIn(page)
  })

  test('clicking email address opens profile modal', async ({ page }) => {
    await page.getByText(mockUser.email).click()
    await expect(page.getByText('— TRAINER PROFILE —')).toBeVisible()
  })

  test('profile modal loads existing name fields', async ({ page }) => {
    await page.getByText(mockUser.email).click()
    // Wait for loading state to resolve
    await expect(page.locator('[name="firstName"]')).toHaveValue(mockProfile.firstName)
    await expect(page.locator('[name="lastName"]')).toHaveValue(mockProfile.lastName)
    await expect(page.locator('[name="email"]').first()).toHaveValue(mockProfile.email)
  })

  test('role field is read-only', async ({ page }) => {
    await page.getByText(mockUser.email).click()
    await page.locator('[name="firstName"]').waitFor()
    const roleInputs = page.locator('input[readonly]')
    await expect(roleInputs.first()).toBeVisible()
  })

  test('can update first name and see success message', async ({ page }) => {
    await page.getByText(mockUser.email).click()
    await page.locator('[name="firstName"]').waitFor()
    await page.locator('[name="firstName"]').fill('Gary')
    await page.getByRole('button', { name: /^save$/i }).click()
    await expect(page.getByText('Profile updated!')).toBeVisible()
  })

  test('shows API error if save fails', async ({ page }) => {
    await page.route('**/api/user', route => {
      if (route.request().method() === 'PUT')
        return route.fulfill({ status: 500 })
      return route.fulfill({ json: mockProfile })
    })
    await page.getByText(mockUser.email).click()
    await page.locator('[name="firstName"]').waitFor()
    await page.locator('[name="firstName"]').fill('Gary')
    await page.getByRole('button', { name: /^save$/i }).click()
    await expect(page.getByText('Server error')).toBeVisible()
  })

  test('Close button dismisses the modal', async ({ page }) => {
    await page.getByText(mockUser.email).click()
    await expect(page.getByText('— TRAINER PROFILE —')).toBeVisible()
    await page.getByRole('button', { name: /close/i }).click()
    await expect(page.getByText('— TRAINER PROFILE —')).not.toBeVisible()
  })
})
