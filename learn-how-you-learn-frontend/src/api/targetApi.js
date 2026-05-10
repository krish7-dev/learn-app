import axiosClient from './axiosClient'

export const targetApi = {
  list: () => axiosClient.get('/targets').then((r) => r.data),

  getById: (id) => axiosClient.get(`/targets/${id}`).then((r) => r.data),

  create: (data) => axiosClient.post('/targets', data).then((r) => r.data),

  update: (id, data) => axiosClient.put(`/targets/${id}`, data).then((r) => r.data),

  delete: (id) => axiosClient.delete(`/targets/${id}`).then((r) => r.data),

  generateTimeline: (id) =>
    axiosClient.post(`/targets/${id}/generate-timeline`).then((r) => r.data),

  getToday: (id) => axiosClient.get(`/targets/${id}/timeline/today`).then((r) => r.data),

  getWeek: (id) => axiosClient.get(`/targets/${id}/timeline/week`).then((r) => r.data),

  deleteTimeline: (id) => axiosClient.delete(`/targets/${id}/timeline`).then((r) => r.data),
}
