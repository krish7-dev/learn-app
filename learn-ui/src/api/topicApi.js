import axiosClient from './axiosClient'

export const topicApi = {
  list: (page = 0, size = 50) =>
    axiosClient.get('/topics', { params: { page, size } }).then((r) => r.data),

  getById: (id) => axiosClient.get(`/topics/${id}`).then((r) => r.data),

  teachBack: (id, explanation) =>
    axiosClient.post(`/topics/${id}/teach-back`, { explanation }).then((r) => r.data),

  delete: (id) => axiosClient.delete(`/topics/${id}`).then((r) => r.data),
}
