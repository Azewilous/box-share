import client from './client'

export type UploadStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'
export type FileVisibility = 'PRIVATE' | 'PUBLIC'

export interface FileRecord {
  id: number | null
  name: string
  size: number
  mimeType: string
  uploadedBy: string
  status: UploadStatus
  visibility: FileVisibility
  shareToken: string | null
  presignedUrl: string | null
  shared: boolean
}

export async function listMyFiles(): Promise<FileRecord[]> {
  const { data } = await client.get<FileRecord[]>('/api/file')
  return data
}

export async function initiateUpload(payload: Omit<FileRecord, 'id' | 'shareToken' | 'presignedUrl' | 'shared'>): Promise<FileRecord> {
  const { data } = await client.post<FileRecord>('/api/file', { ...payload, id: null, shareToken: null, presignedUrl: null, shared: false })
  return data
}

export async function getFileForView(filename: string): Promise<FileRecord> {
  const { data } = await client.get<FileRecord>(`/api/file/${encodeURIComponent(filename)}`)
  return data
}

export async function updateVisibility(id: number, visibility: FileVisibility): Promise<FileRecord> {
  const { data } = await client.patch<FileRecord>(`/api/file/${id}/visibility`, { visibility })
  return data
}

export async function deleteFile(id: number): Promise<void> {
  await client.delete(`/api/file/${id}`)
}

export async function shareFileWithUser(fileId: number, email: string): Promise<void> {
  await client.post(`/api/file/${fileId}/share`, null, { params: { email } })
}

export async function getPublicFile(shareToken: string): Promise<FileRecord> {
  const { data } = await client.get<FileRecord>(`/api/public/file/${shareToken}`)
  return data
}
