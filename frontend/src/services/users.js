import http from './http'

export const listUsers = async (params = {}) => {
  const { data } = await http.get('/users', { params })
  return data
}

export const getUserById = async (id) => {
  const { data } = await http.get(`/users/${id}`)
  return data
}

export const patchUser = async (id, payload) => {
  const { data } = await http.patch(`/users/${id}`, payload)
  return data
}

export const deleteUser = async (id) => {
  await http.delete(`/users/${id}`)
}


