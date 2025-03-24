import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Box, 
  Typography, 
  Paper, 
  Grid, 
  Chip, 
  Button, 
  Divider, 
  CircularProgress, 
  Alert, 
  Tabs, 
  Tab,
  Breadcrumbs,
  Link as MuiLink,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions
} from '@mui/material';
import { 
  ArrowBack as ArrowBackIcon, 
  Edit as EditIcon, 
  Delete as DeleteIcon,
  History as HistoryIcon,
  Description as DescriptionIcon,
  Code as CodeIcon
} from '@mui/icons-material';
import { Link } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import promptTemplateService, { PromptTemplate } from '../../api/promptService';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

const TabPanel = (props: TabPanelProps) => {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`template-tabpanel-${index}`}
      aria-labelledby={`template-tab-${index}`}
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
    id: `template-tab-${index}`,
    'aria-controls': `template-tabpanel-${index}`,
  };
};

const TemplateDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [tabValue, setTabValue] = useState(0);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

  // Fetch template details
  const { 
    data: templateData, 
    isLoading, 
    isError, 
    error 
  } = useQuery({
    queryKey: ['template', id],
    queryFn: () => id ? promptTemplateService.getTemplateById(id, true) : Promise.reject('No template ID provided'),
    enabled: !!id
  });

  // Cast the template data to the correct type
  const template = templateData as PromptTemplate | undefined;

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const handleEdit = () => {
    navigate(`/templates/edit/${id}`);
  };

  const handleDelete = async () => {
    if (!id) return;
    
    try {
      await promptTemplateService.deleteTemplate(id);
      queryClient.invalidateQueries({ queryKey: ['templates'] });
      navigate('/templates');
    } catch (err) {
      console.error('Failed to delete template:', err);
    }
  };

  const handleDeleteClick = () => {
    setDeleteDialogOpen(true);
  };

  const handleDeleteCancel = () => {
    setDeleteDialogOpen(false);
  };

  const handleDeleteConfirm = () => {
    setDeleteDialogOpen(false);
    handleDelete();
  };

  if (isLoading) {
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
          {error instanceof Error ? error.message : 'Failed to load template details'}
        </Alert>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate('/templates')}
          sx={{ mt: 3 }}
        >
          Back to Templates
        </Button>
      </Box>
    );
  }

  return (
    <Box>
      {/* Breadcrumbs navigation */}
      <Breadcrumbs sx={{ mb: 3 }}>
        <MuiLink component={Link} to="/dashboard" color="inherit">
          Dashboard
        </MuiLink>
        <MuiLink component={Link} to="/templates" color="inherit">
          Templates
        </MuiLink>
        <Typography color="text.primary">{template?.name}</Typography>
      </Breadcrumbs>

      {/* Header with actions */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 4 }}>
        <Box>
          <Typography variant="h4" gutterBottom>
            {template?.name}
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', mb: 1 }}>
            <Chip 
              label={template?.category} 
              color="primary" 
              variant="outlined" 
              size="small" 
            />
            <Typography variant="body2" color="text.secondary">
              Project: {template?.projectId}
            </Typography>
          </Box>
          <Typography variant="body2" color="text.secondary">
            Created by {template?.createdBy} on {new Date(template?.createdAt || '').toLocaleDateString()}
            {template?.updatedAt && ` • Last updated on ${new Date(template?.updatedAt).toLocaleDateString()}`}
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button 
            variant="outlined"
            startIcon={<EditIcon />}
            onClick={handleEdit}
          >
            Edit
          </Button>
          <Button 
            variant="outlined" 
            color="error"
            startIcon={<DeleteIcon />}
            onClick={handleDeleteClick}
          >
            Delete
          </Button>
        </Box>
      </Box>

      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={tabValue} onChange={handleTabChange} aria-label="template tabs">
          <Tab label="Overview" icon={<DescriptionIcon />} iconPosition="start" {...a11yProps(0)} />
          <Tab label="Versions" icon={<HistoryIcon />} iconPosition="start" {...a11yProps(1)} />
          <Tab label="Usage Examples" icon={<CodeIcon />} iconPosition="start" {...a11yProps(2)} />
        </Tabs>
      </Box>

      {/* Tab Panels */}
      <TabPanel value={tabValue} index={0}>
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Description
          </Typography>
          <Typography variant="body1" paragraph>
            {template?.description}
          </Typography>

          <Divider sx={{ my: 3 }} />

          <Typography variant="h6" gutterBottom>
            Details
          </Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <Typography variant="subtitle2">ID</Typography>
              <Typography variant="body2" sx={{ mb: 2 }}>{template?.id}</Typography>
              
              <Typography variant="subtitle2">Category</Typography>
              <Typography variant="body2" sx={{ mb: 2 }}>{template?.category}</Typography>
              
              <Typography variant="subtitle2">Project</Typography>
              <Typography variant="body2">{template?.projectId}</Typography>
            </Grid>
            <Grid item xs={12} sm={6}>
              <Typography variant="subtitle2">Created By</Typography>
              <Typography variant="body2" sx={{ mb: 2 }}>{template?.createdBy}</Typography>
              
              <Typography variant="subtitle2">Created Date</Typography>
              <Typography variant="body2" sx={{ mb: 2 }}>
                {new Date(template?.createdAt || '').toLocaleString()}
              </Typography>
              
              <Typography variant="subtitle2">Last Updated</Typography>
              <Typography variant="body2">
                {new Date(template?.updatedAt || '').toLocaleString()}
              </Typography>
            </Grid>
          </Grid>
        </Paper>
      </TabPanel>
      <TabPanel value={tabValue} index={1}>
        <Paper sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
            <Typography variant="h6">Version History</Typography>
            <Button 
              variant="contained" 
              color="primary"
              onClick={() => navigate(`/versions/create/${id}`)}
            >
              Create New Version
            </Button>
          </Box>
          
          {template?.versionCount === 0 ? (
            <Alert severity="info">
              No versions have been created for this template yet.
            </Alert>
          ) : (
            <Typography variant="body1">
              Version history will be implemented in the next phase.
            </Typography>
          )}
        </Paper>
      </TabPanel>
      <TabPanel value={tabValue} index={2}>
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Usage Examples
          </Typography>
          <Alert severity="info" sx={{ mb: 3 }}>
            Usage examples will be available in a future update.
          </Alert>
        </Paper>
      </TabPanel>

      {/* Delete confirmation dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={handleDeleteCancel}
      >
        <DialogTitle>Confirm Template Deletion</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete the template "{template?.name}"? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDeleteCancel}>Cancel</Button>
          <Button onClick={handleDeleteConfirm} color="error" autoFocus>
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default TemplateDetailPage; 