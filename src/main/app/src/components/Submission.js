import React from 'react'
import { Link } from 'react-router-dom'
import moment from 'moment'
import Verdict from './Verdict';

function SubmissionOverview({ tid, submission }) {
  return (
    <div className="box-body">
      <table style={{ tableLayout: 'fixed', wordWrap: 'break-word' }} className="table table-bordered">
        <tbody>
          <tr>
            <th style={{ width: '15%' }}>Час</th>
            <th style={{ width: '6%' }}>Група</th>
            <th style={{ width: '10%' }}>Задача</th>
            <th style={{ width: '6%' }}>Точки</th>
            <th style={{ width: '63%' }}>Статус</th>
          </tr>
          <tr>
            <td>{moment.unix(submission.upload_time / 1000).format("DD MMM YYYY hh:mm:ss")}</td>
            <td>{submission.contest}</td>
            <td><Link to={`/task/${tid}`} >{submission.name}</Link></td>
            <td>{submission.points}</td>
            <td>
              <Verdict verdict={submission.verdict} />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  )
}

function SubmissionDetails({ tests }) {
  return (
    <div className="box-body">
      <table style={{ tableLayout: 'fixed', wordWrap: 'break-word' }} className="table table-bordered table-striped">
        <tbody>
          <tr>
            <th>Стъпка</th>
            <th>Резултат</th>
            <th>Време</th>
          </tr>
          {
            tests.map((test, i) => {
              const color = test.verdict === 'OK' ? '#00FF00' : test.verdict === 'HIDDEN' ? '#d2d6de' : '#ff0000';
              return <tr key={i}>
                <td>{test.name.replace('Test', 'Тест ')}</td>
                <td style={{ backgroundColor: color }}>{test.verdict}</td>
                <td>{test.time}</td>
              </tr>
            })}
        </tbody>
      </table>
    </div>
  )
}

function SubmissionSource({ source }) {
  return (
    <pre>{source}</pre>
  )
}

export default { SubmissionOverview, SubmissionDetails, SubmissionSource }
