import React from 'react'
import { useAppError } from '../AppErrorContext';

export default function Modal() {
  const error = useAppError().error;
  const setError = useAppError().setError;

  return (
      <div>
    {error &&
    <div className='modal modal-info fade in modal-open' id="modal-default" style={{display: 'block'}}>
    <div className="modal-dialog">
      <div className="modal-content">
        <div className="modal-header">
          <button type="button" onClick={() => setError(null)} className="close" aria-label="Close">
            <span aria-hidden="true">&times;</span></button>
          <h4 className="modal-title">{error.title}</h4>
        </div>
        <div className="modal-body">
          <p>{error.message}</p>
        </div>
        <div className="modal-footer">
          <button type="button" onClick={() => setError(null)} className="btn btn-default pull-left" data-dismiss="modal">Затвори</button>
          {false && <button type="button" className="btn btn-primary">Save changes</button>}
        </div>
      </div>
    </div>
  </div>
}
  </div>
  )
}