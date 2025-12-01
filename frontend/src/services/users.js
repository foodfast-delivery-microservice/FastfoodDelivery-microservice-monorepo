import http from './http'

// Helper để unwrap ApiResponse
const unwrapData = (responseData) => {
  if (responseData?.data !== undefined && responseData?.status !== undefined) {
    return responseData.data
  }
  return responseData
}

export const listUsers = async (params = {}) => {
  const { data } = await http.get('/users', { params })
  const unwrapped = unwrapData(data)
  if (Array.isArray(unwrapped)) {
    return unwrapped
  }
  return unwrapped?.content || []
}

export const getUserById = async (id) => {
  const { data } = await http.get(`/users/${id}`)
  return unwrapData(data)
}

export const patchUser = async (id, payload) => {
  const { data } = await http.patch(`/users/${id}`, payload)
  return unwrapData(data)
}

export const deleteUser = async (id) => {
  await http.delete(`/users/${id}`)
}







