import request from './request'

export const addFavorite = postId => request.post('/favorites', null, { params: { postId } })
export const removeFavorite = postId => request.delete('/favorites', { params: { postId } })
export const getFavoriteStatus = postId => request.get('/favorites/status', { params: { postId } })
export const getFavorites = () => request.get('/favorites')
