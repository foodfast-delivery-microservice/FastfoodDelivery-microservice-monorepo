import http from './http'

export const login = async (credentials) => {
  const { data } = await http.post('/auth/login', credentials)
  return data
}

export const register = async (payload) => {
  const { data } = await http.post('/auth/register', payload)
  return data
}

export const getProfile = async () => {
  const { data } = await http.get('/users/me')
  return data
}

export const updateProfile = async (partial) => {
  const { data } = await http.patch('/users/me', partial)
  return data
}


