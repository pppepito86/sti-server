import React from 'react'

function Verdict({verdict}) {
  return (
    <div>
      {
        verdict.slice().split(',').map((v, i) => {
          const color = v === 'OK' ? 'label-success' :
                        v === '?' ? 'label-default' :
                        v === 'waiting' || v === 'judging' ? 'label-warning' :
                        'label-danger';
          const width = v === 'waiting' || v === 'judging' ? '50px' : '26px';
          return <span key={i} className={`label ${color}`} 
              style={{display: 'inline-block', 
              width: width, 
              marginRight: '1px'}}>
            {v}
          </span>
        })

      }
    </div>
  )
}

export default Verdict
