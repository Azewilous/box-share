import type { FileSlot } from '../types/file'

const mockSlots: (FileSlot | null)[] = [
  { emoji: '📄', name: 'report.pdf',   type: 'PDF',         size: '2.4 MB'  },
  { emoji: '🖼️', name: 'photo.png',    type: 'Image',       size: '3.8 MB'  },
  { emoji: '🎵', name: 'track.mp3',    type: 'Audio',       size: '8.1 MB'  },
  { emoji: '📦', name: 'archive.zip',  type: 'Archive',     size: '14.2 MB' },
  { emoji: '📝', name: 'notes.txt',    type: 'Text',        size: '12 KB'   },
  { emoji: '🎬', name: 'clip.mp4',     type: 'Video',       size: '42.0 MB' },
  null,
  { emoji: '📊', name: 'data.csv',     type: 'Spreadsheet', size: '560 KB'  },
  { emoji: '🖼️', name: 'banner.jpg',   type: 'Image',       size: '1.1 MB'  },
  null,
  null,
  { emoji: '📄', name: 'resume.pdf',   type: 'PDF',         size: '380 KB'  },
  { emoji: '🎵', name: 'mix.mp3',      type: 'Audio',       size: '11.4 MB' },
  null,
  null,
  null,
  { emoji: '📦', name: 'backup.zip',   type: 'Archive',     size: '98.3 MB' },
  null, null, null, null, null, null, null, null, null, null, null, null, null,
]

export default mockSlots
