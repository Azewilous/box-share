import type { FileSlot } from '../types/file'

const mockSlots: (FileSlot | null)[] = [
  { id: -1, emoji: '📄', name: 'report.pdf',   type: 'PDF',         size: '2.4 MB',  visibility: 'PRIVATE', shareToken: null, shared: false },
  { id: -2, emoji: '🖼️', name: 'photo.png',    type: 'Image',       size: '3.8 MB',  visibility: 'PUBLIC',  shareToken: 'demo', shared: false },
  { id: -3, emoji: '🎵', name: 'track.mp3',    type: 'Audio',       size: '8.1 MB',  visibility: 'PRIVATE', shareToken: null, shared: false },
  { id: -4, emoji: '📦', name: 'archive.zip',  type: 'Archive',     size: '14.2 MB', visibility: 'PRIVATE', shareToken: null, shared: false },
  { id: -5, emoji: '📝', name: 'notes.txt',    type: 'Text',        size: '12 KB',   visibility: 'PRIVATE', shareToken: null, shared: false },
  { id: -6, emoji: '🎬', name: 'clip.mp4',     type: 'Video',       size: '42.0 MB', visibility: 'PRIVATE', shareToken: null, shared: false },
  null,
  { id: -7, emoji: '📊', name: 'data.csv',     type: 'Spreadsheet', size: '560 KB',  visibility: 'PRIVATE', shareToken: null, shared: false },
  { id: -8, emoji: '🖼️', name: 'banner.jpg',   type: 'Image',       size: '1.1 MB',  visibility: 'PUBLIC',  shareToken: 'demo2', shared: false },
  null, null,
  { id: -9,  emoji: '📄', name: 'resume.pdf',  type: 'PDF',         size: '380 KB',  visibility: 'PRIVATE', shareToken: null, shared: false },
  { id: -10, emoji: '🎵', name: 'mix.mp3',     type: 'Audio',       size: '11.4 MB', visibility: 'PRIVATE', shareToken: null, shared: false },
  null, null, null,
  { id: -11, emoji: '📦', name: 'backup.zip',  type: 'Archive',     size: '98.3 MB', visibility: 'PRIVATE', shareToken: null, shared: false },
  null, null, null, null, null, null, null, null, null, null, null, null, null,
]

export default mockSlots
