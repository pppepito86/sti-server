import React from 'react';
import { useAuth } from './AuthContext';
import ProtectedRoute from './ProtectedRoute';
import Login from './pages/Login';

function App() {
  const isAuth = useAuth().isAuth;
  return isAuth ? <ProtectedRoute /> : <Login />
}

export default App;
