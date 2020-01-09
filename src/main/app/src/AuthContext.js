import React, { useState } from 'react';
import { sendRequestWithToken } from './rest'

const AuthContext = React.createContext()

const AuthProvider = ({children}) => {

    const [isAuth, setIsAuth] = useState(localStorage.hasOwnProperty('token'));

    async function login(username, password) {
        const token = window.btoa(username + ':' + password);
        const response = await sendRequestWithToken('user', 'json', token);
        console.log(JSON.stringify(response));
        if (!response.ok) return;

        const user = await response.json();

        localStorage.setItem("name", user.display_name);
        localStorage.setItem("contest", user.contest);
        localStorage.setItem("token", token);
        setIsAuth(true);
    }

    function logout() {
        localStorage.removeItem('display_name');
        localStorage.removeItem('contest');
        localStorage.removeItem('token');
        setIsAuth(false);
    }

    return (
        <AuthContext.Provider
            value={{
                isAuth: isAuth,
                login: login,
                logout: logout
            }}>
            {children}
        </AuthContext.Provider>
    )

}

const useAuth = () => React.useContext(AuthContext)

export { AuthProvider, useAuth }
