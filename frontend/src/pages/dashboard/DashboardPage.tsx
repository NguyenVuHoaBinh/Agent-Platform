import React from 'react';
import { Grid, Card, CardContent, Typography, Paper, Box, Divider } from '@mui/material';
import {
  Description as DescriptionIcon,
  Code as CodeIcon,
  Science as ScienceIcon,
  Insights as InsightsIcon
} from '@mui/icons-material';

const DashboardPage: React.FC = () => {
  // Mock data for dashboard
  const summaryData = {
    templates: 24,
    versions: 86,
    tests: 145,
    optimizations: 32
  };

  const recentActivity = [
    { type: 'template', action: 'created', name: 'Customer Support Prompt', time: '2 hours ago', user: 'John Doe' },
    { type: 'version', action: 'published', name: 'Product Description v1.2.0', time: '4 hours ago', user: 'Jane Smith' },
    { type: 'test', action: 'executed', name: 'Customer Onboarding Test', time: '5 hours ago', user: 'Michael Brown' },
    { type: 'optimization', action: 'completed', name: 'FAQ Template Optimization', time: '1 day ago', user: 'Sarah Wilson' }
  ];

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>
      
      {/* Summary Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <DescriptionIcon fontSize="large" color="primary" sx={{ mb: 1 }} />
              <Typography variant="h4" component="div">
                {summaryData.templates}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Total Templates
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <CodeIcon fontSize="large" color="secondary" sx={{ mb: 1 }} />
              <Typography variant="h4" component="div">
                {summaryData.versions}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Total Versions
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <ScienceIcon fontSize="large" color="success" sx={{ mb: 1 }} />
              <Typography variant="h4" component="div">
                {summaryData.tests}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Total Tests
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ height: '100%' }}>
            <CardContent sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <InsightsIcon fontSize="large" color="error" sx={{ mb: 1 }} />
              <Typography variant="h4" component="div">
                {summaryData.optimizations}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Optimizations
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      
      {/* Recent Activity */}
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="h6" gutterBottom>
              Recent Activity
            </Typography>
            <Divider sx={{ mb: 2 }} />
            
            {recentActivity.map((activity, index) => (
              <Box key={index} sx={{ mb: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body1" fontWeight="medium">
                    {activity.name}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {activity.time}
                  </Typography>
                </Box>
                <Typography variant="body2" color="text.secondary">
                  {activity.user} {activity.action} a {activity.type}
                </Typography>
                {index < recentActivity.length - 1 && <Divider sx={{ mt: 1.5, mb: 1.5 }} />}
              </Box>
            ))}
          </Paper>
        </Grid>
        
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="h6" gutterBottom>
              Quick Actions
            </Typography>
            <Divider sx={{ mb: 2 }} />
            
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <Card variant="outlined" sx={{ p: 2, cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }}>
                  <Typography variant="body1" fontWeight="medium">
                    Create Template
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Create a new prompt template
                  </Typography>
                </Card>
              </Grid>
              
              <Grid item xs={12} sm={6}>
                <Card variant="outlined" sx={{ p: 2, cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }}>
                  <Typography variant="body1" fontWeight="medium">
                    Test Prompt
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Run a test on an existing prompt
                  </Typography>
                </Card>
              </Grid>
              
              <Grid item xs={12} sm={6}>
                <Card variant="outlined" sx={{ p: 2, cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }}>
                  <Typography variant="body1" fontWeight="medium">
                    Optimize Prompt
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Improve an existing prompt
                  </Typography>
                </Card>
              </Grid>
              
              <Grid item xs={12} sm={6}>
                <Card variant="outlined" sx={{ p: 2, cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }}>
                  <Typography variant="body1" fontWeight="medium">
                    View Reports
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    See performance metrics
                  </Typography>
                </Card>
              </Grid>
            </Grid>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default DashboardPage; 