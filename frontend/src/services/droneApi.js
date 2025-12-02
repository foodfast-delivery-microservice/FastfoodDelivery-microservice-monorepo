import http from './http'

const unwrap = (payload) => {
  if (payload && typeof payload === 'object') {
    const hasStatus = Object.prototype.hasOwnProperty.call(payload, 'status')
    const hasData = Object.prototype.hasOwnProperty.call(payload, 'data')
    if (hasStatus && hasData) {
      return payload.data
    }
  }
  return payload
}

const normalizeDrone = (drone = {}) => ({
  id: drone.id,
  serialNumber: drone.serialNumber,
  model: drone.model,
  batteryLevel: drone.batteryLevel,
  state: drone.state,
  currentLatitude: drone.currentLatitude,
  currentLongitude: drone.currentLongitude,
  baseLatitude: drone.baseLatitude,
  baseLongitude: drone.baseLongitude,
  weightCapacity: drone.weightCapacity,
})

export const getDrones = async () => {
  const { data } = await http.get('/drones')
  const list = unwrap(data)
  if (!Array.isArray(list)) return []
  return list.map(normalizeDrone)
}

export const getDroneById = async (id) => {
  if (!id) return null
  const { data } = await http.get(`/drones/${id}`)
  return normalizeDrone(unwrap(data))
}

export const assignDroneToOrder = async (payload) => {
  console.log('📡 [droneApi] Gọi POST /drones/assignments với payload:', payload)
  try {
    const response = await http.post('/drones/assignments', payload)
    console.log('📡 [droneApi] Response:', response)
    const data = response.data
    const unwrapped = unwrap(data)
    console.log('📡 [droneApi] Unwrapped data:', unwrapped)
    return unwrapped
  } catch (error) {
    console.error('❌ [droneApi] Lỗi khi gọi assignDroneToOrder:', error)
    console.error('❌ [droneApi] Error response:', error?.response?.data)
    throw error
  }
}

export const getMissionByOrderId = async (orderId) => {
  if (!orderId) return null
  const { data } = await http.get(`/missions/order/${orderId}`)
  return unwrap(data)
}

export const getTrackingByOrderId = async (orderId) => {
  if (!orderId) return null
  const { data } = await http.get(`/missions/order/${orderId}/tracking`)
  return unwrap(data)
}

export const getTrackingByMissionId = async (missionId) => {
  if (!missionId) return null
  const { data } = await http.get(`/missions/${missionId}/tracking`)
  return unwrap(data)
}

export const getMissionsByDroneId = async (droneId) => {
  if (!droneId) return []
  const { data } = await http.get(`/missions/drone/${droneId}`)
  const list = unwrap(data)
  if (!Array.isArray(list)) return []
  return list
}

export const getAllMissions = async () => {
  const { data } = await http.get('/missions')
  const list = unwrap(data)
  if (!Array.isArray(list)) return []
  return list
}

export const updateDroneBattery = async (id, level) => {
  if (!id) throw new Error('Drone ID is required')
  await http.put(`/drones/${id}/battery`, null, {
    params: { level },
  })
}

export const updateDroneState = async (id, state) => {
  if (!id) throw new Error('Drone ID is required')
  await http.put(`/drones/${id}/state`, null, {
    params: { state },
  })
}

