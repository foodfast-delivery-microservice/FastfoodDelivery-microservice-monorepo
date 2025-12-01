import http from './http'

export const getUserAddresses = async () => {
  const res = await http.get('/addresses')
  return res.data
}

export const createUserAddress = async (payload) => {
  const res = await http.post('/addresses', payload)
  return res.data
}

export const updateAddressLocation = async (addressId, payload) => {
  const res = await http.patch(`/addresses/${addressId}/location`, payload)
  return res.data
}

export const getProvinces = async () => {
  const res = await http.get('/addresses/provinces')
  return res.data
}

export const getCommunes = async (provinceCode) => {
  const res = await http.get(`/addresses/provinces/${provinceCode}/communes`)
  return res.data
}



