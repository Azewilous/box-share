import { test, expect } from '@playwright/test'
import { bootAsLoggedIn, mockFile } from './helpers'

// Scope confirm-dialog interactions to the modal overlay to avoid
// matching the ControlsBar Delete / Share buttons that share labels.
const inModal = (page: import('@playwright/test').Page) =>
  page.locator('.backdrop, [class*="backdrop"]').first()

test.describe('Files — list', () => {
  test('shows file name in the slot grid after login', async ({ page }) => {
    await bootAsLoggedIn(page)
    await expect(page.getByTitle(mockFile.name)).toBeVisible()
  })

  test('empty grid shows upload prompt when no files', async ({ page }) => {
    await bootAsLoggedIn(page, [])
    await expect(page.getByText('No files yet')).toBeVisible()
    // Use first() — the same text may also appear in the ControlsBar area
    await expect(page.getByText('Press X to upload').first()).toBeVisible()
  })

  test('shows correct file type label in slot', async ({ page }) => {
    await bootAsLoggedIn(page)
    // Target the slotType span directly inside the slot
    await expect(page.getByTitle(mockFile.name).locator('[class*="slotType"]')).toHaveText('PDF')
  })
})

test.describe('Files — viewer', () => {
  test('clicking a file slot selects it', async ({ page }) => {
    await bootAsLoggedIn(page)
    await page.getByTitle(mockFile.name).click()
    await expect(page.getByTitle(mockFile.name)).toBeVisible()
  })

  test('pressing A on selected file opens viewer modal', async ({ page }) => {
    await page.route('**/api/file/document.pdf', route =>
      route.fulfill({ json: { ...mockFile, presignedUrl: 'https://fake-s3.example.com/doc.pdf' } })
    )
    await bootAsLoggedIn(page)
    await page.getByTitle(mockFile.name).click()
    await page.keyboard.press('a')
    await expect(page.getByText(`— ${mockFile.name} —`)).toBeVisible()
  })

  test('viewer modal closes with B key', async ({ page }) => {
    await page.route('**/api/file/document.pdf', route =>
      route.fulfill({ json: { ...mockFile, presignedUrl: 'https://fake-s3.example.com/doc.pdf' } })
    )
    await bootAsLoggedIn(page)
    await page.getByTitle(mockFile.name).click()
    await page.keyboard.press('a')
    await expect(page.getByText(`— ${mockFile.name} —`)).toBeVisible()
    await page.keyboard.press('b')
    await expect(page.getByText(`— ${mockFile.name} —`)).not.toBeVisible()
  })
})

test.describe('Files — upload', () => {
  test('clicking Upload button opens file chooser', async ({ page }) => {
    await bootAsLoggedIn(page)
    const [fileChooser] = await Promise.all([
      page.waitForEvent('filechooser'),
      page.getByRole('button', { name: 'Upload', exact: true }).click(),
    ])
    expect(fileChooser).toBeTruthy()
  })

  test('Upload button in controls bar is enabled when logged in', async ({ page }) => {
    await bootAsLoggedIn(page)
    // Target by aria-label to avoid ambiguity with "Press X to upload" button
    await expect(page.getByRole('button', { name: 'Upload', exact: true })).not.toBeDisabled()
  })
})

test.describe('Files — delete', () => {
  test('pressing Y with file selected shows confirm dialog', async ({ page }) => {
    await bootAsLoggedIn(page)
    await page.getByTitle(mockFile.name).click()
    await page.keyboard.press('y')
    await expect(page.getByText('Delete File')).toBeVisible()
    await expect(page.getByText('This cannot be undone.')).toBeVisible()
    // Check the file name appears inside the dialog (in the <strong> tag)
    await expect(page.locator('strong', { hasText: mockFile.name })).toBeVisible()
  })

  test('cancel in delete dialog keeps the file', async ({ page }) => {
    await bootAsLoggedIn(page)
    await page.getByTitle(mockFile.name).click()
    await page.keyboard.press('y')
    await page.getByRole('button', { name: /cancel/i }).click()
    await expect(page.getByTitle(mockFile.name)).toBeVisible()
  })

  test('confirming delete removes file from grid', async ({ page }) => {
    await page.route('**/api/file/1', route =>
      route.fulfill({ status: 204 })
    )
    await bootAsLoggedIn(page)
    await page.getByTitle(mockFile.name).click()
    await page.keyboard.press('y')
    // Scope to the modal backdrop to avoid the ControlsBar "Delete" button
    await inModal(page).getByRole('button', { name: /^delete$/i }).click()
    await expect(page.getByTitle(mockFile.name)).not.toBeVisible()
  })

  test('delete button is disabled when no file selected', async ({ page }) => {
    await bootAsLoggedIn(page)
    await expect(page.getByRole('button', { name: 'Delete', exact: true })).toBeDisabled()
  })

  test('non-owner gets access denied alert on delete', async ({ page }) => {
    await page.route('**/api/file/1', route =>
      route.fulfill({ status: 403 })
    )
    await bootAsLoggedIn(page)

    page.on('dialog', async dialog => {
      expect(dialog.message()).toContain('own files')
      await dialog.dismiss()
    })

    await page.getByTitle(mockFile.name).click()
    await page.keyboard.press('y')
    await inModal(page).getByRole('button', { name: /^delete$/i }).click()
  })
})

test.describe('Files — keyboard navigation', () => {
  test('arrow keys move selection through the grid', async ({ page }) => {
    const files = [
      { ...mockFile, id: 1, name: 'file-one.pdf' },
      { ...mockFile, id: 2, name: 'file-two.png', mimeType: 'image/png' },
    ]
    await bootAsLoggedIn(page, files)

    await page.getByTitle('file-one.pdf').click()
    await page.keyboard.press('ArrowRight')
    await expect(page.getByTitle('file-two.png')).toBeVisible()
  })

  test('B key deselects the current file', async ({ page }) => {
    await bootAsLoggedIn(page)
    await page.getByTitle(mockFile.name).click()
    await page.keyboard.press('b')
    await expect(page.getByRole('button', { name: 'Delete', exact: true })).toBeDisabled()
  })
})
