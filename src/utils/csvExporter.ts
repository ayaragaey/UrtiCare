import { Platform, Share } from 'react-native';
import { LogEntry } from '../types/tracker.types';
import { parseISO, format } from 'date-fns';

const escapeCSV = (str: string) => {
  if (str === null || str === undefined) return '';
  const clean = String(str).replace(/"/g, '""');
  // If string contains comma, double quote, or newline, wrap it in double quotes
  if (clean.includes(',') || clean.includes('\n') || clean.includes('"')) {
    return `"${clean}"`;
  }
  return clean;
};

const getFriendlyTypeName = (type: string) => {
  switch (type) {
    case 'FLARE_UP': return 'Symptom Flare-up';
    case 'ANTIHISTAMINE': return 'Antihistamine Intake';
    case 'CORTISONE': return 'Biological Treatment';
    case 'CONSUMPTION': return 'Consumption';
    default: return type;
  }
};

export const exportLogsToCSV = async (entries: LogEntry[]): Promise<boolean> => {
  try {
    if (!entries || entries.length === 0) {
      return false;
    }

    const headers = [
      'ID',
      'Date & Time',
      'Type',
      'Item Name',
      'Category',
      'Amount/Dose',
      'Notes',
      'Status'
    ];

    const rows = entries.map(entry => {
      let formattedDate = entry.timestamp;
      try {
        formattedDate = format(parseISO(entry.timestamp), 'yyyy-MM-dd HH:mm');
      } catch (e) {
        // Fallback to raw string
      }

      return [
        entry.id,
        formattedDate,
        getFriendlyTypeName(entry.type),
        entry.itemName || '',
        entry.category || '',
        entry.amount || '',
        entry.notes || '',
        (entry as any).status || ''
      ].map(escapeCSV).join(',');
    });

    const csvContent = [headers.join(','), ...rows].join('\n');

    if (Platform.OS === 'web') {
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `urticare_logs_${format(new Date(), 'yyyyMMdd_HHmmss')}.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      return true;
    } else {
      // Dynamic require to prevent errors if expo modules are missing/loading on different platforms
      try {
        const FileSystem = require('expo-file-system');
        const Sharing = require('expo-sharing');

        if (FileSystem && Sharing) {
          const fileUri = `${FileSystem.documentDirectory}urticare_logs_${format(new Date(), 'yyyyMMdd_HHmmss')}.csv`;
          await FileSystem.writeAsStringAsync(fileUri, csvContent, {
            encoding: FileSystem.EncodingType.UTF8
          });

          const isAvailable = await Sharing.isAvailableAsync();
          if (isAvailable) {
            await Sharing.shareAsync(fileUri);
            return true;
          }
        }
      } catch (err) {
        console.warn('Expo FileSystem/Sharing not available, falling back to Share API:', err);
      }

      // Fallback to React Native Share API
      await Share.share({
        title: 'Exported UrtiCare Logs',
        message: csvContent
      });
      return true;
    }
  } catch (error) {
    console.error('Error exporting logs to CSV:', error);
    return false;
  }
};
