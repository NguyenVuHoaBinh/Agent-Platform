import React from 'react';
import { Box, Typography } from '@mui/material';
import VersionListComponent from '../../components/versions/VersionListComponent';

const VersionListPage: React.FC = () => {
  return (
    <Box sx={{ py: 4, px: 3 }}>
      <Typography variant="h4" gutterBottom>All Versions</Typography>
      <VersionListComponent />
    </Box>
  );
};

export default VersionListPage; 