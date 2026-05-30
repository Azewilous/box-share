import type { FileRecord } from '../api/files'
import type { FileSlot } from '../types/file'

export function emojiForMime(mimeType: string): string {
  if (mimeType.startsWith('image/'))  return '🖼️'
  if (mimeType.startsWith('video/'))  return '🎬'
  if (mimeType.startsWith('audio/'))  return '🎵'
  if (mimeType === 'application/pdf') return '📄'
  if (mimeType.includes('zip') || mimeType.includes('archive') || mimeType.includes('tar') || mimeType.includes('gzip')) return '📦'
  if (mimeType.includes('json') || mimeType.includes('xml') || mimeType.includes('yaml')) return '🗂️'
  if (mimeType.includes('html') || mimeType.includes('css') || mimeType.includes('javascript') || mimeType.includes('typescript')) return '🌐'
  if (mimeType.includes('spreadsheet') || mimeType.includes('csv') || mimeType.includes('excel')) return '📊'
  if (mimeType.startsWith('text/'))   return '📝'
  if (mimeType.includes('word') || mimeType.includes('document')) return '📃'
  if (mimeType.includes('font')) return '🔤'
  if (mimeType.includes('sql') || mimeType.includes('database')) return '🗄️'
  return '📁'
}

export function typeLabel(mimeType: string): string {
  if (mimeType.startsWith('image/'))  return 'Image'
  if (mimeType.startsWith('video/'))  return 'Video'
  if (mimeType.startsWith('audio/'))  return 'Audio'
  if (mimeType === 'application/pdf') return 'PDF'
  if (mimeType.includes('zip') || mimeType.includes('archive') || mimeType.includes('tar')) return 'Archive'
  if (mimeType.includes('spreadsheet') || mimeType.includes('csv')) return 'Spreadsheet'
  if (mimeType.startsWith('text/'))   return 'Text'
  if (mimeType.includes('word') || mimeType.includes('document')) return 'Document'
  return 'File'
}

export function formatSize(bytes: number): string {
  if (bytes < 1024)             return `${bytes} B`
  if (bytes < 1024 * 1024)      return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 ** 3)        return `${(bytes / 1024 ** 2).toFixed(1)} MB`
  return `${(bytes / 1024 ** 3).toFixed(1)} GB`
}

/** In dev, rewrite LocalStack presigned URLs to go through the Vite proxy so any device on the network can fetch them. */
export function resolvePresignedUrl(url: string | null): string | null {
  if (!url) return null
  if (import.meta.env.DEV) {
    return url.replace(/^https?:\/\/localhost:4566/, '/localstack')
  }
  return url
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
    shared:     file.shared,
  }
}
