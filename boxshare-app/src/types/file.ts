export interface FileSlot {
  id: number
  emoji: string
  name: string
  type: string
  size: string
  visibility: 'PRIVATE' | 'PUBLIC'
  shareToken: string | null
}
