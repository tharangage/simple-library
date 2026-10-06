import { createBrowserRouter } from 'react-router'
import { RequireAuth } from '../auth/RequireAuth.jsx'
import { SigninCallbackPage } from '../auth/SigninCallbackPage.jsx'
import { BooksPage } from '../features/books/BooksPage.jsx'
import { HomePage } from './HomePage.jsx'
import { Layout } from './Layout.jsx'
import { NotFoundPage } from './NotFoundPage.jsx'

/** URL → page. Every page is rendered inside <Layout>, which supplies the header. */
export const router = createBrowserRouter([
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
