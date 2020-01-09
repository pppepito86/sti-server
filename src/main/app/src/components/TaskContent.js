import React, { useState } from 'react';
import { useParams } from "react-router";
import Task from './Task';
import LoadingContent from './LoadingContent';
import { json } from '../rest'
import useAsync from '../useAsync'
import { useApp } from '../AppContext';
import useTask from '../useTask';

const TaskContent = () => {
  const contestIsRunning = useApp().contestIsRunning;
  const contestIsFinished = useApp().contestIsFinished;

  const { tid } = useParams();
  const { task, submissions, nextSubmissionTime, loading } = useTask(tid);

  if (loading || (!contestIsRunning && !contestIsFinished)) return <LoadingContent />

  const points = submissions?submissions.reduce((prev, current) => Math.max(prev, current.points), 0) : 0;
  return (
    <div className="content-wrapper" style={{ minHeight: '550px' }}>
      <section className="content-header" style={{ display: 'flex', justifyContent: 'space-between' }}>
        <h1 style={{ display: 'inline-block', verticalAlign: 'top' }}>Задача {tid} - <b>{task.name}</b></h1>
        <div className="progress-group" style={{ display: 'inline-block', height: '26px', verticalAlign: 'top', width: '48.5%' }}>
          <span className="progress-text">Точки от видими тестове</span>
          <span className="progress-number"><b>{points}</b>/100</span>

          <div className="progress sm">
            <div className="progress-bar progress-bar-aqua" style={{ width: points + '%' }}></div>
          </div>
        </div>
      </section>

      <section className="content">
        <div className="row">
          <div className="col-md-6">
            <Task.TaskDescription tid={tid} />
            {contestIsRunning && <Task.TaskLimits time={task.time} memory={task.memory} />}
          </div>
          <div className="col-md-6">
            {contestIsFinished && <Task.TaskLimits time={task.time} memory={task.memory} />}
            {contestIsRunning && <Task.TaskSubmit tid={tid} nextSubmissionTime={nextSubmissionTime} />}
          </div>
        </div>
        <div className="row">
          <div className="col-md-12">
            {submissions && <Task.TaskSubmissions tid={tid} submissions={submissions} />}
          </div>
        </div>
      </section>
    </div>
  )
}

export default TaskContent
