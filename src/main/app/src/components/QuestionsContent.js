import React from 'react';
import { ShowQuestion, SendQuestion } from './Questions';
import { useApp } from '../AppContext';

const QuestionsContent = () => {
  const questions = useApp().questions;

  return (
    <div className="content-wrapper" style={{ minHeight: '550px' }}>
      <section className="content">
        <div className="row">
          <div className="col-md-8">
            {
              questions.slice().reverse().map((q) => {
                return <ShowQuestion key={q.id} question={q} />
            })}
          </div>
          <div className="col-md-4">
            <SendQuestion />
          </div>
        </div>
      </section>
    </div>
  )
}

export default QuestionsContent
