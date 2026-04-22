import request from './request'

export const getPosts = (page = 1, size = 10) => request.get('/posts', { params: { page, size } })
export const getPostById = id => request.get(`/posts/${id}`)
export const createPost = data => request.post('/posts', data)
export const updatePost = (id, data) => request.put(`/posts/${id}`, data)
export const deletePost = id => request.delete(`/posts/${id}`)
