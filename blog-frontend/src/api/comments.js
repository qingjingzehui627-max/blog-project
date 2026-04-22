import request from './request'

export const getComments = postId => request.get(`/comments/${postId}`)
export const createComment = data => request.post('/comments', data)
