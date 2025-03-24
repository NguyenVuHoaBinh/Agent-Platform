import React, { useState } from 'react';
import { useParams, useNavigate, Link as RouterLink } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Grid,
  Button,
  Divider,
  Alert,
  CircularProgress,
  Tabs,
  Tab,
  Chip,
  Breadcrumbs,
  Link as MuiLink,
  TextField,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Tooltip,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Edit as EditIcon,
  FileCopy as BranchIcon,
  History as HistoryIcon,
  CompareArrows as CompareIcon,
  Timeline as TimelineIcon,
  ContentCopy as CopyIcon,
  Publish as PublishIcon,
  RotateLeft as UnpublishIcon
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { format } from 'date-fns';
import SyntaxHighlighter from 'react-syntax-highlighter';
import { docco } from 'react-syntax-highlighter/dist/esm/styles/hljs';
import versionService, { PromptVersion, PromptParameter } from '../../api/versionService';
import StatusChip from '../../components/versions/StatusChip';

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
      id={`version-tabpanel-${index}`}
      aria-labelledby={`version-tab-${index}`}
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
    id: `version-tab-${index}`,
    'aria-controls': `version-tabpanel-${index}`,
  };
};

const VersionDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [tabValue, setTabValue] = useState(0);
  const [statusDialogOpen, setStatusDialogOpen] = useState(false);
  const [statusToChange, setStatusToChange] = useState<PromptVersion['status'] | null>(null);

  // Fetch version details
  const { 
    data: version, 
    isLoading, 
    isError, 
    error 
  } = useQuery({
    queryKey: ['version', id],
    queryFn: () => id ? versionService.getVersionById(id) : Promise.reject('No version ID provided'),
    enabled: !!id
  });

  // Status change mutation
  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string, status: PromptVersion['status'] }) => 
      versionService.updateVersionStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['version', id] });
      queryClient.invalidateQueries({ queryKey: ['versions'] });
      setStatusDialogOpen(false);
    }
  });

  // Tab handling
  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  // Copy prompt to clipboard
  const handleCopyPrompt = () => {
    if (version?.content) {
      navigator.clipboard.writeText(version.content)
        .then(() => {
          alert('Prompt copied to clipboard');
        })
        .catch(err => {
          console.error('Failed to copy prompt: ', err);
        });
    }
  };

  // Status change handling
  const handleStatusChangeClick = (status: PromptVersion['status']) => {
    setStatusToChange(status);
    setStatusDialogOpen(true);
  };

  const handleStatusDialogClose = () => {
    setStatusDialogOpen(false);
    setStatusToChange(null);
  };

  const handleStatusChangeConfirm = () => {
    if (id && statusToChange) {
      statusMutation.mutate({ id, status: statusToChange });
    }
  };

  // Determine available status transitions
  const getAvailableStatusTransitions = () => {
    if (!version) return [];

    switch (version.status) {
      case 'DRAFT':
        return ['REVIEW'];
      case 'REVIEW':
        return ['PUBLISHED', 'REJECTED', 'DRAFT'];
      case 'PUBLISHED':
        return ['ARCHIVED'];
      case 'ARCHIVED':
        return ['PUBLISHED'];
      case 'REJECTED':
        return ['DRAFT'];
      default:
        return [];
    }
  };

  // Navigation handlers
  const handleBack = () => {
    navigate(-1);
  };

  const handleEditVersion = () => {
    navigate(`/versions/edit/${id}`);
  };

  const handleCreateBranch = () => {
    navigate(`/versions/branch/${id}`);
  };

  const handleViewHistory = () => {
    navigate(`/versions/history/${id}`);
  };

  const handleCompare = () => {
    navigate(`/versions/compare?sourceId=${id}`);
  };

  if (isLoading || statusMutation.isPending) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (isError) {
    return (
      <Box sx={{ py: 4 }}>
        <Alert severity="error">
          {error instanceof Error ? error.message : 'Failed to load version details'}
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

  const availableTransitions = getAvailableStatusTransitions();

  return (
    <Box>
      {/* Breadcrumbs navigation */}
      <Breadcrumbs sx={{ mb: 3 }}>
        <MuiLink component={RouterLink} to="/dashboard" color="inherit">
          Dashboard
        </MuiLink>
        <MuiLink component={RouterLink} to={`/templates/${version?.templateId}`} color="inherit">
          {version?.templateName}
        </MuiLink>
        <Typography color="text.primary">Version {version?.versionNumber}</Typography>
      </Breadcrumbs>

      {/* Header with actions */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 4 }}>
        <Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Typography variant="h4">
              Version {version?.versionNumber}
            </Typography>
            <StatusChip status={version?.status || 'DRAFT'} size="medium" />
          </Box>
          <Typography variant="body2" color="text.secondary">
            Created by {version?.createdBy} on {format(new Date(version?.createdAt || ''), 'MMM d, yyyy HH:mm')}
          </Typography>
          {version?.parentVersionId && (
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
          {/* Edit button - only for DRAFT status */}
          {version?.status === 'DRAFT' && (
            <Button 
              variant="outlined" 
              startIcon={<EditIcon />} 
              onClick={handleEditVersion}
            >
              Edit
            </Button>
          )}
          
          {/* Branch button */}
          <Button 
            variant="outlined" 
            startIcon={<BranchIcon />} 
            onClick={handleCreateBranch}
          >
            Create Branch
          </Button>
          
          {/* Status change button - show if there are available transitions */}
          {availableTransitions.length > 0 && (
            <>
              {availableTransitions.includes('PUBLISHED') && (
                <Button 
                  variant="contained" 
                  color="success" 
                  startIcon={<PublishIcon />} 
                  onClick={() => handleStatusChangeClick('PUBLISHED')}
                >
                  Publish
                </Button>
              )}
              {availableTransitions.includes('REVIEW') && (
                <Button 
                  variant="contained" 
                  color="info" 
                  onClick={() => handleStatusChangeClick('REVIEW')}
                >
                  Submit for Review
                </Button>
              )}
              {availableTransitions.includes('DRAFT') && (
                <Button 
                  variant="outlined" 
                  onClick={() => handleStatusChangeClick('DRAFT')}
                >
                  Return to Draft
                </Button>
              )}
              {availableTransitions.includes('ARCHIVED') && (
                <Button 
                  variant="outlined" 
                  color="warning" 
                  onClick={() => handleStatusChangeClick('ARCHIVED')}
                >
                  Archive
                </Button>
              )}
              {availableTransitions.includes('REJECTED') && (
                <Button 
                  variant="outlined" 
                  color="error" 
                  onClick={() => handleStatusChangeClick('REJECTED')}
                >
                  Reject
                </Button>
              )}
            </>
          )}
        </Box>
      </Box>

      {/* Version content tabs */}
      <Paper sx={{ mb: 4 }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={handleTabChange} aria-label="version tabs">
            <Tab label="Prompt Content" {...a11yProps(0)} />
            <Tab label="Parameters" {...a11yProps(1)} />
            <Tab label="System Prompt" {...a11yProps(2)} />
            <Tab label="History & Lineage" {...a11yProps(3)} />
          </Tabs>
        </Box>

        {/* Prompt Content Tab */}
        <TabPanel value={tabValue} index={0}>
          <Box sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6">Prompt Content</Typography>
              <Tooltip title="Copy to clipboard">
                <IconButton onClick={handleCopyPrompt}>
                  <CopyIcon />
                </IconButton>
              </Tooltip>
            </Box>
            <Paper variant="outlined" sx={{ p: 2, bgcolor: '#f5f5f5', maxHeight: '500px', overflow: 'auto' }}>
              <SyntaxHighlighter language="plaintext" style={docco} showLineNumbers>
                {version?.content || ''}
              </SyntaxHighlighter>
            </Paper>
          </Box>
        </TabPanel>

        {/* Parameters Tab */}
        <TabPanel value={tabValue} index={1}>
          <Box sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>Parameters</Typography>
            {version?.parameters && version.parameters.length > 0 ? (
              <TableContainer component={Paper} variant="outlined">
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell width="20%">Name</TableCell>
                      <TableCell width="35%">Description</TableCell>
                      <TableCell width="15%">Type</TableCell>
                      <TableCell width="15%">Default Value</TableCell>
                      <TableCell width="15%">Required</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {version.parameters.map((param: PromptParameter) => (
                      <TableRow key={param.id}>
                        <TableCell>{param.name}</TableCell>
                        <TableCell>{param.description}</TableCell>
                        <TableCell>{param.parameterType}</TableCell>
                        <TableCell>{param.defaultValue || '-'}</TableCell>
                        <TableCell>{param.required ? 'Yes' : 'No'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : (
              <Alert severity="info">This version doesn't have any parameters.</Alert>
            )}
          </Box>
        </TabPanel>

        {/* System Prompt Tab */}
        <TabPanel value={tabValue} index={2}>
          <Box sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>System Prompt</Typography>
            {version?.systemPrompt ? (
              <Paper variant="outlined" sx={{ p: 2, bgcolor: '#f5f5f5', maxHeight: '500px', overflow: 'auto' }}>
                <SyntaxHighlighter language="plaintext" style={docco}>
                  {version.systemPrompt}
                </SyntaxHighlighter>
              </Paper>
            ) : (
              <Alert severity="info">This version doesn't have a system prompt.</Alert>
            )}
          </Box>
        </TabPanel>

        {/* History & Lineage Tab */}
        <TabPanel value={tabValue} index={3}>
          <Box sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>Version History & Lineage</Typography>
            <Alert severity="info" sx={{ mb: 3 }}>
              Detailed history and lineage information is available on the History page.
            </Alert>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <Button 
                variant="outlined" 
                startIcon={<HistoryIcon />} 
                onClick={handleViewHistory}
              >
                View History
              </Button>
              <Button 
                variant="outlined" 
                startIcon={<CompareIcon />} 
                onClick={handleCompare}
              >
                Compare Versions
              </Button>
            </Box>
          </Box>
        </TabPanel>
      </Paper>

      {/* Status Change Dialog */}
      <Dialog open={statusDialogOpen} onClose={handleStatusDialogClose}>
        <DialogTitle>{`Change Status to ${statusToChange}`}</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {statusToChange === 'PUBLISHED' && 
              'Publishing will make this version available for use in production. Are you sure you want to continue?'}
            {statusToChange === 'REVIEW' && 
              'This will submit the version for review. Are you sure you want to continue?'}
            {statusToChange === 'DRAFT' && 
              'This will return the version to draft status. Are you sure you want to continue?'}
            {statusToChange === 'ARCHIVED' && 
              'Archiving will make this version inactive. Are you sure you want to continue?'}
            {statusToChange === 'REJECTED' && 
              'Rejecting will mark this version as rejected. Are you sure you want to continue?'}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleStatusDialogClose}>Cancel</Button>
          <Button 
            onClick={handleStatusChangeConfirm} 
            variant="contained" 
            color={statusToChange === 'PUBLISHED' ? 'success' : 'primary'}
          >
            Confirm
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default VersionDetailPage; 