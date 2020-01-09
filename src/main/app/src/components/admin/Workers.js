import React, { useState } from 'react'
import { useHistory, Link } from 'react-router-dom';
import useInterval from '../../useInterval'
import moment from 'moment'
import { json, blob, post } from '../../rest'
import { useApp } from '../../AppContext';
import Scoreboard from '../Scoreboard';
import ScoreBoardBox from '../ScoreBoardBox';
import Verdict from '../Verdict';

function Workers() {
  return (
    <div className="nav-tabs-custom" style={{ borderTop: '3px solid #d2d6de', borderBottom: '1px solid #f4f4f4' }}>
      <ul className="nav nav-tabs pull-right">
        <li className="active" style={{ marginTop: '-3px' }}><a href="#list-workers" data-toggle="tab">List</a></li>
        <li style={{ marginTop: '-3px' }}><a href="#ensure-workers" data-toggle="tab">Ensure</a></li>
        <li className="pull-left header" style={{ fontSize: '18px' }}>Workers</li>
      </ul>
      <div className="tab-content no-padding">
        <div className="tab-pane" id="ensure-workers" style={{ position: 'relative' }}>
          <EnsureWorkers />
        </div>
        <div className="tab-pane active" id="list-workers" style={{ position: 'relative', height: 'auto' }}>
          <ListWorkers workers={[]} />
        </div>
      </div>
    </div>
  )
}

function EnsureWorkers() {
  return (
    <div className="box-body">
      <form method="post" encType="multipart/form-data" action="/">
        <div className="box-body">
          <div className="form-group">
            <label htmlFor="count">Count</label>
            <input type="text" className="form-control" name="count" id="count" placeholder="How many workers would you like?" />
          </div>
        </div>
        <div class="box-footer">
          <button type="submit" class="btn btn-primary">Ensure</button>
        </div>
      </form>
    </div>
  )
}

function SubmitButton({submit, timeToSubmit}) {
  const [currentTime, setCurrentTime] = useState(Date.now());
  const [submissionTime] = useState(currentTime + timeToSubmit);

  useInterval(() => {
    setCurrentTime(Date.now());
  }, currentTime < submissionTime ? 1000 : null);

  const secondssLeft = currentTime < submissionTime ? parseInt((submissionTime - currentTime)/1000+1, 10):0;
  return (
    <div className="box-footer">
      <button disabled={secondssLeft>0} type="submit" onClick={e => submit(e)} id="submitcodebutton2" className="btn btn-primary">Предай</button>
      {secondssLeft>0 && <span id="timetosubmit3" style={{ marginLeft: '5px' }}>
        {`след ${secondssLeft} секунд${secondssLeft !== 1 ? 'и':'а'}`}
      </span>}
     </div>
    )
}

function ListWorkers({workers}) {
  return (
    <div className="box-body">
      {workers.length === 0 && <div>No workers</div>}

      {workers.length !== 0 && 
        <table class="table table-bordered">
          <thead>
            <tr>
              <th style={{width: '10px'}}>Id</th>
              <th>Url</th>
              <th>Type</th>
            </tr>
          </thead>
          <tbody>
            {
              workers.map((worker, i) => {
                return <tr key={i}>
                  <td>{worker.id}</td>
                  <td>{worker.url}</td>
                  <td>{worker.type}</td>
                </tr>
            })}
          </tbody>
        </table>
      }
    </div>
  )
}

export default { Workers }
