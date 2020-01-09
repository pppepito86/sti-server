import React from 'react'
import Header from '../components/Header';
import Footer from '../components/Footer';
import ScoreboardSidebar from '../components/Sidebar';
import ScoreboardContent from '../components/ScoreboardContent';

const ScoreboardPage = () => {
    return (
        <div className="wrapper" style={{ height: 'auto', minHeight: '100%' }}>
            <Header />
            <ScoreboardSidebar />
            <ScoreboardContent />
            <Footer />
        </div>
    )
}

export default ScoreboardPage
