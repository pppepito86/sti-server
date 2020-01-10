import { useState, useEffect } from 'react';
import { json } from './rest';
import useInterval from './useInterval';

function useContestTime(params) {
    const [contestStartTime, setContestStartTime] = useState(null);
    const [contestEndTime, setContestEndTime] = useState(null);
    const [contestState, setContestState] = useState(null);
    const [contestIsRunning, setContestIsRunning] = useState(false);
    const [contestHasStarted, setContestHasStarted] = useState(false);
    const [contestHasFinished, setContestHasFinished] = useState(false);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(true);

    async function updateResource() {
        try {
            var startRequest = Date.now();
            const result = await json('time');
            var endRequest = Date.now();
            if (contestStartTime == null || contestEndTime == null || (endRequest-startRequest < 2000)) {
                setContestStartTime(endRequest + result.timeTillStart);
                setContestEndTime(startRequest + result.timeTillEnd);
                
                var newState = (result.timeTillEnd <= 0) ? "FINISHED" :
                    (result.timeTillStart <= 0) ? "RUNNING" :
                    "NOT_STARTED";
                if (newState !== contestState) setContestState(newState);
            }
            
        } catch (e) {
        }
    }
    useEffect(() => {
        async function getResource() {
            try {
                setLoading(true);
                await updateResource();
            } catch (e) {
                setError(e);
            } finally {
                setLoading(false);
            }
        }

        getResource();
    }, [...params]);

    useEffect(() => {
        if (contestStartTime === null || contestEndTime === null) return;
        
        var currentTime = Date.now();
        if (contestStartTime <= currentTime && currentTime < contestEndTime) {
            if (!contestIsRunning) setContestIsRunning(true);
        } else {
            if (contestIsRunning) setContestIsRunning(false);
        }
        if (contestStartTime <= currentTime) {
            if (!contestHasStarted) setContestHasStarted(true);
        } else {
            if (contestHasStarted) setContestHasStarted(false);
        }
        if (contestEndTime <= currentTime) {
            if (!contestHasFinished) setContestHasFinished(true);
        } else {
            if (contestHasFinished) setContestHasFinished(false);
        }
    }, [contestStartTime, contestEndTime, contestIsRunning, contestHasStarted, contestHasFinished]);

    function timeTillUpdate() {
        var currentTime = Date.now();
        if (contestState === "NOT_STARTED") {
            return Math.min(10000, Math.max(contestStartTime-currentTime, 0));
        } else if (contestState === "RUNNING") {
            return Math.min(10000, Math.max(contestEndTime-currentTime, 0));
        } else {
            return 10000;
        }
        
    }

    useInterval(async () => {
        await updateResource();
    }, timeTillUpdate());

  
    return { contestStartTime, contestEndTime, contestState, 
        contestIsRunning, contestHasStarted, contestHasFinished,
        error, loading };
}

export default useContestTime;