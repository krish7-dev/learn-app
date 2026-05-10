import axiosClient from './axiosClient'

export const timelineApi = {
  markItem: (itemId, data) =>
    axiosClient.put(`/timeline/${itemId}`, data).then((r) => r.data),
}
