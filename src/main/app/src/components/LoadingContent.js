import React from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSpinner } from '@fortawesome/free-solid-svg-icons';

const LoadingContent = () => {
  return (
    <div className="content-wrapper" style={{ minHeight: '550px' }}>
      <section className="content">
        <div className="overlay fa-3x">
          <FontAwesomeIcon icon={faSpinner} className="fa-spin" />
        </div>
      </section>
    </div>
  )
}

export default LoadingContent
