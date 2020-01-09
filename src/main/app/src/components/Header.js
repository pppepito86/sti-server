import React from 'react'
import { useLocation } from "react-router";
import { Link } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faBars, faSignOutAlt, faEnvelope, faBell } from '@fortawesome/free-solid-svg-icons'
import { useAuth } from '../AuthContext'
import { useApp } from '../AppContext';
import { useTitle } from '../TitleContext';

function Header() {
  const title = useTitle().shortTitle;
  const location = useLocation();
  const unreadQuestions = useApp().unreadQuestions;
  const unreadAnnouncements = useApp().unreadAnnouncements;
  const markQuestionsSeen = useApp().markQuestionsSeen;
  const markAnnouncementsSeen = useApp().markAnnouncementsSeen;

  return (
    <header className="main-header">
      <Link to="/" className="logo">
        <span className="logo-mini"><b>{title}</b></span>
        <span className="logo-lg">{title}</span>
      </Link>
      <nav className="navbar navbar-static-top">
        <div className="logo" style={{width: 'auto', backgroundColor: 'rgba(255, 255, 255, 0)'}}>
          {localStorage.getItem("name")}, {localStorage.getItem("contest")} група
        </div>

        <div className="navbar-custom-menu">
          <ul className="nav navbar-nav">
            <li className={location.pathname === '/' ? 'active' : ''}>
              <Link to="/" onClick={markAnnouncementsSeen}><FontAwesomeIcon icon={faBell} />
                {unreadAnnouncements > 0 && <span className="label label-danger">
                  {unreadAnnouncements}
                </span>}
              </Link>
            </li>
            <li className={location.pathname === '/questions' ? 'active' : ''}>
              <Link to="/questions" onClick={markQuestionsSeen}><FontAwesomeIcon icon={faEnvelope} />
                {unreadQuestions > 0 && <span className="label label-danger">
                  {unreadQuestions}
                </span>}
              </Link>
            </li>
            <li>
              <Link to="/" onClick={useAuth().logout}><FontAwesomeIcon icon={faSignOutAlt} /></Link>
            </li>
          </ul>
        </div>
      </nav>
    </header>
  )
}

export default Header
