import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Box, 
  Typography, 
  Paper, 
  TextField, 
  Button, 
  Grid, 
  FormControl, 
  InputLabel, 
  Select, 
  MenuItem, 
  FormHelperText, 
  CircularProgress, 
  Alert, 
  Breadcrumbs,
  Link as MuiLink,
  Divider
} from '@mui/material';
import { 
  Save as SaveIcon, 
  Cancel as CancelIcon, 
  Add as AddIcon 
} from '@mui/icons-material';
import { Link } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import promptTemplateService, { PromptTemplate, PromptTemplateRequest } from '../../api/promptService';
import CategoryDialog from '../../components/templates/CategoryDialog';

// Define validation schema
const schema = yup.object({
  name: yup.string().required('Name is required').max(100, 'Name must be at most 100 characters'),
  description: yup.string().required('Description is required'),
  category: yup.string().required('Category is required'),
  projectId: yup.string().required('Project is required')
}).required();

// Form input types
interface TemplateFormInputs {
  name: string;
  description: string;
  category: string;
  projectId: string;
}

const TemplateFormPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isEditMode = !!id;
  const [categories, setCategories] = useState<string[]>([]);
  const [categoryDialogOpen, setCategoryDialogOpen] = useState(false);
  const [projects] = useState<{id: string, name: string}[]>([
    { id: 'project1', name: 'Customer Support' },
    { id: 'project2', name: 'Marketing' },
    { id: 'project3', name: 'Sales' }
  ]); // Mock data for now

  // Form initialization
  const { 
    control, 
    handleSubmit, 
    reset, 
    formState: { errors } 
  } = useForm<TemplateFormInputs>({
    resolver: yupResolver(schema),
    defaultValues: {
      name: '',
      description: '',
      category: '',
      projectId: ''
    }
  });

  // Fetch categories
  const { data: categoriesData } = useQuery({
    queryKey: ['templateCategories'],
    queryFn: () => promptTemplateService.getAllCategories()
  });

  // Update categories when data changes
  useEffect(() => {
    if (categoriesData) {
      setCategories(categoriesData || []);
    }
  }, [categoriesData]);

  // Fetch template for edit mode
  const { data: templateData, isLoading: isLoadingTemplate, isError: isErrorTemplate } = useQuery({
    queryKey: ['template', id],
    queryFn: () => promptTemplateService.getTemplateById(id!),
    enabled: isEditMode
  });

  // Update form when template data changes
  useEffect(() => {
    if (templateData) {
      reset({
        name: templateData.name,
        description: templateData.description,
        category: templateData.category,
        projectId: templateData.projectId
      });
    }
  }, [templateData, reset]);

  // Create mutation
  const createMutation = useMutation({
    mutationFn: (data: PromptTemplateRequest) => promptTemplateService.createTemplate(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['templates'] });
      navigate('/templates');
    }
  });

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string, data: PromptTemplateRequest }) => 
      promptTemplateService.updateTemplate(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['templates'] });
      if (id) queryClient.invalidateQueries({ queryKey: ['template', id] });
      navigate(`/templates/${id}`);
    }
  });

  // Combined loading and error states
  const isLoading = isLoadingTemplate || createMutation.isPending || updateMutation.isPending;
  const isError = isErrorTemplate || createMutation.isError || updateMutation.isError;
  const error = createMutation.error || updateMutation.error;

  // Form submission handler
  const onSubmit = (data: TemplateFormInputs) => {
    const templateData: PromptTemplateRequest = {
      name: data.name,
      description: data.description,
      category: data.category,
      projectId: data.projectId
    };

    if (isEditMode && id) {
      updateMutation.mutate({ id, data: templateData });
    } else {
      createMutation.mutate(templateData);
    }
  };

  // Handle category dialog
  const handleAddCategory = () => {
    setCategoryDialogOpen(true);
  };

  const handleCategoryDialogClose = () => {
    setCategoryDialogOpen(false);
  };

  const handleCategoryCreate = (newCategory: string) => {
    // In a real app, you would call an API to create the category
    // For now, we'll just add it to the local state
    if (!categories.includes(newCategory)) {
      setCategories([...categories, newCategory]);
    }
    setCategoryDialogOpen(false);
  };

  if (isLoadingTemplate) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (isErrorTemplate) {
    return (
      <Box sx={{ py: 4 }}>
        <Alert severity="error">
          Failed to load template data. Please try again.
        </Alert>
        <Button
          onClick={() => navigate('/templates')}
          sx={{ mt: 3 }}
        >
          Back to Templates
        </Button>
      </Box>
    );
  }

  // Cast the template data to the correct type
  const template = templateData as PromptTemplate | undefined;

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
        <Typography color="text.primary">
          {isEditMode ? `Edit ${template?.name}` : 'Create Template'}
        </Typography>
      </Breadcrumbs>

      <Typography variant="h4" gutterBottom>
        {isEditMode ? 'Edit Template' : 'Create Template'}
      </Typography>

      {isError && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error instanceof Error ? error.message : 'An error occurred while saving the template'}
        </Alert>
      )}

      <Paper sx={{ p: 3, mb: 4 }}>
        <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <Controller
                name="name"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Template Name"
                    fullWidth
                    error={!!errors.name}
                    helperText={errors.name?.message}
                    disabled={isLoading}
                    required
                  />
                )}
              />
            </Grid>
            <Grid item xs={12}>
              <Controller
                name="description"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Description"
                    fullWidth
                    multiline
                    rows={4}
                    error={!!errors.description}
                    helperText={errors.description?.message}
                    disabled={isLoading}
                    required
                  />
                )}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <Controller
                name="category"
                control={control}
                render={({ field }) => (
                  <FormControl fullWidth error={!!errors.category} disabled={isLoading} required>
                    <InputLabel id="category-select-label">Category</InputLabel>
                    <Select
                      {...field}
                      labelId="category-select-label"
                      label="Category"
                    >
                      {categories.map((category) => (
                        <MenuItem key={category} value={category}>
                          {category}
                        </MenuItem>
                      ))}
                    </Select>
                    {errors.category && (
                      <FormHelperText>{errors.category.message}</FormHelperText>
                    )}
                  </FormControl>
                )}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
                <Box sx={{ flexGrow: 1 }}>
                  <Controller
                    name="projectId"
                    control={control}
                    render={({ field }) => (
                      <FormControl fullWidth error={!!errors.projectId} disabled={isLoading} required>
                        <InputLabel id="project-select-label">Project</InputLabel>
                        <Select
                          {...field}
                          labelId="project-select-label"
                          label="Project"
                        >
                          {projects.map((project) => (
                            <MenuItem key={project.id} value={project.id}>
                              {project.name}
                            </MenuItem>
                          ))}
                        </Select>
                        {errors.projectId && (
                          <FormHelperText>{errors.projectId.message}</FormHelperText>
                        )}
                      </FormControl>
                    )}
                  />
                </Box>
                <Button
                  variant="outlined"
                  onClick={handleAddCategory}
                  startIcon={<AddIcon />}
                  sx={{ mt: 1 }}
                  disabled={isLoading}
                >
                  New Category
                </Button>
              </Box>
            </Grid>
          </Grid>

          <Divider sx={{ my: 4 }} />

          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
            <Button
              variant="outlined"
              onClick={() => navigate('/templates')}
              startIcon={<CancelIcon />}
              disabled={isLoading}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              color="primary"
              startIcon={<SaveIcon />}
              disabled={isLoading}
            >
              {isLoading ? (
                <>
                  <CircularProgress size={24} sx={{ mr: 1 }} />
                  {isEditMode ? 'Saving...' : 'Creating...'}
                </>
              ) : (
                isEditMode ? 'Save Changes' : 'Create Template'
              )}
            </Button>
          </Box>
        </Box>
      </Paper>

      {/* Category Dialog */}
      <CategoryDialog
        open={categoryDialogOpen}
        onClose={handleCategoryDialogClose}
        onSave={handleCategoryCreate}
        existingCategories={categories}
      />
    </Box>
  );
};

export default TemplateFormPage; 