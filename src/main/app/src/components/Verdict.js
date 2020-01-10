import React from 'react'

function Verdict({verdict}) {
  return (
    <div>
      {
        verdict.slice().split(',').map((v, i) => {
          const colorClass = v === 'OK' ? 'label-success' :
                        v === '?' ? 'label-default' :
                        v === 'waiting' || v === 'judging' ? 'label-warning' :
                        'label-danger';
          const color = v === 'OK' ? '#4e9a05' :
                        v === '?' ? '#d2d6de' :
                        v === 'waiting' || v === 'judging' ? '#f39c12' :
                        '#f40000';
          const width = v === 'waiting' || v === 'judging' ? '50px' : '26px';
          return <span key={i} className={`label`} 
              style={{display: 'inline-block', 
              width: width,
              backgroundColor: color,
              marginRight: '1px'}}>
            {v}
          </span>
        })

      }
    </div>
  )
}

export default Verdict
