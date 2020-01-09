import React from 'react'
import ScoreboardTable from './Scoreboard'

function ScoreboardContent() {
  return (
    <div className="content-wrapper" style={{ minHeight: '498px' }}>
      <section className="content-header">
        <h1>
          Е <b>група</b>
        </h1>
      </section>

      <section className="content">
        <div className="row">
          <div className="col-md-12">
            <ScoreboardTable />
          </div>
        </div>
      </section>

    </div>
  )
}

export default ScoreboardContent
