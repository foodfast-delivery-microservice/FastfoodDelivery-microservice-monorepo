const IS_DEMO = import.meta.env.VITE_DEMO_MODE === 'true'

export const assetUrl = (path) => `${import.meta.env.BASE_URL}${String(path || '').replace(/^\/+/, '')}`

export const resolveImageUrl = (source) => {
  if (!source) return null
  if (/^https?:\/\//i.test(source)) return source
  if (IS_DEMO) return assetUrl(source)

  const normalized = source.startsWith('/') ? source : `/${source}`
  return `http://localhost:8089${normalized}`
}

