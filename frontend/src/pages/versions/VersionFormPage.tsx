import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation, Link as RouterLink } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  Grid,
  Divider,
  Alert,
  CircularProgress,
  Breadcrumbs,
  Link as MuiLink,
  Tooltip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControlLabel,
  Checkbox,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  FormHelperText,
  Card,
  CardContent,
  CardActions
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Add as AddIcon,
  Delete as DeleteIcon,
  Save as SaveIcon,
  Cancel as CancelIcon
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import versionService, { PromptVersion, PromptParameter } from '../../api/versionService';
import templateService from '../../api/templateService';
import { SelectChangeEvent } from '@mui/material';

// Parameter type options
const PARAMETER_TYPES = [
  { value: 'STRING', label: 'String' },
  { value: 'NUMBER', label: 'Number' },
  { value: 'BOOLEAN', label: 'Boolean' },
  { value: 'ARRAY', label: 'Array' },
  { value: 'OBJECT', label: 'Object' }
];

interface ParameterFormData extends Omit<PromptParameter, 'id'> {
  id?: string;
  tempId?: string; // For new parameters not saved yet
}

interface VersionFormData {
  content: string;
  systemPrompt: string;
  parameters: ParameterFormData[];
  templateId: string;
  comment: string;
  parentVersionId?: string;
  versionNumber?: string;
}

const VersionFormPage: React.FC = () => {
  const { id, templateId } = useParams<{ id?: string, templateId?: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const isEditMode = Boolean(id);
  const isCreateBranchMode = location.pathname.includes('/branch/');
  
  // Determine if it's a new version for an existing template
  const isNewVersionForTemplate = Boolean(templateId) && !id;

  // Form state
  const [formData, setFormData] = useState<VersionFormData>({
    content: '',
    systemPrompt: '',
    parameters: [],
    templateId: templateId || '',
    comment: '',
    parentVersionId: isCreateBranchMode ? id : undefined
  });
  
  // Form validation errors
  const [errors, setErrors] = useState<{
    content?: string;
    templateId?: string;
    parameters?: Record<string, Record<string, string>>;
  }>({});
  
  // Parameter dialog state
  const [paramDialogOpen, setParamDialogOpen] = useState(false);
  const [currentParam, setCurrentParam] = useState<ParameterFormData | null>(null);
  const [paramErrors, setParamErrors] = useState<Record<string, string>>({});
  const [editingParamIndex, setEditingParamIndex] = useState<number | null>(null);

  // Fetch version data if in edit mode
  const { 
    data: version,
    isLoading: isVersionLoading,
    isError: isVersionError,
    error: versionError
  } = useQuery({
    queryKey: ['version', id],
    queryFn: () => id ? versionService.getVersionById(id) : Promise.reject('No version ID provided'),
    enabled: !!id,
  });
  
  // Fetch template data if creating a new version
  const {
    data: template,
    isLoading: isTemplateLoading,
    isError: isTemplateError,
    error: templateError
  } = useQuery({
    queryKey: ['template', templateId],
    queryFn: () => templateId ? templateService.getTemplateById(templateId) : Promise.reject('No template ID provided'),
    enabled: !!templateId && !id,
  });

  // Mutations for creating and updating versions
  const createVersionMutation = useMutation({
    mutationFn: (data: VersionFormData) => {
      const requestData = {
        ...data,
        versionNumber: data.versionNumber || '1',
        parameters: data.parameters.map(param => ({
          name: param.name,
          description: param.description,
          parameterType: param.parameterType,
          required: param.required,
          defaultValue: param.defaultValue
        }))
      };
      
      if (isCreateBranchMode && id) {
        return versionService.createVersionBranch(id, requestData);
      }
      
      return versionService.createVersion(requestData);
    },
    onSuccess: (data: any) => {
      queryClient.invalidateQueries({ queryKey: ['versions'] });
      navigate(`/versions/${data.id}`);
    }
  });
  
  const updateVersionMutation = useMutation({
    mutationFn: (data: VersionFormData) => {
      if (!id) throw new Error('Version ID is required for update');
      
      const requestData = {
        ...data,
        versionNumber: version?.versionNumber || '1',
        parameters: data.parameters.map(param => ({
          id: param.id,
          name: param.name,
          description: param.description,
          parameterType: param.parameterType,
          required: param.required,
          defaultValue: param.defaultValue
        }))
      };
      
      return versionService.updateVersion(id, requestData);
    },
    onSuccess: (data: any) => {
      queryClient.invalidateQueries({ queryKey: ['version', id] });
      queryClient.invalidateQueries({ queryKey: ['versions'] });
      navigate(`/versions/${data.id}`);
    }
  });

  // Set form data when version or template data loads
  useEffect(() => {
    if (version) {
      setFormData({
        content: version.content || '',
        systemPrompt: version.systemPrompt || '',
        parameters: version.parameters || [],
        templateId: version.templateId,
        comment: isCreateBranchMode ? `Branch created from version ${version.id}` : '',
        parentVersionId: isCreateBranchMode ? version.id : version.parentVersionId
      });
    }
  }, [version, isCreateBranchMode]);
  
  useEffect(() => {
    if (template && !isEditMode) {
      setFormData(prev => ({
        ...prev,
        templateId: template.id
      }));
    }
  }, [template, isEditMode]);

  // Handle form input changes
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  // Parameter dialog handlers
  const handleOpenParamDialog = (param?: ParameterFormData, index?: number) => {
    if (param) {
      setCurrentParam(param);
      setEditingParamIndex(index !== undefined ? index : null);
    } else {
      setCurrentParam({
        name: '',
        description: '',
        parameterType: 'STRING',
        required: false,
        defaultValue: '',
        tempId: `new-${Date.now()}`
      });
      setEditingParamIndex(null);
    }
    setParamErrors({});
    setParamDialogOpen(true);
  };

  const handleCloseParamDialog = () => {
    setParamDialogOpen(false);
    setCurrentParam(null);
    setEditingParamIndex(null);
  };

  const handleParamInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    if (currentParam) {
      setCurrentParam(prev => prev ? ({
        ...prev,
        [name]: value
      }) : null);
    }
  };

  const handleParamCheckboxChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, checked } = e.target;
    if (currentParam) {
      setCurrentParam(prev => prev ? ({
        ...prev,
        [name]: checked
      }) : null);
    }
  };

  const handleParamTypeChange = (event: SelectChangeEvent<string>) => {
    if (currentParam) {
      setCurrentParam({
        ...currentParam,
        parameterType: event.target.value
      });
    }
  };

  const validateParameter = (param: ParameterFormData): boolean => {
    const errors: Record<string, string> = {};
    
    if (!param.name.trim()) {
      errors.name = 'Name is required';
    }
    
    if (!param.description.trim()) {
      errors.description = 'Description is required';
    }
    
    if (param.required && !param.defaultValue && param.parameterType !== 'BOOLEAN') {
      errors.defaultValue = 'Default value is required for required parameters';
    }
    
    setParamErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSaveParameter = () => {
    if (currentParam && validateParameter(currentParam)) {
      setFormData(prev => {
        const updatedParameters = [...prev.parameters];
        
        if (editingParamIndex !== null && editingParamIndex >= 0) {
          updatedParameters[editingParamIndex] = currentParam;
        } else {
          updatedParameters.push(currentParam);
        }
        
        return {
          ...prev,
          parameters: updatedParameters
        };
      });
      
      handleCloseParamDialog();
    }
  };

  const handleDeleteParameter = (index: number) => {
    setFormData(prev => ({
      ...prev,
      parameters: prev.parameters.filter((_, i) => i !== index)
    }));
  };

  // Form submission
  const validateForm = (): boolean => {
    const newErrors: typeof errors = {};
    
    if (!formData.content.trim()) {
      newErrors.content = 'Prompt content is required';
    }
    
    if (!formData.templateId) {
      newErrors.templateId = 'Template is required';
    }
    
    // Check parameters for duplicated names
    const paramErrors: Record<string, Record<string, string>> = {};
    const paramNames = new Set<string>();
    
    formData.parameters.forEach((param, index) => {
      if (paramNames.has(param.name)) {
        if (!paramErrors[index]) paramErrors[index] = {};
        paramErrors[index].name = 'Parameter name must be unique';
      }
      paramNames.add(param.name);
    });
    
    if (Object.keys(paramErrors).length > 0) {
      newErrors.parameters = paramErrors;
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (validateForm()) {
      if (isEditMode) {
        updateVersionMutation.mutate(formData);
      } else {
        createVersionMutation.mutate(formData);
      }
    }
  };

  const handleCancel = () => {
    navigate(-1);
  };

  // Determine page title and buttons
  const getPageTitle = () => {
    if (isCreateBranchMode) return 'Create Branch from Version';
    if (isEditMode) return 'Edit Version';
    return 'Create New Version';
  };

  if (isVersionLoading || isTemplateLoading || createVersionMutation.isPending || updateVersionMutation.isPending) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if ((isVersionError && isEditMode) || (isTemplateError && isNewVersionForTemplate)) {
    return (
      <Box sx={{ py: 4 }}>
        <Alert severity="error">
          {isVersionError && (versionError instanceof Error ? versionError.message : 'Failed to load version')}
          {isTemplateError && (templateError instanceof Error ? templateError.message : 'Failed to load template')}
        </Alert>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={handleCancel}
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
        {(version || template) && (
          <MuiLink 
            component={RouterLink} 
            to={`/templates/${version?.templateId || template?.id}`} 
            color="inherit"
          >
            {version?.templateName || template?.name}
          </MuiLink>
        )}
        {isEditMode && !isCreateBranchMode && (
          <MuiLink 
            component={RouterLink} 
            to={`/versions/${id}`} 
            color="inherit"
          >
            Version {version?.versionNumber}
          </MuiLink>
        )}
        {isCreateBranchMode && version && (
          <MuiLink 
            component={RouterLink} 
            to={`/versions/${id}`} 
            color="inherit"
          >
            Version {version.versionNumber}
          </MuiLink>
        )}
        <Typography color="text.primary">{getPageTitle()}</Typography>
      </Breadcrumbs>

      {/* Page header */}
      <Typography variant="h4" sx={{ mb: 4 }}>
        {getPageTitle()}
      </Typography>

      {/* Form */}
      <Paper sx={{ p: 3, mb: 4 }}>
        <form onSubmit={handleSubmit}>
          <Grid container spacing={3}>
            {/* Template ID (hidden if already set) */}
            {!templateId && !version && (
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Template ID"
                  name="templateId"
                  value={formData.templateId}
                  onChange={handleInputChange}
                  error={!!errors.templateId}
                  helperText={errors.templateId}
                  required
                />
              </Grid>
            )}
            
            {/* Comment field */}
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Version Comment"
                name="comment"
                value={formData.comment}
                onChange={handleInputChange}
                placeholder="Describe the changes in this version"
                multiline
                rows={2}
              />
            </Grid>
            
            {/* Prompt Content */}
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Prompt Content"
                name="content"
                value={formData.content}
                onChange={handleInputChange}
                multiline
                rows={8}
                error={!!errors.content}
                helperText={errors.content}
                required
                placeholder="Enter your prompt content here..."
              />
            </Grid>
            
            {/* System Prompt */}
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="System Prompt (Optional)"
                name="systemPrompt"
                value={formData.systemPrompt}
                onChange={handleInputChange}
                multiline
                rows={4}
                placeholder="Enter system prompt content if needed..."
              />
            </Grid>
            
            {/* Parameters section */}
            <Grid item xs={12}>
              <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="h6">Parameters</Typography>
                <Button
                  variant="outlined"
                  startIcon={<AddIcon />}
                  onClick={() => handleOpenParamDialog()}
                >
                  Add Parameter
                </Button>
              </Box>
              
              {formData.parameters.length === 0 ? (
                <Alert severity="info" sx={{ mb: 2 }}>
                  No parameters added yet. Parameters allow you to make your prompt template more flexible.
                </Alert>
              ) : (
                <Grid container spacing={2}>
                  {formData.parameters.map((param, index) => (
                    <Grid item xs={12} md={6} lg={4} key={param.id || param.tempId}>
                      <Card variant="outlined">
                        <CardContent>
                          <Typography variant="subtitle1" fontWeight="bold">
                            {param.name}
                            {param.required && (
                              <Typography component="span" color="error" ml={0.5}>*</Typography>
                            )}
                          </Typography>
                          <Typography variant="body2" color="text.secondary" gutterBottom>
                            Type: {param.parameterType}
                          </Typography>
                          <Typography variant="body2" gutterBottom>
                            {param.description}
                          </Typography>
                          {param.defaultValue && (
                            <Typography variant="body2">
                              Default: {param.defaultValue}
                            </Typography>
                          )}
                        </CardContent>
                        <CardActions>
                          <Button 
                            size="small" 
                            onClick={() => handleOpenParamDialog(param, index)}
                          >
                            Edit
                          </Button>
                          <Button 
                            size="small" 
                            color="error" 
                            onClick={() => handleDeleteParameter(index)}
                          >
                            Delete
                          </Button>
                        </CardActions>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              )}
            </Grid>
            
            {/* Form actions */}
            <Grid item xs={12}>
              <Divider sx={{ mb: 2 }} />
              <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
                <Button
                  variant="outlined"
                  onClick={handleCancel}
                  startIcon={<CancelIcon />}
                >
                  Cancel
                </Button>
                <Button
                  variant="contained"
                  color="primary"
                  type="submit"
                  startIcon={<SaveIcon />}
                  disabled={createVersionMutation.isPending || updateVersionMutation.isPending}
                >
                  {isEditMode ? 'Update Version' : 'Create Version'}
                </Button>
              </Box>
            </Grid>
          </Grid>
        </form>
      </Paper>

      {/* Parameter Dialog */}
      <Dialog open={paramDialogOpen} onClose={handleCloseParamDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editingParamIndex !== null ? 'Edit Parameter' : 'Add Parameter'}
        </DialogTitle>
        <DialogContent>
          <Box component="form" sx={{ pt: 1 }}>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Parameter Name"
                  name="name"
                  value={currentParam?.name || ''}
                  onChange={handleParamInputChange}
                  error={!!paramErrors.name}
                  helperText={paramErrors.name}
                  required
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Description"
                  name="description"
                  value={currentParam?.description || ''}
                  onChange={handleParamInputChange}
                  error={!!paramErrors.description}
                  helperText={paramErrors.description}
                  required
                  multiline
                  rows={2}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControl fullWidth>
                  <InputLabel id="parameter-type-label">Type</InputLabel>
                  <Select
                    labelId="parameter-type-label"
                    id="parameter-type"
                    value={currentParam?.parameterType || 'STRING'}
                    onChange={handleParamTypeChange}
                    label="Type"
                  >
                    {PARAMETER_TYPES.map((type) => (
                      <MenuItem key={type.value} value={type.value}>
                        {type.label}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={currentParam?.required || false}
                      onChange={handleParamCheckboxChange}
                      name="required"
                    />
                  }
                  label="Required"
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Default Value"
                  name="defaultValue"
                  value={currentParam?.defaultValue || ''}
                  onChange={handleParamInputChange}
                  error={!!paramErrors.defaultValue}
                  helperText={paramErrors.defaultValue || 'Leave empty for optional parameters'}
                />
              </Grid>
            </Grid>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseParamDialog}>Cancel</Button>
          <Button onClick={handleSaveParameter} variant="contained">
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default VersionFormPage; 