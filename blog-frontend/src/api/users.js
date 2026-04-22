import request from './request'

export const getUserById = id => request.get(`/user/${id}`)
export const updateProfile = data => request.put('/user/profile', data)
export const getPostsByUser = userId => request.get(`/posts/user/${userId}`)
