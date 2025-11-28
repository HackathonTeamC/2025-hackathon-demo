import React from 'react';
import { Select, MenuItem, FormControl, Box } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Language } from '@mui/icons-material';

const LanguageSwitcher: React.FC = () => {
  const { i18n } = useTranslation();

  const handleChange = (event: any) => {
    i18n.changeLanguage(event.target.value);
  };

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', minWidth: 100 }}>
      <Language sx={{ mr: 0.5, fontSize: '1.2rem', color: 'inherit' }} />
      <FormControl size="small" sx={{ minWidth: 70 }}>
        <Select
          value={i18n.language}
          onChange={handleChange}
          sx={{
            color: 'inherit',
            '& .MuiOutlinedInput-notchedOutline': {
              border: 'none'
            },
            '& .MuiSelect-select': {
              py: 0.5,
              fontSize: '0.875rem'
            }
          }}
        >
          <MenuItem value="en">English</MenuItem>
          <MenuItem value="ja">日本語</MenuItem>
          <MenuItem value="zh">中文</MenuItem>
        </Select>
      </FormControl>
    </Box>
  );
};

export default LanguageSwitcher;
