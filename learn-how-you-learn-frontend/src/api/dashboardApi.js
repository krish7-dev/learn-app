import axiosClient from './axiosClient'

export const dashboardApi = {
  getToday: () => axiosClient.get('/dashboard/today').then((r) => r.data),
}
