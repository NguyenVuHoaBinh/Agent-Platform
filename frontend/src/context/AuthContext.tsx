import React, { createContext, useState, useEffect, useContext } from 'react';
import axios from 'axios';

// Define types for auth state and context
interface User {
  id: string;
  username: string;
  email: string;
  roles: string[];
}

interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  token: string | null;
  loading: boolean;
}

interface AuthContextType extends AuthState {
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  checkAuth: () => Promise<boolean>;
}

// Create the context with a default value
const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Create a provider component
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [authState, setAuthState] = useState<AuthState>({
    isAuthenticated: false,
    user: null,
    token: localStorage.getItem('token'),
    loading: true
  });

  // Check token on mount
  useEffect(() => {
    const checkToken = async () => {
      if (authState.token) {
        try {
          await checkAuth();
        } catch (error) {
          // Invalid token, clear it
          localStorage.removeItem('token');
          setAuthState({
            isAuthenticated: false,
            user: null,
            token: null,
            loading: false
          });
        }
      } else {
        setAuthState(prev => ({ ...prev, loading: false }));
      }
    };

    checkToken();
  }, []);

  const checkAuth = async (): Promise<boolean> => {
    if (!authState.token) return false;
    
    // DEVELOPMENT ONLY: Mock authentication check
    if (authState.token === 'mock-jwt-token') {
      const mockUser = {
        id: '1',
        username: 'admin',
        email: 'admin@example.com',
        roles: ['ADMIN']
      };
      
      setAuthState({
        isAuthenticated: true,
        user: mockUser,
        token: authState.token,
        loading: false
      });
      
      return true;
    }
    
    try {
      // Make an API call to verify the token (in production)
      const response = await axios.get('/api/auth/me', {
        headers: {
          Authorization: `Bearer ${authState.token}`
        }
      });
      
      const userData = response.data;
      
      setAuthState({
        isAuthenticated: true,
        user: userData,
        token: authState.token,
        loading: false
      });
      
      return true;
    } catch (error) {
      // If token validation fails, clear everything
      setAuthState({
        isAuthenticated: false,
        user: null,
        token: null,
        loading: false
      });
      localStorage.removeItem('token');
      return false;
    }
  };

  const login = async (username: string, password: string): Promise<void> => {
    try {
      // Set loading state
      setAuthState(prev => ({ ...prev, loading: true }));
      
      // DEVELOPMENT ONLY: Mock successful login with test credentials
      if (username === 'admin' && password === 'password') {
        console.log('Using mock authentication');
        
        const mockUser = {
          id: '1',
          username: 'admin',
          email: 'admin@example.com',
          roles: ['ADMIN', 'TEMPLATE_CREATE', 'TEMPLATE_READ', 'TEMPLATE_UPDATE', 'TEMPLATE_DELETE', 
                  'VERSION_CREATE', 'VERSION_READ', 'VERSION_UPDATE', 'PROMPT_TEST', 'PROMPT_READ']
        };
        const mockToken = 'mock-jwt-token';
        
        // Simulate network delay for realistic testing
        await new Promise(resolve => setTimeout(resolve, 500));
        
        localStorage.setItem('token', mockToken);
        
        setAuthState({
          isAuthenticated: true,
          user: mockUser,
          token: mockToken,
          loading: false
        });
        
        return;
      }
      
      // Regular API call (will be used in production)
      const response = await axios.post('/api/auth/login', {
        username,
        password
      });
      
      const { token, user } = response.data;
      
      // Save token to localStorage
      localStorage.setItem('token', token);
      
      // Update state
      setAuthState({
        isAuthenticated: true,
        user,
        token,
        loading: false
      });
    } catch (error) {
      setAuthState(prev => ({ ...prev, loading: false }));
      throw error;
    }
  };

  const logout = (): void => {
    // Clear token from localStorage
    localStorage.removeItem('token');
    
    // Reset state
    setAuthState({
      isAuthenticated: false,
      user: null,
      token: null,
      loading: false
    });
  };

  return (
    <AuthContext.Provider
      value={{
        ...authState,
        login,
        logout,
        checkAuth
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

// Create a hook for using the auth context
export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}; 