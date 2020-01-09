import React from 'react'
import { BrowserRouter as Router, Route, Switch, Redirect } from 'react-router-dom'
import PageTemplate from './pages/PageTemplate'
import AdminPageTemplate from './pages/AdminPageTemplate';

export default () => (
    <Router>
        <Switch>
            <Route path="/task/:tid(\d+)/submission/:sid(\d+)" render={() => <PageTemplate content="submission" />} />
            <Route path="/task/:tid(\d+)/pdf" render={() => <PageTemplate content="pdf" />} />
            <Route path="/task/:tid(\d+)" render={() => <PageTemplate content="task" />} />            
            <Route path="/questions" render={() => <PageTemplate content="questions" />} />
            <Route path="/scoreboard/:gid" render={() => <PageTemplate content="scoreboard" />} />
            <Route path="/admin/dashboard" render={() => <AdminPageTemplate content="admin" />} />
            <Route path="/" render={() => <PageTemplate content="dashboard" />} />

            <Route render={() => <Redirect to="/task/1" />} />
        </Switch>
    </Router>

);

export { };