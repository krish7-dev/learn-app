import axiosClient from './axiosClient'

export const treeApi = {
  getFullTree:   ()       => axiosClient.get('/learning-tree').then(r => r.data),
  getNodeDetail: (nodeId) => axiosClient.get(`/learning-tree/${nodeId}`).then(r => r.data),
  backfill:      ()       => axiosClient.post('/learning-tree/backfill').then(r => r.data),
  reset:         ()       => axiosClient.post('/learning-tree/reset').then(r => r.data),
}
