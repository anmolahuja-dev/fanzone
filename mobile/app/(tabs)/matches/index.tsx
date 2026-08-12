import { View, Text, StyleSheet } from 'react-native';
import { useClubTheme } from '@/hooks/useClubTheme';

export default function MatchesScreen() {
  const theme = useClubTheme();

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <Text style={[theme.typography.h2, { color: theme.colors.text }]}>
        Matches
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', alignItems: 'center' },
});
