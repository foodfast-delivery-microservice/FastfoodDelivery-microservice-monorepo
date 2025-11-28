import { fetchProducts, fetchProductById, createProduct, updateProduct, deleteProduct } from '../services/products'
import { listRestaurants, appendLocalRestaurant } from '../services/restaurants'
import { listDrones, upsertDrone, deleteDrone } from '../services/drones'
import {
  listMyOrders,
  listMerchantOrders,
  listAllOrders,
  createOrder,
  getOrderById,
  updateOrderStatus,
} from '../services/orders'
import { getUsers as adminGetUsers, updateUserStatus as adminUpdateUserStatus } from '../services/admin'
import { register as registerUser } from '../services/auth'

const createDocSnapshot = (item) => ({
  id: item?.id ?? item?.orderCode ?? String(Date.now()),
  data: () => item,
})

const createQuerySnapshot = (items) => ({
  docs: items.map(createDocSnapshot),
  size: items.length,
  empty: items.length === 0,
  forEach(cb) {
    items.forEach((item) => cb(createDocSnapshot(item)))
  },
})

const collection = (_db, name) => ({ type: 'collection', name })

const doc = (_db, name, id) => ({ type: 'doc', name, id })

const where = (field, operator, value) => ({ type: 'where', field, operator, value })

const orderBy = (field, direction) => ({ type: 'orderBy', field, direction })

const limit = (value) => ({ type: 'limit', value })

const query = (collectionRef, ...clauses) => ({
  ...collectionRef,
  clauses,
})

const serverTimestamp = () => new Date().toISOString()

const matchWhere = (item, clause) => {
  if (!clause) return true
  const { field, operator, value } = clause
  const target = field === '__name__' ? item.id : item[field]
  switch (operator) {
    case '==':
      return String(target) === String(value)
    case '!=':
      return String(target) !== String(value)
    case 'in':
      return Array.isArray(value) && value.includes(target)
    default:
      return true
  }
}

const applyClauses = (items, clauses = []) => {
  let result = [...items]
  clauses.forEach((clause) => {
    if (clause?.type === 'where') {
      result = result.filter((item) => matchWhere(item, clause))
    }
    if (clause?.type === 'orderBy') {
      result = result.sort((a, b) => {
        const av = a[clause.field]
        const bv = b[clause.field]
        if (av === bv) return 0
        if (clause.direction === 'desc') {
          return av < bv ? 1 : -1
        }
        return av > bv ? 1 : -1
      })
    }
    if (clause?.type === 'limit') {
      result = result.slice(0, clause.value)
    }
  })
  return result
}

const resolveClauses = (ref) => ref?.clauses ?? []

const getDocs = async (ref) => {
  const clauses = resolveClauses(ref)
  switch (ref.name) {
    case 'products': {
      const products = await fetchProducts()
      return createQuerySnapshot(applyClauses(products, clauses))
    }
    case 'restaurants': {
      const restaurants = await listRestaurants()
      return createQuerySnapshot(applyClauses(restaurants, clauses))
    }
    case 'orders': {
      const userClause = clauses.find((c) => c.field === 'userId')
      const merchantClause = clauses.find((c) => c.field === 'restaurantId')
      let orders = []
      if (userClause) {
        orders = await listMyOrders()
      } else if (merchantClause) {
        const response = await listMerchantOrders()
        orders = Array.isArray(response?.content) ? response.content : response ?? []
      } else {
        const response = await listAllOrders()
        orders = response?.content ?? response ?? []
      }
      return createQuerySnapshot(applyClauses(orders, clauses))
    }
    case 'users': {
      const response = await adminGetUsers({ size: 200 })
      const users = response?.content ?? []
      return createQuerySnapshot(applyClauses(users, clauses))
    }
    case 'drones': {
      const drones = await listDrones()
      return createQuerySnapshot(applyClauses(drones, clauses))
    }
    default:
      return createQuerySnapshot([])
  }
}

const getDoc = async (ref) => {
  switch (ref.name) {
    case 'orders': {
      const order = await getOrderById(ref.id)
      return {
        id: order.id,
        exists: () => Boolean(order),
        data: () => order,
      }
    }
    case 'products': {
      const product = await fetchProductById(ref.id)
      return {
        id: product.id,
        exists: () => Boolean(product),
        data: () => product,
      }
    }
    default:
      return {
        id: ref.id,
        exists: () => false,
        data: () => null,
      }
  }
}

const addDoc = async (ref, payload) => {
  switch (ref.name) {
    case 'orders': {
      const created = await createOrder(payload)
      return { id: created.id }
    }
    case 'products': {
      const created = await createProduct(payload)
      return { id: created.id }
    }
    case 'drones': {
      const created = await upsertDrone(payload)
      return { id: created.id }
    }
    default:
      return { id: String(Date.now()) }
  }
}

const setDoc = async (ref, payload) => {
  switch (ref.name) {
    case 'drones':
      await upsertDrone({ ...payload, id: ref.id })
      break
    case 'restaurants':
      appendLocalRestaurant({ ...payload, id: ref.id })
      break
    case 'users': {
      const username = payload.phonenumber ?? payload.username ?? `merchant_${ref.id}`
      const email = payload.email ?? `${username}@merchant.local`
      const password = payload.password ?? '123456'
      const role = (payload.role ?? 'MERCHANT').toUpperCase()
      const restaurantName = payload.restaurantName || payload.name
      const restaurantAddress = payload.restaurantAddress || payload.address
      const restaurantImage = payload.restaurantImage || payload.image
      await registerUser({
        username, email, password, role,
        restaurantName, restaurantAddress, restaurantImage
      })
      break
    }
    default:
      break
  }
}

const updateDoc = async (ref, payload) => {
  switch (ref.name) {
    case 'orders':
      if (payload.status) {
        await updateOrderStatus(ref.id, payload.status)
      }
      break
    case 'products':
      await updateProduct(ref.id, payload)
      break
    case 'users':
      if (typeof payload.active === 'boolean') {
        await adminUpdateUserStatus(ref.id, payload.active)
      }
      break
    case 'drones':
      await upsertDrone({ ...payload, id: ref.id })
      break
    default:
      break
  }
}

const deleteDoc = async (ref) => {
  switch (ref.name) {
    case 'products':
      await deleteProduct(ref.id)
      break
    case 'drones':
      await deleteDrone(ref.id)
      break
    default:
      break
  }
}

const onSnapshot = async (ref, callback) => {
  const snapshot = await getDocs(ref)
  callback(snapshot)
  return () => { }
}

export {
  collection,
  doc,
  query,
  where,
  orderBy,
  limit,
  getDocs,
  getDoc,
  addDoc,
  setDoc,
  updateDoc,
  deleteDoc,
  serverTimestamp,
  onSnapshot,
}

