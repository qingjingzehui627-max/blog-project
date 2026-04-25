import request from './request'

export const getAiConfig = () => request.get('/ai/config')

export const getAiQuota = () => request.get('/ai/quota')

export const chatWithAi = data => request.post('/ai/chat', data)
