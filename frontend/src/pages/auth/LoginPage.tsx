import React, { useState } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import {
  Container,
  Box,
  Typography,
  TextField,
  Button,
  Paper,
  Link,
  Grid,
  Alert,
  AlertTitle,
  InputAdornment,
  IconButton,
  CircularProgress,
  Divider
} from '@mui/material';
import { Visibility, VisibilityOff, Info } from '@mui/icons-material';
import { useAuth } from '../../context/AuthContext';

// Define validation schema using yup
const schema = yup.object({
  username: yup.string().required('Username is required'),
  password: yup.string().required('Password is required')
}).required();

// Define form input types
interface LoginFormInputs {
  username: string;
  password: string;
}

const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [showMockInfo, setShowMockInfo] = useState(true);

  // Initialize form with react-hook-form
  const { control, handleSubmit, formState: { errors }, setValue } = useForm<LoginFormInputs>({
    resolver: yupResolver(schema),
    defaultValues: {
      username: '',
      password: ''
    }
  });

  // Handle form submission
  const onSubmit = async (data: LoginFormInputs) => {
    setError(null);
    setLoading(true);
    
    try {
      await login(data.username, data.password);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  // Toggle password visibility
  const handleClickShowPassword = () => {
    setShowPassword(!showPassword);
  };

  // Apply mock credentials automatically
  const applyMockCredentials = () => {
    setValue('username', 'admin');
    setValue('password', 'password');
  };

  return (
    <Container component="main" maxWidth="xs">
      <Box
        sx={{
          marginTop: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        {showMockInfo && (
          <Alert 
            severity="info" 
            variant="filled"
            icon={<Info />}
            sx={{ 
              width: '100%', 
              mb: 2,
              boxShadow: 3,
              '& .MuiAlert-message': { width: '100%' }
            }}
            onClose={() => setShowMockInfo(false)}
          >
            <AlertTitle>Development Environment</AlertTitle>
            <Typography variant="body2" sx={{ mb: 1 }}>
              This is a mock authentication setup for development.
            </Typography>
            <strong>Use these credentials:</strong>
            <Box sx={{ mt: 1, mb: 1, pl: 2 }}>
              <Typography variant="body2">Username: <strong>admin</strong></Typography>
              <Typography variant="body2">Password: <strong>password</strong></Typography>
            </Box>
            <Button
              size="small"
              variant="outlined"
              color="inherit"
              onClick={applyMockCredentials}
              sx={{ mt: 1, bgcolor: 'rgba(255,255,255,0.2)' }}
            >
              Apply Mock Credentials
            </Button>
          </Alert>
        )}
        
        <Paper
          elevation={3}
          sx={{
            p: 4,
            width: '100%',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
          }}
        >
          <Typography component="h1" variant="h5" sx={{ mb: 3 }}>
            Agent Platform
          </Typography>
          
          {error && (
            <Alert severity="error" sx={{ width: '100%', mb: 2 }}>
              {error}
            </Alert>
          )}
          
          <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate sx={{ mt: 1, width: '100%' }}>
            <Controller
              name="username"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  margin="normal"
                  required
                  fullWidth
                  id="username"
                  label="Username"
                  autoComplete="username"
                  autoFocus
                  error={!!errors.username}
                  helperText={errors.username?.message}
                />
              )}
            />
            
            <Controller
              name="password"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  margin="normal"
                  required
                  fullWidth
                  id="password"
                  label="Password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  error={!!errors.password}
                  helperText={errors.password?.message}
                  InputProps={{
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          aria-label="toggle password visibility"
                          onClick={handleClickShowPassword}
                          edge="end"
                        >
                          {showPassword ? <VisibilityOff /> : <Visibility />}
                        </IconButton>
                      </InputAdornment>
                    )
                  }}
                />
              )}
            />
            
            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2 }}
              disabled={loading}
            >
              {loading ? <CircularProgress size={24} /> : 'Sign In'}
            </Button>
            
            <Grid container>
              <Grid item xs>
                <Link component={RouterLink} to="/forgot-password" variant="body2">
                  Forgot password?
                </Link>
              </Grid>
              <Grid item>
                <Link component={RouterLink} to="/register" variant="body2">
                  {"Don't have an account? Sign Up"}
                </Link>
              </Grid>
            </Grid>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};

export default LoginPage; 