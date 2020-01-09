import React, { useState } from 'react';
import { sendRequestWithToken } from './rest'
import { useAppError } from './AppErrorContext';

const AuthContext = React.createContext()

const AuthProvider = ({children}) => {

    const [isAuth, setIsAuth] = useState(localStorage.hasOwnProperty('token'));
    const setError = useAppError().setError;

    function validate(text) {
        return [...text].every(c => ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || ('0' <= c && c <= '9'));
    }

    async function login(username, password) {
        if (username === "") {
            setError({title: "Невалидни данни", message: "Не е въведено потребителско име!"});
            return;
        }
        if (password === "") {
            setError({title: "Невалидни данни", message: "Не е въведена парола!"});
            return;
        }
        if (!validate(username)) {
            setError({title: "Невалидни данни", message: "В потребителското име има символи различни от латински букви и цифри!"});
            return;
        }
        if (!validate(password)) {
            setError({title: "Невалидни данни", message: "В паролата има символи различни от латински букви и цифри!"});
            return;
        }

        const token = window.btoa(username + ':' + password);
        const response = await sendRequestWithToken('user', 'json', token);

        if (!response.ok) {
            setError({title: "Отказан достъп", message: "Грешно потребителско име или парола!"});
            return;
        }
            
        const user = await response.json();
        localStorage.setItem("name", user.display_name);
        localStorage.setItem("contest", user.contest);
        localStorage.setItem("token", token);
        setIsAuth(true);
    }

    function logout() {
        setIsAuth(false);
        localStorage.removeItem("name");
        localStorage.removeItem("contest");
        localStorage.removeItem("token");
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
