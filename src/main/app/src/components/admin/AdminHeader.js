import React from 'react'
import { Link } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faBars, faSignOutAlt, faEnvelope, faBell } from '@fortawesome/free-solid-svg-icons'
import { useAuth } from '../../AuthContext'
import { useApp } from '../../AppContext';
import { useTitle } from '../../TitleContext';

function AdminHeader() {
  const title = useTitle().shortTitle;
  const unreadQuestions = useApp().unreadQuestions;
  const unreadAnnouncements = useApp().unreadAnnouncements;

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
            <li>
              <Link to="/"><FontAwesomeIcon icon={faBell} />
                {unreadAnnouncements > 0 && <span className="label label-danger">
                  {unreadAnnouncements}
                </span>}
              </Link>
            </li>
            <li>
              <Link to="/questions"><FontAwesomeIcon icon={faEnvelope} />
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

export default AdminHeader
