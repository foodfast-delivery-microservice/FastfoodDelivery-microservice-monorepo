import sharedDb from '../../shared/db.json'

const STORAGE_KEY = 'mock_drones'

const loadState = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return JSON.parse(raw)
  } catch {
    // ignore
  }
  return sharedDb.drones ?? []
}

const saveState = (drones) => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(drones))
  } catch {
    // ignore
  }
}

export const listDrones = async () => {
  return loadState()
}

export const upsertDrone = async (drone) => {
  const drones = loadState()
  const idx = drones.findIndex((d) => d.id === drone.id)
  if (idx >= 0) {
    drones[idx] = { ...drones[idx], ...drone }
  } else {
    drones.push({ ...drone, id: drone.id ?? String(Date.now()) })
  }
  saveState(drones)
  return drones[idx >= 0 ? idx : drones.length - 1]
}

export const deleteDrone = async (id) => {
  const drones = loadState().filter((d) => d.id !== id)
  saveState(drones)
}

