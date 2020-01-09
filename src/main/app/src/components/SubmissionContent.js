import React, { useState } from 'react';
import { useParams } from "react-router";
import Submission from './Submission';
import LoadingContent from './LoadingContent';
import useAsync from '../useAsync'
import useInterval from '../useInterval'
import { json } from '../rest'
import { useApp } from '../AppContext';

function SubmissionContent() {
  const contestIsRunning = useApp().contestIsRunning;
  const contestIsFinished = useApp().contestIsFinished;

  const { tid, sid } = useParams();
  const [refresh, setRefresh] = useState(0);
  const { value: submission } = useAsync(json, `tasks/${tid}/solutions/${sid}`, [tid, sid, refresh]);

  useInterval(() => {
    setRefresh(refresh+1);
  }, submission && !submission.points ? 5000 : null);

  if ((!contestIsRunning && !contestIsFinished) || !submission) return <LoadingContent />

  return (
    <div className="content-wrapper" style={{ minHeight: '550px' }}>
      <section className="content-header">
        <h1>
          Решение
          <small>детайли</small>
        </h1>
      </section>

      <section className="content">
        <div className="row">
          <section className="col-lg-12 connectedSortable ui-sortable">
            <div className="box">
              <Submission.SubmissionOverview tid={tid} submission={submission} />
              {submission.tests && <Submission.SubmissionDetails tests={submission.tests} />}
            </div>
            <Submission.SubmissionSource source={submission.source} />
          </section>
        </div>
      </section>
    </div>
  )
}

export default SubmissionContent
