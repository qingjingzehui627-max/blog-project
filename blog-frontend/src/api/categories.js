export const getCategories = async () => {
  const response = await fetch('/api/categories', {
    headers: {
      Accept: 'application/json'
    }
  })

  if (!response.ok) {
    throw new Error('Failed to fetch categories')
  }

  return response.json()
}
