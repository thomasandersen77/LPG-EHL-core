import { useState, useEffect, useRef } from 'react';

/**
 * A hook that provides a smoothly updating counter by interpolating/extrapolating 
 * between real data updates.
 * 
 * @param actualValue The latest value from the backend
 * @param isActive Whether the counter should be currently "running" (e.g. pumping)
 * @param updateInterval Frequency of visual updates in ms (default 100ms)
 */
export function useSmoothCounter(
  actualValue: number,
  isActive: boolean,
  updateInterval: number = 100
) {
  const [displayValue, setDisplayValue] = useState(actualValue);
  
  // Track the history of updates to calculate velocity (rate)
  const stats = useRef({
    v1: actualValue, // value at t1
    t1: Date.now(),
    v2: actualValue, // value at t2 (latest known)
    t2: Date.now(),
    rate: 0,         // units per millisecond
  });

  // When the actual value from backend changes
  useEffect(() => {
    const now = Date.now();
    const s = stats.current;

    if (actualValue !== s.v2) {
      const dt = now - s.t2;
      
      // Calculate rate based on the last two observations
      if (dt > 0) {
        // Use a simple moving average or just the last interval
        // Here we just use the last interval for responsiveness
        const newRate = (actualValue - s.v2) / dt;
        
        // Sanity check: if rate is negative or zero but isActive, maybe use previous rate?
        // For fueling, volume only goes up.
        if (newRate > 0) {
          s.rate = newRate;
        }
      }

      s.v1 = s.v2;
      s.t1 = s.t2;
      s.v2 = actualValue;
      s.t2 = now;
    }

    if (!isActive) {
      // Sync immediately when stopped
      setDisplayValue(actualValue);
      s.rate = 0;
    }
  }, [actualValue, isActive]);

  // Periodic visual update
  useEffect(() => {
    if (!isActive) return;

    const intervalId = setInterval(() => {
      const s = stats.current;
      const now = Date.now();
      const dt = now - s.t2;

      // Extrapolate from the latest known value
      const predictedValue = s.v2 + (s.rate * dt);
      
      // Safety: We don't want to get too far ahead of reality if the connection is slow
      // though the user wants "constant speed", we should maybe bound it slightly.
      // For now, we follow the user's request for constant speed feeling.
      setDisplayValue(predictedValue);
      
    }, updateInterval);

    return () => clearInterval(intervalId);
  }, [isActive, updateInterval]);

  return displayValue;
}
