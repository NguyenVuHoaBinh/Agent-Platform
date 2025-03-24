import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  TextField,
  Button,
  FormHelperText
} from '@mui/material';

interface CategoryDialogProps {
  open: boolean;
  onClose: () => void;
  onSave: (category: string) => void;
  existingCategories: string[];
}

const CategoryDialog: React.FC<CategoryDialogProps> = ({
  open,
  onClose,
  onSave,
  existingCategories
}) => {
  const [category, setCategory] = useState('');
  const [error, setError] = useState<string | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setCategory(e.target.value);
    setError(null);
  };

  const handleSubmit = () => {
    // Validate category name
    if (!category.trim()) {
      setError('Category name is required');
      return;
    }

    // Check if category already exists
    if (existingCategories.includes(category.trim())) {
      setError('Category already exists');
      return;
    }

    // Submit valid category
    onSave(category.trim());
    setCategory('');
  };

  const handleClose = () => {
    setCategory('');
    setError(null);
    onClose();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleSubmit();
    }
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Add New Category</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ mb: 2 }}>
          Enter a name for the new template category. This will be available for all templates.
        </DialogContentText>
        <TextField
          autoFocus
          margin="dense"
          label="Category Name"
          fullWidth
          value={category}
          onChange={handleChange}
          error={!!error}
          onKeyDown={handleKeyDown}
        />
        {error && <FormHelperText error>{error}</FormHelperText>}
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>Cancel</Button>
        <Button onClick={handleSubmit} variant="contained" color="primary">
          Add Category
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default CategoryDialog; 