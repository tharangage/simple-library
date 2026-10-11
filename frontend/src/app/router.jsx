import { createBrowserRouter } from 'react-router'
import { RequireAuth } from '../auth/RequireAuth.jsx'
import { SigninCallbackPage } from '../auth/SigninCallbackPage.jsx'
import { BooksPage } from '../features/books/BooksPage.jsx'
import { RegisterPage } from '../features/registration/RegisterPage.jsx'
import { HomePage } from './HomePage.jsx'
import { Layout } from './Layout.jsx'
import { NotFoundPage } from './NotFoundPage.jsx'

/** URL → page. Pages are rendered inside <Layout>, which supplies the header (except /register, see below). */
export const router = createBrowserRouter([
  // Full-screen page from the Figma design: it has its own two-panel frame, so no app header.
  { path: 'register', element: <RegisterPage /> },
  {
    element: <Layout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'auth/callback', element: <SigninCallbackPage /> },
      {
        path: 'books',
        element: (
          <RequireAuth>
            <BooksPage />
          </RequireAuth>
        ),
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
