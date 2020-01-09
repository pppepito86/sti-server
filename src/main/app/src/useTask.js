import { useState, useEffect } from 'react';
import { json } from './rest'
import useInterval from './useInterval';

function useTask(tid) {
    const [task, setTask] = useState(null);
    const [submissions, setSubmissions] = useState(null);
    const [nextSubmissionTime, setNextSubmissionTime] = useState(null);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function getResource() {
            try {
                setLoading(true);
                setTask(null);
                setSubmissions(null);
                const task = await json(`tasks/${tid}`);
                const submissions = await json(`tasks/${tid}/submissions`);
                const timeLeft = await json(`timeToSubmit`);
                setNextSubmissionTime(Date.now() + timeLeft.timeToSubmit);
                setSubmissions(submissions);
                setTask(task);
            } catch (e) {
                setError(e);
            } finally {
                setLoading(false);
            }
        }

        getResource();
    }, [tid]);

    useInterval(async () => {
        const submissions = await json(`tasks/${tid}/submissions`);
        setSubmissions(submissions);
    }, submissions && submissions.some(s => s.verdict==='waiting'||s.verdict==='judging') ? 5000 : null);
    
    return { task, submissions, nextSubmissionTime, error, loading };
}

export default useTask;
