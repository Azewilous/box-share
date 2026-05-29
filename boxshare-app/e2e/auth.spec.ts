import { test, expect } from '@playwright/test'
import { mockLoggedOut, mockUser, bootAsLoggedIn } from './helpers'

test.describe('Auth — logged out state', () => {
  test.beforeEach(async ({ page }) => {
    await mockLoggedOut(page)
    await page.goto('/')
  })

  test('shows Log In and Register buttons', async ({ page }) => {
    await expect(page.getByRole('button', { name: /log in/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /register/i })).toBeVisible()
  })

  test('opens login modal on Log In click', async ({ page }) => {
    await page.getByRole('button', { name: /log in/i }).click()
    // Modal title uses em-dashes; check for the form instead
    await expect(page.locator('[name="email"]')).toBeVisible()
    await expect(page.locator('[name="password"]')).toBeVisible()
  })

  test('opens register modal on Register click', async ({ page }) => {
    await page.getByRole('button', { name: /register/i }).click()
    await expect(page.locator('[name="firstName"]')).toBeVisible()
    await expect(page.locator('[name="username"]')).toBeVisible()
  })

  test('login modal switches to register', async ({ page }) => {
    await page.getByRole('button', { name: /log in/i }).click()
    await page.getByRole('button', { name: /register here/i }).click()
    await expect(page.locator('[name="firstName"]')).toBeVisible()
  })

  test('register modal switches to login', async ({ page }) => {
    await page.getByRole('button', { name: /register/i }).click()
    await page.getByRole('button', { name: /log in here/i }).click()
    await expect(page.locator('[name="password"]')).toBeVisible()
    await expect(page.locator('[name="firstName"]')).not.toBeVisible()
  })

  test('shows validation errors for empty login submit', async ({ page }) => {
    await page.getByRole('button', { name: /log in/i }).click()
    // Scope submit to the form to avoid matching the TopBar "Log In" button
    await page.locator('form').getByRole('button', { name: /^log in$/i }).click()
    await expect(page.getByText('Email is required')).toBeVisible()
    await expect(page.getByText('Password is required')).toBeVisible()
  })

  test('shows validation errors for invalid register fields', async ({ page }) => {
    await page.getByRole('button', { name: /register/i }).click()
    await page.locator('form').getByRole('button', { name: /^register$/i }).click()
    await expect(page.getByText('First name is required')).toBeVisible()
    await expect(page.getByText('Enter a valid email')).toBeVisible()
  })

  test('shows API error on bad credentials', async ({ page }) => {
    await page.route('**/api/auth/login', route =>
      route.fulfill({ status: 401, json: {} })
    )
    await page.getByRole('button', { name: /log in/i }).click()
    await page.locator('[name="email"]').fill('wrong@example.com')
    await page.locator('[name="password"]').fill('wrongpass')
    await page.locator('form').getByRole('button', { name: /^log in$/i }).click()
    await expect(page.getByText('Invalid email or password.')).toBeVisible()
  })

  test('closes modal on backdrop click', async ({ page }) => {
    await page.getByRole('button', { name: /log in/i }).click()
    await expect(page.locator('[name="email"]')).toBeVisible()
    await page.mouse.click(10, 10)
    await expect(page.locator('[name="email"]')).not.toBeVisible()
  })
})

test.describe('Auth — login flow', () => {
  test('successful login shows user email and Log Out button', async ({ page }) => {
    await page.route('**/api/auth/login', route =>
      route.fulfill({ status: 200, json: {} })
    )
    await page.route('**/api/auth/me', route =>
      route.fulfill({ json: mockUser })
    )
    await page.route('**/api/file', route =>
      route.fulfill({ json: [] })
    )
    await page.goto('/')

    await page.getByRole('button', { name: /log in/i }).click()
    await page.locator('[name="email"]').fill('ash@example.com')
    await page.locator('[name="password"]').fill('Password123!')
    await page.locator('form').getByRole('button', { name: /^log in$/i }).click()

    await expect(page.getByText(mockUser.email)).toBeVisible()
    await expect(page.getByRole('button', { name: /log out/i })).toBeVisible()
  })
})

test.describe('Auth — register flow', () => {
  test('successful register closes modal and shows user email', async ({ page }) => {
    await page.route('**/api/auth/register', route =>
      route.fulfill({ status: 200, json: {} })
    )
    await page.route('**/api/auth/me', route =>
      route.fulfill({ json: mockUser })
    )
    await page.route('**/api/file', route =>
      route.fulfill({ json: [] })
    )
    await page.goto('/')

    await page.getByRole('button', { name: /register/i }).click()
    await page.locator('[name="firstName"]').fill('Ash')
    await page.locator('[name="lastName"]').fill('Ketchum')
    await page.locator('[name="username"]').fill('AshKetchum')
    await page.locator('[name="email"]').fill('ash@example.com')
    await page.locator('[name="password"]').fill('Password123!')
    await page.locator('[name="confirmPassword"]').fill('Password123!')
    await page.locator('form').getByRole('button', { name: /^register$/i }).click()

    await expect(page.getByText(mockUser.email)).toBeVisible()
  })
})

test.describe('Auth — logout', () => {
  test('logout returns to logged-out state', async ({ page }) => {
    await page.route('**/api/auth/logout', route =>
      route.fulfill({ status: 200, json: {} })
    )
    await bootAsLoggedIn(page)
    await expect(page.getByText(mockUser.email)).toBeVisible()

    await page.getByRole('button', { name: /log out/i }).click()

    await expect(page.getByRole('button', { name: /log in/i })).toBeVisible()
    await expect(page.getByText(mockUser.email)).not.toBeVisible()
  })
})
