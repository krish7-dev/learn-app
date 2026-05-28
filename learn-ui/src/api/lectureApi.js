import axiosClient from './axiosClient'

export const lectureApi = {
  listByCourse: (courseId, page = 0, size = 50) =>
    axiosClient
      .get(`/courses/${courseId}/lectures`, { params: { page, size } })
      .then((r) => r.data),

  getById: (id) => axiosClient.get(`/lectures/${id}`).then((r) => r.data),

  create: (courseId, data) =>
    axiosClient.post(`/courses/${courseId}/lectures`, data).then((r) => r.data),

  bulkCreate: (courseId, data) =>
    axiosClient.post(`/courses/${courseId}/lectures/bulk`, data).then((r) => r.data),

  parseList: (courseId, rawText) =>
    axiosClient.post(`/courses/${courseId}/lectures/parse-list`, { rawText }).then((r) => r.data),

  update: (id, data) => axiosClient.put(`/lectures/${id}`, data).then((r) => r.data),

  delete: (id) => axiosClient.delete(`/lectures/${id}`).then((r) => r.data),

  generateNotes: (id) =>
    axiosClient.post(`/lectures/${id}/generate-notes`).then((r) => r.data),

  retryParseNotes: (id) =>
    axiosClient.post(`/lectures/${id}/retry-parse-notes`).then((r) => r.data),

  importNotes: (id, content) =>
    axiosClient.post(`/lectures/${id}/import-notes`, { content }).then((r) => r.data),

  cleanTranscript: (id) =>
    axiosClient.post(`/lectures/${id}/clean-transcript`).then((r) => r.data),

  importNotesBatch: (courseId, data) =>
    axiosClient.post(`/courses/${courseId}/import-notes-batch`, data).then((r) => r.data),

  chat: (id, message) =>
    axiosClient.post(`/lectures/${id}/chat`, { message }).then((r) => r.data),

  logConfusion: (id, data) =>
    axiosClient.post(`/lectures/${id}/confusions`, data).then((r) => r.data),

  addToNotes: (id, content) =>
    axiosClient.patch(`/lectures/${id}/notes/additions`, { content }).then((r) => r.data),

  updateNotesContent: (id, fullCleanNotes) =>
    axiosClient.patch(`/lectures/${id}/notes/content`, { fullCleanNotes }).then((r) => r.data),
}
