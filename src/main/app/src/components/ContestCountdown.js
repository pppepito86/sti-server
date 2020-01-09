import React from 'react'
import Countdown from 'react-countdown-now';
import { useApp } from '../AppContext';

const ContestCountdown = () => {
    const contestState = useApp().contestState;
    const contestStartTime = useApp().contestStartTime;
    const contestEndTime = useApp().contestEndTime;

    return (
        <div id="timer" style={{ color: '#b8c7ce', textAlign: 'center', fontSize: '36px' }}>
            {contestState==="NOT_STARTED" &&
                <Countdown date={contestStartTime} daysInHours={true} />
            }
            {contestState==="RUNNING" &&
                <Countdown date={contestEndTime} daysInHours={true} >
                    <span>Състезанието приключи</span>
                </Countdown>
            }
            {contestState==="FINISHED" && 
                <span>Състезанието приключи</span>
            }
        </div>
    )
}

export default ContestCountdown
