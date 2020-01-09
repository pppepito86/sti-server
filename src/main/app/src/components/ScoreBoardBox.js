import React from 'react'

function getColor(ratio) {
  return ratio === 1 ? '#4E9A05' :
    ratio > 0.9 ? '#639B04' :
      ratio > 0.8 ? '#789C03' :
        ratio > 0.7 ? '#8E9D02' :
          ratio > 0.6 ? '#A39E01' :
            ratio > 0.5 ? '#B99F00' :
              ratio > 0.4 ? '#C89100' :
                ratio > 0.3 ? '#D17400' :
                  ratio > 0.2 ? '#D95700' :
                    ratio > 0.1 ? '#E23A00' :
                      ratio > 0 ? '#EB1D00' :
                        '#F40000';
}
  
function getImage(ratio) {
  return ratio === 1 ? 'linear-gradient(rgb(115, 210, 22), rgb(78, 154, 5))' :
    ratio > 0.9 ? 'linear-gradient(rgb(137, 210, 18), rgb(99, 155, 4))' :
      ratio > 0.8 ? 'linear-gradient(rgb(159, 210, 14), rgb(120, 156, 3))' :
        ratio > 0.7 ? 'linear-gradient(rgb(181, 211, 10), rgb(142, 157, 2))' :
          ratio > 0.6 ? 'linear-gradient(rgb(203, 211, 6), rgb(163, 158, 1))' :
            ratio > 0.5 ? 'linear-gradient(rgb(225, 211, 2), rgb(185, 159, 0))' :
              ratio > 0.4 ? 'linear-gradient(rgb(234, 192, 0), rgb(200, 145, 0))' :
                ratio > 0.3 ? 'linear-gradient(rgb(228, 154, 0), rgb(209, 116, 0))' :
                  ratio > 0.2 ? 'linear-gradient(rgb(222, 115, 0), rgb(217, 87, 0))' :
                    ratio > 0.1 ? 'linear-gradient(rgb(216, 77, 0), rgb(226, 58, 0))' :
                      ratio > 0 ? 'linear-gradient(rgb(210, 38, 0), rgb(235, 29, 0))' :
                        'linear-gradient(rgb(204, 0, 0), rgb(244, 0, 0))';
}
  
function ScoreBoardBox({ points, maxPoints, hovered }) {
  if (!points) return <td />;

  const ratio = points ? points * 1.0 / maxPoints : -1;
  const color = getColor(ratio);
  const image = hovered ? getImage(ratio) : undefined;
  return (
    <td style={{ padding: '0', whiteSpace: 'nowrap', verticalAlign: 'middle' }}>
      <span className="btn btn-success"
        style={{
          fontWeight: 'bold', width: '98%', padding: '0',
          backgroundColor: color,
          borderColor: color,
          color: 'black',
          backgroundImage: image,
          cursor: 'auto'
    }}>{points?points:"N/A"}</span></td>
  )
}

export default ScoreBoardBox;