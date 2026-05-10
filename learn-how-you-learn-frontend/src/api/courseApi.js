import axiosClient from './axiosClient'

export const courseApi = {
  list: (page = 0, size = 20) =>
    axiosClient.get('/courses', { params: { page, size } }).then((r) => r.data),

  getById: (id) => axiosClient.get(`/courses/${id}`).then((r) => r.data),

  create: (data) => axiosClient.post('/courses', data).then((r) => r.data),

  update: (id, data) => axiosClient.put(`/courses/${id}`, data).then((r) => r.data),

  delete: (id) => axiosClient.delete(`/courses/${id}`).then((r) => r.data),
}
