import React, { useState, useEffect } from 'react';
import useAsync from './useAsync'
import useInterval from './useInterval'
import { json, post } from './rest'
import { getLocalIp, getLocalIPs } from './ip'
import useContestTime from './useContestTime';

const AppContext = React.createContext()

const AppProvider = ({children}) => {
    const [error, setError] = useState();

    const [ip, setIp] = useState();

    const { contestStartTime, contestEndTime, contestState, 
        contestIsRunning, contestHasStarted, contestHasFinished } = useContestTime([]);
    
    const [unreadQuestions, setUnreadQuestions] = useState(0);
    const [questions, setQuestions] = useState([]);
    const [unreadAnnouncements, setUnreadAnnouncements] = useState(0);
    const [announcements, setAnnouncements] = useState([]);

    const [shouldUpdateQuestions, setShouldUpdateQuestions] = useState(false);
    const { value: questionsData } = useAsync(json, 'questions', [shouldUpdateQuestions]);

    const [shouldUpdateAnnouncements, setShouldUpdateAnnouncements] = useState(false);
    const { value: announcementsData } = useAsync(json, 'announcements', [shouldUpdateAnnouncements]);

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
        updateQuestions();
        updateAnnouncements();
    }, 10000);
    
    useEffect(() => {
        getLocalIp().then(function(ip) {
            setIp(ip);
		});
    }, []);

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
                contestIsRunning: contestIsRunning,
                contestHasStarted: contestHasStarted,
                contestHasFinished: contestHasFinished,
                contestStartTime: contestStartTime, 
                contestEndTime: contestEndTime,
                contestState: contestState,
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
