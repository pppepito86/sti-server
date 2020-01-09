import React from 'react'
import { useParams, useLocation } from "react-router";
import { Link } from 'react-router-dom';
import { json } from '../../rest'
import useAsync from '../../useAsync'

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faFile, faBook, faHome, faQuestion } from '@fortawesome/free-solid-svg-icons'
import ContestCountdown from '../ContestCountdown';
import { useApp } from '../../AppContext';

const AdminSidebar = () => {
  const { tid } = useParams();
  const location = useLocation();
  const { value: tasks, loading } = useAsync(json, 'tasks', []);
  const contestIsRunning = useApp().contestIsRunning;
  const contestIsFinished = useApp().contestIsFinished;
  const unreadQuestions = useApp().unreadQuestions;
  const unreadAnnouncements = useApp().unreadAnnouncements;

  return (
    <aside className="main-sidebar">
      <section className="sidebar">
        
        <ContestCountdown />
        <ul className="sidebar-menu tree" data-widget="tree">      
          <li className="header">МЕНЮ</li>
          <li className={location.pathname === '/admin' ? 'active' : ''}>
            <Link to="/admin">
              <FontAwesomeIcon icon={faHome} /> &nbsp;<span>Начало</span>
            </Link>
          </li>
          <li className={location.pathname === '/admin/contests' ? 'active' : ''}>
            <Link to="/admin/contests">
              <FontAwesomeIcon icon={faHome} /> &nbsp;<span>Състезания</span>
            </Link>
          </li>
          <li className={location.pathname === '/admin/submissions' ? 'active' : ''}>
            <Link to="/admin/submissions">
              <FontAwesomeIcon icon={faHome} /> &nbsp;<span>Решения</span>
            </Link>
          </li>
          <li className={location.pathname === '/admin/results' ? 'active' : ''}>
            <Link to="/admin/results">
              <FontAwesomeIcon icon={faHome} /> &nbsp;<span>Резултати</span>
            </Link>
          </li>
          <li className={location.pathname === '/admin/users' ? 'active' : ''}>
            <Link to="/admin/users">
              <FontAwesomeIcon icon={faHome} /> &nbsp;<span>Състезатели</span>
            </Link>
          </li>
          <li className={location.pathname === '/admin/questions' ? 'active' : ''}>
            <Link to="/admin/questions">
              <FontAwesomeIcon icon={faQuestion} /> &nbsp;<span>Въпроси</span>
              {unreadQuestions > 0 && 
                <span className="pull-right-container">
                  <small className="label pull-right bg-red">new</small>
                </span>
              }
            </Link>
          </li>
         </ul>
      </section>
    </aside>
  )
}

export default AdminSidebar
