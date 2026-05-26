import { create } from 'zustand'

export const useAppStore = create((set) => ({
  sidebarOpen: window.innerWidth > 768,
  toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),

  activeModal: null,
  openModal: (name) => set({ activeModal: name }),
  closeModal: () => set({ activeModal: null }),
}))
