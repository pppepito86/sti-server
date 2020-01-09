import React from 'react'
import Countdown from 'react-countdown-now';
import { useApp } from '../AppContext';

const ContestCountdown = () => {
    const time = useApp().time;
   
    return (
        <div id="timer" style={{ color: '#b8c7ce', textAlign: 'center', fontSize: '36px' }}>
            {time && time.timeTillStart > 0 &&
                <Countdown date={time.startTime} daysInHours={true} />
            }
            {time && time.timeTillStart <= 0 &&
                <Countdown date={time.endTime} daysInHours={true} >
                    <span>Състезанието приключи</span>
                </Countdown>
            }            
        </div>
    )
}

export default ContestCountdown
