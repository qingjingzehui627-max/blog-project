import request from './request'

export const like = (targetId, type) => request.post('/like', null, { params: { targetId, type } })
export const unlike = (targetId, type) => request.delete('/like', { params: { targetId, type } })
export const getLikeStatus = (targetId, type) => request.get('/like/status', { params: { targetId, type } })
export const getLikeCount = (targetId, type) => request.get('/like/count', { params: { targetId, type } })
