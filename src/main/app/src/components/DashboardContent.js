import React from 'react';
import { useApp } from '../AppContext';
import {post} from '../rest';
import { json } from '../rest'
import useAsync from '../useAsync';
import LoadingContent from './LoadingContent';

const DashboardContent = () => {
  const contestIsRunning = useApp().contestIsRunning;
  const contestIsFinished = useApp().contestIsFinished;

  const { value: tasks } = useAsync(json, `tasks/info`, [contestIsRunning]);

  if (!tasks) return <LoadingContent />

  return (
    <div className="content-wrapper" style={{ minHeight: '550px' }}>
      <section className="content">
        <div className="row">
          <div className="col-md-8">
            <Announcements />
            {(contestIsRunning || contestIsFinished) && tasks && tasks.length > 0 && <TasksInfo tasks={tasks} />}
          </div>

        {(contestIsRunning || contestIsFinished) && tasks && tasks.length > 0 &&  <TasksPoints tasks={tasks} />}
        </div>
      </section>
    </div>
  )
}

const SeenAnnouncement = ({id}) => {
  const updateAnnouncements = useApp().updateAnnouncements;

  const markAnnouncementSeen = async () => {
    const formData = new FormData();
    formData.append('id', id);

    await post(`announcements/seen`, formData);
    updateAnnouncements();
  }

  return (
    <div>
      <input name={id} onClick={markAnnouncementSeen} type="checkbox" className="checkbox pull-left" />
      &nbsp;Прочетох
      </div>

  )
}

function Announcements() {
  const announcements = useApp().announcements;
  return (
    <div>
    {
      announcements.slice().reverse().map((a) => {
        return <div key={a.id} className={'callout callout-'+(a.seen?'info':'danger')}>
          {a.topic && <h4>{a.topic}</h4>}
          <p>{a.announcement}</p>
          {!a.seen && false && <SeenAnnouncement id={a.id} />}
        </div>
    })}
    </div>
  )
}

function TasksInfo({tasks}) {
  return (
    <div className="box">
      <div className="box-header with-border">
        <h3 className="box-title">Ограничения</h3>
      </div>
      <div className="box-body table-responsive">
        <table className="table table-bordered table-hover">
          <thead>
            <tr>
              <th>Задача</th>
              <th>Време</th>
              <th>Памет</th>
              <th>Фийдбек</th>
              <th>Събмити</th>
            </tr>  
          </thead>
          <tbody>
            {
              tasks && tasks.map((t, i) => {
                return <tr key={i}>
                  <td>{t.name}</td>
                  <td>{t.time?t.time+' s':'-'} </td>
                  <td>{t.memory?t.memory+' MB':'-'}</td>
                  <td>{t.feedback?t.feedback:'-'}</td>
                  <td>{t.submissions?t.submissions:'-'}</td>
                </tr>
              })}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function TasksPoints({tasks}) {
  return (
    <div className="col-md-4">
      <p className="text-center">
        <strong>Видими точки</strong>
      </p>

      {
        tasks && tasks.map((t, i) => {
          return <div key={i} className="progress-group">
            <span className="progress-text">{t.name}</span>
            <span className="progress-number"><b>{t.publicScore}</b>/{t.maxPublicScore}</span>

            <div className="progress sm">
              <div className="progress-bar progress-bar-aqua" style={{ width: 100*t.publicScore/t.maxPublicScore+'%' }}></div>
            </div>
          </div>
      })}
    </div>
  )
}

export default DashboardContent
