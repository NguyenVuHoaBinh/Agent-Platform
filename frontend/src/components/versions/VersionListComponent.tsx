import React, { useState, useEffect } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  TableSortLabel,
  InputAdornment,
  Tooltip,
  FormControl,
  InputLabel,
  Select,
  SelectChangeEvent,
  Divider,
  Grid
} from '@mui/material';
import {
  Search as SearchIcon,
  Add as AddIcon,
  MoreVert as MoreVertIcon,
  Visibility as ViewIcon,
  Edit as EditIcon,
  FileCopy as BranchIcon,
  History as HistoryIcon,
  Compare as CompareIcon
} from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { format } from 'date-fns';
import versionService, { PromptVersion } from '../../api/versionService';
import StatusChip from '../../components/versions/StatusChip';

// Define column keys for sorting
type OrderKey = 'versionNumber' | 'status' | 'createdAt' | 'createdBy';

// Define order direction
type Order = 'asc' | 'desc';

// Props for the component
export interface VersionListComponentProps {
  templateId?: string;
  templateName?: string;
}

const VersionListComponent: React.FC<VersionListComponentProps> = ({ templateId, templateName }) => {
  const navigate = useNavigate();
  
  // Fetch versions based on templateId or all versions
  const {
    data: versionsResponse,
    isLoading,
    isError,
    error
  } = useQuery({
    queryKey: templateId ? ['versions', templateId] : ['versions'],
    queryFn: () => templateId 
      ? versionService.getVersionsByTemplate(templateId)
      : versionService.getAllVersions()
  });
  
  // Extract versions from the response
  const versions = versionsResponse?.content || [];
  
  // State for filtering and sorting
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [order, setOrder] = useState<Order>('desc');
  const [orderBy, setOrderBy] = useState<OrderKey>('createdAt');
  
  // Action menu state
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedVersionId, setSelectedVersionId] = useState<string | null>(null);
  
  // Filtered versions
  const filteredVersions = versions.filter((version: PromptVersion) => {
    // Filter by search term
    const matchesSearch = 
      version.content?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      version.templateName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      version.versionNumber?.toString().includes(searchTerm);
    
    // Filter by status
    const matchesStatus = statusFilter === 'ALL' || version.status === statusFilter;
    
    return matchesSearch && matchesStatus;
  });
  
  // Sort versions
  const sortedVersions = [...filteredVersions].sort((a, b) => {
    let comparison = 0;
    
    if (orderBy === 'versionNumber') {
      comparison = String(a.versionNumber).localeCompare(String(b.versionNumber), undefined, {numeric: true});
    } else if (orderBy === 'status') {
      comparison = a.status.localeCompare(b.status);
    } else if (orderBy === 'createdAt') {
      comparison = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
    } else if (orderBy === 'createdBy') {
      comparison = a.createdBy.localeCompare(b.createdBy);
    }
    
    return order === 'asc' ? comparison : -comparison;
  });
  
  // Pagination
  const paginatedVersions = sortedVersions.slice(
    page * rowsPerPage,
    page * rowsPerPage + rowsPerPage
  );
  
  // Calculate empty rows for pagination
  const emptyRows = page > 0 
    ? Math.max(0, (1 + page) * Number(rowsPerPage) - filteredVersions.length) 
    : 0;
  
  // Handle search input change
  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(event.target.value);
    setPage(0); // Reset to first page when searching
  };
  
  // Handle status filter change
  const handleStatusFilterChange = (event: SelectChangeEvent<string>) => {
    setStatusFilter(event.target.value);
    setPage(0); // Reset to first page when filtering
  };
  
  // Handle sort request
  const handleRequestSort = (property: OrderKey) => {
    const isAsc = orderBy === property && order === 'asc';
    setOrder(isAsc ? 'desc' : 'asc');
    setOrderBy(property);
  };
  
  // Pagination handlers
  const handleChangePage = (event: unknown, newPage: number) => {
    setPage(newPage);
  };
  
  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };
  
  // Action menu handlers
  const handleOpenMenu = (event: React.MouseEvent<HTMLButtonElement>, versionId: string) => {
    setAnchorEl(event.currentTarget);
    setSelectedVersionId(versionId);
  };
  
  const handleCloseMenu = () => {
    setAnchorEl(null);
    setSelectedVersionId(null);
  };
  
  const handleViewDetails = () => {
    if (selectedVersionId) {
      navigate(`/versions/${selectedVersionId}`);
    }
    handleCloseMenu();
  };
  
  const handleEditVersion = () => {
    if (selectedVersionId) {
      navigate(`/versions/edit/${selectedVersionId}`);
    }
    handleCloseMenu();
  };
  
  const handleCreateBranch = () => {
    if (selectedVersionId) {
      navigate(`/versions/branch/${selectedVersionId}`);
    }
    handleCloseMenu();
  };
  
  const handleViewHistory = () => {
    if (selectedVersionId) {
      navigate(`/versions/history/${selectedVersionId}`);
    }
    handleCloseMenu();
  };
  
  const handleCompareVersions = () => {
    if (selectedVersionId) {
      navigate(`/versions/compare?sourceId=${selectedVersionId}`);
    }
    handleCloseMenu();
  };
  
  const createSortHandler = (property: OrderKey) => () => {
    handleRequestSort(property);
  };
  
  // Button to create a new version
  const handleCreateVersion = () => {
    if (templateId) {
      navigate(`/versions/create/${templateId}`);
    } else {
      navigate('/versions/create');
    }
  };
  
  return (
    <Box>
      {/* Header section with title and create button */}
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        mb: 3 
      }}>
        <Typography variant="h5">
          {templateId && templateName ? `${templateName} Versions` : 'All Versions'}
        </Typography>
        <Button 
          variant="contained" 
          startIcon={<AddIcon />} 
          onClick={handleCreateVersion}
        >
          Create Version
        </Button>
      </Box>
      
      {/* Filters and search */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              placeholder="Search by content, version number, or creator"
              value={searchTerm}
              onChange={handleSearchChange}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
              size="small"
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <FormControl fullWidth size="small">
              <InputLabel id="status-filter-label">Status</InputLabel>
              <Select
                labelId="status-filter-label"
                id="status-filter"
                value={statusFilter}
                label="Status"
                onChange={handleStatusFilterChange}
              >
                <MenuItem value="ALL">All Statuses</MenuItem>
                <MenuItem value="DRAFT">Draft</MenuItem>
                <MenuItem value="REVIEW">Review</MenuItem>
                <MenuItem value="PUBLISHED">Published</MenuItem>
                <MenuItem value="ARCHIVED">Archived</MenuItem>
                <MenuItem value="REJECTED">Rejected</MenuItem>
              </Select>
            </FormControl>
          </Grid>
        </Grid>
      </Paper>
      
      {/* Versions table */}
      <Paper>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Version</TableCell>
                <TableCell>
                  <TableSortLabel
                    active={orderBy === 'status'}
                    direction={orderBy === 'status' ? order : 'asc'}
                    onClick={createSortHandler('status')}
                  >
                    Status
                  </TableSortLabel>
                </TableCell>
                <TableCell>
                  <TableSortLabel
                    active={orderBy === 'createdAt'}
                    direction={orderBy === 'createdAt' ? order : 'asc'}
                    onClick={createSortHandler('createdAt')}
                  >
                    Created Date
                  </TableSortLabel>
                </TableCell>
                <TableCell>
                  <TableSortLabel
                    active={orderBy === 'createdBy'}
                    direction={orderBy === 'createdBy' ? order : 'asc'}
                    onClick={createSortHandler('createdBy')}
                  >
                    Created By
                  </TableSortLabel>
                </TableCell>
                <TableCell>Parent Version</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    Loading versions...
                  </TableCell>
                </TableRow>
              ) : isError ? (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    Error loading versions: {error instanceof Error ? error.message : 'Unknown error'}
                  </TableCell>
                </TableRow>
              ) : paginatedVersions.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    No versions found
                  </TableCell>
                </TableRow>
              ) : (
                paginatedVersions.map((version) => (
                  <TableRow key={version.id} hover>
                    <TableCell>
                      <RouterLink 
                        to={`/versions/${version.id}`}
                        style={{ textDecoration: 'none', color: 'inherit' }}
                      >
                        <Typography component="span" fontWeight="medium">
                          {version.versionNumber}
                        </Typography>
                      </RouterLink>
                    </TableCell>
                    <TableCell>
                      <StatusChip status={version.status} />
                    </TableCell>
                    <TableCell>
                      {format(new Date(version.createdAt), 'MMM d, yyyy HH:mm')}
                    </TableCell>
                    <TableCell>{version.createdBy}</TableCell>
                    <TableCell>
                      {version.parentVersionId ? (
                        <RouterLink 
                          to={`/versions/${version.parentVersionId}`}
                          style={{ textDecoration: 'none', color: 'inherit' }}
                        >
                          {version.parentVersionId.substring(0, 8)}...
                        </RouterLink>
                      ) : (
                        <Typography variant="body2" color="text.secondary">
                          None
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell align="right">
                      <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                        <Tooltip title="View Details">
                          <IconButton 
                            size="small" 
                            onClick={() => navigate(`/versions/${version.id}`)}
                          >
                            <ViewIcon />
                          </IconButton>
                        </Tooltip>
                        
                        {version.status === 'DRAFT' && (
                          <Tooltip title="Edit">
                            <IconButton 
                              size="small" 
                              onClick={() => navigate(`/versions/edit/${version.id}`)}
                            >
                              <EditIcon />
                            </IconButton>
                          </Tooltip>
                        )}
                        
                        <IconButton
                          size="small"
                          onClick={(e) => handleOpenMenu(e, version.id)}
                        >
                          <MoreVertIcon />
                        </IconButton>
                      </Box>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
        
        <TablePagination
          rowsPerPageOptions={[5, 10, 25]}
          component="div"
          count={filteredVersions.length}
          rowsPerPage={rowsPerPage}
          page={page}
          onPageChange={handleChangePage}
          onRowsPerPageChange={handleChangeRowsPerPage}
        />
      </Paper>
      
      {/* Action menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleCloseMenu}
      >
        <MenuItem onClick={handleViewDetails}>
          <ViewIcon fontSize="small" sx={{ mr: 1 }} />
          View Details
        </MenuItem>
        
        <MenuItem onClick={handleCreateBranch}>
          <BranchIcon fontSize="small" sx={{ mr: 1 }} />
          Create Branch
        </MenuItem>
        
        <MenuItem onClick={handleViewHistory}>
          <HistoryIcon fontSize="small" sx={{ mr: 1 }} />
          View History
        </MenuItem>
        
        <MenuItem onClick={handleCompareVersions}>
          <CompareIcon fontSize="small" sx={{ mr: 1 }} />
          Compare
        </MenuItem>
        
        <Divider />
        
        {selectedVersionId && versions.find((v: PromptVersion) => v.id === selectedVersionId)?.status === 'DRAFT' && (
          <MenuItem onClick={handleEditVersion}>
            <EditIcon fontSize="small" sx={{ mr: 1 }} />
            Edit
          </MenuItem>
        )}
      </Menu>
    </Box>
  );
};

export default VersionListComponent; 