import axiosClient from './axiosClient'

export const revisionApi = {
  getPending: () => axiosClient.get('/revision').then((r) => r.data),

  update: (id, status) =>
    axiosClient.put(`/revision/${id}`, { status }).then((r) => r.data),
}
