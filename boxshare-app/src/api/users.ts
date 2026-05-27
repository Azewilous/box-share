import client from './client'

export interface UserProfile {
  id: number
  firstName: string
  lastName: string
  email: string
}

export async function fetchProfile(email: string): Promise<UserProfile> {
  const { data } = await client.post<UserProfile>('/api/user', { email })
  return data
}

export async function updateProfile(profile: UserProfile): Promise<UserProfile> {
  const { data } = await client.put<UserProfile>('/api/user', profile)
  return data
}
