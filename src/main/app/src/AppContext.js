import React, { useState, useEffect } from 'react';
import useAsync from './useAsync'
import useInterval from './useInterval'
import { json, post } from './rest'
import { getLocalIp, getLocalIPs } from './ip'

const AppContext = React.createContext()

const AppProvider = ({children}) => {
    const [error, setError] = useState();

    const [ip, setIp] = useState();

    const [now, setNow] = useState(Date.now());
    const [time, setTime] = useState();
    const [contestIsRunning, setContestIsRunning] = useState(false);
    const [contestIsFinished, setContestIsFinished] = useState(false);
    const [contestIsStarted, setContestIsStarted] = useState(false);
    
    const [unreadQuestions, setUnreadQuestions] = useState(0);
    const [questions, setQuestions] = useState([]);
    const [unreadAnnouncements, setUnreadAnnouncements] = useState(0);
    const [announcements, setAnnouncements] = useState([]);

    const [shouldUpdateTime, setShouldUpdateTime] = useState(false);
    const { value: timeData } = useAsync(json, 'time', [shouldUpdateTime]);

    const [shouldUpdateQuestions, setShouldUpdateQuestions] = useState(false);
    const { value: questionsData } = useAsync(json, 'questions', [shouldUpdateQuestions]);

    const [shouldUpdateAnnouncements, setShouldUpdateAnnouncements] = useState(false);
    const { value: announcementsData } = useAsync(json, 'announcements', [shouldUpdateAnnouncements]);

    function updateTime() {
        setShouldUpdateTime(shouldUpdateTime => !shouldUpdateTime);
    }

    function updateQuestions() {
        setShouldUpdateQuestions(shouldUpdateQuestions => !shouldUpdateQuestions);
    }

    function updateAnnouncements() {
        setShouldUpdateAnnouncements(shouldUpdateAnnouncements => !shouldUpdateAnnouncements);
    }

    async function markQuestionsSeen() {
        for (var i = 0; i < questions.length; i++) {
            if (questions[i].seen) continue;
            await markQuestionSeen(questions[i].id);
        }
        updateQuestions();
    }

    const markQuestionSeen = async (id) => {
        const formData = new FormData();
        formData.append('id', id);
        await post(`questions/seen`, formData);
    }

    async function markAnnouncementsSeen() {
        for (var i = 0; i < announcements.length; i++) {
            if (announcements[i].seen) continue;
            await markAnnouncementSeen(announcements[i].id);
        }
        updateAnnouncements();
    }

    const markAnnouncementSeen = async (id) => {
        const formData = new FormData();
        formData.append('id', id);
        await post(`announcements/seen`, formData);
    }

    useInterval(() => {
        setNow(Date.now());
        updateTime();
        updateQuestions();
        updateAnnouncements();
    }, 10000);
    
    useInterval(() => {
        if (!contestIsRunning) setContestIsRunning(true);
        if (!contestIsFinished) setContestIsFinished(false);
        if (!contestIsStarted) setContestIsStarted(true);
    }, time && !contestIsRunning && !contestIsFinished ? time.timeTillStart : null);

    useInterval(() => {
        if (contestIsRunning) setContestIsRunning(false);
        if (!contestIsFinished) setContestIsFinished(true);
        if (!contestIsStarted) setContestIsStarted(true);
    }, time && contestIsRunning ? time.timeTillEnd : null);

    useEffect(() => {
        getLocalIp().then(function(ip) {
            setIp(ip);
		});
    }, []);

    useEffect(() => {
        if (timeData) {
            setTime({...timeData,
                startTime: Date.now() + timeData.timeTillStart,
                endTime: now + timeData.timeTillEnd
            });
            if (!contestIsRunning && timeData.timeTillStart <= 0 && timeData.timeTillEnd > 0) {
                setContestIsRunning(true);
            }
            if (contestIsRunning && !(timeData.timeTillStart <= 0 && timeData.timeTillEnd > 0)) {
                setContestIsRunning(false);
            }
            if (!contestIsFinished && timeData.timeTillEnd <= 0) setContestIsFinished(true);
            if (contestIsFinished && timeData.timeTillEnd > 0) setContestIsFinished(false);
            if (!contestIsStarted && (contestIsRunning || contestIsFinished)) setContestIsStarted(true);
            if (contestIsStarted && !(contestIsRunning || contestIsFinished)) setContestIsStarted(false);
        }
    }, [timeData]);

    useEffect(() => {
        if (questionsData) {
            setQuestions(questionsData);
            setUnreadQuestions(questionsData.slice().filter(q => q.answer).filter(q => !q.seen).length);
        }
    }, [questionsData]);

    useEffect(() => {
        if (announcementsData) {
            setAnnouncements(announcementsData);
            setUnreadAnnouncements(announcementsData.slice().filter(a => !a.seen).length);
        }
    }, [announcementsData]);

    return (
        <AppContext.Provider
            value={{
                error: error,
                setError: setError,
                time: time,
                contestIsRunning: contestIsRunning,
                contestIsFinished: contestIsFinished,
                contestIsStarted: contestIsStarted,
                questions: questions,
                unreadQuestions: unreadQuestions,
                markQuestionsSeen: markQuestionsSeen,
                updateQuestions: updateQuestions,
                announcements: announcements,
                unreadAnnouncements: unreadAnnouncements,
                updateAnnouncements: updateAnnouncements,
                markAnnouncementsSeen: markAnnouncementsSeen
            }}>
            {children}
        </AppContext.Provider>
    )
}

const useApp = () => React.useContext(AppContext)

export { AppProvider, useApp }
