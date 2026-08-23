import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Platform } from 'react-native';
import { useTrackerStore } from '../store/useTrackerStore';
import { theme } from '../styles/theme';
import { IconCapsule, IconDroplet, IconTablet, IconBackArrow } from './common/CustomIcons';
import { parseISO, differenceInSeconds } from 'date-fns';

function formatElapsedTimer(timestamp?: string, nowMs: number = Date.now()): { display: string; hasData: boolean; progressRatio: number } {
  if (!timestamp) {
    return { display: '--', hasData: false, progressRatio: 0 };
  }
  try {
    const start = new Date(timestamp).getTime();
    const diffSecs = Math.max(0, Math.floor((nowMs - start) / 1000));
    const totalHrs = Math.floor(diffSecs / 3600);
    const mins = Math.floor((diffSecs % 3600) / 60);
    const secs = diffSecs % 60;

    // Progress on a 24-hour cycle
    const progressRatio = Math.min(1, Math.max(0.02, diffSecs / (24 * 3600)));

    if (totalHrs >= 48) {
      const days = Math.floor(totalHrs / 24);
      const remHrs = totalHrs % 24;
      return { display: `${days}d ${remHrs}h`, hasData: true, progressRatio };
    } else if (totalHrs >= 1) {
      return { display: `${totalHrs}h ${mins}m`, hasData: true, progressRatio };
    } else {
      return { display: `${mins}m ${secs}s`, hasData: true, progressRatio };
    }
  } catch (e) {
    return { display: '--', hasData: false, progressRatio: 0 };
  }
}

export default function CircularStatusTimer() {
  const [activeViewIndex, setActiveViewIndex] = useState(0); // 0: Antihistamine, 1: Flare-Up, 2: Corticosteroid
  const [nowMs, setNowMs] = useState(() => Date.now());

  // Ticker for live real-time countdown/elapsed updates
  useEffect(() => {
    const timer = setInterval(() => {
      setNowMs(Date.now());
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const entries = useTrackerStore(state => state.entries);

  // 1. Antihistamine
  const lastAntihistamine = entries.find(e => e.type === 'ANTIHISTAMINE')?.timestamp;
  const ahData = formatElapsedTimer(lastAntihistamine, nowMs);

  // 2. Flare-Up
  const lastFlare = entries.find(e => e.type === 'FLARE_UP')?.timestamp;
  const flareData = formatElapsedTimer(lastFlare, nowMs);

  // 3. Corticosteroid / Biological Treatment
  const lastCortisone = entries.find(e => e.type === 'CORTISONE')?.timestamp;
  const cortisoneData = formatElapsedTimer(lastCortisone, nowMs);

  const handleNextView = () => {
    setActiveViewIndex(prev => (prev + 1) % 3);
  };

  // Determine current active item details
  let currentDisplay = ahData.display;
  let currentHasData = ahData.hasData;
  let currentRatio = ahData.progressRatio;
  let currentSubtitle = ahData.hasData ? 'Since last antihistamine' : 'No antihistamine logged yet';

  if (activeViewIndex === 1) {
    currentDisplay = flareData.display;
    currentHasData = flareData.hasData;
    currentRatio = flareData.progressRatio;
    currentSubtitle = flareData.hasData ? 'Since last flare-up' : 'No flare-up logged yet';
  } else if (activeViewIndex === 2) {
    currentDisplay = cortisoneData.display;
    currentHasData = cortisoneData.hasData;
    currentRatio = cortisoneData.progressRatio;
    currentSubtitle = cortisoneData.hasData ? 'Since last corticosteroid' : 'No corticosteroid logged yet';
  }

  // Calculate SVG strokeDashoffset (circumference ~ 515)
  const strokeOffset = currentHasData ? 515 * (1 - currentRatio) : 515;

  // Calculate dynamic Milestone from actual logs
  const calculateMilestone = () => {
    if (entries.length === 0) {
      return {
        isSet: false,
        text: 'No logs yet • Ready to track'
      };
    }

    const now = new Date();
    if (lastFlare) {
      try {
        const flareDate = parseISO(lastFlare);
        const diffSec = Math.max(0, differenceInSeconds(now, flareDate));
        const diffHours = Math.floor(diffSec / 3600);
        const days = Math.floor(diffHours / 24);
        const remHours = diffHours % 24;

        if (days > 0) {
          return {
            isSet: true,
            stable: `${days}d ${remHours}h Stable`,
            remission: `${days} Days Remission`,
          };
        } else if (diffHours > 0) {
          return {
            isSet: true,
            stable: `${diffHours} Hours Stable`,
            remission: 'Day 1 Remission',
          };
        } else {
          return {
            isSet: true,
            stable: 'Flare Active',
            remission: 'Monitoring Symptoms',
          };
        }
      } catch {
        // Fallback if parsing fails
      }
    }

    // If user has logs but zero flare-ups logged, calculate stability from oldest log
    const oldestEntry = entries[entries.length - 1];
    if (oldestEntry?.timestamp) {
      try {
        const oldestDate = parseISO(oldestEntry.timestamp);
        const diffSec = Math.max(0, differenceInSeconds(now, oldestDate));
        const diffHours = Math.floor(diffSec / 3600);
        const days = Math.floor(diffHours / 24);
        const remHours = diffHours % 24;

        if (days > 0) {
          return {
            isSet: true,
            stable: `${days}d ${remHours}h Flare-Free`,
            remission: `${days} Days Remission`,
          };
        } else if (diffHours > 0) {
          return {
            isSet: true,
            stable: `${diffHours}h Flare-Free`,
            remission: 'Remission Active',
          };
        }
      } catch {
        // Fallback
      }
    }

    return {
      isSet: false,
      text: 'Tracking Active'
    };
  };

  const milestone = calculateMilestone();

  return (
    <View style={styles.outerWrapper}>
      {/* Container holding Circle Ring and Side Swipe Arrow */}
      <View style={styles.circleOuterRow}>
        <TouchableOpacity
          style={styles.circleContainer}
          activeOpacity={0.9}
          onPress={handleNextView}
        >
          {/* Outer Progress Ring */}
          {Platform.OS === 'web' ? (
            <svg style={styles.svgRing as any} width="180" height="180" viewBox="0 0 180 180">
              <defs>
                <linearGradient id="brandRingGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#814b92" />
                  <stop offset="50%" stopColor="#509729" />
                  <stop offset="100%" stopColor="#1a7e97" />
                </linearGradient>
              </defs>

              <circle cx="90" cy="90" r="82" stroke="#E2E8F0" strokeWidth="5" fill="none" />
              {currentHasData && (
                <circle
                  cx="90"
                  cy="90"
                  r="82"
                  stroke="url(#brandRingGradient)"
                  strokeWidth="6"
                  strokeDasharray="515"
                  strokeDashoffset={strokeOffset}
                  strokeLinecap="round"
                  fill="none"
                  transform="rotate(-90 90 90)"
                />
              )}
            </svg>
          ) : (
            <View style={[styles.fallbackProgressRing, !currentHasData && { borderColor: '#E2E8F0' }]} />
          )}

          {/* Inner Circle Content */}
          <View style={styles.innerCircle}>
            {activeViewIndex === 0 && (
              <>
                <View style={[styles.iconWrapper, { backgroundColor: '#ECFDF5' }]}>
                  <IconCapsule color="#509729" size={22} />
                </View>
                <Text style={[styles.timerValue, !ahData.hasData && { fontSize: 26, color: theme.colors.textMuted }]}>
                  {ahData.display}
                </Text>
                <Text style={styles.subtitle}>{currentSubtitle}</Text>
              </>
            )}

            {activeViewIndex === 1 && (
              <>
                <View style={[styles.iconWrapper, { backgroundColor: '#F5F3FF' }]}>
                  <IconDroplet color="#814B92" size={22} />
                </View>
                <Text style={[styles.timerValue, !flareData.hasData && { fontSize: 26, color: theme.colors.textMuted }]}>
                  {flareData.display}
                </Text>
                <Text style={styles.subtitle}>{currentSubtitle}</Text>
              </>
            )}

            {activeViewIndex === 2 && (
              <>
                <View style={[styles.iconWrapper, { backgroundColor: '#F0FDFA' }]}>
                  <IconTablet color="#1A7E97" size={22} />
                </View>
                <Text style={[styles.timerValue, !cortisoneData.hasData && { fontSize: 26, color: theme.colors.textMuted }]}>
                  {cortisoneData.display}
                </Text>
                <Text style={styles.subtitle}>{currentSubtitle}</Text>
              </>
            )}
          </View>
        </TouchableOpacity>

        {/* Side Swipe Arrow Button */}
        <TouchableOpacity
          style={styles.swipeBtn}
          activeOpacity={0.8}
          onPress={handleNextView}
        >
          <View style={{ transform: [{ rotate: '180deg' }] }}>
            <IconBackArrow size={18} />
          </View>
        </TouchableOpacity>
      </View>

      {/* Pager Indicator Dots */}
      <View style={styles.dotsRow}>
        {[0, 1, 2].map(idx => {
          const isActive = activeViewIndex === idx;
          const dotColor = idx === 0 ? '#509729' : idx === 1 ? '#814B92' : '#1A7E97';
          return (
            <View
              key={idx}
              style={[
                styles.dot,
                { backgroundColor: isActive ? dotColor : '#CBD5E1', width: isActive ? 8 : 6, height: isActive ? 8 : 6 }
              ]}
            />
          );
        })}
      </View>

      {/* Milestone Section with "Current Milestone" label aligned to the left */}
      <View style={styles.milestoneSection}>
        <Text style={styles.milestoneLabel}>Current Milestone</Text>
        <View style={styles.statusSummaryContainer}>
          {milestone.isSet ? (
            <Text style={styles.statusSummaryText}>
              <Text style={styles.diamondEmoji}>💎 </Text>
              <Text style={styles.statusHighlight}>{milestone.stable}</Text>
              <Text style={styles.statusDivider}>  |  </Text>
              <Text style={styles.statusHighlight}>{milestone.remission}</Text>
              <Text style={styles.diamondEmoji}> 💎</Text>
            </Text>
          ) : (
            <Text style={styles.statusSummaryText}>
              <Text style={styles.diamondEmoji}>💎 </Text>
              <Text style={styles.statusMutedHighlight}>{milestone.text}</Text>
              <Text style={styles.diamondEmoji}> 💎</Text>
            </Text>
          )}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  outerWrapper: {
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: 12,
    width: '100%',
  },
  circleOuterRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
    width: '100%',
  },
  circleContainer: {
    width: 180,
    height: 180,
    borderRadius: 90,
    justifyContent: 'center',
    alignItems: 'center',
    position: 'relative',
    backgroundColor: '#FFFFFF',
    ...theme.shadows.subtle,
  },
  svgRing: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
  },
  fallbackProgressRing: {
    position: 'absolute',
    width: 176,
    height: 176,
    borderRadius: 88,
    borderWidth: 5,
    borderColor: '#509729',
    borderBottomColor: '#814b92',
  },
  innerCircle: {
    width: 162,
    height: 162,
    borderRadius: 81,
    backgroundColor: '#FFFFFF',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 12,
  },
  iconWrapper: {
    marginVertical: 4,
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
  },
  timerValue: {
    fontSize: 30,
    fontWeight: theme.typography.weight.heavy,
    color: theme.colors.textDark,
    letterSpacing: -0.5,
    marginVertical: 2,
  },
  subtitle: {
    fontSize: 11,
    fontWeight: theme.typography.weight.medium,
    color: theme.colors.textMuted,
    textAlign: 'center',
  },
  swipeBtn: {
    position: 'absolute',
    right: 24,
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#E2E8F0',
    justifyContent: 'center',
    alignItems: 'center',
    ...theme.shadows.subtle,
  },
  dotsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginTop: 8,
  },
  dot: {
    borderRadius: 4,
  },
  milestoneSection: {
    marginTop: 12,
    alignItems: 'flex-start',
  },
  milestoneLabel: {
    fontSize: 10,
    fontWeight: theme.typography.weight.bold,
    color: theme.colors.textMuted,
    marginBottom: 4,
    marginLeft: 4,
  },
  statusSummaryContainer: {
    paddingHorizontal: 16,
    paddingVertical: 6,
    backgroundColor: '#FFFFFF',
    borderRadius: theme.borderRadius.full,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    ...theme.shadows.subtle,
  },
  statusSummaryText: {
    fontSize: 12,
    textAlign: 'center',
  },
  diamondEmoji: {
    fontSize: 11,
  },
  statusHighlight: {
    color: theme.colors.textDark,
    fontWeight: theme.typography.weight.bold,
  },
  statusMutedHighlight: {
    color: theme.colors.textMuted,
    fontWeight: theme.typography.weight.semibold,
  },
  statusDivider: {
    color: '#CBD5E1',
  },
});
