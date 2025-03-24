import React, { useState } from 'react';
import { useParams, useNavigate, Link as RouterLink } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Button,
  Grid,
  Alert,
  CircularProgress,
  Breadcrumbs,
  Link as MuiLink,
  Card,
  CardContent,
  CardActions,
  Divider,
  Tooltip,
  Tabs,
  Tab,
  Stack,
  Chip,
  Avatar
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  History as HistoryIcon,
  AccountTree as TreeIcon,
  Compare as CompareIcon,
  Visibility as ViewIcon,
  Edit as EditIcon,
  FileCopy as BranchIcon,
  Circle as CircleIcon,
  FiberManualRecord as DotIcon
} from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { format } from 'date-fns';
import versionService, { PromptVersion } from '../../api/versionService';
import StatusChip from '../../components/versions/StatusChip';

// Define types for our custom data
interface AuditTrailEntry {
  id: string;
  versionId: string;
  userId: string;
  action: string;
  previousValue?: string;
  newValue?: string;
  timestamp: string;
}

interface VersionNode {
  id: string;
  versionNumber: number;
  status: PromptVersion['status'];
  createdAt: string;
  createdBy: string;
  x: number;
  y: number;
}

interface VersionEdge {
  source: string;
  target: string;
  sourceX: number;
  sourceY: number;
  targetX: number;
  targetY: number;
}

interface VersionLineage {
  nodes: VersionNode[];
  edges: VersionEdge[];
}

// Define tab values
interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

const TabPanel: React.FC<TabPanelProps> = ({ children, value, index, ...other }) => {
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`version-history-tabpanel-${index}`}
      aria-labelledby={`version-history-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box sx={{ pt: 3 }}>
          {children}
        </Box>
      )}
    </div>
  );
};

const a11yProps = (index: number) => {
  return {
    id: `version-history-tab-${index}`,
    'aria-controls': `version-history-tabpanel-${index}`,
  };
};

const VersionHistoryPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [tabValue, setTabValue] = useState(0);
  
  // Fetch version details
  const { 
    data: version, 
    isLoading: versionLoading, 
    isError: versionError,
    error: versionErrorData
  } = useQuery({
    queryKey: ['version', id],
    queryFn: () => id ? versionService.getVersionById(id) : Promise.reject('No version ID provided'),
    enabled: !!id
  });
  
  // Fetch version lineage
  // Note: We're using getVersionById as a placeholder - in a real implementation
  // you would need to implement the actual getVersionLineage API call
  const { 
    data: rawLineage, 
    isLoading: lineageLoading, 
    isError: lineageError,
    error: lineageErrorData
  } = useQuery({
    queryKey: ['lineage', id],
    queryFn: () => id ? 
      // Mocking the lineage data for demo purposes
      Promise.resolve({
        nodes: [
          {
            id: id,
            versionNumber: version?.versionNumber || 1,
            status: version?.status || 'DRAFT',
            createdAt: version?.createdAt || new Date().toISOString(),
            createdBy: version?.createdBy || 'User',
            x: 1,
            y: 1
          },
          // Add more mock nodes as needed
        ],
        edges: []
      } as VersionLineage) : 
      Promise.reject('No version ID provided'),
    enabled: !!id && !!version
  });
  
  // Create a proper lineage object from the raw data
  const lineage = rawLineage as VersionLineage;
  
  // Fetch version audit trail
  // Note: Similar to lineage, this is a placeholder
  const { 
    data: auditTrail, 
    isLoading: auditLoading, 
    isError: auditError,
    error: auditErrorData
  } = useQuery({
    queryKey: ['audit', id],
    queryFn: () => id ? 
      // Mocking the audit trail data for demo purposes
      Promise.resolve([
        {
          id: '1',
          versionId: id,
          userId: version?.createdBy || 'User',
          action: 'CREATED',
          timestamp: version?.createdAt || new Date().toISOString()
        },
        {
          id: '2',
          versionId: id,
          userId: 'admin',
          action: 'STATUS_CHANGE',
          previousValue: 'DRAFT',
          newValue: version?.status,
          timestamp: new Date().toISOString()
        }
      ] as AuditTrailEntry[]) : 
      Promise.reject('No version ID provided'),
    enabled: !!id && !!version
  });
  
  // Handle tab change
  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };
  
  // Handle back navigation
  const handleBack = () => {
    navigate(-1);
  };
  
  // Helper to get status color for timeline
  const getStatusColor = (status: PromptVersion['status']) => {
    switch (status) {
      case 'DRAFT': return 'info';
      case 'REVIEW': return 'warning';
      case 'PUBLISHED': return 'success';
      case 'ARCHIVED': return 'error';
      case 'REJECTED': return 'error';
      default: return 'default';
    }
  };
  
  // Loading state
  const isLoading = versionLoading || lineageLoading || auditLoading;
  
  // Error state
  const hasError = versionError || lineageError || auditError;
  
  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }
  
  if (hasError || !id) {
    return (
      <Box sx={{ py: 4 }}>
        <Alert severity="error">
          {versionError && (versionErrorData instanceof Error ? versionErrorData.message : 'Failed to load version')}
          {lineageError && (lineageErrorData instanceof Error ? lineageErrorData.message : 'Failed to load lineage')}
          {auditError && (auditErrorData instanceof Error ? auditErrorData.message : 'Failed to load audit trail')}
          {!id && 'Version ID is required'}
        </Alert>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={handleBack}
          sx={{ mt: 3 }}
        >
          Back
        </Button>
      </Box>
    );
  }
  
  if (!version) {
    return (
      <Box sx={{ py: 4 }}>
        <Alert severity="warning">Version not found</Alert>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={handleBack}
          sx={{ mt: 3 }}
        >
          Back
        </Button>
      </Box>
    );
  }
  
  return (
    <Box>
      {/* Breadcrumbs navigation */}
      <Breadcrumbs sx={{ mb: 3 }}>
        <MuiLink component={RouterLink} to="/dashboard" color="inherit">
          Dashboard
        </MuiLink>
        <MuiLink component={RouterLink} to={`/templates/${version.templateId}`} color="inherit">
          {version.templateName}
        </MuiLink>
        <MuiLink component={RouterLink} to={`/versions/${id}`} color="inherit">
          Version {version.versionNumber}
        </MuiLink>
        <Typography color="text.primary">History & Lineage</Typography>
      </Breadcrumbs>
      
      {/* Page header */}
      <Typography variant="h4" gutterBottom>
        Version History & Lineage
      </Typography>
      <Typography variant="subtitle1" sx={{ mb: 4 }}>
        Tracking the evolution of Version {version.versionNumber} ({version.id.substring(0, 8)}...)
      </Typography>
      
      {/* Version details card */}
      <Paper variant="outlined" sx={{ p: 3, mb: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
          <Box>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Typography variant="h5">
                Version {version.versionNumber}
              </Typography>
              <StatusChip status={version.status} size="medium" />
            </Box>
            <Typography variant="body2" color="text.secondary">
              Created by {version.createdBy} on {format(new Date(version.createdAt), 'MMM d, yyyy HH:mm')}
            </Typography>
            {version.parentVersionId && (
              <Typography variant="body2" color="text.secondary">
                Based on version{' '}
                <MuiLink 
                  component={RouterLink} 
                  to={`/versions/${version.parentVersionId}`}
                >
                  {version.parentVersionId.substring(0, 8)}...
                </MuiLink>
              </Typography>
            )}
          </Box>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button 
              variant="outlined" 
              startIcon={<ViewIcon />} 
              component={RouterLink}
              to={`/versions/${id}`}
            >
              View Details
            </Button>
            {version.status === 'DRAFT' && (
              <Button 
                variant="outlined" 
                startIcon={<EditIcon />} 
                component={RouterLink}
                to={`/versions/edit/${id}`}
              >
                Edit
              </Button>
            )}
            <Button 
              variant="outlined" 
              startIcon={<BranchIcon />} 
              component={RouterLink}
              to={`/versions/branch/${id}`}
            >
              Create Branch
            </Button>
          </Box>
        </Box>
      </Paper>
      
      {/* Tabs for history views */}
      <Paper sx={{ mb: 4 }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={handleTabChange} aria-label="version history tabs">
            <Tab icon={<HistoryIcon />} label="Audit Trail" {...a11yProps(0)} />
            <Tab icon={<TreeIcon />} label="Version Lineage" {...a11yProps(1)} />
          </Tabs>
        </Box>
        
        {/* Audit Trail Tab */}
        <TabPanel value={tabValue} index={0}>
          <Box sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>Audit Trail</Typography>
            {auditTrail && auditTrail.length > 0 ? (
              <Stack spacing={2}>
                {auditTrail.map((entry: AuditTrailEntry, index: number) => (
                  <Card key={`${entry.timestamp}-${index}`} variant="outlined" sx={{ position: 'relative' }}>
                    <Box 
                      sx={{ 
                        position: 'absolute', 
                        left: 0, 
                        top: 0, 
                        bottom: 0, 
                        width: 4, 
                        bgcolor: 
                          entry.action === 'STATUS_CHANGE' && entry.newValue 
                            ? getStatusColor(entry.newValue as PromptVersion['status']) 
                            : 'primary.main' 
                      }}
                    />
                    <CardContent>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <Typography variant="subtitle1" fontWeight="bold">
                          {entry.action === 'CREATED' && 'Version Created'}
                          {entry.action === 'UPDATED' && 'Version Updated'}
                          {entry.action === 'STATUS_CHANGE' && 'Status Changed'}
                          {entry.action === 'BRANCHED' && 'Branch Created'}
                          {entry.action === 'ROLLBACK' && 'Rolled Back'}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          {format(new Date(entry.timestamp), 'MMM d, yyyy HH:mm:ss')}
                        </Typography>
                      </Box>
                      <Box sx={{ mt: 1 }}>
                        <Typography variant="body2">
                          {entry.action === 'STATUS_CHANGE' && (
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                              Status changed from 
                              <Chip 
                                label={entry.previousValue} 
                                color={getStatusColor(entry.previousValue as PromptVersion['status']) as any} 
                                size="small" 
                              /> 
                              to 
                              <Chip 
                                label={entry.newValue} 
                                color={getStatusColor(entry.newValue as PromptVersion['status']) as any} 
                                size="small" 
                              />
                            </Box>
                          )}
                          {entry.action === 'CREATED' && 'Initial version created'}
                          {entry.action === 'UPDATED' && 'Version content or parameters updated'}
                          {entry.action === 'BRANCHED' && `New branch created: ${entry.newValue}`}
                          {entry.action === 'ROLLBACK' && `Rolled back to previous version: ${entry.previousValue}`}
                        </Typography>
                      </Box>
                      <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                        By {entry.userId}
                      </Typography>
                    </CardContent>
                  </Card>
                ))}
              </Stack>
            ) : (
              <Alert severity="info">No audit trail entries found.</Alert>
            )}
          </Box>
        </TabPanel>
        
        {/* Version Lineage Tab */}
        <TabPanel value={tabValue} index={1}>
          <Box sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>Version Lineage</Typography>
            {lineage && lineage.nodes && lineage.nodes.length > 0 ? (
              <Box sx={{ 
                overflowX: 'auto'
              }}>
                <Alert severity="info" sx={{ mb: 3 }}>
                  This is a simplified view of version lineage. Each card represents a version in the lineage chain.
                </Alert>
                
                <Stack spacing={2}>
                  {lineage.nodes.map((node) => (
                    <Card 
                      key={node.id}
                      variant={node.id === id ? 'elevation' : 'outlined'}
                      sx={{ 
                        border: node.id === id ? '2px solid #1976d2' : undefined,
                        boxShadow: node.id === id ? 3 : undefined
                      }}
                    >
                      <CardContent>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                          <Box>
                            <Typography variant="h6">
                              Version {node.versionNumber}
                            </Typography>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, my: 1 }}>
                              <StatusChip status={node.status} size="small" />
                              <Typography variant="body2">
                                Created on {format(new Date(node.createdAt), 'MMM d, yyyy')}
                              </Typography>
                            </Box>
                            <Typography variant="body2" color="text.secondary">
                              By {node.createdBy}
                            </Typography>
                          </Box>
                          
                          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                            <Button 
                              size="small" 
                              variant="outlined"
                              startIcon={<ViewIcon />}
                              component={RouterLink} 
                              to={`/versions/${node.id}`}
                            >
                              View
                            </Button>
                            <Button 
                              size="small"
                              variant="outlined"
                              startIcon={<CompareIcon />}
                              component={RouterLink} 
                              to={`/versions/compare?sourceId=${id}&targetId=${node.id}`}
                              disabled={node.id === id}
                            >
                              Compare
                            </Button>
                          </Box>
                        </Box>
                      </CardContent>
                    </Card>
                  ))}
                </Stack>
              </Box>
            ) : (
              <Alert severity="info">No lineage information available.</Alert>
            )}
          </Box>
        </TabPanel>
      </Paper>
      
      {/* Back button */}
      <Box sx={{ mt: 4 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={handleBack}
        >
          Back
        </Button>
      </Box>
    </Box>
  );
};

export default VersionHistoryPage; 