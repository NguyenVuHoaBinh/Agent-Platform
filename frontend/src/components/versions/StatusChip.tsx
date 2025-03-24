import React from 'react';
import { Chip, ChipProps } from '@mui/material';
import { PromptVersion } from '../../api/versionService';

interface StatusChipProps {
  status: PromptVersion['status'];
  size?: ChipProps['size'];
}

/**
 * A component that displays the status of a version as a colored chip
 */
const StatusChip: React.FC<StatusChipProps> = ({ status, size = 'medium' }) => {
  // Get the color and label based on status
  const getStatusConfig = (status: PromptVersion['status']) => {
    switch (status) {
      case 'DRAFT':
        return { color: 'info' as const, label: 'Draft' };
      case 'REVIEW':
        return { color: 'warning' as const, label: 'In Review' };
      case 'PUBLISHED':
        return { color: 'success' as const, label: 'Published' };
      case 'ARCHIVED':
        return { color: 'default' as const, label: 'Archived' };
      case 'REJECTED':
        return { color: 'error' as const, label: 'Rejected' };
      default:
        return { color: 'default' as const, label: status };
    }
  };

  const { color, label } = getStatusConfig(status);
  
  return (
    <Chip 
      label={label} 
      color={color} 
      size={size}
      variant="filled"
    />
  );
};

export default StatusChip; 