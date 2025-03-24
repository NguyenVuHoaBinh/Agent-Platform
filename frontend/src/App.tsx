import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { createTheme, ThemeProvider, CssBaseline, Box, CircularProgress, Typography } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './context/AuthContext';
import MainLayout from './layouts/MainLayout';
import LoginPage from './pages/auth/LoginPage';
import DashboardPage from './pages/dashboard/DashboardPage';
import TemplateListPage from './pages/templates/TemplateListPage';
import TemplateDetailPage from './pages/templates/TemplateDetailPage';
import TemplateFormPage from './pages/templates/TemplateFormPage';
import VersionListPage from './pages/versions/VersionListPage';
import VersionDetailPage from './pages/versions/VersionDetailPage';
import VersionFormPage from './pages/versions/VersionFormPage';
import VersionComparePage from './pages/versions/VersionComparePage';
import VersionHistoryPage from './pages/versions/VersionHistoryPage';

// Create React Query client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5 * 60 * 1000, // 5 minutes
    },
  },
});

// Create theme
const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
    background: {
      default: '#f5f5f5',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
    h4: {
      fontWeight: 600,
    },
    h6: {
      fontWeight: 600,
    },
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: 8,
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 12,
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          borderRadius: 8,
        },
      },
    },
  },
});

// Protected route component
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();
  
  // Show a better loading indicator
  if (loading) {
    return (
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh' 
      }}>
        <CircularProgress />
        <Typography variant="h6" sx={{ ml: 2 }}>
          Loading...
        </Typography>
      </Box>
    );
  }

  console.log('Authentication state:', isAuthenticated ? 'Authenticated' : 'Not authenticated');
  
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
};

const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              {/* Public routes */}
              <Route path="/login" element={<LoginPage />} />
              
              {/* Protected routes */}
              <Route
                path="/dashboard"
                element={
                  <ProtectedRoute>
                    <MainLayout>
                      <DashboardPage />
                    </MainLayout>
                  </ProtectedRoute>
                }
              />
              
              {/* Template routes */}
              <Route path="/templates">
                <Route
                  index
                  element={
                    <ProtectedRoute>
                      <MainLayout>
                        <TemplateListPage />
                      </MainLayout>
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="create"
                  element={
                    <ProtectedRoute>
                      <MainLayout>
                        <TemplateFormPage />
                      </MainLayout>
                    </ProtectedRoute>
                  }
                />
                <Route
                  path=":id"
                  element={
                    <ProtectedRoute>
                      <MainLayout>
                        <TemplateDetailPage />
                      </MainLayout>
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="edit/:id"
                  element={
                    <ProtectedRoute>
                      <MainLayout>
                        <TemplateFormPage />
                      </MainLayout>
                    </ProtectedRoute>
                  }
                />
              </Route>
              
              {/* Version management routes */}
              <Route path="/versions">
                <Route
                  index
                  element={
                    <ProtectedRoute>
                      <MainLayout>
                        <VersionListPage />
                      </MainLayout>
                    </ProtectedRoute>
                  }
                />
                <Route
                  path=":id"
                  element={
                    <ProtectedRoute>
                      <MainLayout>
                        <VersionDetailPage />
                      </MainLayout>
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="create"
                  element={
                    <ProtectedRoute>
                      <MainLayout>
                        <VersionFormPage />
                      </MainLayout>
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="create/:templateId"
                  element={
                    <ProtectedRoute>
                      <MainLayout>
                        <VersionFormPage />
                      </MainLayout>
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="edit/:id"
                  element={
                    <ProtectedRoute>
                      <MainLayout>
                        <VersionFormPage />
                      </MainLayout>
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="branch/:id"
                  element={
                    <ProtectedRoute>
                      <MainLayout>
                        <VersionFormPage />
                      </MainLayout>
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="compare"
                  element={
                    <ProtectedRoute>
                      <MainLayout>
                        <VersionComparePage />
                      </MainLayout>
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="history/:id"
                  element={
                    <ProtectedRoute>
                      <MainLayout>
                        <VersionHistoryPage />
                      </MainLayout>
                    </ProtectedRoute>
                  }
                />
              </Route>
              
              <Route
                path="/testing/*"
                element={
                  <ProtectedRoute>
                    <MainLayout>
                      <div>Testing page (to be implemented)</div>
                    </MainLayout>
                  </ProtectedRoute>
                }
              />
              
              <Route
                path="/optimization/*"
                element={
                  <ProtectedRoute>
                    <MainLayout>
                      <div>Optimization page (to be implemented)</div>
                    </MainLayout>
                  </ProtectedRoute>
                }
              />
              
              {/* Redirect from root to dashboard */}
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              
              {/* 404 route */}
              <Route path="*" element={<div>Page not found</div>} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
};

export default App;
