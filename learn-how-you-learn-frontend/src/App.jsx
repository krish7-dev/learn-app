import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import './App.css'
import Sidebar from './components/layout/Sidebar'
import TopNav from './components/layout/TopNav'
import DashboardPage from './pages/DashboardPage'
import CoursesPage from './pages/CoursesPage'
import CourseDetailPage from './pages/CourseDetailPage'
import LecturePage from './pages/LecturePage'
import TopicListPage from './pages/TopicListPage'
import TopicDetailPage from './pages/TopicDetailPage'
import RevisionPage from './pages/RevisionPage'

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 30_000 } },
})

function Layout({ title, children }) {
  return (
    <div className="main-area">
      <TopNav title={title} />
      <div className="page-content">{children}</div>
    </div>
  )
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <div className="app-shell">
          <Sidebar />
          <Routes>
            <Route path="/" element={<Layout title="Dashboard"><DashboardPage /></Layout>} />
            <Route path="/courses" element={<Layout title="Courses"><CoursesPage /></Layout>} />
            <Route path="/courses/:id" element={<Layout><CourseDetailPage /></Layout>} />
            <Route path="/lectures/:id" element={<Layout><LecturePage /></Layout>} />
            <Route path="/topics" element={<Layout title="Topics"><TopicListPage /></Layout>} />
            <Route path="/topics/:id" element={<Layout><TopicDetailPage /></Layout>} />
            <Route path="/revision" element={<Layout title="Revision Queue"><RevisionPage /></Layout>} />
          </Routes>
        </div>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
