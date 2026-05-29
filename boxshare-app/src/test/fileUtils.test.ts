import { describe, it, expect } from 'vitest'
import { emojiForMime, typeLabel, formatSize, fileRecordToSlot } from '../utils/fileUtils'
import type { FileRecord } from '../api/files'

describe('emojiForMime', () => {
  it.each([
    ['image/png',               '🖼️'],
    ['image/jpeg',              '🖼️'],
    ['video/mp4',               '🎬'],
    ['audio/mpeg',              '🎵'],
    ['application/pdf',         '📄'],
    ['application/zip',         '📦'],
    ['application/x-tar',       '📦'],
    ['application/gzip',        '📦'],
    ['application/json',        '🗂️'],
    ['application/xml',         '🗂️'],
    ['text/yaml',               '🗂️'],
    ['text/html',               '🌐'],
    ['text/css',                '🌐'],
    ['application/javascript',  '🌐'],
    ['text/plain',              '📝'],
    ['application/vnd.ms-excel','📊'],
    ['text/csv',                '📊'],
    ['application/msword',      '📃'],
    ['font/woff2',              '🔤'],
    ['application/sql',         '🗄️'],
    ['application/octet-stream','📁'],
  ])('returns correct emoji for %s', (mime, expected) => {
    expect(emojiForMime(mime)).toBe(expected)
  })
})

describe('typeLabel', () => {
  it.each([
    ['image/png',               'Image'],
    ['video/mp4',               'Video'],
    ['audio/mpeg',              'Audio'],
    ['application/pdf',         'PDF'],
    ['application/zip',         'Archive'],
    ['text/plain',              'Text'],
    ['text/csv',                'Spreadsheet'],
    ['application/msword',      'Document'],
    ['application/octet-stream','File'],
  ])('returns correct label for %s', (mime, expected) => {
    expect(typeLabel(mime)).toBe(expected)
  })
})

describe('formatSize', () => {
  it('formats bytes', () => expect(formatSize(512)).toBe('512 B'))
  it('formats kilobytes', () => expect(formatSize(2048)).toBe('2.0 KB'))
  it('formats megabytes', () => expect(formatSize(5 * 1024 * 1024)).toBe('5.0 MB'))
  it('formats gigabytes', () => expect(formatSize(2 * 1024 ** 3)).toBe('2.0 GB'))
  it('formats boundary: exactly 1 KB', () => expect(formatSize(1024)).toBe('1.0 KB'))
})

describe('fileRecordToSlot', () => {
  const base: FileRecord = {
    id: 42,
    name: 'photo.jpg',
    size: 204800,
    mimeType: 'image/jpeg',
    uploadedBy: 'ash@example.com',
    status: 'COMPLETED',
    visibility: 'PRIVATE',
    shareToken: null,
    presignedUrl: null,
    shared: false,
  }

  it('maps id, name, visibility and shareToken', () => {
    const slot = fileRecordToSlot(base)
    expect(slot.id).toBe(42)
    expect(slot.name).toBe('photo.jpg')
    expect(slot.visibility).toBe('PRIVATE')
    expect(slot.shareToken).toBeNull()
  })

  it('derives emoji from mimeType', () => {
    expect(fileRecordToSlot(base).emoji).toBe('🖼️')
  })

  it('derives type label from mimeType', () => {
    expect(fileRecordToSlot(base).type).toBe('Image')
  })

  it('formats size', () => {
    expect(fileRecordToSlot(base).size).toBe('200.0 KB')
  })

  it('passes shared flag through', () => {
    expect(fileRecordToSlot({ ...base, shared: true }).shared).toBe(true)
    expect(fileRecordToSlot(base).shared).toBe(false)
  })

  it('passes shareToken through for public files', () => {
    const slot = fileRecordToSlot({ ...base, visibility: 'PUBLIC', shareToken: 'abc-123' })
    expect(slot.shareToken).toBe('abc-123')
    expect(slot.visibility).toBe('PUBLIC')
  })
})
