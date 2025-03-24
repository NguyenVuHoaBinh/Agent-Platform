import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Box, 
  Typography, 
  Button, 
  TextField, 
  Grid, 
  Card, 
  CardContent, 
  CardActions, 
  IconButton, 
  Chip, 
  Divider, 
  Paper, 
  InputAdornment, 
  FormControl, 
  InputLabel, 
  Select, 
  MenuItem, 
  SelectChangeEvent, 
  Pagination, 
  CircularProgress, 
  Alert,
  Menu,
  ListItemIcon,
  ListItemText
} from '@mui/material';
import { 
  Add as AddIcon, 
  Search as SearchIcon, 
  FilterList as FilterListIcon, 
  MoreVert as MoreVertIcon, 
  Edit as EditIcon, 
  Delete as DeleteIcon, 
  Visibility as ViewIcon,
  Clear as ClearIcon
} from '@mui/icons-material';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import promptTemplateService, { PromptTemplate, PromptTemplateSearchCriteria, PageResponse } from '../../api/promptService';

const TemplateListPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  
  // State variables
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [searchText, setSearchText] = useState('');
  const [category, setCategory] = useState<string>('');
  const [projectId, setProjectId] = useState<string>('');
  const [categories, setCategories] = useState<string[]>([]);
  const [projects] = useState<{id: string, name: string}[]>([
    { id: 'project1', name: 'Customer Support' },
    { id: 'project2', name: 'Marketing' },
    { id: 'project3', name: 'Sales' }
  ]); // Mock data for now
  const [searchCriteria, setSearchCriteria] = useState<PromptTemplateSearchCriteria>({});
  const [menuAnchorEl, setMenuAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedTemplateId, setSelectedTemplateId] = useState<string | null>(null);
  
  // Fetch template data
  const { 
    data: templatesData, 
    isLoading, 
    isError, 
    error 
  } = useQuery({
    queryKey: ['templates', page, pageSize, searchCriteria],
    queryFn: () => promptTemplateService.searchTemplates(searchCriteria, page, pageSize),
    staleTime: 5 * 60 * 1000 // 5 minutes
  });
  
  // Fetch categories
  const { data: categoriesData } = useQuery({
    queryKey: ['templateCategories'],
    queryFn: () => promptTemplateService.getAllCategories()
  });

  // Update categories when data changes
  useEffect(() => {
    if (categoriesData) {
      setCategories(categoriesData);
    }
  }, [categoriesData]);
  
  // Apply search filters
  const handleSearch = () => {
    const criteria: PromptTemplateSearchCriteria = {};
    
    if (searchText) criteria.searchText = searchText;
    if (category) criteria.category = category;
    if (projectId) criteria.projectId = projectId;
    
    setSearchCriteria(criteria);
    setPage(0); // Reset to first page when searching
  };
  
  // Clear all filters
  const handleClearFilters = () => {
    setSearchText('');
    setCategory('');
    setProjectId('');
    setSearchCriteria({});
  };
  
  // Handle page change
  const handlePageChange = (event: React.ChangeEvent<unknown>, value: number) => {
    setPage(value - 1); // API is 0-indexed, MUI Pagination is 1-indexed
  };
  
  // Handle category change
  const handleCategoryChange = (event: SelectChangeEvent) => {
    setCategory(event.target.value);
  };
  
  // Handle project change
  const handleProjectChange = (event: SelectChangeEvent) => {
    setProjectId(event.target.value);
  };
  
  // Menu handling
  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, templateId: string) => {
    setMenuAnchorEl(event.currentTarget);
    setSelectedTemplateId(templateId);
  };
  
  const handleMenuClose = () => {
    setMenuAnchorEl(null);
    setSelectedTemplateId(null);
  };
  
  // Navigation handlers
  const handleViewTemplate = (id: string) => {
    handleMenuClose();
    navigate(`/templates/${id}`);
  };
  
  const handleEditTemplate = (id: string) => {
    handleMenuClose();
    navigate(`/templates/edit/${id}`);
  };
  
  const handleDeleteTemplate = async (id: string) => {
    handleMenuClose();
    if (window.confirm('Are you sure you want to delete this template?')) {
      try {
        await promptTemplateService.deleteTemplate(id);
        queryClient.invalidateQueries({ queryKey: ['templates'] });
      } catch (err) {
        console.error('Failed to delete template:', err);
      }
    }
  };
  
  const handleCreateTemplate = () => {
    navigate('/templates/create');
  };

  // Convert API response to proper data type
  const templates = templatesData as PageResponse<PromptTemplate> | undefined;
  
  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 4 }}>
        <Typography variant="h4">Template Management</Typography>
        <Button 
          variant="contained" 
          color="primary" 
          startIcon={<AddIcon />} 
          onClick={handleCreateTemplate}
        >
          Create Template
        </Button>
      </Box>
      
      {/* Search and filters */}
      <Paper sx={{ p: 3, mb: 4 }}>
        <Grid container spacing={2} alignItems="flex-end">
          <Grid item xs={12} sm={4}>
            <TextField
              fullWidth
              label="Search Templates"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
              placeholder="Search by name or description"
            />
          </Grid>
          <Grid item xs={12} sm={3}>
            <FormControl fullWidth>
              <InputLabel id="category-select-label">Category</InputLabel>
              <Select
                labelId="category-select-label"
                value={category}
                label="Category"
                onChange={handleCategoryChange}
              >
                <MenuItem value="">
                  <em>All Categories</em>
                </MenuItem>
                {categories.map((cat) => (
                  <MenuItem key={cat} value={cat}>{cat}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={3}>
            <FormControl fullWidth>
              <InputLabel id="project-select-label">Project</InputLabel>
              <Select
                labelId="project-select-label"
                value={projectId}
                label="Project"
                onChange={handleProjectChange}
              >
                <MenuItem value="">
                  <em>All Projects</em>
                </MenuItem>
                {projects.map((project) => (
                  <MenuItem key={project.id} value={project.id}>{project.name}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={2}>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button 
                variant="contained" 
                color="primary" 
                onClick={handleSearch}
                startIcon={<FilterListIcon />}
              >
                Filter
              </Button>
              <Button 
                variant="outlined" 
                onClick={handleClearFilters}
                startIcon={<ClearIcon />}
              >
                Clear
              </Button>
            </Box>
          </Grid>
        </Grid>
      </Paper>
      
      {/* Results */}
      <Box>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 5 }}>
            <CircularProgress />
          </Box>
        ) : isError ? (
          <Alert severity="error" sx={{ mb: 3 }}>
            Error loading templates: {error instanceof Error ? error.message : 'Unknown error'}
          </Alert>
        ) : templates?.content?.length === 0 ? (
          <Alert severity="info" sx={{ mb: 3 }}>
            No templates found. Try adjusting your search criteria or create a new template.
          </Alert>
        ) : (
          <>
            <Typography variant="subtitle1" sx={{ mb: 2 }}>
              Showing {templates?.content?.length} of {templates?.totalElements} templates
            </Typography>
            
            <Grid container spacing={3}>
              {templates?.content?.map((template) => (
                <Grid item xs={12} sm={6} md={4} key={template.id}>
                  <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                    <CardContent sx={{ flexGrow: 1 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <Typography variant="h6" component="div" noWrap sx={{ maxWidth: '80%' }}>
                          {template.name}
                        </Typography>
                        <IconButton 
                          size="small" 
                          onClick={(e) => handleMenuOpen(e, template.id)}
                        >
                          <MoreVertIcon />
                        </IconButton>
                      </Box>
                      
                      <Chip 
                        label={template.category} 
                        size="small" 
                        color="primary" 
                        variant="outlined" 
                        sx={{ mt: 1, mb: 2 }} 
                      />
                      
                      <Typography variant="body2" color="text.secondary" sx={{ 
                        mb: 2,
                        display: '-webkit-box',
                        WebkitLineClamp: 3,
                        WebkitBoxOrient: 'vertical',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        height: '4.5em'
                      }}>
                        {template.description}
                      </Typography>
                      
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 'auto' }}>
                        <Typography variant="caption" color="text.secondary">
                          Versions: {template.versionCount || 0}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {new Date(template.updatedAt).toLocaleDateString()}
                        </Typography>
                      </Box>
                    </CardContent>
                    <Divider />
                    <CardActions>
                      <Button 
                        size="small" 
                        startIcon={<ViewIcon />}
                        onClick={() => handleViewTemplate(template.id)}
                      >
                        View
                      </Button>
                      <Button 
                        size="small" 
                        startIcon={<EditIcon />}
                        onClick={() => handleEditTemplate(template.id)}
                      >
                        Edit
                      </Button>
                    </CardActions>
                  </Card>
                </Grid>
              ))}
            </Grid>
            
            {/* Pagination */}
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
              <Pagination 
                count={templates?.totalPages || 1} 
                page={page + 1} 
                onChange={handlePageChange}
                color="primary"
              />
            </Box>
          </>
        )}
      </Box>
      
      {/* Context Menu */}
      <Menu
        anchorEl={menuAnchorEl}
        open={Boolean(menuAnchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={() => selectedTemplateId && handleViewTemplate(selectedTemplateId)}>
          <ListItemIcon>
            <ViewIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>View Details</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => selectedTemplateId && handleEditTemplate(selectedTemplateId)}>
          <ListItemIcon>
            <EditIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Edit</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => selectedTemplateId && handleDeleteTemplate(selectedTemplateId)}>
          <ListItemIcon>
            <DeleteIcon fontSize="small" color="error" />
          </ListItemIcon>
          <ListItemText sx={{ color: 'error.main' }}>Delete</ListItemText>
        </MenuItem>
      </Menu>
    </Box>
  );
};

export default TemplateListPage; 