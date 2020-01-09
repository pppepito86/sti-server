import React, { useState } from 'react'
import { withRouter } from "react-router";
import ScoreBoardBox from './ScoreBoardBox';

function ScoreboardTable() {
  const [hover, setHover] = useState(0);
  const data = [
    { rank: 1, name: 'Кръстан Асенов Драганов', task1: 80, task2: 75, task3: 100, total: 255 },
    { rank: 2, name: 'Кристина Валентинова Стоянова', task1: 100, task2: 20, task3: 100, total: 220 },
    { rank: 3, name: 'Теодор Цветалинов Тотев', task1: 100, task2: 10, task3: 100, total: 210 },
    { rank: 3, name: 'Борис Владимиров Михов', task1: 100, task2: 10, task3: 100, total: 210 },
    { rank: 5, name: 'Георги Георгиев Славчев', task1: 100, task2: 10, task3: 40, total: 150 },
    { rank: 6, name: 'Емилиана Иванова Димитрова', task1: 72, task2: 25, task3: 50, total: 147 },
    { rank: 7, name: 'Александър Мирославов Гатев', task1: 88, task2: 10, task3: 40, total: 138 },
    { rank: 8, name: 'Петър Велиславов Михов', task1: 92, task2: 0, task3: 40, total: 132 },
    { rank: 9, name: 'Георги Николаев Илиев', task1: 80, task2: 45, task3: 0, total: 125 },
    { rank: 10, name: 'Калоян Георгиев Еленков', task1: 92, task2: 10, task3: 20, total: 122 },
    { rank: 11, name: 'Калоян Калинов Димитров', task1: 64, task2: 0, task3: 40, total: 104 },
    { rank: 12, name: 'Мария Николаева Дренчева', task1: 64, task2: 25, task3: 0, total: 89 },
    { rank: 13, name: 'Мартин Ивов Минков', task1: 76, task2: 0, task3: 10, total: 86 },
    { rank: 14, name: 'Огнян Мирославов Йоргов', task1: 72, task2: 0, task3: 10, total: 82 },
    { rank: 15, name: 'Богдан Людмилов Люцканов', task1: 72, task2: 0, task3: 0, total: 72 },
    { rank: 16, name: 'Калоян Августинов Маринов', task1: 68, task2: 0, task3: 0, total: 68 },
    { rank: 17, name: 'Иван Станимиров Атанасов', task1: 56, task2: 10, task3: 0, total: 66 },
    { rank: 18, name: 'Мила Борисова Дачева', task1: 64, task2: 0, task3: 0, total: 64 },
    { rank: 18, name: 'Лазар Иванов Тодоров', task1: 64, task2: 0, task3: 0, total: 64 },
    { rank: 18, name: 'Александър Иванов Коджабашийски', task1: 64, task2: 0, task3: 0, total: 64 }
  ];
  return (
    <div className="box">
      <div className="box-header">
        <h3 className="box-title">Класиране</h3>

        <div className="box-tools">
          <div className="input-group input-group-sm hidden-xs" style={{ width: '150px' }}>
            <input type="text" name="table_search" className="form-control pull-right" placeholder="Search" />

            <div className="input-group-btn">
              <button type="submit" className="btn btn-default"><i className="fa fa-search"></i></button>
            </div>
          </div>
        </div>
      </div>
      <div className="box-body table-responsive no-padding">
        <table className="table table-hover">
          <thead>
            <tr>
              <th width='10px'></th>
              <th width='10px'>Място</th>
              <th>Участник</th>
              <th width='50px'>З.1</th>
              <th width='50px'>З.2</th>
              <th width='50px'>З.3</th>
              <th width='75px'>Общо</th>
            </tr>
          </thead>
          <tbody>
            {data.map((row, i) => {
              return (
                <tr key={i} onMouseEnter={() => setHover(i)} onMouseLeave={() => setHover(-1)}>
                  <td style={{ padding: '0' }}></td>
                  <td style={{ padding: '0' }}><span style={{ padding: '0', marginLeft: '16px', fontWeight: 'bold' }}>{row.rank}</span></td>
                  <td style={{ padding: '0' }}><span style={{ padding: '0', fontWeight: 'bold' }}>{row.name}</span></td>
                  <ScoreBoardBox points={row.task1} maxPoints={100} hovered={hover === i} />
                  <ScoreBoardBox points={row.task2} maxPoints={100} hovered={hover === i} />
                  <ScoreBoardBox points={row.task3} maxPoints={100} hovered={hover === i} />
                  <ScoreBoardBox points={row.total} maxPoints={300} hovered={hover === i} />
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default withRouter(ScoreboardTable);
