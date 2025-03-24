import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';

// Define base API URL from environment or use default
const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

// Create axios instance with default config
const axiosInstance: AxiosInstance = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 seconds timeout
});

// Request interceptor to add auth token to requests
axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
    const token = localStorage.getItem('token');
    
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle common errors and refresh tokens
axiosInstance.interceptors.response.use(
  (response: AxiosResponse): AxiosResponse => {
    return response;
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig;
    
    // Handle 401 Unauthorized errors
    if (error.response?.status === 401) {
      // If not a refresh token request itself and we have a token
      if (
        originalRequest && 
        originalRequest.url !== '/api/auth/refresh' && 
        localStorage.getItem('token')
      ) {
        try {
          // Try to refresh the token
          const refreshResponse = await axios.post(
            `${API_URL}/api/auth/refresh`,
            { refreshToken: localStorage.getItem('refreshToken') }
          );
          
          const { token } = refreshResponse.data;
          
          // Update stored token
          localStorage.setItem('token', token);
          
          // Retry the original request with new token
          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${token}`;
          }
          
          return axiosInstance(originalRequest);
        } catch (refreshError) {
          // If refresh fails, log out the user
          localStorage.removeItem('token');
          localStorage.removeItem('refreshToken');
          
          // Redirect to login
          window.location.href = '/login';
          return Promise.reject(refreshError);
        }
      } else {
        // Clear any stored tokens and redirect to login
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        
        // Redirect to login if not already there
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
    }
    
    // Handle other errors
    return Promise.reject(error);
  }
);

export default axiosInstance; 