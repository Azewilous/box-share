import { describe, it, expect } from 'vitest'
import mockSlots from '../data/mockSlots'

describe('mockSlots', () => {
  it('has exactly 30 entries (BOX_SIZE)', () => {
    expect(mockSlots).toHaveLength(30)
  })

  it('all non-null slots have required fields', () => {
    const filled = mockSlots.filter(Boolean)
    for (const slot of filled) {
      expect(slot!.id).toBeDefined()
      expect(slot!.name).toBeTruthy()
      expect(slot!.emoji).toBeTruthy()
      expect(slot!.type).toBeTruthy()
      expect(slot!.size).toBeTruthy()
      expect(['PRIVATE', 'PUBLIC']).toContain(slot!.visibility)
      expect(typeof slot!.shared).toBe('boolean')
    }
  })

  it('all non-null slot ids are negative (demo data, not real)', () => {
    mockSlots.filter(Boolean).forEach(slot => {
      expect(slot!.id).toBeLessThan(0)
    })
  })
})
