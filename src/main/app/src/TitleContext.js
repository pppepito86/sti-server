import React, { useState, useEffect } from 'react';

const TitleContext = React.createContext()

const TitleProvider = ({children}) => {

    const [shortTitle] = useState("НОИ 1");
    const [fullTitle] = useState("Национална олимпиада по информатика, общински кръг");

    useEffect(() => {
        document.title = shortTitle;
     }, [shortTitle]);

    return (
        <TitleContext.Provider
            value={{
                shortTitle: shortTitle,
                fullTitle: fullTitle
            }}>
            {children}
        </TitleContext.Provider>
    )

}

const useTitle = () => React.useContext(TitleContext)

export { TitleProvider, useTitle }
