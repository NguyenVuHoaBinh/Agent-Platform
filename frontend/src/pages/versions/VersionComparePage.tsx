import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate, Link as RouterLink } from 'react-router-dom';
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
  Divider,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  ToggleButtonGroup,
  ToggleButton,
  SelectChangeEvent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Card,
  CardContent,
  Stack
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Compare as CompareIcon,
  SyncAlt as SwapIcon,
  Add as AddIcon,
  Remove as RemoveIcon
} from '@mui/icons-material';
import { useQuery } from '@tanstack/react-query';
import { format } from 'date-fns';
import SyntaxHighlighter from 'react-syntax-highlighter';
import { docco } from 'react-syntax-highlighter/dist/esm/styles/hljs';
import versionService, { PromptVersion } from '../../api/versionService';
import StatusChip from '../../components/versions/StatusChip';

// Define custom types for version comparison
interface VersionDiff {
  type: 'added' | 'removed' | 'unchanged';
  content: string;
  lineNumber?: number;
}

interface VersionComparisonResult {
  contentDiff: VersionDiff[];
  systemPromptDiff: VersionDiff[];
  parametersDiff: VersionDiff[];
}

// Define view modes
type ViewMode = 'unified' | 'split';

const VersionComparePage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  
  // Get sourceId and targetId from URL params
  const [sourceId, setSourceId] = useState<string | null>(queryParams.get('sourceId'));
  const [targetId, setTargetId] = useState<string | null>(queryParams.get('targetId'));
  
  // Compare options
  const [viewMode, setViewMode] = useState<ViewMode>('split');
  const [showParameters, setShowParameters] = useState<boolean>(true);
  const [showSystemPrompt, setShowSystemPrompt] = useState<boolean>(true);
  
  // Get all versions for selection
  const { 
    data: versions, 
    isLoading: versionsLoading,
    isError: versionsError 
  } = useQuery({
    queryKey: ['versions'],
    queryFn: () => 
      // Mock data for versions since we don't have getAllVersions implemented
      Promise.resolve([
        {
          id: '1',
          versionNumber: '1',
          status: 'PUBLISHED' as PromptVersion['status'],
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          createdBy: 'admin',
          content: 'Original prompt content',
          systemPrompt: 'System prompt content',
          parameters: [],
          templateId: '1',
          templateName: 'Sample Template',
          parentVersionId: undefined
        },
        {
          id: '2',
          versionNumber: '2',
          status: 'DRAFT' as PromptVersion['status'],
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          createdBy: 'admin',
          content: 'Updated prompt content',
          systemPrompt: 'Updated system prompt',
          parameters: [],
          templateId: '1',
          templateName: 'Sample Template',
          parentVersionId: '1'
        }
      ] as PromptVersion[])
  });
  
  // Fetch comparison data
  const { 
    data: comparison,
    isLoading: comparisonLoading,
    isError: comparisonError,
    error: comparisonErrorData,
    refetch: refetchComparison
  } = useQuery({
    queryKey: ['compare', sourceId, targetId],
    queryFn: () => {
      if (!sourceId || !targetId) {
        return Promise.reject('Both source and target versions are required for comparison');
      }
      
      // Mock comparison data
      return Promise.resolve({
        contentDiff: [
          { type: 'unchanged', content: 'This is the same in both versions', lineNumber: 1 },
          { type: 'removed', content: 'This line was removed', lineNumber: 2 },
          { type: 'added', content: 'This line was added', lineNumber: 2 },
          { type: 'unchanged', content: 'More unchanged content', lineNumber: 3 }
        ],
        systemPromptDiff: [
          { type: 'unchanged', content: 'System prompt intro', lineNumber: 1 },
          { type: 'removed', content: 'Old system instruction', lineNumber: 2 },
          { type: 'added', content: 'New system instruction', lineNumber: 2 }
        ],
        parametersDiff: [
          { type: 'unchanged', content: '"name": "param1"', lineNumber: 1 },
          { type: 'removed', content: '"required": false', lineNumber: 2 },
          { type: 'added', content: '"required": true', lineNumber: 2 }
        ]
      } as VersionComparisonResult);
    },
    enabled: !!sourceId && !!targetId
  });
  
  // Get source version details
  const {
    data: sourceVersion,
    isLoading: sourceLoading,
    isError: sourceError
  } = useQuery({
    queryKey: ['version', sourceId],
    queryFn: () => sourceId ? versionService.getVersionById(sourceId) : Promise.reject('No source ID provided'),
    enabled: !!sourceId
  });
  
  // Get target version details
  const {
    data: targetVersion,
    isLoading: targetLoading,
    isError: targetError
  } = useQuery({
    queryKey: ['version', targetId],
    queryFn: () => targetId ? versionService.getVersionById(targetId) : Promise.reject('No target ID provided'),
    enabled: !!targetId
  });
  
  // Update URL when source or target changes
  useEffect(() => {
    if (sourceId || targetId) {
      const params = new URLSearchParams();
      if (sourceId) params.set('sourceId', sourceId);
      if (targetId) params.set('targetId', targetId);
      navigate({ search: params.toString() }, { replace: true });
    }
  }, [sourceId, targetId, navigate]);
  
  // Handle version selection changes
  const handleSourceChange = (event: SelectChangeEvent<string>) => {
    const newSourceId = event.target.value;
    setSourceId(newSourceId);
  };
  
  const handleTargetChange = (event: SelectChangeEvent<string>) => {
    const newTargetId = event.target.value;
    setTargetId(newTargetId);
  };
  
  // Handle view mode change
  const handleViewModeChange = (
    event: React.MouseEvent<HTMLElement>,
    newViewMode: ViewMode | null
  ) => {
    if (newViewMode !== null) {
      setViewMode(newViewMode);
    }
  };
  
  // Swap source and target
  const handleSwapVersions = () => {
    const temp = sourceId;
    setSourceId(targetId);
    setTargetId(temp);
  };
  
  // Handle back navigation
  const handleBack = () => {
    navigate(-1);
  };
  
  // Determine if we can compare
  const canCompare = !!sourceId && !!targetId && sourceId !== targetId;
  
  // Render diff for unified view
  const renderUnifiedDiff = (diff: VersionDiff[]) => {
    if (!diff || diff.length === 0) return null;
    
    return (
      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell width="5%">Line</TableCell>
              <TableCell>Content</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {diff.map((line, index) => (
              <TableRow 
                key={index}
                sx={{ 
                  backgroundColor: 
                    line.type === 'added' ? 'rgba(76, 175, 80, 0.1)' : 
                    line.type === 'removed' ? 'rgba(244, 67, 54, 0.1)' : 
                    'inherit'
                }}
              >
                <TableCell>{line.lineNumber}</TableCell>
                <TableCell sx={{ fontFamily: 'monospace' }}>
                  {line.type === 'added' && <AddIcon color="success" fontSize="small" sx={{ mr: 1, verticalAlign: 'middle' }} />}
                  {line.type === 'removed' && <RemoveIcon color="error" fontSize="small" sx={{ mr: 1, verticalAlign: 'middle' }} />}
                  {line.content}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    );
  };
  
  // Render diff for split view
  const renderSplitDiff = (diff: VersionDiff[]) => {
    if (!diff || diff.length === 0) return null;
    
    // Prepare data for split view
    const sourceLines: Array<{ content: string, lineNumber: number }> = [];
    const targetLines: Array<{ content: string, lineNumber: number }> = [];
    
    diff.forEach(line => {
      if (line.type === 'unchanged' || line.type === 'removed') {
        sourceLines.push({ content: line.content, lineNumber: line.lineNumber || 0 });
      }
      
      if (line.type === 'unchanged' || line.type === 'added') {
        targetLines.push({ content: line.content, lineNumber: line.lineNumber || 0 });
      }
    });
    
    return (
      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Typography variant="subtitle2" gutterBottom>Source Version</Typography>
          <Paper variant="outlined" sx={{ p: 2, bgcolor: '#f5f5f5', maxHeight: '500px', overflow: 'auto' }}>
            <Box component="pre" sx={{ m: 0, fontFamily: 'monospace', fontSize: '0.875rem' }}>
              {sourceLines.map((line, index) => (
                <Box 
                  key={index} 
                  component="div" 
                  sx={{ 
                    lineHeight: '1.5rem',
                    px: 1,
                    backgroundColor: diff.find(d => d.lineNumber === line.lineNumber && d.type === 'removed') ? 
                      'rgba(244, 67, 54, 0.1)' : 'transparent'
                  }}
                >
                  <Box component="span" sx={{ color: 'text.secondary', display: 'inline-block', width: '2rem', mr: 2, textAlign: 'right' }}>
                    {line.lineNumber}
                  </Box>
                  {line.content}
                </Box>
              ))}
            </Box>
          </Paper>
        </Grid>
        <Grid item xs={12} md={6}>
          <Typography variant="subtitle2" gutterBottom>Target Version</Typography>
          <Paper variant="outlined" sx={{ p: 2, bgcolor: '#f5f5f5', maxHeight: '500px', overflow: 'auto' }}>
            <Box component="pre" sx={{ m: 0, fontFamily: 'monospace', fontSize: '0.875rem' }}>
              {targetLines.map((line, index) => (
                <Box 
                  key={index} 
                  component="div" 
                  sx={{ 
                    lineHeight: '1.5rem',
                    px: 1,
                    backgroundColor: diff.find(d => d.lineNumber === line.lineNumber && d.type === 'added') ? 
                      'rgba(76, 175, 80, 0.1)' : 'transparent'
                  }}
                >
                  <Box component="span" sx={{ color: 'text.secondary', display: 'inline-block', width: '2rem', mr: 2, textAlign: 'right' }}>
                    {line.lineNumber}
                  </Box>
                  {line.content}
                </Box>
              ))}
            </Box>
          </Paper>
        </Grid>
      </Grid>
    );
  };
  
  // Render content section
  const renderContent = () => {
    if (!comparison || !sourceVersion || !targetVersion) {
      return <Alert severity="info">Select two different versions to compare.</Alert>;
    }
    
    return (
      <Box>
        <Typography variant="h6" gutterBottom>Prompt Content Differences</Typography>
        {comparison.contentDiff && comparison.contentDiff.length > 0 ? (
          viewMode === 'unified' ? renderUnifiedDiff(comparison.contentDiff) : renderSplitDiff(comparison.contentDiff)
        ) : (
          <Alert severity="info">No differences in prompt content.</Alert>
        )}
      </Box>
    );
  };
  
  // Render system prompt section
  const renderSystemPrompt = () => {
    if (!showSystemPrompt || !comparison || !sourceVersion || !targetVersion) {
      return null;
    }
    
    return (
      <Box sx={{ mt: 4 }}>
        <Typography variant="h6" gutterBottom>System Prompt Differences</Typography>
        {comparison.systemPromptDiff && comparison.systemPromptDiff.length > 0 ? (
          viewMode === 'unified' ? renderUnifiedDiff(comparison.systemPromptDiff) : renderSplitDiff(comparison.systemPromptDiff)
        ) : (
          <Alert severity="info">No differences in system prompt.</Alert>
        )}
      </Box>
    );
  };
  
  // Render parameters section
  const renderParameters = () => {
    if (!showParameters || !comparison || !sourceVersion || !targetVersion) {
      return null;
    }
    
    return (
      <Box sx={{ mt: 4 }}>
        <Typography variant="h6" gutterBottom>Parameters Differences</Typography>
        {comparison.parametersDiff && comparison.parametersDiff.length > 0 ? (
          viewMode === 'unified' ? renderUnifiedDiff(comparison.parametersDiff) : renderSplitDiff(comparison.parametersDiff)
        ) : (
          <Alert severity="info">No differences in parameters.</Alert>
        )}
      </Box>
    );
  };
  
  // Loading state
  const isLoading = versionsLoading || comparisonLoading || sourceLoading || targetLoading;
  
  // Error state
  const hasError = versionsError || comparisonError || sourceError || targetError;
  
  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
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
        <Typography color="text.primary">Compare Versions</Typography>
      </Breadcrumbs>
      
      {/* Page header */}
      <Typography variant="h4" sx={{ mb: 4 }}>
        Compare Versions
      </Typography>
      
      {/* Version selection */}
      <Paper sx={{ p: 3, mb: 4 }}>
        <Grid container spacing={3} alignItems="center">
          <Grid item xs={12} md={5}>
            <FormControl fullWidth>
              <InputLabel id="source-version-label">Source Version</InputLabel>
              <Select
                labelId="source-version-label"
                id="source-version"
                value={sourceId || ''}
                onChange={handleSourceChange}
                label="Source Version"
                error={!sourceId}
              >
                {versions?.map((v: PromptVersion) => (
                  <MenuItem key={v.id} value={v.id}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Typography>Version {v.versionNumber}</Typography>
                      <StatusChip status={v.status} size="small" />
                      <Typography variant="caption" color="text.secondary">
                        ({format(new Date(v.createdAt), 'MMM d, yyyy')})
                      </Typography>
                    </Box>
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          
          <Grid item xs={12} md={2} sx={{ display: 'flex', justifyContent: 'center' }}>
            <Button
              variant="outlined"
              startIcon={<SwapIcon />}
              onClick={handleSwapVersions}
              disabled={!sourceId || !targetId}
            >
              Swap
            </Button>
          </Grid>
          
          <Grid item xs={12} md={5}>
            <FormControl fullWidth>
              <InputLabel id="target-version-label">Target Version</InputLabel>
              <Select
                labelId="target-version-label"
                id="target-version"
                value={targetId || ''}
                onChange={handleTargetChange}
                label="Target Version"
                error={!targetId}
              >
                {versions?.map((v: PromptVersion) => (
                  <MenuItem key={v.id} value={v.id}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Typography>Version {v.versionNumber}</Typography>
                      <StatusChip status={v.status} size="small" />
                      <Typography variant="caption" color="text.secondary">
                        ({format(new Date(v.createdAt), 'MMM d, yyyy')})
                      </Typography>
                    </Box>
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          
          <Grid item xs={12}>
            <Divider sx={{ mb: 2 }} />
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <ToggleButtonGroup
                value={viewMode}
                exclusive
                onChange={handleViewModeChange}
                aria-label="view mode"
                size="small"
              >
                <ToggleButton value="split" aria-label="split view">
                  Split View
                </ToggleButton>
                <ToggleButton value="unified" aria-label="unified view">
                  Unified View
                </ToggleButton>
              </ToggleButtonGroup>
              
              <Box>
                <Button
                  variant="contained"
                  startIcon={<CompareIcon />}
                  onClick={() => refetchComparison()}
                  disabled={!canCompare}
                  color="primary"
                  sx={{ ml: 2 }}
                >
                  Compare
                </Button>
              </Box>
            </Box>
          </Grid>
        </Grid>
      </Paper>
      
      {/* Version details */}
      {sourceVersion && targetVersion && (
        <Grid container spacing={4} sx={{ mb: 4 }}>
          <Grid item xs={12} md={6}>
            <Paper variant="outlined" sx={{ p: 3 }}>
              <Typography variant="h6">Source: Version {sourceVersion.versionNumber}</Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <StatusChip status={sourceVersion.status} size="small" />
                <Typography variant="body2">
                  Created by {sourceVersion.createdBy} on {format(new Date(sourceVersion.createdAt), 'MMM d, yyyy')}
                </Typography>
              </Box>
              <MuiLink 
                component={RouterLink} 
                to={`/versions/${sourceVersion.id}`}
              >
                View Version Details
              </MuiLink>
            </Paper>
          </Grid>
          <Grid item xs={12} md={6}>
            <Paper variant="outlined" sx={{ p: 3 }}>
              <Typography variant="h6">Target: Version {targetVersion.versionNumber}</Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <StatusChip status={targetVersion.status} size="small" />
                <Typography variant="body2">
                  Created by {targetVersion.createdBy} on {format(new Date(targetVersion.createdAt), 'MMM d, yyyy')}
                </Typography>
              </Box>
              <MuiLink 
                component={RouterLink} 
                to={`/versions/${targetVersion.id}`}
              >
                View Version Details
              </MuiLink>
            </Paper>
          </Grid>
        </Grid>
      )}
      
      {/* Comparison error */}
      {comparisonError && (
        <Alert severity="error" sx={{ mb: 4 }}>
          {comparisonErrorData instanceof Error ? comparisonErrorData.message : 'Error comparing versions'}
        </Alert>
      )}
      
      {/* Comparison content */}
      {!comparisonError && canCompare && (
        <Paper sx={{ p: 3 }}>
          {renderContent()}
          {renderSystemPrompt()}
          {renderParameters()}
        </Paper>
      )}
      
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

export default VersionComparePage; 