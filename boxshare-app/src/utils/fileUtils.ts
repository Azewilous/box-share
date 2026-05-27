import type { FileRecord } from '../api/files'
import type { FileSlot } from '../types/file'

export function emojiForMime(mimeType: string): string {
  if (mimeType.startsWith('image/'))  return '🖼️'
  if (mimeType.startsWith('video/'))  return '🎬'
  if (mimeType.startsWith('audio/'))  return '🎵'
  if (mimeType === 'application/pdf') return '📄'
  if (mimeType.includes('zip') || mimeType.includes('archive') || mimeType.includes('tar')) return '📦'
  if (mimeType.startsWith('text/'))   return '📝'
  if (mimeType.includes('spreadsheet') || mimeType.includes('csv') || mimeType.includes('excel')) return '📊'
  if (mimeType.includes('word') || mimeType.includes('document')) return '📃'
  return '📁'
}

export function typeLabel(mimeType: string): string {
  if (mimeType.startsWith('image/'))  return 'Image'
  if (mimeType.startsWith('video/'))  return 'Video'
  if (mimeType.startsWith('audio/'))  return 'Audio'
  if (mimeType === 'application/pdf') return 'PDF'
  if (mimeType.includes('zip') || mimeType.includes('archive') || mimeType.includes('tar')) return 'Archive'
  if (mimeType.startsWith('text/'))   return 'Text'
  if (mimeType.includes('spreadsheet') || mimeType.includes('csv')) return 'Spreadsheet'
  if (mimeType.includes('word') || mimeType.includes('document')) return 'Document'
  return 'File'
}

export function formatSize(bytes: number): string {
  if (bytes < 1024)             return `${bytes} B`
  if (bytes < 1024 * 1024)      return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 ** 3)        return `${(bytes / 1024 ** 2).toFixed(1)} MB`
  return `${(bytes / 1024 ** 3).toFixed(1)} GB`
}

export function fileRecordToSlot(file: FileRecord): FileSlot {
  return {
    id:         file.id!,
    emoji:      emojiForMime(file.mimeType),
    name:       file.name,
    type:       typeLabel(file.mimeType),
    size:       formatSize(file.size),
    visibility: file.visibility,
    shareToken: file.shareToken,
  }
}
