import React, { useState } from 'react';

const AppErrorContext = React.createContext()

const AppErrorProvider = ({children}) => {
    const [error, setError] = useState();

    return (
        <AppErrorContext.Provider
            value={{
                error: error,
                setError: setError
            }}>
            {children}
        </AppErrorContext.Provider>
    )
}

const useAppError = () => React.useContext(AppErrorContext)

export { AppErrorProvider, useAppError }
