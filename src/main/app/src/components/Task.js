import React, { useState } from 'react'
import { useHistory, Link } from 'react-router-dom';
import useInterval from '../useInterval'
import moment from 'moment'
import { json, blob, post } from '../rest'
import Verdict from './Verdict';
import ScoreBoardBox from './ScoreBoardBox';
import { useAppError } from '../AppErrorContext';

var FileSaver = require('file-saver');

async function download(tid) {
  const contest = localStorage.getItem("contest");
  const name = (await json(`tasks/${tid}`)).name;
  const data = await blob(`tasks/${tid}/pdf`);
  const pdf = new Blob([data], { type: 'application/pdf' });
  FileSaver.saveAs(pdf, `${contest}${tid}-${name}.pdf`);
}

function TaskDescription({ tid }) {
  return (
    <div className="box">
      <div className="box-header with-border">
        <h3 className="box-title">Условие</h3>
      </div>
      <div className="box-body">
        <Link to={`/task/${tid}/pdf`} className="btn btn-info" >Отвори</Link>
        <button onClick={() => download(tid)} to={`/task/${tid}/pdf`} style={{ marginLeft: '3px' }} className="btn btn-info">Изтегли</button>
      </div>
    </div>
  )
}

function TaskLimits({ time, memory }) {
  return (
    <div className="box">
      <div className="box-header with-border">
        <h3 className="box-title">Ограничения</h3>
      </div>
      <div className="box-body">
        <table className="table table-bordered">
          <tbody>
            <tr>
              <td>Време</td>
              <td>{time} s</td>
            </tr>
            <tr>
              <td>Памет</td>
              <td>{memory} MB</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  )
}

function TaskSubmit({ tid, nextSubmissionTime }) {
  return (
    <div className="nav-tabs-custom" style={{ borderTop: '3px solid #d2d6de', borderBottom: '1px solid #f4f4f4' }}>
      <ul className="nav nav-tabs pull-right">
        <li className="active" style={{ marginTop: '-3px' }}><a href="#file-upload" data-toggle="tab">Файл</a></li>
        <li style={{ marginTop: '-3px' }}><a href="#source-upload" data-toggle="tab">Код</a></li>
        <li className="pull-left header" style={{ fontSize: '18px' }}>Предай решение</li>
      </ul>
      <div className="tab-content no-padding">
        <div className="tab-pane active" id="file-upload" style={{ position: 'relative' }}>
          <TaskSubmitFile tid={tid} nextSubmissionTime={nextSubmissionTime} />
        </div>
        <div className="tab-pane" id="source-upload" style={{ position: 'relative', height: 'auto' }}>
          <TaskSubmitCode tid={tid} nextSubmissionTime={nextSubmissionTime} />
        </div>
      </div>
    </div>
  )
}

function TaskSubmitFile({ tid, nextSubmissionTime }) {
  const history = useHistory();
  const [file, setFile] = useState();
  const setError = useAppError().setError;

  async function submit(e) {
    e.preventDefault();
    if (!file) {
      setError({title: "Няма файл", message: "Не сте изброли файл!"});
      return;
    }
    if (file.size > 64*1024) {
      setError({title: "Твърде голям файл", message: `Превишавате максималната големина на файл! Вашият файл е ${file.size}B при максимално позволени ${64*1024}B.`});
      return;
    }

    const formData = new FormData();
    formData.append('file', new Blob([file]), file.name);
    formData.append('ip', '127.0.0.1');

    const data = await post(`tasks/${tid}/solutions`, formData);
    history.push(`/task/${tid}/submission/${data.sid}`);
  }

  return (
    <div className="box-body">
      <form method="post" encType="multipart/form-data" action="/user/submit-file">
        <div className="box-body">

          <input type="hidden" className="form-control" name="problemNumber" id="problemNumber" value="1" />

          <div className="form-group">
            <label htmlFor="file">Файл</label>
            <input onChange={(e)=>setFile(e.target.files[0])} type="file" name="file" id="file" accept=".cpp,.c" />
          </div>
          <input type="hidden" name="ip" id="ip" value="92ed92a0-4e42-4b8e-b686-a6eb0c1d80c9.local" />
        </div>

        <SubmitButton submit={submit} nextSubmissionTime={nextSubmissionTime}/>
      </form>
    </div>
  )
}

function TaskSubmitCode({ tid, nextSubmissionTime }) {
  const history = useHistory();
  const [code, setCode] = useState("");
  const setError = useAppError().setError;

  async function submit(e) {
    e.preventDefault();
    if (!code || !code.trim().length) {
      setError({title: "Няма код", message: "Не сте въвели код!"});
      return;
    }

    const formData = new FormData();
    formData.append('code', code);
    formData.append('ip', '127.0.0.1');

    const data = await post(`tasks/${tid}/solutions`, formData);
    history.push(`/task/${tid}/submission/${data.sid}`);
  }

  return (
    <div className="box-body">
      <form method="post" action="/">
        <div className="box-body">
          <input type="hidden" className="form-control" name="problemNumber" id="problemNumber" value="1" />

          <div className="form-group">
            <textarea onChange={(e) => setCode(e.target.value)} name="code" placeholder="Поставете вашият код" maxLength="65536" style={{ width: '100%', height: '80px', fontSize: '14px', lineHeight: '18px', border: '1px solid #dddddd', padding: '10px' }}></textarea>
          </div>
          <input type="hidden" name="ip" id="ip" value="92ed92a0-4e42-4b8e-b686-a6eb0c1d80c9.local" />
        </div>

        <SubmitButton submit={submit} nextSubmissionTime={nextSubmissionTime}/>
      </form>
    </div>
  )
}

function SubmitButton({submit, nextSubmissionTime}) {
  const [currentTime, setCurrentTime] = useState(Date.now());

  useInterval(() => {
    setCurrentTime(Date.now());
  }, currentTime < nextSubmissionTime ? 500 : null);

  const secondsLeft = currentTime < nextSubmissionTime ? parseInt((nextSubmissionTime - currentTime)/1000+1, 10):0;
  return (
    <div className="box-footer">
      <button disabled={secondsLeft>0} type="submit" onClick={e => submit(e)} id="submitcodebutton2" className="btn btn-primary">Предай</button>
      {secondsLeft>0 && <span id="timetosubmit3" style={{ marginLeft: '5px' }}>
        {`след ${secondsLeft} секунд${secondsLeft !== 1 ? 'и':'а'}`}
      </span>}
     </div>
    )
}

function TaskNoSubmissions() {
  return (
    <div className="box">
      <div className="box-header with-border">
        <h3 className="box-title">Няма предадени решения</h3>
      </div>
    </div>
  )
}

function TaskSubmissions({ tid, submissions }) {
  if (!submissions || submissions.length === 0) return <TaskNoSubmissions/>
  
  return (
    <div className="box">
      <div className="box-header with-border">
        <h3 className="box-title">Предадени решения</h3>
      </div>
      <div className="box-body box-responsive">
        <table className="table table-bordered table-hover" style={{ tableLayout: 'fixed', wordWrap: 'break-word' }}>
          <thead>
            <tr>
              <th style={{ width: '5%' }}>#</th>
              <th style={{ width: '15%' }}>Час</th>
              <th style={{ width: '70%' }}>Детайли</th>
              <th style={{ width: '10%' }}>Точки</th>
            </tr>
          </thead>
          <tbody>
            {
              submissions.map((s, i) => {
                return <tr key={i}>
                  <td><Link to={`/task/${tid}/submission/${submissions.length - i}`}>{submissions.length - i}</Link></td>
                  <td>{moment.unix(s.upload_time / 1000).format("DD MMM YYYY hh:mm:ss")}</td>
                  <td><Verdict verdict={s.verdict} /></td>
                  <ScoreBoardBox points={s.points} maxPoints={100} hovered={false} />
                </tr>
              })}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default { TaskDescription, TaskLimits, TaskSubmit, TaskSubmissions }
