import request from './request'

export const uploadImage = file => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/upload/image', form, { headers: { 'Content-Type': 'multipart/form-data' } })
}
