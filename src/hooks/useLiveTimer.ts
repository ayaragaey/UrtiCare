import { useState, useEffect } from 'react';
import { getLiveElapsed } from '../utils/dateHelpers';

/**
 * Decouples high-frequency (1s) ticking from the main UI layout.
 * Runs an interval to update elapsed time only when a valid timestamp is provided.
 */
export function useLiveTimer(lastTimestamp: string | undefined | null): string {
  const [elapsed, setElapsed] = useState<string>('0h 0m 0s');

  useEffect(() => {
    if (!lastTimestamp) {
      setElapsed('No dosage recorded');
      return;
    }

    const tick = () => {
      setElapsed(getLiveElapsed(lastTimestamp, new Date()));
    };

    tick(); // initial tick
    const timerId = setInterval(tick, 1000);

    return () => clearInterval(timerId);
  }, [lastTimestamp]);

  return elapsed;
}
